package edu.buaa.act.gephi.plugin.task;

import edu.buaa.act.gephi.plugin.utils.GUIHook;
import org.act.neo4j.temporal.demo.utils.Helper;
import org.act.neo4j.temporal.demo.utils.Hook;
import org.act.neo4j.temporal.demo.utils.TransactionWrapper;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Traverse the whole graph to get some properties.
 * Created by song on 16-5-12.
 */
public class GetTGraphDatabaseInfoAsyncTask extends TransactionWrapper<Map<String, Object>> implements LongTask, Runnable{

    private String dbPath;
    private GraphDatabaseService db;
    private ProgressTicket progress;

    public GetTGraphDatabaseInfoAsyncTask(File dbPath) {
        this.dbPath=dbPath.getAbsolutePath();
    }

    @Override
    public void runInTransaction() {
        int nodeCounts = 0;
        int relationshipCounts =0;
        for (Node node : GlobalGraphOperations.at(db).getAllNodes()) {
            Progress.progress(progress,nodeCounts+" node counted.");
            nodeCounts++;
        }
        int minTimeOfNetwork = Integer.MAX_VALUE-1;
        int maxTimeOfNetwork = 0;
        for (Relationship r : GlobalGraphOperations.at(db).getAllRelationships()) {
            Integer minT = (Integer) r.getProperty("min-time");
            Integer maxT = (Integer) r.getProperty("max-time");
            if(minT!=null && maxT!=null){
                if(minT<minTimeOfNetwork) minTimeOfNetwork = minT;
                if(maxT>maxTimeOfNetwork) maxTimeOfNetwork = maxT;
            }
            if(relationshipCounts%100==0){
                Progress.progress(progress, relationshipCounts + " edge counted. min time:" +
                                Helper.timeStamp2String(minTimeOfNetwork) + " max time:" +
                                Helper.timeStamp2String(maxTimeOfNetwork)
                );
            }
            relationshipCounts++;
        }
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("node-count", nodeCounts);
        result.put("edge-count", relationshipCounts);
        result.put("graph-min-time", minTimeOfNetwork);
        result.put("graph-max-time", maxTimeOfNetwork);
        result.put("db-instance", db);
        setReturnValue(result);
    }

    @Override
    public void run() {
        Progress.start(progress);
        Progress.switchToIndeterminate(progress);
        Progress.setDisplayName(progress, "TGraph: Connecting to database...");
        Progress.progress(progress, "DB Path: "+dbPath);
//        File configFile = new File("/tmp/TGraphDemo.neo4j.conf");
//        if(!configFile.exists()){
//            try {
//                configFile.createNewFile();
//                Writer w = new BufferedWriter(new FileWriter(configFile));
//                w.write("allow_store_upgrade=true");
//                w.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(dbPath).setConfig("allow_store_upgrade", "true")
//                .loadPropertiesFromFile("/tmp/TGraphDemo.neo4j.conf")
                .newGraphDatabase();
        Progress.setDisplayName(progress, "TGraph: Getting information from database...");
        this.start(db);
        Progress.finish(progress);
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;
    }

}
