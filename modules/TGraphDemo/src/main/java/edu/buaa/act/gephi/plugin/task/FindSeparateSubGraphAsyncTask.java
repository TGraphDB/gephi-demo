package edu.buaa.act.gephi.plugin.task;

import org.act.neo4j.temporal.demo.algo.TGraphTraversal;
import org.act.neo4j.temporal.demo.utils.TransactionWrapper;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Find disconnected sub-graph in database.
 * this class exist because the source data we have do not have x,y axis.
 * so only connected roads can we know(compute) its position.
 * Created by song on 16-5-12.
 */

public class FindSeparateSubGraphAsyncTask extends TransactionWrapper<List<FindSeparateSubGraphAsyncTask.IsolatedNetworkInfo>> implements LongTask, Runnable  {
    private final int totalNodes;
    private ProgressTicket progress;
    private GraphDatabaseService db;
    public FindSeparateSubGraphAsyncTask(GraphDatabaseService db, int totalNodes) {
        this.db = db;
        this.totalNodes = totalNodes;
    }


    @Override
    public void runInTransaction() {
        Progress.start(progress,totalNodes);
        final Set<Long> visited = new HashSet<Long>();
        List<IsolatedNetworkInfo> result = new ArrayList<IsolatedNetworkInfo>();
        TGraphTraversal traversal = new TGraphTraversal(db);

        for (Node node : GlobalGraphOperations.at(db).getAllNodes()) {
            if (!visited.contains(node.getId())) {
                final IsolatedNetworkInfo categoryResult = new IsolatedNetworkInfo();
                categoryResult.setStartNodeId(node.getId());
                traversal.DFS(node, visited, new TGraphTraversal.DFSAction<Node>() {
                    public boolean visit(Node node) {
                        Progress.progress(progress);
                        categoryResult.incNodeCount();
                        return true;
                    }
                }, false);
                result.add(categoryResult);
            }
        }
        Collections.sort(result, new Comparator<IsolatedNetworkInfo>(){
            public int compare(IsolatedNetworkInfo t, IsolatedNetworkInfo t1) {
                if(t.getNodeCount()<t1.getNodeCount()){
                    return 1;
                }else if(t.getNodeCount()==t1.getNodeCount()){
                    return 0;
                }else{
                    return -1;
                }
            }
        });
        setReturnValue(result);
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

    @Override
    public void run() {
        this.start(db);
    }

    public static class IsolatedNetworkInfo{
        private int nodeCount;
        private int edgeCount;
        private long startNodeId;

        public IsolatedNetworkInfo() {
            this.edgeCount = 0;
            this.nodeCount = 0;
        }

        public int getNodeCount() {
            return nodeCount;
        }

        public int getEdgeCount() {
            return edgeCount;
        }

        public long getStartNodeId() {
            return startNodeId;
        }

        public void incNodeCount() {
            this.nodeCount++;
        }

        public void incEdgeCount() {
            this.edgeCount++;
        }

        public void setStartNodeId(long startNodeId) {
            this.startNodeId = startNodeId;
        }
    }

}
