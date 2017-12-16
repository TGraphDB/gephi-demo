package edu.buaa.act.gephi.plugin.task;

import com.graphhopper.reader.ReaderElement;
import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.osm.OSMInputFile;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.Helper;
import edu.buaa.act.gephi.plugin.utils.GUIHook;
import gnu.trove.list.TLongList;
import org.act.neo4j.temporal.demo.utils.CrossFactory;
import org.act.neo4j.temporal.demo.utils.RoadNetworkStorage;
import org.act.neo4j.temporal.demo.vo.RoadChain;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read source files(Topo.csv) to RoadChain list.
 * Created by song on 16-5-27.
 */
public class ReadNetworkFileTask implements LongTask, Runnable {

    private final File networkFile;
    private ProgressTicket progress;

    public ReadNetworkFileTask(File networkFile){
        this.networkFile = networkFile;
    }
    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;
    }

    @Override
    public void run() {

        final List<RoadChain> roadChainList = new ArrayList<RoadChain>();

        if(this.networkFile.getName().toLowerCase().endsWith("csv")){
            Progress.start(progress, 130000);
            readTopoCSV(roadChainList);
        }else{
            Progress.start(progress, 220000);
            readOSMPbf(roadChainList);
        }
        Progress.finish(progress);
        new GUIHook<List<RoadChain>>(){
            @Override
            public void guiHandler(List<RoadChain> value) {
                onResult(value);
            }
        }.guiHandler(roadChainList);
    }

    private void readOSMPbf(List<RoadChain> roadChainList) {

        PBFReader reader = new PBFReader(this.networkFile, progress);
        reader.firstPhase(roadChainList);
        reader.secondPhase(roadChainList);
    }

    private void readTopoCSV(List<RoadChain> roadChainList) {
        CrossFactory crossFactory = new CrossFactory();

        try{
            BufferedReader br = new BufferedReader(new FileReader(this.networkFile));
            String line;
            for (int lineCount = 0; (line = br.readLine()) != null; lineCount++) {
                if (lineCount == 0) continue;//ignore headers
                try {
                    roadChainList.add(RoadNetworkStorage.getDefault().newRoadChainFromCSV(line, lineCount));
                }catch (RuntimeException e){
                    System.out.println(e.getMessage()+" at line:"+lineCount);
                }
                Progress.progress(progress, "reading "+ lineCount +" lines",1);
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long emptyAllCount = 0;
        long emptyInCount = 0;
        long emptyOutCount = 0;
        long normalRoadCount = 0;
        for (RoadChain roadChain : roadChainList) {
            int inCount = roadChain.getInNum();
            int outCount = roadChain.getOutNum();
            if (inCount == 0 && outCount == 0) {
                emptyAllCount++;
            } else {
                if (inCount == 0 && outCount > 0) {
                    emptyInCount++;
                } else if (inCount > 0 && outCount == 0) {
                    emptyOutCount++;
                } else{
                    normalRoadCount++;
                }
                crossFactory.getStartCross(roadChain);
                crossFactory.getEndCross(roadChain);
            }
        }
    }

    protected void onResult(List<RoadChain> list){

    }

    private static class OSMNode
    {
        long osmId = -1;
        double latitude = -1;
        double longitude = -1;
        boolean isTower = false;

        OSMNode(long id){
            osmId = id;
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
    }

    private static class OSMEdge extends RoadChain
    {
        private final long wayId;
        private final String name;
        private final long startId;
        private final long endId;

        OSMEdge(long start, long end, long wayId, String wayName)
        {
            super("-1", "-1",-1,-1);
            startId = start;
            endId = end;
            this.wayId = wayId;
            this.name = wayName;
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
    }

    public static class PBFReader {
        private final ProgressTicket progress;
        private File osmFile;
        private Map<Long, OSMNode> nodeMap = new HashMap<Long, OSMNode>();

        PBFReader(File osmFile, ProgressTicket progress){
            this.osmFile = osmFile;
            this.progress = progress;
        }

        public void firstPhase(List<RoadChain> edgeList) {
            OSMInputFile in = null;

            long wayCount = 0;
            try {
                in = new OSMInputFile(osmFile).setWorkerThreads(1).open();
                ReaderElement item;
                while ((item = in.getNext()) != null) {
                    if (item.isType(ReaderElement.WAY)) {
                        final ReaderWay way = (ReaderWay) item;
                        boolean valid = filterWay(way);
                        if (valid) {
                            TLongList wayNodes = way.getNodes();
                            long wayId = way.getId();
                            String wayName = way.getTag("name", "");
                            for (int i = 0; i < wayNodes.size(); i++) {
                                long osmNodeId = wayNodes.get(i);
                                if (nodeMap.containsKey(osmNodeId)) {
                                    nodeMap.get(osmNodeId).setTower(true);
                                } else {
                                    nodeMap.put(osmNodeId, new OSMNode(osmNodeId));
                                }

                                if (i > 0) {
                                    long pre = wayNodes.get(i - 1);
                                    edgeList.add(new OSMEdge(pre, osmNodeId, wayId, wayName));
                                }
                            }
                            wayCount++;
                            progress.progress();
                        }
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException("Problem while parsing file", ex);
            } finally {
                Helper.close(in);
            }
        }

        public Map<Long, OSMNode> secondPhase(List<RoadChain> edgeList) {
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
                            n.setLatitude(node.getLat());
                            progress.progress();
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

            for (RoadChain item : edgeList) {
                OSMEdge e = (OSMEdge) item;
                OSMNode start = nodeMap.get(e.getStartId());
                OSMNode end = nodeMap.get(e.getEndId());
                if (start != null && end != null) {
                    double sLat = start.getLatitude();
                    double sLon = start.getLongitude();
                    double eLat = end.getLatitude();
                    double eLon = end.getLongitude();
                    if (!Double.isNaN(sLat) && !Double.isNaN(sLon) && !Double.isNaN(eLat) && !Double.isNaN(eLon)) {
                        double len = distCalc.calcDist(sLat, sLon, eLat, eLon);
                        if (len < 0) throw new RuntimeException("SNH: len<0 at edge index " + e.getWayId());
                        e.setLength(len);
                        totalDis+=len;

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
            return nodeMap;

        }

        private static boolean filterWay(ReaderWay way) {
            // ignore broken geometry
            if (way.getNodes().size() < 2)
                return false;

            // ignore multipolygon geometry
            if (!way.hasTags())
                return false;

            String highwayValue = way.getTag("highway");
            if (highwayValue == null) {
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
    }
}
