package edu.buaa.act.gephi.plugin.task;

import edu.buaa.act.gephi.plugin.utils.GUIHook;
import edu.buaa.act.gephi.plugin.utils.OSMStorage;
import org.gephi.graph.api.*;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by song on 17-12-18.
 */
public class OSMVisualizeAsyncTask  extends GUIHook<Object> implements LongTask, Runnable {
    private final OSMStorage osm;
    private final GraphModel model;
    private final GraphFactory factory;
    private final Graph graph;
    private ProgressTicket progress;

    private Map<Long, Node> nodeMap = new HashMap<Long, Node>();

    public OSMVisualizeAsyncTask(GraphModel model, OSMStorage storage){
        this.osm = storage;
        this.model = model;
        this.graph = model.getGraph();
        this.factory = model.factory();
    }


    @Override
    public void run() {
        Collection<OSMStorage.OSMNode> allNodes = this.osm.allNodes();
        Collection<OSMStorage.OSMEdge> allEdges = this.osm.allEdges();

        Progress.start(progress, allNodes.size()+allEdges.size());

        Table nodeTable = model.getNodeTable();
        addColumnIfNotExist(nodeTable, "latitude", Double.class);
        addColumnIfNotExist(nodeTable,"longitude", Double.class);

        for(OSMStorage.OSMNode osmNode : allNodes){
            Node gephiNode = factory.newNode(String.valueOf(osmNode.getOsmId()));
            gephiNode.setX((float) (osmNode.getLongitude()-116.39)*10000);
            gephiNode.setY((float) (osmNode.getLatitude()-39.91)*10000);
            gephiNode.setSize(1f);
            gephiNode.setAttribute("latitude",osmNode.getLatitude());
            gephiNode.setAttribute("longitude",osmNode.getLongitude());
            graph.addNode(gephiNode);
            nodeMap.put(osmNode.getOsmId(), gephiNode);
            Progress.progress(progress, "adding nodes...");
        }

        for(OSMStorage.OSMEdge osmEdge : allEdges){
            Node source = nodeMap.get(osmEdge.getStartId());
            Node target = nodeMap.get(osmEdge.getEndId());
            Edge gephiEdge = factory.newEdge(source, target, true);
            graph.addEdge(gephiEdge);
            Progress.progress(progress, "add edges...");
        }

        Progress.finish(progress);
    }

    private void addColumnIfNotExist(Table table, String name, Class type){
        if(!table.hasColumn(name)){
            table.addColumn(name, type);
        }
    }

    @Override
    public boolean cancel() {
        return true;
    }
    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;
    }
    @Override
    public void guiHandler(Object value) {}

}
