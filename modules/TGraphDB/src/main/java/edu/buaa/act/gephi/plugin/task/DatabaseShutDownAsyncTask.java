package edu.buaa.act.gephi.plugin.task;

import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * Shutdown database in background because sometimes it is to slow.
 * Created by song on 16-6-3.
 */
public class DatabaseShutDownAsyncTask implements LongTask, Runnable {
    GraphDatabaseService db;
    private ProgressTicket p;

    public DatabaseShutDownAsyncTask(GraphDatabaseService db){
        this.db = db;
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.p = progressTicket;
    }

    @Override
    public void run() {
        Progress.start(p);
        Progress.setDisplayName(p, "TGraph Shutting Down...");
        this.db.shutdown();
        Progress.finish(p);
        onFinish();
    }

    public void onFinish() {

    }
}
