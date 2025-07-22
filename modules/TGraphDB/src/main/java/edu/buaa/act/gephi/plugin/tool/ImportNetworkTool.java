package edu.buaa.act.gephi.plugin.tool;

import org.gephi.tools.spi.Tool;
import org.gephi.tools.spi.ToolEventListener;
import org.gephi.tools.spi.ToolSelectionType;
import org.gephi.tools.spi.ToolUI;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

import java.awt.*;
import javax.swing.*;

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
