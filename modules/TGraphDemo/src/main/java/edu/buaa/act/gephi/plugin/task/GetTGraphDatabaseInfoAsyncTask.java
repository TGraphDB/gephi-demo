package edu.buaa.act.gephi.plugin.task;

import edu.buaa.act.gephi.plugin.utils.GUIHook;
import org.act.neo4j.temporal.demo.utils.Hook;
import org.act.neo4j.temporal.demo.utils.TransactionWrapper;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by song on 16-5-12.
 */
public class GetTGraphDatabaseInfoAsyncTask extends TransactionWrapper<Map<String, String>> implements LongTask, Runnable{


    private GraphDatabaseService db;
    private ProgressTicket progress;

    public GetTGraphDatabaseInfoAsyncTask(GraphDatabaseService db) {
        this.db=db;
    }

    @Override
    public void runInTransaction() {
        long nodeCounts = 0;
        long relationshipCounts =0;
        for (Node node : GlobalGraphOperations.at(db).getAllNodes()) {
//            Progress.progress(progress);
            nodeCounts++;
        }
        for (Relationship r : GlobalGraphOperations.at(db).getAllRelationships()) {
//            Progress.progress(progress);
            relationshipCounts++;
        }
        HashMap<String, String> result = new HashMap<String, String>();
        result.put("node-count",String.valueOf(nodeCounts));
        result.put("edge-count",String.valueOf(relationshipCounts));
        setReturnValue(result);
    }

    @Override
    public void run() {
        Progress.start(progress);
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
