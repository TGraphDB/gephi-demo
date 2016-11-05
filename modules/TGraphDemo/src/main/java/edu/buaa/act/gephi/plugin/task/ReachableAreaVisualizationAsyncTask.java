/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.buaa.act.gephi.plugin.task;

import edu.buaa.act.gephi.plugin.utils.LinearGradientInt;
import org.act.neo4j.temporal.demo.algo.TGraphTraversal;
import org.act.neo4j.temporal.demo.algo.TGraphTraversal.DFSAction;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphModel;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.awt.*;
import java.util.*;

/**
 * Algorithm to find reachable area from a given start position at a given departure time in a temporal
 * graph satisfying FIFO property. Roads are colorized according to the time needed to get there.
 * Roads can not be reached in 2000 seconds is colored grey.
 * Note: This algorithm runs in one transaction and commit NOTHING to database.
 * 2016-10
 * @author song
 */
public class ReachableAreaVisualizationAsyncTask extends TimeDependentDijkstraOneTransactionAsyncTask{
    private GraphDatabaseService db;
    private GraphModel model;
    private ProgressTicket progress;
    private long start;
    private long end;
    private int startTime;
    volatile private boolean shouldGo=true;
    private Map<Long,org.gephi.graph.api.Node> tgraphNode2GephiNode = new HashMap<Long,org.gephi.graph.api.Node>();
    private int maxTravelTime = 0;
    private long searchCount=0;
    private LinearGradientInt colors;
    private int timeOutSeconds = 2000;


    public ReachableAreaVisualizationAsyncTask(
            GraphDatabaseService db,
            GraphModel model,
            long startId, long endId, int startTime,
            Color pathColor){
        super(db, model, startId, endId, startTime, pathColor);
        this.db = db;
        this.model = model;
        this.start = startId;
        this.end = endId;
        this.startTime = startTime;
        System.out.println("start time: " + timestamp2String(startTime));
    }

    @Override
    public void run() {
        Thread.currentThread().setName("TGraph.E.A.PathVisualization");
        Progress.start(progress);
        this.start(db);
        Progress.finish(progress);
    }

    @Override
    public void runInTransaction() {
        try {
            System.out.println("enter tx");
            Progress.setDisplayName(progress, "initial algorithm...");
            initAlgo(start, end, startTime);
            this.colors = new LinearGradientInt(
                    new Color[]{Color.BLUE , Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED},
                    new float[]{0f, 0.25f, 0.5f, 0.75f, 1f},
                    this.timeOutSeconds);

            System.out.println("init done");
            Progress.setDisplayName(progress, "searching and drawing...");

            Long node;
            while ((node = findSmallestClosedNode())!=null && shouldGo) {
                loopAllNeighborsUpdateGValue(node);
                searchCount++;
                Progress.progress(progress, searchCount + " nodes searched");
            }
            System.out.println("search end. max travel time is "+timePeriod2Str(maxTravelTime));
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
        this.progress = progressTicket;
    }

    private String timestamp2String(final int timestamp){
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(((long) timestamp) * 1000);
        String result = c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH)+1)+"-"+c.get(Calendar.DAY_OF_MONTH)+" "+
                c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE);
        return result;
    }

    private int getGvalue(Node node) {
        return (Integer) node.getProperty("algo-astar-G");
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
        int g = getGvalue(node);
        for(Relationship r : node.getRelationships(Direction.OUTGOING)){
            if(!shouldGo) return;
            Node neighbor = r.getOtherNode(node);
            int travelTime;
            switch (getStatus(neighbor)){
                case OPEN:
                    travelTime = getEarliestTravelTime(r, g);
                    setG(neighbor, travelTime + g);
                    setStatus(neighbor, Status.CLOSE);
                    setParent(neighbor, node);
                    break;
                case CLOSE:
                    travelTime = getEarliestTravelTime(r, g);
                    int gNeighbor = (Integer) neighbor.getProperty("algo-astar-G");
                    if(gNeighbor>g+travelTime){
                        setG(neighbor,g+travelTime);
                        setParent(neighbor,node);
                    }
                    break;
            }
        }
        if(nodeId!=start) drawPathToParent(node);
        setStatus(node, Status.FINISH);
    }

    private void drawPathToParent(Node node) {
        int g = getGvalue(node);
        int travelTime = g-startTime;
        if(maxTravelTime <travelTime){
            maxTravelTime = travelTime;
//            System.out.println(timePeriod2Str(maxTravelTime));
        }

        long parentId;
        long childId = node.getId();
        Object parent = node.getProperty("algo-astar-parent");
        if(parent!=null) {
            parentId = (Long) parent;
            org.gephi.graph.api.Node n1 = tgraphNode2GephiNode.get(childId);
            org.gephi.graph.api.Node n2 = tgraphNode2GephiNode.get(parentId);
            Color colorOfGValue;
            if (travelTime < this.timeOutSeconds) {
                colorOfGValue = new Color(this.colors.getValue(travelTime), true);
            } else {
                colorOfGValue = new Color(0x696969);
            }
            n1.setColor(colorOfGValue);
            Edge edge = model.getGraph().getEdge(n2, n1);
            if (edge == null) {
                edge = model.getGraph().getEdge(n1, n2);
                if (edge != null) {
                    edge.setColor(colorOfGValue);
                } else {
                    throw new RuntimeException("edge of " + childId + "-" + parentId + " not found");
                }
            } else {
                edge.setColor(colorOfGValue);
            }
        }else{
            //do nothing
        }
    }

    /**
     * TODO: this should be rewrite with an range query.
     * Use 'earliest arrive time' rather than simply use 'travel-time' property at departureTime
     * Because there exist cases that 'a delay before departureTime decrease the time of
     * arrival'.(eg. wait until road not jammed, See Dreyfus 1969, page 29)
     * This makes the arrive-time-function non-decreasing,
     * thus guarantee FIFO property of this temporal network.
     * This property is the foundational assumption to found
     * earliest arrive time with this algorithm.
     * @param r road.
     * @param departureTime time start from r's start node.
     * @return earliest arrive time to r's end node when departure from r's start node at departureTime.
     */
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
    private Long findSmallestClosedNode() {
        try {
            return minHeap.peek().getId();
        }catch (NullPointerException e){
            return null;
        }
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
//            node.setColor(new Color(0xc0c0c0));
//            node.setLabel("");
        }
        Node startNode = db.getNodeById(from);
        TGraphTraversal traversal = new TGraphTraversal(db);
        traversal.DFS(startNode, new HashSet<Long>(), new DFSAction<Node>(){
            public boolean visit(Node node) {
                setStatus(node, Status.OPEN);
                return shouldGo;
            }
        }, false);
        setStatus(startNode, Status.CLOSE);
        setG(startNode, t0);
    }

    private void setG(Node node, int value) {
        node.setProperty("algo-astar-G", value);
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
//            tgraphNode2GephiNode.get(node.getId()).setColor(Color.MAGENTA);
            minHeap.add(node);
        }else if( status == Status.FINISH){
//            tgraphNode2GephiNode.get(node.getId()).setColor(Color.GREEN);
            minHeap.remove(node);
        }
    }

    private Status getStatus(final Node node){
        Object status = node.getProperty("algo-astar-status");
        if (status != null) {
            return Status.valueOf((Integer) status);
        } else {
            throw new RuntimeException("node property algo-astar-status null");
        }
    }

}
