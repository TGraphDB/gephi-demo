/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.buaa.act.gephi.plugin.task;

import edu.buaa.act.gephi.plugin.utils.GUIHook;
import org.act.neo4j.temporal.demo.algo.TGraphTraversal;
import org.act.neo4j.temporal.demo.algo.TGraphTraversal.DFSAction;
import org.act.neo4j.temporal.demo.utils.Helper;
import org.act.neo4j.temporal.demo.utils.TransactionWrapper;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphModel;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Algorithm to find shortest path in a graph based on the 'length' property.
 * An implementation of the Dijkstra algorithm.
 * Note: This algorithm runs in one transaction and commit NOTHING to database.
 *
 * @author song
 */
public class DijkstraOneTransactionAsyncTask extends Traverse{
    // properties used in this algorithm.
    private final String G_VALUE = "algorithm-dijkstra-G";
    private final String PARENT = "algorithm-dijkstra-parent";
    private final String STATUS = "algorithm-dijkstra-status";
    private final String TIME_COST = "algorithm-dijkstra-time";


    private GraphDatabaseService db;
    private GraphModel model;
    private ProgressTicket progress;
    private long start;
    private long end;
    private int startTime;
    private Color pathColor;
    private int totalNodeCount;
    private boolean shouldGo=true;
    private Map<Long,org.gephi.graph.api.Node> tgraphNode2GephiNode = new HashMap<Long,org.gephi.graph.api.Node>();
    private List<Long> path;
    private List<Integer> timeList;
    private int pathRealLength = 0;
    private long searchCount=0;


    public DijkstraOneTransactionAsyncTask(
            GraphDatabaseService db,
            GraphModel model,
            long startId, long endId, int startTime,
            Color pathColor,
            GUICallBack callback){
        super(false); // do not commit this transaction.
        this.db = db;
        this.model = model;
        this.start = startId;
        this.end = endId;
        this.startTime = startTime;
        this.pathColor = pathColor;
        this.callback = callback;
        System.out.println("start time: " + timestamp2String(startTime));
    }

    @Override
    public void run() {
        Thread.currentThread().setName("TGraph.Dijkstra.PathFinding");
        Progress.start(progress, Integer.MAX_VALUE);
        this.start(db);
        Progress.finish(progress);
        new GUIHook<Traverse.GUICallBack>() {
            @Override
            public void guiHandler(Traverse.GUICallBack value) {
                value.onResult(searchCount, path, timeList, pathRealLength);
            }
        }.guiHandler(this.callback);

    }

    @Override
    public void runInTransaction() {
        try {
            System.out.println("enter tx");
            Progress.setDisplayName(progress, "initial algorithm...");
            Progress.switchToIndeterminate(progress);
            initAlgo(start, end);
            System.out.println("init done");
            Progress.switchToDeterminate(progress, totalNodeCount);
            Progress.setDisplayName(progress, "searching...");

            long node;
            while ((node = findSmallestClosedNode()) != end && shouldGo) {
//                System.out.println("search " + node);
                loopAllNeighborsUpdateGValue(node);
                searchCount++;
                Progress.progress(progress, searchCount + " nodes searched");
            }
            System.out.println("search end");
            Progress.setDisplayName(progress, "generating results...");
            path = getPath();
            System.out.println("get path done");
            timeList = getArriveTime(path);
            System.out.println("get time done");
            System.out.println("Path found~ length:" + path.size() + " time:" + timePeriod2Str(timeList.get(timeList.size() - 1) - timeList.get(0)));

        }catch(RuntimeException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * use this queue as a 'set' which contains all nodes labeled 'CLOSED'.
     * use priority queue(min heap) data structure, this guarantee:
     * 1. O(1) for "peek(get node with min g value among queue)" operation
     * 2. O(log(n)) for "add" and "remove" operation(since it will adjust the heap)
     * not the 'add' and 'remove' operation should perform in a transaction.
     */
    PriorityQueue<Node> minHeap = new PriorityQueue<Node>(100,new Comparator<Node>() {
        @Override
        public int compare(Node o1, Node o2) {
            int g1 = (Integer) o1.getProperty(G_VALUE);
            int g2 = (Integer) o2.getProperty(G_VALUE);
            if(g1< g2) return -1;
            if(g1==g2) return 0;
            else return 1;
        }
    });

    @Override
    public boolean cancel() {
        shouldGo=false;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;
    }

    private List<Long> getPath() {
        final List<Long> rPath = new ArrayList<Long>();
        Long parent=end;
        long child = end;
        rPath.add(end);
        while((parent = getParent(child))!=start){
            if(!shouldGo) break;
            Progress.progress(progress,"path length:"+rPath.size());
            rPath.add(parent);
            org.gephi.graph.api.Node n1 = tgraphNode2GephiNode.get(child);
            org.gephi.graph.api.Node n2 = tgraphNode2GephiNode.get(parent);
            n1.setColor(pathColor);
            n2.setColor(pathColor);
            Edge edge = model.getGraph().getEdge(n2, n1);
            if(edge==null){
                edge = model.getGraph().getEdge(n1, n2);
                if(edge!=null){
                    edge.setColor(pathColor);
                    edge.setWeight(2f);
                    pathRealLength += (Integer) edge.getAttribute("road_length");
                    System.out.println("edge has direct.");
                }else{
                    System.out.println("no edge between nodes! "+parent+"--"+child);
                }
            }else{
                edge.setColor(pathColor);
                edge.setWeight(2f);
                pathRealLength += (Integer) edge.getAttribute("road_length");
            }
            child = parent;
        }
        rPath.add(start);
        tgraphNode2GephiNode.get(end).setSize(3f);
        tgraphNode2GephiNode.get(end).setColor(Color.GREEN);
        // connect last path: start->first child.
        org.gephi.graph.api.Node begin = tgraphNode2GephiNode.get(start);
        org.gephi.graph.api.Node end = tgraphNode2GephiNode.get(child);
        Edge edge = model.getGraph().getEdge(begin, end);
        edge.setColor(pathColor);
        edge.setWeight(2f);
        pathRealLength += (Integer) edge.getAttribute("road_length");
        begin.setSize(3f);
        begin.setColor(Color.RED);
        //reverse array
        final List<Long> path = new ArrayList<Long>();
        for(int i=rPath.size()-1;i>=0;i--){
            path.add(rPath.get(i));
        }
        return path;
    }

    private List<Integer> getArriveTime(final List<Long> path){
        final List<Integer> timeList = new ArrayList<Integer>();
        int currentTime = this.startTime;
        for (int i=0;i<path.size();i++){
            long nodeId = path.get(i);
            if(!shouldGo) break;

            org.gephi.graph.api.Node node = tgraphNode2GephiNode.get(nodeId);
            String label;

            if(i==0)  //start node
            {
                timeList.add(currentTime);
                label = "Start At " + Helper.timeStamp2String(currentTime);
            }
            else
            {
                Node start = this.db.getNodeById(path.get(i-1));
                Node end = this.db.getNodeById(nodeId);
                int period = getTravelTime(start, end, currentTime);
                currentTime += period;
                timeList.add(currentTime);

                if(i==(path.size()-1)){ //end node
                    label = "Arrive At " + Helper.timeStamp2String(currentTime) + ", "+timePeriod2Str(currentTime-timeList.get(0));
                }else{ // node in between
                    label = Helper.timeStamp2String(currentTime).substring(11);
                }

            }
            node.setLabel(label);
            System.out.println(label);
            Progress.progress(progress,nodeId+" "+label);
        }
        return timeList;
    }

    private int getTravelTime(Node start, Node end, int currentTime)
    {
        for(Relationship r : start.getRelationships(Direction.OUTGOING))
        {
            if(r.getEndNode().getId()==end.getId()) {
                if (r.hasProperty("travel-time")) {
                    Object tObj = r.getDynPropertyPointValue("travel-time", currentTime);
                    if (tObj != null) {
                        return (int) (Integer) tObj;
                    }
                }
                return 5;
            }
        }
        throw new RuntimeException("Should not happen: end node of road not found! ("+start.getId()+" -> "+end.getId()+")");
    }


    private int getGvalue(long nodeId) {
        return getGvalue(db.getNodeById(nodeId));
    }
    private int getGvalue(Node node) {
        return (Integer) node.getProperty(G_VALUE);
    }

    /**
     * get parent node, must used in transaction.
     * @param me current node id
     * @return parent node id
     */
    private Long getParent(long me) {
        Object parent = db.getNodeById(me).getProperty(PARENT);
//        System.out.println(me+" "+parent);
        if(parent!=null){
            return db.getNodeById((Long)parent).getId();
        }else{
            return null;
        }
    }

    /**
     * loop through all neighbors of a given node,
     * and for each neighbor node:
     * 1. update its G value( earliest arrive time )
     * 2. set parent to source node
     * 3. mark node status to CLOSE
     * after the loop above, mark given node status to FINISH
     * @param nodeId given node's id
     */
    private void loopAllNeighborsUpdateGValue(final long nodeId) {
        Node node = db.getNodeById(nodeId);
        int totalLength = getGvalue(node);
        for(Relationship r : node.getRelationships(Direction.OUTGOING)){
            if(!shouldGo) return;
            Node neighbor = r.getOtherNode(node);
            int length;
            switch (getStatus(neighbor)){
                case OPEN:
                    length = getRoadLength(r);
                    setG(neighbor, length + totalLength);
                    setStatus(neighbor, Status.CLOSE);
                    setParent(neighbor, node);
                    break;
                case CLOSE:
                    length = getRoadLength(r);
                    int gNeighbor = (Integer) neighbor.getProperty(G_VALUE);
                    if(gNeighbor>totalLength+length){
                        setG(neighbor,totalLength+length);
                        setParent(neighbor,node);
                    }
                    break;
            }
        }
        setStatus(node, Status.FINISH);
    }

    /**
     * @param r road.
     * @return road length
     */
    private int getRoadLength(Relationship r) {
        if(r.hasProperty("length")) {
            Object tObj = r.getProperty("length");
            if (tObj != null) {
                return (Integer) tObj;
            }else{ // no data, filled with 5.
                throw new RuntimeException("no length data of road "+r.getId());
            }
        }else{
            throw new RuntimeException("road "+r.getId()+"do no contain a property called length.");
        }
    }

    private int getEarliestTravelTime(Relationship r, int departureTime) {
        int minArriveTime = Integer.MAX_VALUE;
        for(int curT = departureTime; curT<minArriveTime; curT++){
            if(r.hasProperty("travel-time")) {
                Object tObj = r.getDynPropertyPointValue("travel-time", curT);
                if (tObj != null) {
                    int period = (Integer) tObj;
                    if (curT + period < minArriveTime) {
                        minArriveTime = curT + period;
                    }
                }else{ // not all time has data. return 5. maybe we should continue loop, because later there are data.
                    return 5;
                }
            }else{ // no data, filled with 5.
                return 5;
            }
        }
        return minArriveTime - departureTime;
    }

    /**
     * this is an O(1) implementation, because:
     * 1. node status only transfer from OPEN to CLOSE, never back.
     * 2. we use an [minimum heap(PriorityQueue in Java)] data structure.
     * BE CAREFUL! this implementation is only valid based on the assumption [1]!
     * therefore it may only work for Dijkstra Shortest Path algorithm.
     * RE VALID this when using algorithm other than Dijkstra.
     * @return node in set( status == CLOSE ) and has smallest G value among the set.
     */
    private long findSmallestClosedNode() {
        return minHeap.peek().getId();
    }

    /**
     * we do the following:
     * 1. set all node status to OPEN, except source node (CLOSE)
     * 2. set G value of source node to t0
     * 3. set min G value to be t0, min point to source node
     * @param from source node
     * @param to target/destination node
     */
    private void initAlgo(final long from, long to) {
        for(org.gephi.graph.api.Node node:model.getGraph().getNodes()){
            if(!shouldGo) return;
            long tgraphNodeId = (Long) node.getAttribute("tgraph_id");
            tgraphNode2GephiNode.put(tgraphNodeId, node);
//            node.setColor(new Color(0xc0c0c0));
//            node.setLabel("");
            Progress.progress(progress);
        }
        Node startNode = db.getNodeById(from);
        TGraphTraversal traversal = new TGraphTraversal(db);
        traversal.DFS(startNode, new HashSet<Long>(), new DFSAction<Node>(){
            public boolean visit(Node node) {
                Progress.progress(progress);
                setStatus(node, Status.OPEN);
                return shouldGo;
            }
        }, false);
        totalNodeCount = (int) traversal.getVisitedNodeCount();
        setStatus(startNode, Status.CLOSE);
        setG(startNode, 0);
    }

    private void setG(Node node, int value) {
        node.setProperty(G_VALUE, value);
    }

    private void setParent(Node node, Node parent) {
//        pathColor.
        node.setProperty(PARENT,parent.getId());
    }

    /**
     * when status transfer from OPEN to CLOSE,
     * will add the node to min G Value heap.
     */
    private void setStatus(Node node, Status status){
        node.setProperty(STATUS,status.value());
        if (status == Status.CLOSE) {
//            tgraphNode2GephiNode.get(node.getId()).setColor(Color.MAGENTA);
            minHeap.add(node);
        }else if( status == Status.FINISH){
//            tgraphNode2GephiNode.get(node.getId()).setColor(Color.GREEN);
            minHeap.remove(node);
        }
    }

    private Status getStatus(final Node node){
        Object status = node.getProperty(STATUS);
        if (status != null) {
            return Status.valueOf((Integer) status);
        } else {
            throw new RuntimeException("node property algo-astar-status null");
        }
    }


    public enum Status{
        OPEN(0),CLOSE(1),FINISH(2);
        private int value;
        Status(int value){
            this.value=value;
        }
        public int value(){
            return this.value;
        }

        public static Status valueOf(int status) {
            switch (status){
                case 0: return OPEN;
                case 1: return CLOSE;
                case 2: return FINISH;
            }
            throw new RuntimeException("no such value in Status!");
        }
    }
}
