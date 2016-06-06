/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.buaa.act.gephi.plugin.task;

import edu.buaa.act.gephi.plugin.utils.GUIHook;
import org.act.neo4j.temporal.demo.algo.TGraphTraversal;
import org.act.neo4j.temporal.demo.algo.TGraphTraversal.DFSAction;
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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * @deprecated and the code is incorrect for it does not grantee the FIFO 
 * property of the temporal network. Use TimeDependentDijkstraOneTransactionAsyncTask instead.
 * @author song
 */
public class TimeDependentDijkstraAsyncTask implements LongTask, Runnable{
    private GraphDatabaseService db;
    private GraphModel model;
    private ProgressTicket progress;
    private long start;
    private long end;
    private int startTime;
    private int totalNodeCount;
    private boolean shouldGo=true;
    private Map<Long,org.gephi.graph.api.Node> tgraphNode2GephiNode = new HashMap<Long,org.gephi.graph.api.Node>();

    public TimeDependentDijkstraAsyncTask(
            GraphDatabaseService db,
            GraphModel model,
            long startId, long endId, int startTime){
        this.db = db;
        this.model = model;
        this.start = startId;
        this.end = endId;
        this.startTime = startTime;
        System.out.println("start time: "+timestamp2String(startTime));
    }

    @Override
    public void run() {
        Thread.currentThread().setName("TGraph.T.D.PathFinding.");
        Progress.start(progress,Integer.MAX_VALUE);
        Progress.setDisplayName(progress, "initial algorithm...");
        Progress.switchToIndeterminate(progress);
        System.out.println("init algo...");
        initAlgo(start, end, startTime);
        System.out.println("init done...");
        
//        Progress.switchToDeterminate(progress, totalNodeCount);
        Progress.setDisplayName(progress, "searching...");
        long searchCount=0;
        long node;
        while((node = findSmallestClosedNode())!=end && shouldGo){
            loopAllNeighborsUpdateGValue(node);
            searchCount++;
            Progress.progress(progress, searchCount+" nodes searched");
        }

        Progress.setDisplayName(progress, "generating results...");
        final List<Long> path = getPath();
        System.out.println("get path done.");
        final List<Integer> timeList = getArriveTime(path);
        System.out.println("get time done.");
        final long finalSearchCount = searchCount;
        Progress.finish(progress,"Path found~ length:"+path.size()+" time:"+(timeList.get(timeList.size()-1)-timeList.get(0))+" minutes");
        new GUIHook<Object>(){
            public void guiHandler(Object value) {
                onResult(finalSearchCount, path, timeList);
            }
        }.guiHandler(null);
    }

    public void onResult(long searchNodeCount, List<Long> path, List<Integer> arriveTimes){

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
            int g1 = (Integer) o1.getProperty("algo-astar-G");
            int g2 = (Integer) o2.getProperty("algo-astar-G");
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
//        System.out.println("setting progress ticket.");
        this.progress = progressTicket;
    }

    private List<Long> getPath() {
        final List<Long> rPath = new ArrayList<Long>();
        new TransactionWrapper() {
            public void runInTransaction() {
                Long parent=end;
                long child = end;
                rPath.add(end);
                while((parent = getParent(child))!=start){
                    if(!shouldGo) return;
                    Progress.progress(progress,"path length:"+rPath.size());
                    rPath.add(parent);
                    org.gephi.graph.api.Node n1 = tgraphNode2GephiNode.get(child);
                    org.gephi.graph.api.Node n2 = tgraphNode2GephiNode.get(parent);
                    n1.setColor(Color.RED);
                    n2.setColor(Color.RED);
                    Edge edge = model.getGraph().getEdge(n2, n1);
                    if(edge==null){
                        edge = model.getGraph().getEdge(n1, n2);
                        if(edge!=null){
                            edge.setColor(Color.RED);
                            System.out.println("edge has direct.");
                        }else{
                            System.out.println("no edge between nodes! "+parent+"--"+child);
                        }
                    }else{
                        edge.setColor(Color.RED);
                        edge.setWeight(1.1f);
                    }
                    child = parent;
                }
                rPath.add(start);
                System.out.println("loop end");
                // connect last path: start->first child.
                org.gephi.graph.api.Node begin = tgraphNode2GephiNode.get(start);
                org.gephi.graph.api.Node end = tgraphNode2GephiNode.get(child);
                Edge edge = model.getGraph().getEdge(begin, end);
                edge.setColor(Color.red);
                edge.setWeight(1.1f);
                begin.setSize(4f);
            }
        }.start(db);
        System.out.println("tx end");
        final List<Long> path = new ArrayList<Long>();
        for(int i=rPath.size()-1;i>=0;i--){
            path.add(rPath.get(i));
        }
        return path;
    }

    private List<Integer> getArriveTime(final List<Long> path){
        final List<Integer> timeList = new ArrayList<Integer>();
        new TransactionWrapper() {
            public void runInTransaction() {
                for (long nodeId : path) {
                    if(!shouldGo) return;
                    int arriveTime = getGvalue(nodeId);
                    timeList.add(arriveTime);
                    org.gephi.graph.api.Node node = tgraphNode2GephiNode.get(nodeId);
                    String label = "Arrive At["+timestamp2String(arriveTime)+"]";
                    node.setLabel(label);
                    System.out.println(label);
                    Progress.progress(progress,nodeId+" "+label);
                }
            }
        }.start(db);
        return timeList;
    }
    
    private String timestamp2String(final int timestamp){
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp*1000);
        System.out.println(timestamp*1000);
        String result = c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH)+1)+"-"+c.get(Calendar.DAY_OF_MONTH)+" "+
                c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE);
//        System.out.println(result);
        return result;
    }

    private int getGvalue(long nodeId) {
        return getGvalue(db.getNodeById(nodeId));
    }
    private int getGvalue(Node node) {
        return (Integer) node.getProperty("algo-astar-G");
    }

    /**
     * get parent node, must used in transaction.
     * @param me current node id
     * @return parent node id
     */
    private Long getParent(long me) {
        Object parent = db.getNodeById(me).getProperty("algo-astar-parent");
        System.out.println(me+" "+parent);
        if(parent!=null){
            return db.getNodeById((Long)parent).getId();
        }else{
            return null;
        }
    }

    /**
     * loop through all neighbors of a given node,
     * and for each neighbor node:
     * 1. update its G value
     * 2. set parent to source node
     * 3. mark node status to CLOSE
     * after the loop above, mark given node status to FINISH
     * @param nodeId given node's id
     */
    private void loopAllNeighborsUpdateGValue(final long nodeId) {
        new TransactionWrapper(){
            public void runInTransaction(){
                Node node = db.getNodeById(nodeId);
                int g = getGvalue(node);
                for(Relationship r : node.getRelationships(Direction.OUTGOING)){
                    if(!shouldGo) return;
                    Node neighbor = r.getOtherNode(node);
//                    logger.info(r.getDynPropertyPointValue("travel-time", g));
                    Object travelTimeObj = r.getDynPropertyPointValue("travel-time", g);
                    int travelTime;
                    if(travelTimeObj!=null) {
                        travelTime = (Integer) travelTimeObj;
                    }else{
//                        logger.info(r.getId()+" "+g);
                        travelTime = 5;
                    }
                    switch (getStatus(neighbor)){
                        case OPEN:
                            setG(neighbor, travelTime + g);
                            setStatus(neighbor, Status.CLOSE);
                            setParent(neighbor, node);
                            break;
                        case CLOSE:
                            int gNeighbor = (Integer) neighbor.getProperty("algo-astar-G");
                            if(gNeighbor>g+travelTime){
                                setG(neighbor,g+travelTime);
                                setParent(neighbor,node);
                            }
                            break;
                    }
                }
                setStatus(node, Status.FINISH);
            }
        }.start(db);
    }

    /**
     * this is an O(1) implementation, because:
     * 1. node status only transfer from OPEN to CLOSE, never back.
     * 2. we use an [minimum heap(PriorityQueue in Java)] data structure.
     * BE CAREFUL! this implementation is only valid based on the assumption [1]!
     * therefore it may only work for Dijkstra SP algorithm.
     * RE VALID this when using algorithm other than Dijkstra SP.
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
     * @param t0 start from source node at time t0
     */
    private void initAlgo(final long from, long to, final int t0) {
        for(org.gephi.graph.api.Node node:model.getGraph().getNodes()){
            if(!shouldGo) return;
            long tgraphNodeId = (Long) node.getAttribute("tgraph_id");
            tgraphNode2GephiNode.put(tgraphNodeId, node);
            node.setColor(new Color(0xc0c0c0));
            node.setLabel("");
//            Progress.progress(progress);
        }
        new TransactionWrapper(){
            @Override
            public void runInTransaction() {
                Node startNode = db.getNodeById(from);
                TGraphTraversal traversal = new TGraphTraversal(db);
                traversal.DFS(startNode, new HashSet<Long>(), new DFSAction<Node>(){
                    public boolean visit(Node node) {
//                        Progress.progress(progress);
                        setStatus(node, Status.OPEN);
                        return shouldGo;
                    }
                }, false);
                totalNodeCount = (int) traversal.getVisitedNodeCount();
                setStatus(startNode, Status.CLOSE);
                setG(startNode, t0);
                minHeap.add(startNode);
            }
        }.start(db);
    }

    private void setG(Node node, int value) {
        node.setProperty("algo-astar-G",value);
    }

    private void setParent(Node node, Node parent) {
        node.setProperty("algo-astar-parent",parent.getId());
    }

    /**
     * when status transfer from OPEN to CLOSE,
     * will add the node to min G Value heap.
     */
    private void setStatus(Node node, Status status){
        node.setProperty("algo-astar-status",status.value());
        if (status == Status.CLOSE) {
            tgraphNode2GephiNode.get(node.getId()).setColor(Color.MAGENTA);
            minHeap.add(node);
        }else if( status == Status.FINISH){
            tgraphNode2GephiNode.get(node.getId()).setColor(Color.GREEN);
            minHeap.remove(node);
        }
    }

    private Status getStatus(final Node node){
        Status result = (Status) new TransactionWrapper() {
            public void runInTransaction() {
                Object status = node.getProperty("algo-astar-status");
                if (status != null) {
                    setReturnValue(Status.valueOf((Integer) status));
                } else {
                    throw new RuntimeException("node property algo-astar-status null");
                }
            }
        }.start(db).getReturnValue();
        return result;
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
