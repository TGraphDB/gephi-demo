package edu.buaa.act.gephi.plugin.tool;

import edu.buaa.act.gephi.plugin.task.CorrectNetworkAsyncTask;
import edu.buaa.act.gephi.plugin.task.LoadOSMDataAsyncTask;
import edu.buaa.act.gephi.plugin.utils.OSMStorage;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.Node;
import org.gephi.tools.spi.*;
import org.gephi.utils.longtask.api.LongTaskExecutor;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * Created by song on 17-12-17.
 *
 * Used for correct mass in road network which is draw from Topo.csv
 * by applying OpenStreetMap data to it.
 *
 * Before all, press this tool's icon to start it.
 * First, load OSM file (beijing_around.pbf e.g.)
 * Second, choose node whose GPS coordinate can be found on http://openstreetmap.org , enter its GPS coordinate
 * Third, press 'Correct' button. This would start an process of correction as background task.
 */

@ServiceProvider(service = Tool.class)
public class CorrectNetworkTool implements Tool {

    CorrectNetToolUI ui;
    Node node;

    byte status = 0;
    byte STATUS_INIT=0;
    byte STATUS_DATA_LOAD=1;
    byte STATUS_READY=2;
    byte STATUS_BUSY=3;

    OSMStorage osmStorage;

    @Override
    public void select() {
        //
    }

    @Override
    public void unselect() {
        //
    }

    @Override
    public ToolEventListener[] getListeners() {
        return new org.gephi.tools.spi.ToolEventListener[]{
                new NodeClickEventListener() {
                    public void clickNodes(Node[] nodes) {
                        int last = nodes.length-1;
                        if(status>=STATUS_DATA_LOAD && status!=STATUS_BUSY){
                            node = nodes[last];
                            node.setColor(Color.red);
//                        start.setZ(20f);
//                        start.setSize(4f);
                            ui.note.setText("Start node chosen.");
                            System.out.println(nodes.length+" nodes. "+status);
                            for(Node node: nodes){
                                System.out.println("x"+node.x()+" y"+node.y()+" z"+node.z());
                            }
                        }
                    }
                }
        };
    }

    @Override
    public ToolUI getUI() {
        if(ui==null){
            ui = new CorrectNetToolUI();
        }
        return ui;
    }

    @Override
    public ToolSelectionType getSelectionType() {
        return ToolSelectionType.SELECTION;
    }




    private class CorrectNetToolUI extends Component implements ToolUI{

        JLabel note;
        JTextField latitudeTxt;
        JTextField longitudeTxt;
        JButton selectOSMFileBtn;
        JButton startCorrectBtn;

        @Override
        public JPanel getPropertiesBar(Tool tool) {
            JPanel panel = new JPanel();
            note = new JLabel("STEP1: select OSM file");
            selectOSMFileBtn = new JButton("Browse.");
            latitudeTxt = new JTextField("latitude");
            longitudeTxt = new JTextField("longitude");
            startCorrectBtn = new JButton("Correct");

            selectOSMFileBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    Button_chooseOSMFileMouseClicked(evt);
                }
            });

            startCorrectBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    Button_beginCorrectMouseClicked(evt);
                }
            });


            panel.add(note);
            panel.add(selectOSMFileBtn);
            panel.add(latitudeTxt);
            panel.add(longitudeTxt);
            panel.add(startCorrectBtn);
            return panel;
        }


        @Override
        public Icon getIcon() {
            return ImageUtilities.loadImageIcon("neo4j-logo-2015.png", false);
        }

        @Override
        public String getDescription() {
            return "correct network from select point with OSM data";
        }

        @Override
        public int getPosition() {
            return 1338;
        }

        private void Button_chooseOSMFileMouseClicked(MouseEvent evt) {
            byte preStatus = status;
            status = STATUS_BUSY;
            JFileChooser fc = new JFileChooser();
//        fc.setFileFilter(new TGraphDatabaseFolderFilter());
            fc.setDialogTitle("choose OSM file (e.g. beijing_around.pbf)");
            fc.setDialogType(JFileChooser.OPEN_DIALOG);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File osmFile = fc.getSelectedFile();
                if (osmFile.exists()) {
                    LoadOSMDataAsyncTask task = new LoadOSMDataAsyncTask(osmFile){
                        @Override
                        public void guiHandler(final OSMStorage value) {
                            osmStorage = value;
                            status = STATUS_READY;
                        }
                    };
                    new LongTaskExecutor(true).execute(task, task, "reading...", null);
                }else{
                    throw new RuntimeException("TGraph: file not exist");
                }
            }else{
                status = preStatus;
            }
        }


        private void Button_beginCorrectMouseClicked(MouseEvent evt) {
            if(status>=STATUS_READY && status!=STATUS_BUSY){
                status = STATUS_BUSY;
                double lat = Double.parseDouble(latitudeTxt.getText());
                double lon = Double.parseDouble(longitudeTxt.getText());
                GraphController gc = Lookup.getDefault().lookup(GraphController.class);
                CorrectNetworkAsyncTask task = new CorrectNetworkAsyncTask(
                        gc.getGraphModel(),
                        osmStorage,
                        node,
                        lat, lon ){
                    @Override
                    public void guiHandler(Object value) {
                        status = STATUS_READY;
                    }
                };
                new LongTaskExecutor(true).execute(task, task, "reading...", null);
            }
        }



    }
}
