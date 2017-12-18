package edu.buaa.act.gephi.plugin.task;

import com.graphhopper.util.Helper;
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
        Progress.start(this.progress);
        Progress.switchToIndeterminate(this.progress);

        DirectedGraph graph = this.model.getDirectedGraph();

        List<OSMNode> matchedNodeP = new ArrayList<OSMNode>();
        List<Node> matchedNodeT = new ArrayList<Node>();

        long startNodeOSMId = osm.nearestNodeId(this.startLat, this.startLon, 200d);
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

            for(Edge edgeT : graph.getOutEdges(startPointT)){
                double lenT = (Double) edgeT.getAttribute("road_length");
                double angleT = (Double) edgeT.getAttribute("road_angle");

                OSMNode matchedNode = matchOsmEdgeOutDFS(startPointP, startPointP, angleT, lenT);
                if(matchedNode!=null){
                    matchedNodeP.add(matchedNode);
                    matchedNodeT.add(edgeT.getTarget());

                    edgeT.setColor(Color.RED);
                    i++;
                    break;
//                    edgeP.gridId = edgeT.gridId;
//                    edgeP.chainId = edgeT.chainId;
//                    edgeT.end.setLongitude(edgeP.end.longitude);
//                    edgeT.end.setLatitude(edgeP.end.latitude);
//                    log.info("{}", edgeP);
//                    matchedEdgeP.add(edgeP);
//                    matchedEdgeT.add(edgeT);
                }
            }

            for(Edge edgeT : graph.getInEdges(startPointT)){
                double lenT = (Double) edgeT.getAttribute("road_length");
                double angleT = (Double) edgeT.getAttribute("road_angle");

                OSMNode matchedNode = matchOsmEdgeInDFS(startPointP, startPointP, angleT, lenT);
                if(matchedNode!=null){
                    matchedNodeP.add(matchedNode);
                    matchedNodeT.add(edgeT.getSource());
                    edgeT.setColor(Color.RED);
                    i++;
                    break;
                }
            }

            startPointT.setColor(Color.RED);
        }
        handler(null);
        Progress.finish(this.progress);
    }

    private OSMNode matchOsmEdgeOutDFS(OSMNode root, OSMNode start, double angleT, double lenT) {
        for (String eid : start.getOut())
        {
            OSMNode end = osm.getNodeById(osm.getEdgeById(eid).getEndId());
            double angle = calcAngle(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude());
            if(Math.abs(angleT-angle)<6)
            {
                double dis = calcDistance(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude());
                if(Math.abs(lenT-dis)<30){ // match success
                    return end;
                }else if(lenT>dis){ // continue matching
                    return matchOsmEdgeOutDFS(root, end, angleT, lenT);
                }else{ // lenT<dis
                    return null;
                }
            }
        }
        return null; // not found angle match ,failed.
    }

    private OSMNode matchOsmEdgeInDFS(OSMNode root, OSMNode start, double angleT, double lenT) {
        for (String eid : start.getIn())
        {
            OSMNode end = osm.getNodeById(osm.getEdgeById(eid).getStartId());
            double angle = calcAngle(end.getLatitude(), end.getLongitude(), start.getLatitude(), start.getLongitude());
            if(Math.abs(angleT-angle)<6)
            {
                double dis = calcDistance(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude());
                if(Math.abs(lenT-dis)<30){ // match success
                    return end;
                }else if(lenT>dis){ // continue matching
                    return matchOsmEdgeInDFS(root, end, angleT, lenT);
                }else{ // lenT<dis
                    return null;
                }
            }
        }
        return null; // not found angle match ,failed.
    }

    private double calcAngle(double lat1, double lon1, double lat2, double lon2){
        if (!Double.isNaN(lat1) && !Double.isNaN(lat2) && !Double.isNaN(lon1) && !Double.isNaN(lon2)) {
            double theta = Math.atan2(lon2 - lon1, lat2 - lat1);
            double angle = 180 * theta / Math.PI;
            if (angle < 0) angle += 360;
            return angle;
        }else{
            throw new RuntimeException("lat or lon is not a number");
        }
    }

    private double calcDistance(double lat1, double lon1, double lat2, double lon2){
        if (!Double.isNaN(lat1) && !Double.isNaN(lat2) && !Double.isNaN(lon1) && !Double.isNaN(lon2)) {
            return Helper.DIST_EARTH.calcDist(lat1, lon1, lat2, lon2);
        }else{
            throw new RuntimeException("lat or lon is not a number");
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
    public void guiHandler(Object value) {
        //
    }
}
