package edu.buaa.act.gephi.plugin.task;

import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

import java.awt.Color;

/**
 * Clear all path highlighted by TimeDependentDijkstraOneTransaction.
 * Created by song on 16-6-3.
 */
public class ClearAllPathAsyncTask implements LongTask, Runnable{
    final private Graph graph;
    private ProgressTicket p;

    public ClearAllPathAsyncTask(Graph graph){
        this.graph = graph;
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
        final Color color = new Color(0x909090);
        for(Edge edge: graph.getEdges()){
            edge.setWeight(1f);
            edge.setColor(color);
        }
        for(Node node: graph.getNodes()){
            node.setLabel("");
        }
        Progress.finish(p);
    }
}
