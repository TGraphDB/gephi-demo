package edu.buaa.act.gephi.plugin.task;

import edu.buaa.act.gephi.plugin.exception.TaskCancelException;
import edu.buaa.act.gephi.plugin.preview.HeatMapRenderer;
import edu.buaa.act.gephi.plugin.utils.Clock;
import edu.buaa.act.gephi.plugin.utils.LinearGradientInt;
import org.act.dynproperty.impl.RangeQueryCallBack;
import org.act.dynproperty.util.DynPropertyValueConvertor;
import org.act.dynproperty.util.Slice;
import org.act.neo4j.temporal.demo.utils.TransactionWrapper;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.graph.api.Edge;
import org.gephi.preview.api.G2DTarget;
import org.gephi.preview.api.Item;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static edu.buaa.act.gephi.plugin.preview.HeatMapRenderer.HEAT;
import static edu.buaa.act.gephi.plugin.preview.HeatMapRenderer.HEATMAP_IMG;
import static edu.buaa.act.gephi.plugin.preview.HeatMapRenderer.HEATMAP_IMG_SCALE;

/**
 * Created by song on 16-5-29.
 */
public class RenderPreProcessSyncTask extends TransactionWrapper<Object> implements LongTask, Runnable {


    final private GraphDatabaseService db;
    final Item[] itemArray;
    final private int startTime;
    final private int winSize;
    final private float heatAreaScale;
    private final HeatMapRenderer renderer;
    private final double imageScale;//should fix to 1d.这个是当时由于渲染速度太慢所以增加的一个参数，这个参数本身的处理逻辑好像有一点bug（如果是1的话就没问题），而且后来由于渲染速度提高，整个渲染过程只需要不到50ms，所以就没用了，这里先不改了～
    private ProgressTicket progress;
    volatile private boolean shouldGo=true;
    private float maxHeatValue = 0f;
    private long dataCount=0;

    public RenderPreProcessSyncTask(GraphDatabaseService db, Item[] items, HeatMapRenderer renderer, int startTime, int winSize, int shadowSize, double imageScale) {
        this.db = db;
        this.startTime = startTime;
        this.winSize = winSize;
        this.itemArray = items;
        this.renderer = renderer;
        this.shouldGo = true;
        this.heatAreaScale = shadowSize;
        this.imageScale = imageScale;
    }

    @Override
    public boolean cancel() {
        this.shouldGo = false;
        this.renderer.stopRender=true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("TGraph.preview.HeatMap");
        try {
            Clock clock = new Clock();
            clock.start("get data");
            Progress.start(progress, itemArray.length*3);
            Progress.setDisplayName(progress, "TGraph: Rendering, getting data...");

            this.start(db);
            System.out.println(dataCount+" time point data loaded.");

            clock.lap("pre process");
            Progress.setDisplayName(progress, "TGraph: Rendering, pre-processing...");

            BufferedImage image = commonPreProcess(imageScale);

            final int w = image.getWidth();
            final int h = image.getHeight();
            System.out.println("image size :" + w + "x" + h);
            clock.lap("calc heat map");

            Progress.setDisplayName(progress, "TGraph: Rendering, calculation heat map...");
            Progress.switchToIndeterminate(progress);

            LinearGradientInt gradient = renderer.buildColorGradient();
            renderer.calculateHeatColor(image, gradient);

            clock.lap("cut image");
            cutByEdgeCount(image);

            clock.stop();
        }catch (TaskCancelException e){

        }finally {
            Progress.finish(progress);
        }
    }

    private void cutByEdgeCount(BufferedImage image) {
        if (itemArray.length>0) {
            itemArray[0].setData(HEATMAP_IMG, image);
            itemArray[0].setData(HEATMAP_IMG_SCALE, imageScale);
        }
    }

    @Override
    public void runInTransaction() {
        for (Item item : itemArray) {
            if(!shouldGo) return;
            Edge edge = (Edge) item.getSource();
            long edgeIdTGraph = (Long) edge.getAttribute("tgraph_id");
            Relationship r = db.getRelationshipById(edgeIdTGraph);
            int heatValue = calcHeatValue(r, startTime, winSize);
            if(heatValue>maxHeatValue) maxHeatValue = heatValue;
            item.setData(HEAT, heatValue);
            Progress.progress(progress);
        }
    }

    private BufferedImage commonPreProcess(double scale) throws TaskCancelException {
        renderer.resetSize();
        for (Item item : itemArray) {
            if(!shouldGo) throw new TaskCancelException();
            renderer.preProcess(item);
            Progress.progress(progress);
        }
        if(maxHeatValue==0) maxHeatValue=1;
        int width = (int) (renderer.getMaxX() - renderer.getMinX());
        int height = (int) (renderer.getMaxY() - renderer.getMinY());
        BufferedImage image = new BufferedImage((int)(width*scale), (int)(height*scale), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = (Graphics2D) image.getGraphics();
        graphics2D.translate(-(int)renderer.getMinX(), -(int)renderer.getMinY());
        graphics2D.scale(1d/scale, 1d/scale);
        for (Item item : itemArray) {
            if(!shouldGo) throw new TaskCancelException();
            renderer.firstPhaseRender(item, graphics2D, maxHeatValue, heatAreaScale);
            Progress.progress(progress);
        }
        graphics2D.scale(scale,scale);

        return image;
    }

    private int calcHeatValue(Relationship r, int startTime, int winSize) {
//        System.out.println(r + " " + startTime + " " + winSize + " " + r.getDynPropertyPointValue("full-status", startTime + winSize / 2));
//        return (Integer) r.getProperty("length");
        final int[] result = new int[]{0};
        if(r.hasProperty("full-status")){
            Integer tmp = (Integer) r.getDynPropertyRangeValue("full-status", startTime, startTime + winSize, new RangeQueryCallBack() {
                @Override
                public void setValueType(String valueType) {}
                @Override
                public void onCall(Slice value) {
//                value.getInt(0);
                    byte[] statusByte = value.slice(0,4).getBytes();
                    int status = (Integer) DynPropertyValueConvertor.revers("Integer", statusByte);
//                System.out.print(status+",");
                    dataCount++;
                    switch (status) {
                        case 2:
                            result[0]++;
                            break;
                        case 3:
                            result[0] += 3;
                            break;
                    }
                }
                @Override
                public Slice onReturn() {
                    Slice value = new Slice(4);
                    value.setInt(0, result[0]);
                    return value;
                }
            });
//        System.out.println(tmp+" "+result[0]);
            return result[0];
        }else{
            return 0;
        }
    }

    private void saveImageToFile(BufferedImage image){
        try {
            ImageIO.write(image,"png",new File("/tmp/amtf"+System.currentTimeMillis()+".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

//    private void calcHeatShape(G2DTarget g2DTarget, PreviewProperties properties) {
////        HeatMapRenderer edgeRenderer = new HeatMapRenderer();
//////        BufferedImage image = new BufferedImage(10000, 10000, BufferedImage.TYPE_INT_ARGB);
//////        Graphics2D graphics2D = (Graphics2D) image.getGraphics();
////        for (Item item : model.getItems(Item.EDGE)) {
////            if(!shouldGo) return;
////            // System.out.println(heatValue);
////            edgeRenderer.preProcess(item, g2DTarget.getGraphics(), properties);
////        }
////
////        Image image = g2DTarget.getImage();
////        try {
////            ImageIO.write((RenderedImage) image,"png",new File("/tmp/amtf"+System.currentTimeMillis()+".png"));
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
////
//    }
//    public void renderJava2D(Item item, G2DTarget target, PreviewProperties properties) {
//        final Graphics2D graphics = ((G2DTarget) target).getGraphics();
////        graphics.setStroke(new BasicStroke(
////                getThickness(item),
////                BasicStroke.CAP_SQUARE,
////                BasicStroke.JOIN_MITER));
////        graphics.setColor(color);
////        final Line2D.Float line
////                = new Line2D.Float(h.x1, h.y1, h.x2, h.y2);
////        graphics.draw(line);
//
//        Float x = item.getData(NodeItem.X);
//        Float y = item.getData(NodeItem.Y);
//        Float size = item.getData(NodeItem.SIZE);
//        Color color = item.getData(NodeItem.COLOR);
//        Color startColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 32);
//        Color endColor = new Color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), 0);
//        float radius = size * 6;
//
//        //Get Java2D canvas
//        Graphics2D g2 = target.getGraphics();
//
//        RadialGradientPaint p = new RadialGradientPaint(new Point2D.Double(x, y), radius,
//                new float[]{
//                        0.0f, 1.0f},
//                new Color[]{
//                        startColor,
//                        endColor});
//        g2.setPaint(p);
//
//   g2.fillOval((int) (x - radius), (int) (y - radius), (int) (radius * 2), (int) (radius * 2));
//    }

//    private void commonItemPreProcess(PreviewModel previewModel, Item item){
//        //Put nodes in edge item
//        final Edge edge = (Edge) item.getSource();
//        final Node source = edge.getSource();
//        final Node target = edge.getTarget();
//        final Item nodeSource = previewModel.getItem(Item.NODE, source);
//        final Item nodeTarget = previewModel.getItem(Item.NODE, target);
//        item.setData(SOURCE, nodeSource);
//        item.setData(TARGET, nodeTarget);
//        PreviewProperties properties = previewModel.getProperties();
//        if (!(Boolean) item.getData(EdgeItem.SELF_LOOP)) {
//            final float edgeRadius
//                    = properties.getFloatValue(PreviewProperty.EDGE_RADIUS);
//
//            boolean isDirected = (Boolean) item.getData(EdgeItem.DIRECTED);
//            if (isDirected || edgeRadius > 0F) {
//                //Target
//                final Item targetItem = (Item) item.getData(TARGET);
//                final Double weight = item.getData(EdgeItem.WEIGHT);
//                //Avoid negative arrow size:
//                float arrowSize = properties.getFloatValue(
//                        PreviewProperty.ARROW_SIZE);
//                if (arrowSize < 0F) {
//                    arrowSize = 0F;
//                }
//
//                final float arrowRadiusSize = isDirected ? arrowSize * weight.floatValue() : 0f;
//
//                final float targetRadius = -(edgeRadius
//                        + (Float) targetItem.getData(NodeItem.SIZE) / 2f
//                        + properties.getFloatValue(PreviewProperty.NODE_BORDER_WIDTH) / 2f //We have to divide by 2 because the border stroke is not only an outline but also draws the other half of the curve inside the node
//                        + arrowRadiusSize
//                );
//                item.setData(TARGET_RADIUS, targetRadius);
//
//                //Source
//                final Item sourceItem = (Item) item.getData(SOURCE);
//                final float sourceRadius = -(edgeRadius
//                        + (Float) sourceItem.getData(NodeItem.SIZE) / 2f
//                        + properties.getFloatValue(PreviewProperty.NODE_BORDER_WIDTH) / 2f
//                );
//                item.setData(SOURCE_RADIUS, sourceRadius);
//            }
//        }
//    }