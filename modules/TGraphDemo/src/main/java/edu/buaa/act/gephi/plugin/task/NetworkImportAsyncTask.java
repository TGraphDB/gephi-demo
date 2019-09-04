package edu.buaa.act.gephi.plugin.task;
import edu.buaa.act.gephi.plugin.tool.ImportNetworkTool;
import org.act.tgraph.demo.algo.TGraphTraversal;
import org.act.tgraph.demo.utils.TransactionWrapper;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphFactory;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Table;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
/**
 * Created by song on 16-5-12.
 */

public class NetworkImportAsyncTask extends TransactionWrapper<Integer> implements LongTask, Runnable {
    private final GraphModel model;
    private final GraphDatabaseService db;
    private final GraphFactory factory;
    private final Graph graph;
    private final TGraphTraversal traversal;

    private ProgressTicket progressTicket;
    private boolean shouldGo=true;
    private final double scale = 0.1;
    private long startNodeId=0;
    private int totalNodes=0;
    private float importSpeed = 1;
    private int nodeImportedCount = 0;
    private final boolean isDirected;
    
    public NetworkImportAsyncTask(GraphDatabaseService db, GraphModel model, long startNodeId, int nodeCount, float speed, boolean isDirected) {
        this.db = db;
        this.model = model;
//        this.startNodeId = startNodeId==0?46652:startNodeId;
//        this.startNodeId = startNodeId==0?51849:startNodeId;
        ImportNetworkTool tool = Lookup.getDefault().lookup( ImportNetworkTool.class );
        this.startNodeId = tool.getNodeId();
        this.totalNodes = nodeCount;
        this.importSpeed = speed;
        this.factory = model.factory();
        this.graph = model.getGraph();
        this.traversal = new TGraphTraversal(db);
        this.isDirected = isDirected;
    }



    @Override
    public void run() {
        Thread.currentThread().setName("TGraph.ImportNetFromDB");
        Progress.start(progressTicket, totalNodes);
        progressTicket.setDisplayName("creating attributes...");

        // create attributes of node and edges;
        Table nodeTable = model.getNodeTable();
        Table edgeTable = model.getEdgeTable();
        addColumnIfNotExist(nodeTable,"cross_id",String.class);
        addColumnIfNotExist(nodeTable,"tgraph_id", Long.class);
        addColumnIfNotExist(nodeTable,"subnet_id", Integer.class);

        addColumnIfNotExist(edgeTable,"road_uid",Integer.class);
        addColumnIfNotExist(edgeTable,"road_grid",Integer.class);
        addColumnIfNotExist(edgeTable,"road_index",String.class);
        addColumnIfNotExist(edgeTable,"road_in",Integer.class);
        addColumnIfNotExist(edgeTable,"road_out",Integer.class);
        addColumnIfNotExist(edgeTable,"road_type",Integer.class);
        addColumnIfNotExist(edgeTable, "road_length", Double.class);
        addColumnIfNotExist(edgeTable, "real_length", Double.class);
        addColumnIfNotExist(edgeTable, "road_angle", Double.class);
        addColumnIfNotExist(edgeTable, "real_angle", Double.class);
        addColumnIfNotExist(edgeTable,"tgraph_id", Long.class);
        addColumnIfNotExist(edgeTable,"t_max", Integer.class);
        addColumnIfNotExist(edgeTable,"t_min", Integer.class);
        addColumnIfNotExist(edgeTable,"t_count", Integer.class);

        progressTicket.setDisplayName("adding nodes...");
        // loop and draw nodes to gephi.
        this.start(db);
        Progress.finish(progressTicket);
    }

    //old one: import without consider of road level (type)
    @Override
    public void runInTransaction() {
        Node startNode = db.getNodeById(startNodeId);
        Relationship startEdge = startNode.getRelationships().iterator().next();
        addNodeAtRandomPlace(startNode);

        traversal.edgeBFS(startEdge, new TGraphTraversal.EdgeBFSAction() {

            @Override
            public boolean visit(Relationship edge) {
                org.gephi.graph.api.Node start = tryGetStartGephiNode(edge);
                org.gephi.graph.api.Node end = tryGetEndGephiNode(edge);
                if (start != null && end == null) {
                    drawEndNode(edge, start);
                } else if (start == null && end != null) {
                    drawStartNode(edge, end);
                } else if (start != null && end != null) {
                    drawEdge(edge, start, end);
                } else { // start==null && end==null
                     throw new RuntimeException("Both nodes not exist in graph!");
                }
                try {
                    if (importSpeed < 1) {
//                        System.out.println("sleep "+ 1/importSpeed +"ms");
                        Thread.sleep((long) (1 / importSpeed));
                    }
                } catch (InterruptedException e) {
                    return false;
                }
                Progress.progress(progressTicket);
                return shouldGo;
            }

        }, false);
        setReturnValue(nodeImportedCount);
    }

//    Map<Integer, Long> levelStartNode = new HashMap<Integer, Long>();
//
//    // new one: import highway first, then level one, then level two...
//    @Override
//    public void runInTransaction() {
//        levelStartNode.put(1, 23716L);
//        levelStartNode.put(2, 25679L);
//        levelStartNode.put(3, 54824L);
//        levelStartNode.put(4, 44356L);
//        levelStartNode.put(5, 22933L);
//        levelStartNode.put(6, 22542L);
//        levelStartNode.put(11,22450L);
//
//        for(final Map.Entry<Integer,Long> entry : levelStartNode.entrySet())
//        {
//            Node startNode = db.getNodeById(entry.getValue());
//            Relationship startEdge = null;
//            for(Relationship r :startNode.getRelationships())
//            {
//                Integer type = (Integer) r.getProperty("type");
//                if (type != null && type == entry.getKey())
//                {
//                    startEdge = r;
//                }
//            }
//            if(startEdge==null) throw new RuntimeException("SNH: startEdge is null!");
//
//            if(entry.getKey()==1) addNodeAtRandomPlace(startNode);
//
//            traversal.edgeBFS(startEdge, new TGraphTraversal.EdgeBFSAction()
//            {
//                @Override
//                public boolean visit(Relationship edge)
//                {
//                    Integer type = (Integer) edge.getProperty("type");
//                    if (type != null && type == entry.getKey())
//                    {
//                        org.gephi.graph.api.Node start = tryGetStartGephiNode(edge);
//                        org.gephi.graph.api.Node end = tryGetEndGephiNode(edge);
//                        if (start != null && end == null)
//                        {
//                            drawEndNode(edge, start);
//                        } else if (start == null && end != null)
//                        {
//                            drawStartNode(edge, end);
//                        } else if (start != null && end != null)
//                        {
//                            drawEdge(edge, start, end);
//                        } else
//                        { // start==null && end==null
//                            return shouldGo;
//                            // throw new RuntimeException("Both nodes not exist in graph!");
//                        }
//                        try
//                        {
//                            if (importSpeed < 1)
//                            {
////                        System.out.println("sleep "+ 1/importSpeed +"ms");
//                                Thread.sleep((long) (1 / importSpeed));
//                            }
//                        } catch (InterruptedException e)
//                        {
//                            return false;
//                        }
//                        Progress.progress(progressTicket);
//                    }
//                    return shouldGo;
//                }
//            }, false);
//        }
//
//        setReturnValue(nodeImportedCount);
//    }


    private void drawStartNode(Relationship r, org.gephi.graph.api.Node end) {
        Object length = r.getProperty("length");
        Object angle = r.getProperty("angle");
        int len = (Integer)length;
        int theta = (Integer)angle;
        org.gephi.graph.api.Node start = addNode(r.getStartNode(),
                end.x() - len * scale * Math.sin(theta * Math.PI / 180),
                end.y() - len * scale * Math.cos(theta * Math.PI / 180));
//        start.setColor(Color.green);
//        end.setColor(Color.blue);
        Edge edge = addEdge(start, end, r);
//        edge.setColor(Color.green);
    }

    private void drawEndNode(Relationship r, org.gephi.graph.api.Node start) {
        Object length = r.getProperty("length");
        Object angle = r.getProperty("angle");
        int len = (Integer)length;
        int theta = (Integer)angle;
        org.gephi.graph.api.Node end = addNode(r.getEndNode(),
                start.x()+len*scale*Math.sin(theta*Math.PI/180),
                start.y()+len*scale*Math.cos(theta * Math.PI / 180));
//        start.setColor(Color.blue);
//        end.setColor(Color.green);
        Edge edge = addEdge(start, end, r);
//        edge.setColor(Color.GREEN);
    }

    private void drawEdge(Relationship r, org.gephi.graph.api.Node start, org.gephi.graph.api.Node end) {
        Edge edge = addEdge(start, end, r);
//        colorEdgeIfProblem(edge, start, end);
    }

    private void colorEdgeIfProblem(Edge edge, org.gephi.graph.api.Node start, org.gephi.graph.api.Node end) {
        int angleFromDB = (Integer) edge.getAttribute("road_angle");
        int angleFromGephi = (int) ( 180*Math.atan((start.x() - end.x()) / (start.y() - end.y()))/Math.PI);
        if(angleFromGephi<0) angleFromGephi=360+angleFromGephi;

        int lengthFromDB = (Integer) edge.getAttribute("road_length");
        int lengthFromGephi = (int) (Math.sqrt(Math.pow(start.x()-end.x(),2)+Math.pow(start.y()-end.y(),2))/scale);

        boolean badAngle = false;
        boolean badLength = false;
        if(Math.abs(angleFromDB-angleFromGephi)>20){
            badAngle=true;
//            System.out.println(r.getId()+" badAngle "+angleFromDB+" "+angleFromGephi);
            edge.setAttribute("real_angle", angleFromGephi);
        }else{
//            System.out.println(r.getId()+" goodAngle "+angleFromDB+" "+angleFromGephi);
        }

        if(Math.abs(lengthFromDB-lengthFromGephi)>10){
            badLength=true;
//            System.out.println(r.getId() + " badLength " + lengthFromDB + " " + lengthFromGephi);
            edge.setAttribute("real_length", lengthFromGephi);
        }else{
//            System.out.println(r.getId() + " goodLength " + lengthFromDB + " " + lengthFromGephi);
        }

        if(badAngle && badLength) {
            edge.setColor(Color.magenta);
        }else if(badAngle && !badLength){
            edge.setColor(Color.YELLOW);
        }else if(!badAngle && badLength){
            edge.setColor(Color.GREEN);
        }
    }

    private org.gephi.graph.api.Node tryGetEndGephiNode(Relationship edge) {
        Node node = edge.getEndNode();
        return graph.getNode(gephiNodeId(node));
    }

    private org.gephi.graph.api.Node tryGetStartGephiNode(Relationship edge) {
        Node node = edge.getStartNode();
        return graph.getNode(gephiNodeId(node));
    }

    @Override
    public boolean cancel() {
        shouldGo = false;
        traversal.cancel();
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket pt) {
        this.progressTicket = pt;
    }
//  ======================== new version ==========================
    private String gephiNodeId(Node node){
        return String.format("%06d",node.getId());
    }


//    ======================= old version =========================
    private void calcPositionAndDraw(org.gephi.graph.api.Node gNode, Relationship r, Node neighbor) {
        Object length = r.getProperty("length");
        Object angle = r.getProperty("angle");
        int len = (Integer)length;
        int theta = (Integer)angle;
        if(r.getEndNode().getId()==neighbor.getId()){// Directed Edge: gNode-->neighbor
            org.gephi.graph.api.Node gephiNodeNeighbor = addNode(neighbor,
                    gNode.x()+len*scale*Math.cos(theta*Math.PI/180),
                    gNode.y()+len*scale*Math.sin(theta * Math.PI / 180));
            gephiNodeNeighbor.setColor(Color.blue);
            addEdge(gNode, gephiNodeNeighbor, r);
        }else{// neighbor-->gNode
            org.gephi.graph.api.Node gephiNodeNeighbor = addNode(neighbor,
                    gNode.x()+len*scale*Math.cos((theta-180)*Math.PI/180),
                    gNode.y()+len*scale*Math.sin((theta-180) * Math.PI / 180));
            gephiNodeNeighbor.setColor(Color.blue);
            addEdge(gephiNodeNeighbor, gNode, r);
        }
    }

    private org.gephi.graph.api.Node addNode(Node node, double x, double y) {
        String id = gephiNodeId(node);
        org.gephi.graph.api.Node gephiNode = graph.getNode(id);
        if(gephiNode==null){
            gephiNode = factory.newNode(id);
            gephiNode.setX((float)x);
            gephiNode.setY((float)y);
            gephiNode.setSize(1f);
            gephiNode.setAttribute("cross_id",node.getProperty("cross-id"));
            gephiNode.setAttribute("tgraph_id",node.getId());
//            gephiNode.setAttribute("subnet_id", node.getProperty("connected-subnet-id"));
            graph.addNode(gephiNode);
            nodeImportedCount++;
            return gephiNode;
        }else{
            return gephiNode;
        }
    }

    private org.gephi.graph.api.Node addNodeAtRandomPlace(Node node){
        return addNode(node, ThreadLocalRandom.current().nextInt(-100, 101), ThreadLocalRandom.current().nextInt(-100, 101));
    }

    private org.gephi.graph.api.Node getNode(Node node) {
        String id = gephiNodeId(node);
        org.gephi.graph.api.Node gephiNode = graph.getNode(id);
        if(gephiNode==null){
            throw new RuntimeException("Cannot Get: node "+id+" not in gephi!");
        }else{
            return graph.getNode(id);
        }
    }

    private Edge addEdge(org.gephi.graph.api.Node gephiNode,
                         org.gephi.graph.api.Node gephiNodeNeighbor,
                         Relationship r) {
        if(graph.getEdge(gephiNode,gephiNodeNeighbor)==null) {
            //printProperties(r);
            Edge edge = factory.newEdge(gephiNode, gephiNodeNeighbor,isDirected);
            edge.setAttribute("road_uid",r.getProperty("uid"));
//            edge.setAttribute("road_lid",r.getProperty("line-num"));
            edge.setAttribute("road_grid",Integer.parseInt((String) r.getProperty("grid-id")));
            edge.setAttribute("road_index",r.getProperty("chain-id"));
            edge.setAttribute("road_in",r.getProperty("in-count"));
            edge.setAttribute("road_out",r.getProperty("out-count"));
            edge.setAttribute("road_type",r.getProperty("type"));
            edge.setAttribute("road_length",((Integer) r.getProperty("length"))+0d);
            edge.setAttribute("road_angle", ((Integer) r.getProperty("angle"))+0d);
            edge.setAttribute("tgraph_id",r.getId());
            if(r.hasProperty("max-time")) {
                 edge.setAttribute("t_max",r.getProperty("max-time"));
                 edge.setAttribute("t_min",r.getProperty("min-time"));
                 edge.setAttribute("t_count",r.getProperty("data-count"));
            }
            graph.addEdge(edge);
            return edge;
        }else{
            return graph.getEdge(gephiNode,gephiNodeNeighbor);
            // throw new RuntimeException("edge already exist!");
        }
    }

    private void printProperties(PropertyContainer nodeOrEdge){
        for(String key: nodeOrEdge.getPropertyKeys()){
            System.out.print(","+key+":"+nodeOrEdge.getProperty(key,null));
        }
        System.out.println();
    }

    private void notice(String s) {
        DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(s));
    }

    private void addColumnIfNotExist(Table table, String name, Class type){
        if(!table.hasColumn(name)){
            table.addColumn(name, type);
        }
    }

    /**
     * @param importSpeed the importSpeed to set
     */
    public void setImportSpeed(float importSpeed) {
        System.out.println("speed set to "+importSpeed);
        this.importSpeed = importSpeed;
    }

}
