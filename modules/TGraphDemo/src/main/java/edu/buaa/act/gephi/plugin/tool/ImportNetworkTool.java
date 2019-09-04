package edu.buaa.act.gephi.plugin.tool;

import edu.buaa.act.gephi.plugin.task.CorrectNetworkAsyncTask;
import edu.buaa.act.gephi.plugin.task.LoadOSMDataAsyncTask;
import edu.buaa.act.gephi.plugin.task.OSMVisualizeAsyncTask;
import edu.buaa.act.gephi.plugin.utils.OSMStorage;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.Node;
import org.gephi.tools.spi.NodeClickEventListener;
import org.gephi.tools.spi.Tool;
import org.gephi.tools.spi.ToolEventListener;
import org.gephi.tools.spi.ToolSelectionType;
import org.gephi.tools.spi.ToolUI;
import org.gephi.utils.longtask.api.LongTaskExecutor;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

/**
 * Created by song on 2019-7-30.
 *
 * Help Chen Hanqing to import network from specifies node id
 */

@ServiceProvider(service = Tool.class)
public class ImportNetworkTool implements Tool {
    private ImportNetToolUI ui;

    @Override
    public void select() {}
    @Override
    public void unselect() {}

    @Override
    public ToolEventListener[] getListeners() {
        return new ToolEventListener[]{

        };
    }

    @Override
    public ToolUI getUI() {
        if(ui==null){
            ui = new ImportNetToolUI();
        }
        return ui;
    }

    public long getNodeId(){
        long nodeId = Long.valueOf( ui.importFromNodeId.getText() );
        return nodeId;
    }

    @Override
    public ToolSelectionType getSelectionType() {
        return ToolSelectionType.SELECTION;
    }


    private class ImportNetToolUI extends Component implements ToolUI{

        public JTextField importFromNodeId;

        @Override
        public JPanel getPropertiesBar(Tool tool) {
            JPanel panel = new JPanel();
            importFromNodeId = new JTextField("51849", 20);
            panel.add(new JLabel("Draw network from node id:"));
            panel.add(importFromNodeId);
            return panel;
        }

        @Override
        public Icon getIcon() {
            return ImageUtilities.loadImageIcon("cross.png", false);
        }

        @Override
        public String getDescription() {
            return "import network from specified point";
        }

        @Override
        public int getPosition() {
            return 1438;
        }

    }
}
