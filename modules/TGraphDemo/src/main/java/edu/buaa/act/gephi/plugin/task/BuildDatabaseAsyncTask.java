package edu.buaa.act.gephi.plugin.task;

import edu.buaa.act.gephi.plugin.utils.Result;
import org.act.neo4j.temporal.demo.utils.Helper;
import org.act.neo4j.temporal.demo.utils.TransactionWrapper;
import org.act.neo4j.temporal.demo.vo.Cross;
import org.act.neo4j.temporal.demo.vo.RelType;
import org.act.neo4j.temporal.demo.vo.RoadChain;
import org.act.neo4j.temporal.demo.vo.TemporalStatus;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Build a TGraph Demo database from scratch, using Network Topological file
 * and traffic status file.
 * Created by song on 16-5-27.
 */
public class BuildDatabaseAsyncTask implements Runnable, LongTask {

    private List<RoadChain> roadList;
    private List<File> dataFileList;
    private String dbPath;
    private GraphDatabaseService db;
    private ProgressTicket progress;
    private volatile boolean shouldGo=true;
    private int[] maxT;
    private int[] minT;
    private int[] dataCount;

    public BuildDatabaseAsyncTask(List<RoadChain> roadList, List<File> dataFileList, File dbDirectory){
        this.roadList = roadList;
        this.dataFileList = dataFileList;
        this.dbPath = dbDirectory.getAbsolutePath();
        this.maxT = new int[roadList.size()];
        this.minT = new int[roadList.size()];
        this.dataCount = new int[roadList.size()];
        Arrays.fill(minT, Integer.MAX_VALUE);
    }
    @Override
    public boolean cancel() {
        shouldGo=false;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("TGraph.build.database");
        Progress.start(progress, roadList.size());
        Progress.setDisplayName(progress, "building network structure...");
        for(RoadChain roadChain: roadList){
            roadChain.updateNeighbors();
            Progress.progress(progress);
        }
        Progress.switchToIndeterminate(progress);
        Progress.setDisplayName(progress, "Creating new empty database...");
        db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(dbPath)
                .loadPropertiesFromFile("")
                .newGraphDatabase();
        Progress.setDisplayName(progress, "importing road network into database...");
        Progress.switchToDeterminate(progress, roadList.size());
//        Progress.
        Result result = new Result();
        importRoadNetwork(result);
        Progress.setDisplayName(progress, "importing traffic data into database...");
        Progress.switchToDeterminate(progress, dataFileList.size());
        importTrafficData(result);
        Progress.setDisplayName(progress, "finishing...");
        Progress.switchToDeterminate(progress, roadList.size());
        setStatisticData(result);
        Progress.setDisplayName(progress, "shutting down database...");
        Progress.switchToIndeterminate(progress);
        db.shutdown();
        Progress.finish(progress);
    }

    private void setStatisticData(Result result) {
        new TransactionWrapper<Result>(){
            @Override
            public void runInTransaction() {
                for(Relationship r: GlobalGraphOperations.at(db).getAllRelationships()){
                    int id = ((int) r.getId());
                    r.setProperty("max-time",maxT[id]);
                    r.setProperty("min-time",minT[id]);
                    r.setProperty("data-count",dataCount[id]);
                    Progress.progress(progress);
                }
            }
        }.start(db);
    }

    private void importTrafficData(final Result result) {
        for (int i = 0; i < dataFileList.size(); i++) {
            if(!shouldGo){
                return;
            }
            final File file = dataFileList.get(i);
            final int time = Helper.timeStr2int(file.getName().substring(9, 21));
            new TransactionWrapper<Result>(){
                @Override
                public void runInTransaction() {
                    BufferedReader br = null;
                    try {
                        br = new BufferedReader(new FileReader(file));
                        String line;
                        for (int lineCount = 0; (line = br.readLine()) != null; lineCount++) {
                            if (lineCount == 0) continue;
                            if (!shouldGo){
                                br.close();
                                return;
                            }
                            TemporalStatus temporalStatus = new TemporalStatus(line);
                            RoadChain roadChain = RoadChain.get(temporalStatus.gridId, temporalStatus.chainId);
                            if (roadChain.getInNum() > 0 || roadChain.getOutNum() > 0) {
                                Relationship r = roadChain.getRelationship(db);
                                if (r != null) {
                                    int dCount = dataCount[((int) r.getId())];
                                    if(dCount==0){
                                        r.createDynProperty("travel-time", time, 4, temporalStatus.getTravelTime());
                                        r.createDynProperty("full-status", time, 4, temporalStatus.getFullStatus());
                                        r.createDynProperty("vehicle-count", time, 4, temporalStatus.getVehicleCount());
                                        r.createDynProperty("segment-count", time, 4, temporalStatus.getSegmentCount());
                                        r.createDynProperty("temporal-point", 1, 4, time); // count from 1.
                                    }else {
                                        r.setDynProperty("travel-time", time, temporalStatus.getTravelTime());
                                        r.setDynProperty("full-status", time, temporalStatus.getFullStatus());
                                        r.setDynProperty("vehicle-count", time, temporalStatus.getVehicleCount());
                                        r.setDynProperty("segment-count", time, temporalStatus.getSegmentCount());
                                        r.setDynProperty("temporal-point", dCount+1, time);
                                    }
                                    updateRoadMinMaxTime(r.getId(), time);
                                    result.inc("time-point-data-count");
                                    dataCount[((int) r.getId())]++;
                                } else {
                                    System.out.println(roadChain);
                                }
                            } else {
                                continue;
                            }
                        }
                        br.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start(db);
            result.inc("file-import");
            Progress.progress(progress);
            Progress.progress(progress, result.get("time-point-data-count")+" time point data imported.("+result.get("file-import")+" files)");
        }
    }

    private void updateRoadMinMaxTime(long rid, int time) {
        int id = (int) rid;
        int min = minT[id];
        int max = maxT[id];
        if(min>time) minT[id] = time;
        if(max<time) maxT[id] = time;
    }

    private void importRoadNetwork(final Result result){
        int totalPartCount=roadList.size()/2000;
        for(int ith=1; ith<=totalPartCount; ith++) {
            if(!shouldGo) return;
            final int[] tmp = Helper.calcSplit(ith, totalPartCount, roadList.size());
            new TransactionWrapper<Map<String,Integer>>(){
                @Override
                public void runInTransaction() {
                    for (int i = tmp[0]; i <= tmp[1]; i++) {
                        if(!shouldGo) return;
                        RoadChain roadChain = roadList.get(i);
                        int inCount = roadChain.getInNum();
                        int outCount = roadChain.getOutNum();
                        if (inCount == 0 && outCount == 0) {
                            result.inc("emptyAllCount");
                        } else{
                            if (inCount == 0 && outCount > 0) {
                                result.inc("emptyInCount");
                            } else if (inCount > 0 && outCount == 0) {
                                result.inc("emptyOutCount");
                            }
                            result.inc("normalRoadCount");
                            Cross inCross = Cross.getStartCross(roadChain);
                            Cross outCross = Cross.getEndCross(roadChain);
                            Node inNode, outNode;
                            if (inCross.getNode(db) == null) {
                                inNode = db.createNode();
                                inCross.setNode(inNode);
                                inNode.setProperty("cross-id", inCross.id);
                            } else {
                                inNode = inCross.getNode(db);
                            }
                            if (outCross.getNode(db) == null) {
                                outNode = db.createNode();
                                outCross.setNode(outNode);
                                outNode.setProperty("cross-id", outCross.id);
                            } else {
                                outNode = outCross.getNode(db);
                            }

                            Relationship r = inNode.createRelationshipTo(outNode, RelType.ROAD_TO);
                            r.setProperty("uid", roadChain.getUid());
                            r.setProperty("grid-id", roadChain.getGridId());
                            r.setProperty("chain-id", roadChain.getChainId());
                            r.setProperty("type", roadChain.getType());
                            r.setProperty("length", roadChain.getLength());
                            r.setProperty("angle", roadChain.getAngle());
                            r.setProperty("in-count", roadChain.getInNum());
                            r.setProperty("out-count", roadChain.getOutNum());
                            r.setProperty("in-roads", roadChain.briefInChain());
                            r.setProperty("out-roads", roadChain.briefOutChain());
                            r.setProperty("data-count", 0);
                            r.setProperty("min-time", Integer.MAX_VALUE);
                            r.setProperty("max-time", 0);
                            roadChain.setRelationship(r);
                            Progress.progress(progress);
                            Progress.progress(progress,result.get("normalRoadCount")+" road imported.");
                        }
                    }
                }
            }.start(db);
            result.inc("txCount");
        }
    }
}
