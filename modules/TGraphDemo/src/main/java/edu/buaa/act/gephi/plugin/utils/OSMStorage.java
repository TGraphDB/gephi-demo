package edu.buaa.act.gephi.plugin.utils;

import com.graphhopper.reader.ReaderElement;
import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.osm.OSMInputFile;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.Helper;
import gnu.trove.list.TLongList;

import java.io.File;
import java.util.*;

/**
 * Created by song on 17-12-17.
 */

public class OSMStorage {
    private File osmFile;
    private Map<Long, OSMNode> nodeMap = new HashMap<Long, OSMNode>();
    private Map<Long, List<OSMEdge>> inChains = new HashMap<Long, List<OSMEdge>>();
    private Map<Long, List<OSMEdge>> outChains = new HashMap<Long, List<OSMEdge>>();
    private Map<String, OSMEdge> edgeMap = new HashMap<String, OSMEdge>();

    public OSMStorage(File osmFile){
        this.osmFile = osmFile;
    }

    public void loadFile(){
        firstPhase();
        secondPhase();
    }


    public void loadFile(double minLat, double maxLat, double minLon, double maxLon) {
        Map<Long, OSMNode> nodeMap = new HashMap<Long, OSMNode>();
        Map<String, OSMEdge> edgeMap = new HashMap<String, OSMEdge>();
        firstPhase(nodeMap, edgeMap);
        secondPhase(minLat, maxLat, minLon, maxLon, nodeMap, edgeMap);
    }

    public OSMNode getNodeById(long osmId) {
        return nodeMap.get(osmId);
    }

    private void firstPhase() {
        OSMInputFile in = null;
        try {
            in = new OSMInputFile(osmFile).setWorkerThreads(1).open();
            ReaderElement item;
            while ((item = in.getNext()) != null) {
                if (item.isType(ReaderElement.WAY)) {
                    final ReaderWay way = (ReaderWay) item;
                    boolean valid = filterWay(way);
                    if (valid) {
                        int wayCount = 0;
                        TLongList wayNodes = way.getNodes();
                        long wayId = way.getId();
                        String wayName = way.getTag("name", "");
                        String wayType = way.getTag("highway", "");
                        for (int i = 0; i < wayNodes.size(); i++) {
                            long osmNodeId = wayNodes.get(i);
                            if (nodeMap.containsKey(osmNodeId)) {
                                nodeMap.get(osmNodeId).setTower(true);
                            } else {
                                nodeMap.put(osmNodeId, new OSMNode(osmNodeId));
                            }

                            if (i > 0) {
                                long preNodeId = wayNodes.get(i - 1);
                                OSMEdge edge = new OSMEdge(preNodeId, osmNodeId, wayId, wayName, wayType);
                                edge.setId(wayId + ":" + wayCount);
                                edgeMap.put(edge.getId(), edge);
                                wayCount++;
                            }
                        }

                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Problem while parsing file", ex);
        } finally {
            Helper.close(in);
        }
    }

    private void firstPhase(Map<Long, OSMNode> nodeMap,
                            Map<String, OSMEdge> edgeMap) {
        OSMInputFile in = null;


        try {
            in = new OSMInputFile(osmFile).setWorkerThreads(1).open();
            ReaderElement item;
            while ((item = in.getNext()) != null) {
                if (item.isType(ReaderElement.WAY)) {
                    final ReaderWay way = (ReaderWay) item;
                    boolean valid = filterWay(way);
                    if (valid) {
                        int wayCount = 0;
                        TLongList wayNodes = way.getNodes();
                        long wayId = way.getId();
                        String wayName = way.getTag("name", "");
                        String wayType = way.getTag("highway", "");
                        for (int i = 0; i < wayNodes.size(); i++) {
                            long osmNodeId = wayNodes.get(i);
                            if (nodeMap.containsKey(osmNodeId)) {
                                nodeMap.get(osmNodeId).setTower(true);
                            } else {
                                nodeMap.put(osmNodeId, new OSMNode(osmNodeId));
                            }

                            if (i > 0) {
                                long preNodeId = wayNodes.get(i - 1);
                                OSMEdge edge = new OSMEdge(preNodeId, osmNodeId, wayId, wayName, wayType);
                                edge.setId(wayId + ":" + wayCount);
                                edgeMap.put(edge.getId(), edge);
                                wayCount++;
                            }
                        }

                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Problem while parsing file", ex);
        } finally {
            Helper.close(in);
        }
    }

    private void secondPhase(double minLat, double maxLat, double minLon, double maxLon,
                             Map<Long, OSMNode> nodeMap,
                             Map<String, OSMEdge> edgeMap) {
        OSMInputFile in = null;
        try {
            in = new OSMInputFile(osmFile).setWorkerThreads(1).open();
            ReaderElement item;
            while ((item = in.getNext()) != null) {
                if (item.isType(ReaderElement.NODE)) {
                    OSMNode n = nodeMap.get(item.getId());
                    if (n != null) {
                        ReaderNode node = (ReaderNode) item;
                        if(minLat<node.getLat() && node.getLat()<maxLat &&
                                minLon<node.getLon() && node.getLon()<maxLon){
                            n.setLatitude(node.getLat());
                            n.setLongitude(node.getLon());
                            n.setName(node.getTag("name", ""));
                            this.nodeMap.put(item.getId(), n);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Problem while parsing file", ex);
        } finally {
            Helper.close(in);
        }

        DistanceCalc distCalc = Helper.DIST_EARTH;

        for (OSMEdge e : edgeMap.values()) {
            OSMNode start = this.nodeMap.get(e.getStartId());
            OSMNode end = this.nodeMap.get(e.getEndId());
            if (start != null && end != null) {
                this.edgeMap.put(e.getId(), e);
                start.out.add(e.getId());
                end.in.add(e.getId());
                e.setStart(start);
                e.setEnd(end);

                double sLat = start.getLatitude();
                double sLon = start.getLongitude();
                double eLat = end.getLatitude();
                double eLon = end.getLongitude();
                if (!Double.isNaN(sLat) && !Double.isNaN(sLon) && !Double.isNaN(eLat) && !Double.isNaN(eLon)) {
                    double len = distCalc.calcDist(sLat, sLon, eLat, eLon);
                    if (len < 0) throw new RuntimeException("SNH: len<0 at edge index " + e.getWayId());
                    e.setLength(len);

                    double theta = Math.atan2(eLon - sLon, eLat - sLat);
                    double angle = 180 * theta / Math.PI;
                    if (angle < 0) angle += 360;
                    e.setAngle(angle);
                } else {
                    throw new RuntimeException("lat or lon is NaN!");
                }
            }
        }
    }

    private void secondPhase() {
        OSMInputFile in = null;
        try {
            in = new OSMInputFile(osmFile).setWorkerThreads(1).open();
            ReaderElement item;
            while ((item = in.getNext()) != null) {
                if (item.isType(ReaderElement.NODE)) {
                    OSMNode n = nodeMap.get(item.getId());
                    if (n != null) {
                        ReaderNode node = (ReaderNode) item;
                        n.setLatitude(node.getLat());
                        n.setLongitude(node.getLon());
                        n.setName(node.getTag("name", ""));
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Problem while parsing file", ex);
        } finally {
            Helper.close(in);
        }
        long towerCount = 0;
        for (Map.Entry<Long, OSMNode> e : nodeMap.entrySet()) {
            if (e.getValue().isTower()) {
                towerCount++;
            }
        }

        DistanceCalc distCalc = Helper.DIST_EARTH;
        double totalDis = 0;

        for (OSMEdge e : edgeMap.values()) {
            OSMNode start = nodeMap.get(e.getStartId());
            OSMNode end = nodeMap.get(e.getEndId());
            if (start != null && end != null) {
                start.out.add(e.getId());
                end.in.add(e.getId());
                e.setStart(start);
                e.setEnd(end);

                double sLat = start.getLatitude();
                double sLon = start.getLongitude();
                double eLat = end.getLatitude();
                double eLon = end.getLongitude();
                if (!Double.isNaN(sLat) && !Double.isNaN(sLon) && !Double.isNaN(eLat) && !Double.isNaN(eLon)) {
                    double len = distCalc.calcDist(sLat, sLon, eLat, eLon);
                    if (len < 0) throw new RuntimeException("SNH: len<0 at edge index " + e.getWayId());
                    e.setLength(len);
                    totalDis += len;

                    double theta = Math.atan2(eLon - sLon, eLat - sLat);
                    double angle = 180 * theta / Math.PI;
                    if (angle < 0) angle += 360;
                    e.setAngle(angle);
                } else {
                    throw new RuntimeException("lat or lon is NaN!");
                }
            } else {
                throw new RuntimeException("SNH: start or end is null!");
            }
        }

//            log.info("get {} nodes ({} tower) and {} edges ({} ways).", nodeMap.size(), towerCount,  edgeList.size(), wayCount);
    }

    private static Set<String> ferries = new HashSet<String>(Arrays.asList("shuttle_train","ferry"));
    private static Set<String> refuse = new HashSet<String>(Arrays.asList(
            "disused","footway","pedestrian","road","construction","unclassified","residential","service","cycleway"));//
    private static boolean filterWay(ReaderWay way) {
        // ignore broken geometry
        if (way.getNodes().size() < 2)
            return false;

        // ignore multipolygon geometry
        if (!way.hasTags())
            return false;

        String highwayValue = way.getTag("highway");
        if (highwayValue == null) {
            if (way.hasTag("route", ferries)) {
                String motorcarTag = way.getTag("motorcar");
                if (motorcarTag == null)
                    motorcarTag = way.getTag("motor_vehicle");

                if (motorcarTag == null && !way.hasTag("foot") && !way.hasTag("bicycle") || "yes".equals(motorcarTag))
                    return true;
            }
            return false;
        }else if(refuse.contains(highwayValue) || highwayValue.isEmpty()){
            return false;
        }

        if ("track".equals(highwayValue)) {
            String tt = way.getTag("tracktype");
            if (tt != null && !tt.equals("grade1") && !tt.equals("grade2") && !tt.equals("grade3"))
                return false;
        }

        if (way.hasTag("impassable", "yes") || way.hasTag("status", "impassable"))
            return false;

        if (!way.hasTag("oneway", "yes")) return false;

        return true;
    }

    public long nearestNodeId(double startLat, double startLon, double startLenThreshold) {
        DistanceCalc distCalc = Helper.DIST_EARTH;
        LinkedList<Double> nearest10dis = new LinkedList<Double>();
        LinkedList<OSMNode> nearest10nodes = new LinkedList<OSMNode>();
        nearest10dis.add(startLenThreshold);
        nearest10nodes.add(null);
        for(OSMNode n: nodeMap.values()){
            double dis = distCalc.calcDist(n.latitude, n.longitude,  startLat, startLon);
            double curMin = nearest10dis.peekLast();
            if(dis<curMin){
                nearest10dis.add(dis);
                nearest10nodes.add(n);
            }
            if(nearest10dis.size()>10){
                nearest10dis.poll();
                nearest10nodes.poll();
            }
        }
        for(int i=0;i<nearest10dis.size();i++){
            System.out.println("DIS:"+nearest10dis.get(i)+" "+nearest10nodes.get(i));
        }
        return nearest10nodes.peekLast().getOsmId();
    }

    public OSMEdge getEdgeById(String edgeIdP) {
        return this.edgeMap.get(edgeIdP);
    }

    public Collection<OSMNode> allNodes() {
        return nodeMap.values();
    }

    public Collection<OSMEdge> allEdges() {
        return edgeMap.values();
    }



    public static class OSMNode
    {
        private List<String> in = new ArrayList<String>();
        private List<String> out = new ArrayList<String>();
        long osmId = -1;
        double latitude = -1;
        double longitude = -1;
        boolean isTower = false;
        boolean isMatched = false;
        private String name;


        OSMNode(long id){
            osmId = id;
        }

        public OSMNode(List<String> inChains, List<String> outChains) {
            this.in = inChains;
            this.out = outChains;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public void setTower(boolean tower) {
            isTower = tower;
        }

        public long getOsmId() {
            return osmId;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public boolean isTower() {
            return isTower;
        }

        @Override
        public String toString() {
            return "OSMNode{" +
                    "name=" + name +
                    ", osmId=" + osmId +
                    ", latitude=" + latitude +
                    ", longitude=" + longitude +
                    ", isTower=" + isTower +
                    ", isMatched=" + isMatched +
                    '}';
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getIn() {
            return in;
        }

        public List<String> getOut() {
            return out;
        }
    }

    public static class OSMEdge
    {
        private  String type;
        private  long wayId;
        private  String name;
        private  long startId;
        private  long endId;
        private  double angle;
        private  double length;
        private boolean fromOSM;
        private OSMNode start;
        private OSMNode end;

        String gridId;
        String chainId;
        private String id;

        OSMEdge(String gridId, String chainId, double length, double angle){
            this.gridId = gridId;
            this.chainId = chainId;
            this.length = length;
            this.angle = angle;
            this.fromOSM = false;
        }

        OSMEdge(long start, long end, long wayId, String wayName, String type)
        {
            this.startId = start;
            this.endId = end;
            this.wayId = wayId;
            this.name = wayName;
            this.fromOSM = true;
            this.type = type;
        }

        public long getWayId() {
            return wayId;
        }

        public String getName() {
            return name;
        }

        public long getStartId() {
            return startId;
        }

        public long getEndId() {
            return endId;
        }

        public void setEnd(OSMNode end) {
            this.end = end;
        }

        public void setStart(OSMNode start) {
            this.start = start;
        }

        public void setLength(double length) {
            this.length = length;
        }

        public void setAngle(double angle) {
            this.angle = angle;
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return "OSMEdge{" +
                    "wayId=" + wayId +
                    ", name='" + name + '\'' +
                    ", startId=" + startId +
                    ", endId=" + endId +
                    ", angle=" + angle +
                    ", length=" + length +
                    ", fromOSM=" + fromOSM +
                    ", start=" + start +
                    ", end=" + end +
                    ", gridId='" + gridId + '\'' +
                    ", chainId='" + chainId + '\'' +
                    '}';
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public double getAngle() {
            return angle;
        }

        public double getLength() {
            return length;
        }
    }
}