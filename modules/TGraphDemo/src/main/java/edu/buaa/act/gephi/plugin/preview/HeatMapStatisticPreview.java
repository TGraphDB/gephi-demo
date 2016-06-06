package edu.buaa.act.gephi.plugin.preview;

import edu.buaa.act.gephi.plugin.exception.BackgroundTaskErrorHandler;
import static edu.buaa.act.gephi.plugin.preview.HeatMapRenderer.HEATMAP_IMG;
import edu.buaa.act.gephi.plugin.task.RenderPreProcessSyncTask;
import org.gephi.preview.api.CanvasSize;
import org.gephi.preview.api.G2DTarget;
import org.gephi.preview.api.Item;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.RenderTarget;
import org.gephi.preview.plugin.builders.EdgeBuilder;
import org.gephi.preview.plugin.items.EdgeItem;
import org.gephi.preview.spi.ItemBuilder;
import org.gephi.preview.spi.Renderer;
import org.gephi.utils.longtask.api.LongTaskExecutor;
import org.neo4j.graphdb.GraphDatabaseService;
import org.openide.util.lookup.ServiceProvider;

/**
 * Created by song on 16-5-21.
 *
 * Renderer for heat map effect. on top of other renders.
 */
@ServiceProvider(service = Renderer.class, position = 600)
public class HeatMapStatisticPreview implements Renderer{
    public final String TGRAPH_STATISTIC = "edge.tgraph.enable";
    public final String WINDOW_SIZE = "edge.tgraph.window.size";
    public final String START_TIME = "edge.tgraph.start.time";

    private GraphDatabaseService db;
    private HeatMapRenderer heatMapRenderer = new HeatMapRenderer();
    private int startTime = 0;//1288821600; //default 2010-11-04 06:00
    private int winSize = 0;//3600; // default 1 hour.(unit:seconds) range [1800, 3600*24*30(one month)]
    private int shadowSize = 0;//130; // default 100 meter (unit: meter) range [100, 10000(10km)]
    private int imageScale = 0; //

    @Override
    public String getDisplayName() {
        return "TGraph Window HeatMap";
    }

    public void setDB(GraphDatabaseService db){
        this.db = db;
    }

    @Override
    public void preProcess(final PreviewModel previewModel) {
        final PreviewProperties properties = previewModel.getProperties();

        if (properties.getBooleanValue(TGRAPH_STATISTIC) && db!=null &&
                startTime>0 && winSize>0 && shadowSize>0 && imageScale>0) {
            RenderPreProcessSyncTask task =
                    new RenderPreProcessSyncTask(
                            db,
                            previewModel.getItems(Item.EDGE),
                            heatMapRenderer,
                            startTime,
                            winSize,
                            shadowSize,
                            imageScale/1000d);
            new LongTaskExecutor(false).execute(task, task,
                    "TGraph: HeatMap Rendering...",
                    BackgroundTaskErrorHandler.instance());
        }
    }




    @Override
    public void render(Item item, RenderTarget target, PreviewProperties properties) {
        if (target instanceof G2DTarget) {
            heatMapRenderer.secondPhaseRender(item, ((G2DTarget) target).getGraphics());
        }else{
            // will not implement.
        }
    }

    @Override
    public PreviewProperty[] getProperties() {
        return new PreviewProperty[]{
                PreviewProperty.createProperty(
                        this,
                        TGRAPH_STATISTIC,
                        Boolean.class,
                        "Show heat map effect",
                        "Temperature of traffic in a time window",
                        PreviewProperty.CATEGORY_EDGES
                ).setValue(false),
                PreviewProperty.createProperty(
                        this,
                        WINDOW_SIZE,
                        Integer.class,
                        "window size",
                        "specific window size (hour) of statistic",
                        PreviewProperty.CATEGORY_EDGES
                ).setValue(3600),
                PreviewProperty.createProperty(
                        this,
                        START_TIME,
                        Integer.class,
                        "start time",
                        "begin timestamp(seconds) of statistic",
                        PreviewProperty.CATEGORY_EDGES
                ).setValue(1288821600) //2010-11-04 06:00
        };
    }

    @Override
    public boolean isRendererForitem(Item item, PreviewProperties properties) {
        return item instanceof EdgeItem &&
                properties.getBooleanValue(TGRAPH_STATISTIC) &&
                !properties.getBooleanValue(PreviewProperty.MOVING) &&
                item.getData(HEATMAP_IMG)!=null;
    }

    /**
     * This method is called once when press Preview Button.
     * Item is build if any render need them.
     */
    @Override
    public boolean needsItemBuilder(ItemBuilder itemBuilder, PreviewProperties properties) {
        return itemBuilder instanceof EdgeBuilder &&
                properties.getBooleanValue(TGRAPH_STATISTIC) &&
                !properties.getBooleanValue(PreviewProperty.MOVING);
    }

    @Override
    public CanvasSize getCanvasSize(Item item, PreviewProperties previewProperties) {
        return heatMapRenderer.getCanvasSize(item,previewProperties);
    }

    public void setStartTime(int timeStamp) {
        this.startTime = timeStamp;
        System.out.println("start time:"+timeStamp);
    }

    public void setWindowSize(int winSize) {
        this.winSize = winSize;
        System.out.println("window size:"+winSize);
    }

    public void setShadowSize(int shadowSize) {
        this.shadowSize = shadowSize;
        System.out.println("shadow size:"+shadowSize);
    }

    public void setImageScale(int scale) {
        this.imageScale = scale;
        System.out.println("image scale:"+scale);
    }
}
