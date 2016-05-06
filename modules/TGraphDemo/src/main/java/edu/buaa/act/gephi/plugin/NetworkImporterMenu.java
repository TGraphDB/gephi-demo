package edu.buaa.act.gephi.plugin;

import org.act.neo4j.temporal.demo.Config;
import org.act.neo4j.temporal.demo.utils.TransactionWrapper;
import org.gephi.graph.api.*;
import org.gephi.project.api.ProjectController;
import org.gephi.utils.longtask.api.LongTaskExecutor;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.tooling.GlobalGraphOperations;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by song on 16-5-5.
 */

@ActionID(category = "File", id = "org.gephi.desktop.filters.UsingProgressAndCancelAction")
@ActionRegistration(displayName = "#CTL_UsingProgressAndCancelAction")
@ActionReferences({
        @ActionReference(path = "Menu/Plugins", position = 7000)
})
@NbBundle.Messages("CTL_UsingProgressAndCancelAction=Test progress and cancel")
public final class NetworkImporterMenu implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        if (pc.getCurrentProject() == null) {
            notice("no project");
        }
        GraphController gc = Lookup.getDefault().lookup(GraphController.class);
        TimeDependentDijkstraTool tool = Lookup.getDefault().lookup(TimeDependentDijkstraTool.class);
        Config config = tool.config;
        JFileChooser jFileChooser = new JFileChooser(config.dbPath);
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File dbDir;
        if(jFileChooser.showOpenDialog(tool.getUIinstance())== JFileChooser.APPROVE_OPTION){
            dbDir = jFileChooser.getSelectedFile();
            if (!dbDir.exists()) {
                notice("dir not found!");
                return;
            }else{
                GraphDatabaseService db = tool.getDBinstance(dbDir.getAbsolutePath());
                LongTaskExecutor executor = new LongTaskExecutor(true);
                NetworkImporterAsyncTask task = new NetworkImporterAsyncTask(db, gc.getGraphModel());
                executor.execute(task, task, "importing...", null);
                notice("Render Finish:\n"+
                                "File: "+dbDir.getAbsolutePath()+"\n"
                );
            }
        }

    }

    private void notice(String s) {
        DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(s));
    }

    private static class NetworkImporterAsyncTask implements LongTask, Runnable {
        private final GraphModel model;
        private GraphDatabaseService db;

        private ProgressTicket progressTicket;
        private boolean shouldGo=true;


        public NetworkImporterAsyncTask(GraphDatabaseService db, GraphModel model) {
            this.model = model;
            this.db = db;
        }

        @Override
        public void run() {
            final GraphFactory factory = model.factory();
            final Graph graph = model.getGraph();

            new TransactionWrapper() {

//                private Map<Long,org.gephi.graph.api.Node> nodesInGraph= new HashMap<Long, org.gephi.graph.api.Node>();

                private org.gephi.graph.api.Node addNode(Node node){
                    long id = node.getId();
                    org.gephi.graph.api.Node gephiNode = graph.getNode(id);
                    if(gephiNode==null){
                        gephiNode = factory.newNode(id);
                        graph.addNode(gephiNode);
                        return gephiNode;
                    }else{
                        return graph.getNode(id);
                    }
                }

                private void addEdge(org.gephi.graph.api.Node gephiNode,
                                     org.gephi.graph.api.Node gephiNodeNeighbor,
                                     Object length, Object angle) {
                    if(graph.getEdge(gephiNode,gephiNodeNeighbor)==null) {
                        Edge edge = factory.newEdge(gephiNode, gephiNodeNeighbor);
                        edge.setAttribute("length",length);
                        edge.setAttribute("angle",angle);
                        graph.addEdge(edge);
                    }
                }

                public void run() {
                    Progress.start(progressTicket, 150000);
                    for (Node node : GlobalGraphOperations.at(db).getAllNodes()) {
                        if (shouldGo) {
                            org.gephi.graph.api.Node gephiNode = addNode(node);
                            for (Relationship r : node.getRelationships()) {
                                Node neighbor = r.getOtherNode(node);
                                org.gephi.graph.api.Node gephiNodeNeighbor = addNode(neighbor);
                                Object length = r.getProperty("length");
                                Object angle = r.getProperty("angle");
                                addEdge(gephiNode, gephiNodeNeighbor,length,angle);
                            }
                            Progress.progress(progressTicket);
                        }else{
                            break;
                        }
                    }
                    Progress.finish(progressTicket);
                }
            }.start(db);
        }



        @Override
        public boolean cancel() {
            shouldGo = false;
            return true;
        }

        @Override
        public void setProgressTicket(ProgressTicket pt) {
            this.progressTicket = pt;
        }
    }
}

