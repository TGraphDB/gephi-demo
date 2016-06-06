package edu.buaa.act.gephi.plugin.tool;

import edu.buaa.act.gephi.plugin.gui.TGraphDemoPanelTopComponent;
import org.gephi.graph.api.Node;
import org.gephi.tools.spi.NodeClickEventListener;
import org.gephi.tools.spi.Tool;
import org.gephi.tools.spi.ToolEventListener;
import org.gephi.tools.spi.ToolSelectionType;
import org.gephi.tools.spi.ToolUI;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;


/**
 * Created by song on 16-5-09.
 *
 *
 */

@ServiceProvider(service = Tool.class)
public class TimeDependentPathTool implements Tool{

    private Node start;
    private Node end;
    private String status="NULL";
    
    public boolean setUIStatus(String status){
        if(ui!=null && ui.tips!=null){
            this.status = status;
            if(status.equals("start")){
                ui.tips.setText("Please choose start node.");
            }else if(status.equals("end")){
                ui.tips.setText("Please choose end node.");
            }else{
                ui.tips.setText("Please first press [choose] button in TGraph Panel.");
            }
            return true;
        }else{
            notice("Please choose tool first.");
            return false;
        }
    }


    @Override
    public ToolEventListener[] getListeners() {
        return new ToolEventListener[]{
            new NodeClickEventListener() {
                public void clickNodes(Node[] nodes) {
                    int last = nodes.length-1;
                    if(status.equals("start")){
                        start = nodes[last];
                        start.setColor(Color.red);
//                        start.setZ(20f);
//                        start.setSize(4f);
                        ui.tips.setText("Start node chosen.");
                    }else if(status.equals("end")){
                        end = nodes[last];
                        end.setColor(Color.red);
//                        end.setZ(20f);
//                        end.setSize(4f);
                        ui.tips.setText("End node chosen.");
                    }else{
                        ui.tips.setText(nodes.length+" nodes select");
                    }
                    System.out.println(nodes.length+" nodes. "+status);
                    for(Node node: nodes){
                        System.out.println("x"+node.x()+" y"+node.y()+" z"+node.z());
                    }
                    TGraphDemoPanelTopComponent tGraphPanel = Lookup.getDefault().lookup(TGraphDemoPanelTopComponent.class);
                    if(tGraphPanel!=null)tGraphPanel.onNodeClick(nodes,status);
                }
            }
        };
    }
    
    public Node getStartNode(){
        return start;
    }
    
    public Node getEndNode(){
        return end;
    }

    private TimeDependentPathToolUI ui = null;
    public ToolUI getUI() {
        if(ui==null){
            ui = new TimeDependentPathToolUI();            
        }
        return ui;
    }

    public ToolSelectionType getSelectionType() {
        return ToolSelectionType.SELECTION;
    }

    public void select() {}
    public void unselect() {
        setUIStatus("NULL");
    }

    public void notice(String content){
        DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(content));
    }
    
    private class TimeDependentPathToolUI extends Component implements ToolUI{

        JLabel tips;

        public JPanel getPropertiesBar(Tool tool) {
            JPanel panel = new JPanel();
            tips = new JLabel("Please choose Source Node.");
            panel.add(tips);
            return panel;
        }

        public Icon getIcon() {
            return new ImageIcon(getClass().getResource("/nodeColorManagerIcon16x16.png"));
        }

        public String getName() {
            return "find Time Dependent fastest path";
        }

        public String getDescription() {
            return "By using multiple methods: Dijkstra, A*, ALT";
        }

        public int getPosition() {
            return 1238;
        }
    }
}