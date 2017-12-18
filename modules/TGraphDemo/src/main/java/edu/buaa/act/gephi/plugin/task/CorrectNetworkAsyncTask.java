package edu.buaa.act.gephi.plugin.task;

import edu.buaa.act.gephi.plugin.utils.GUIHook;
import edu.buaa.act.gephi.plugin.utils.OSMStorage;
import edu.buaa.act.gephi.plugin.utils.OSMStorage.OSMEdge;
import edu.buaa.act.gephi.plugin.utils.OSMStorage.OSMNode;
import org.gephi.graph.api.*;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by song on 17-12-17.
 */
public class CorrectNetworkAsyncTask extends GUIHook<Object> implements LongTask, Runnable  {

    private final OSMStorage osm;
    private final GraphModel model;
    private final double startLat;
    private final double startLon;
    private final Node startNode;
    private ProgressTicket progress;



    public CorrectNetworkAsyncTask(GraphModel model, OSMStorage storage, Node startNode, double startLat, double startLon){
        this.osm = storage;
        this.model = model;
        this.startNode = startNode;
        this.startLat = startLat;
        this.startLon = startLon;
    }

    @Override
    public void run() {
        DirectedGraph graph = this.model.getDirectedGraph();
        Progress.switchToIndeterminate(this.progress);
        Progress.start(this.progress);

        List<OSMNode> matchedNodeP = new ArrayList<OSMNode>();
        List<Node> matchedNodeT = new ArrayList<Node>();

        long startNodeOSMId = osm.nearestNodeId(this.startLat, this.startLon);
        OSMNode startPointP = osm.getNodeById(startNodeOSMId);
        Node startPointT = this.startNode;

        matchedNodeP.add(startPointP);
        matchedNodeT.add(startPointT);

        Set<OSMNode> matchedNodePSet = new HashSet<OSMNode>();

        int i=0;
        while(i<matchedNodeP.size()){
            startPointP = matchedNodeP.get(i);
            startPointT = matchedNodeT.get(i);
            if(matchedNodePSet.contains(startPointP)){
                i++;
                continue;
            }else{
                matchedNodePSet.add(startPointP);
            }

            for (String edgeIdP : startPointP.getOut()){
                OSMEdge edgeP = this.osm.getEdgeById(edgeIdP);

                for(Edge edgeT : graph.getOutEdges(startPointT)){
                    Object lenObjT = edgeT.getAttribute("road_length");
                    double lenT = (Double) lenObjT;
                    Object angleObjT = edgeT.getAttribute("road_angle");
                    double angleT = (Double) angleObjT;

                    if(Math.abs(angleT-edgeP.getAngle())<6){//Math.abs(edgeT.length-edgeP.length)<20 &&
//                        edgeP.gridId = edgeT.gridId;
//                        edgeP.chainId = edgeT.chainId;
//                        edgeT.end.setLongitude(edgeP.end.longitude);
//                        edgeT.end.setLatitude(edgeP.end.latitude);
//                        log.info("{}", edgeP);
//                        matchedEdgeP.add(edgeP);
//                        matchedEdgeT.add(edgeT);
                        edgeT.setColor(Color.RED);
                        matchedNodeP.add(this.osm.getNodeById(edgeP.getEndId()));
                        matchedNodeT.add(edgeT.getTarget());
                        i++;
                        break;
                    }
                }
            }

            for (String edgeIdP : startPointP.getIn()){
                OSMEdge edgeP = this.osm.getEdgeById(edgeIdP);

                for(Edge edgeT : graph.getInEdges(startPointT)){
                    Object lenObjT = edgeT.getAttribute("road_length");
                    double lenT = (Double) lenObjT;
                    Object angleObjT = edgeT.getAttribute("road_angle");
                    double angleT = (Double) angleObjT;
                    if(Math.abs(angleT-edgeP.getAngle())<6){ //Math.abs(edgeT.length-edgeP.length)<20 &&
//                        edgeP.gridId = edgeT.gridId;
//                        edgeP.chainId = edgeT.chainId;
//                        edgeT.start.setLongitude(edgeP.start.longitude);
//                        edgeT.start.setLatitude(edgeP.start.latitude);
//                        log.info("{}", edgeP);
//                        matchedEdgeP.add(edgeP);
//                        matchedEdgeT.add(edgeT);
                        edgeT.setColor(Color.RED);
                        matchedNodeP.add(this.osm.getNodeById(edgeP.getStartId()));
                        matchedNodeT.add(edgeT.getSource());
                        i++;
                        break;
                    }
                }
            }
            startPointT.setColor(Color.RED);
        }

        Progress.finish(this.progress);
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
    public void guiHandler(Object value) {
        //
    }
}
