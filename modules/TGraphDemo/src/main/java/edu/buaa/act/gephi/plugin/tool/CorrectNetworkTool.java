package edu.buaa.act.gephi.plugin.tool;

import edu.buaa.act.gephi.plugin.task.CorrectNetworkAsyncTask;
import edu.buaa.act.gephi.plugin.task.LoadOSMDataAsyncTask;
import edu.buaa.act.gephi.plugin.task.OSMVisualizeAsyncTask;
import edu.buaa.act.gephi.plugin.utils.OSMStorage;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.Node;
import org.gephi.tools.spi.*;
import org.gephi.utils.longtask.api.LongTaskExecutor;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.concurrent.atomic.AtomicBoolean;

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
    File mappingFile;

    AtomicBoolean isRunning = new AtomicBoolean(false);

    OSMStorage osmStorage;

    @Override
    public void select() {}
    @Override
    public void unselect() {}

    @Override
    public ToolEventListener[] getListeners() {
        return new org.gephi.tools.spi.ToolEventListener[]{
                new NodeClickEventListener() {
                    public void clickNodes(Node[] nodes) {
                        int last = nodes.length-1;
                        if(node!=null) node.setColor(Color.BLACK);
                        node = nodes[last];
                        node.setColor(Color.red);
//                        start.setZ(20f);
//                        start.setSize(4f);
                        ui.note.setText("node chosen");
                        System.out.println(nodes.length+" nodes. ");
                        for(Node node: nodes){
                            System.out.println("x"+node.x()+" y"+node.y()+" z"+node.z());
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
        private JButton selectCorrectMappingFileBtn;

        @Override
        public JPanel getPropertiesBar(Tool tool) {
            JPanel panel = new JPanel();
            note = new JLabel("Correct Road with OSM data (hover for more tips)");
            selectOSMFileBtn = new JButton("Browse.");
            selectCorrectMappingFileBtn = new JButton("Mapping file...");
            latitudeTxt = new JTextField("latitude", 20);
            longitudeTxt = new JTextField("longitude", 20);
            startCorrectBtn = new JButton("Correct");

            selectOSMFileBtn.setToolTipText(
                    "1. browse osm data.\n" +
                            "2. choose a node from graph.\n" +
                            "3. set latitude and longitude.\n" +
                            "4. press the 'Correct' button");

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

            selectCorrectMappingFileBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    Button_selectCorrectMappingMouseClicked(evt);
                }
            });

            panel.add(note);
            panel.add(selectOSMFileBtn);
            panel.add(latitudeTxt);
            panel.add(longitudeTxt);
            panel.add(selectCorrectMappingFileBtn);
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

            JFileChooser fc = new JFileChooser();
//        fc.setFileFilter(new TGraphDatabaseFolderFilter());
            fc.setDialogTitle("choose OSM file (e.g. beijing_around.pbf)");
            fc.setDialogType(JFileChooser.OPEN_DIALOG);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                final File osmFile = fc.getSelectedFile();
                if (osmFile.exists()) {
                    LoadOSMDataAsyncTask task = new LoadOSMDataAsyncTask(osmFile){
                        @Override
                        public void guiHandler(final OSMStorage value) {
                            osmStorage = value;
                            ui.note.setText("OSM File chosen");
                            if(latitudeTxt.getText().equals("vis") && longitudeTxt.getText().equals("osm")){
                                GraphController gc = Lookup.getDefault().lookup(GraphController.class);
                                OSMVisualizeAsyncTask t = new OSMVisualizeAsyncTask(gc.getGraphModel(), osmStorage){
                                    @Override
                                    public void guiHandler(Object value) {
                                        isRunning.set(false);
                                        ui.note.setText("visualization finish.");
                                    }
                                };
                                new LongTaskExecutor(true).execute(t, t, "visualizing...", null);
                            }else{
                                isRunning.set(false);
                            }
                        }
                    };
                    isRunning.set(true);
                    new LongTaskExecutor(true).execute(task, task, "reading...", null);

                }else{
                    throw new RuntimeException("TGraph: file not exist");
                }
            }
        }


        private void Button_selectCorrectMappingMouseClicked(MouseEvent evt)
        {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new MappingFileFilter());
            fc.setDialogTitle("choose OSM file (e.g. beijing_around.pbf)");
            fc.setDialogType(JFileChooser.OPEN_DIALOG);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                final File file = fc.getSelectedFile();
                if (file.exists()) {
                    mappingFile = file;
                    ui.note.setText("mapping file chosen");
                }else{
                    throw new RuntimeException("TGraph: file not exist");
                }
            }
        }

        private void Button_beginCorrectMouseClicked(MouseEvent evt)
        {
            if (!isRunning.get() && osmStorage != null)
            {
                GraphController gc = Lookup.getDefault().lookup(GraphController.class);
                CorrectNetworkAsyncTask task = null;

                if (latitudeTxt.getText().equals("file") && mappingFile != null)
                {
                    task = new CorrectNetworkAsyncTask(
                            gc.getGraphModel(),
                            osmStorage,
                            mappingFile)
                    {
                        @Override
                        public void guiHandler(Object value)
                        {
                            isRunning.set(false);
                            ui.note.setText("correct finish.");
                        }
                    };
                    isRunning.set(true);
                    new LongTaskExecutor(true).execute(task, task, "reading...", null);
                } else if (node != null)
                {
                    double lat = Double.parseDouble(latitudeTxt.getText());
                    double lon = Double.parseDouble(longitudeTxt.getText());
                    task = new CorrectNetworkAsyncTask(
                            gc.getGraphModel(),
                            osmStorage,
                            node,
                            lat, lon)
                    {
                        @Override
                        public void guiHandler(Object value)
                        {
                            isRunning.set(false);
                            ui.note.setText("correct finish.");
                        }
                    };
                    isRunning.set(true);
                    new LongTaskExecutor(true).execute(task, task, "reading...", null);
                } else
                {
                    ui.note.setText("please select node from graph or select mapping file");
                }
            } else if (isRunning.get())
            {
                ui.note.setText("busy running, please wait");
            } else
            { // osmStorage==null
                ui.note.setText("please browse osm data");
            }
        }



    }

    private class MappingFileFilter extends FileFilter{

        @Override
        public boolean accept(File f)
        {
            if(f.exists() && f.isDirectory()){
                return true;
            }else if(f.getName().toLowerCase().endsWith(".mapping")){
                return true;
            }else{
                return false;
            }
        }

        @Override
        public String getDescription()
        {
            return "only show valid mapping files";
        }
    }
}
