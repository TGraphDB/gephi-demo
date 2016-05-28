package edu.buaa.act.gephi.plugin.preview;

import edu.buaa.act.gephi.plugin.utils.Heatmap;
import org.act.dynproperty.impl.RangeQueryCallBack;
import org.act.dynproperty.util.Slice;
import org.act.neo4j.temporal.demo.utils.TransactionWrapper;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Node;
import org.gephi.preview.api.*;
import org.gephi.preview.plugin.builders.EdgeBuilder;
import org.gephi.preview.plugin.items.EdgeItem;
import org.gephi.preview.plugin.items.NodeItem;
import org.gephi.preview.spi.ItemBuilder;
import org.gephi.preview.spi.Renderer;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.openide.util.lookup.ServiceProvider;

/**
 * Created by song on 16-5-21.
 */
@ServiceProvider(service = Renderer.class, position = 10)
public class HeatMapStatisticPreview implements Renderer{
    private GraphDatabaseService db;
    public final String TGRAPH_STATISTIC = "edge.tgraph.enable";
    public final String WINDOW_SIZE = "edge.tgraph.window.size";
    public final String START_TIME = "edge.tgraph.start.time";
    private static final String SOURCE="source";
    private static final String TARGET="target";
    private static final String TARGET_RADIUS = "edge.target.radius";
    private static final String SOURCE_RADIUS = "edge.source.radius";
    private int startTime = 0;
    private EdgeRenderer edgeRenderer = new EdgeRenderer();

    public String getDisplayName() {
        return "TGraph Window HeatMap";
    }

    public void setDB(GraphDatabaseService db){
        this.db = db;
    }

    public void preProcess(final PreviewModel previewModel) {
        final PreviewProperties properties = previewModel.getProperties();
        // CanvasSize size = previewModel.getGraphicsCanvasSize();
        // final Heatmap map = new Heatmap(size.getWidth(),size.getHeight());
        if (properties.getBooleanValue(TGRAPH_STATISTIC) && db!=null) {
            startTime = properties.getIntValue(START_TIME);
            final int winSize = properties.getIntValue(WINDOW_SIZE)*60; // unit: seconds.
            new TransactionWrapper<Object>(){
                @Override
                public void runInTransaction() {
                    for (Item item : previewModel.getItems(Item.EDGE)) {
                        Edge edge = (Edge) item.getSource();
                        long edgeIdTGraph = (Long) edge.getAttribute("tgraph_id");
                        Relationship r = db.getRelationshipById(edgeIdTGraph);
                        int heatValue = calcHeatValue(r, startTime, winSize);
			// System.out.println(heatValue);
                        item.setData("traffic.heat", heatValue);
                        commonPreprocess(previewModel, item);
//                        calcHeatShape(item, heatValue, map, properties);
                    }
                }
            }.start(db);
        }
    }

    private void calcHeatShape(Item item, int heat, Heatmap map, PreviewProperties properties) {
        edgeRenderer.render(item, map.graphics, properties);
    }

    private void commonPreprocess(PreviewModel previewModel, Item item){
        //Put nodes in edge item
        final Edge edge = (Edge) item.getSource();
        final Node source = edge.getSource();
        final Node target = edge.getTarget();
        final Item nodeSource = previewModel.getItem(Item.NODE, source);
        final Item nodeTarget = previewModel.getItem(Item.NODE, target);
        item.setData(SOURCE, nodeSource);
        item.setData(TARGET, nodeTarget);
        PreviewProperties properties = previewModel.getProperties();
        if (!(Boolean) item.getData(EdgeItem.SELF_LOOP)) {
            final float edgeRadius
                    = properties.getFloatValue(PreviewProperty.EDGE_RADIUS);

            boolean isDirected = (Boolean) item.getData(EdgeItem.DIRECTED);
            if (isDirected || edgeRadius > 0F) {
                //Target
                final Item targetItem = (Item) item.getData(TARGET);
                final Double weight = item.getData(EdgeItem.WEIGHT);
                //Avoid negative arrow size:
                float arrowSize = properties.getFloatValue(
                        PreviewProperty.ARROW_SIZE);
                if (arrowSize < 0F) {
                    arrowSize = 0F;
                }

                final float arrowRadiusSize = isDirected ? arrowSize * weight.floatValue() : 0f;

                final float targetRadius = -(edgeRadius
                        + (Float) targetItem.getData(NodeItem.SIZE) / 2f
                        + properties.getFloatValue(PreviewProperty.NODE_BORDER_WIDTH) / 2f //We have to divide by 2 because the border stroke is not only an outline but also draws the other half of the curve inside the node
                        + arrowRadiusSize
                );
                item.setData(TARGET_RADIUS, targetRadius);

                //Source
                final Item sourceItem = (Item) item.getData(SOURCE);
                final float sourceRadius = -(edgeRadius
                        + (Float) sourceItem.getData(NodeItem.SIZE) / 2f
                        + properties.getFloatValue(PreviewProperty.NODE_BORDER_WIDTH) / 2f
                );
                item.setData(SOURCE_RADIUS, sourceRadius);
            }
        }
    }

    private int calcHeatValue(Relationship r, int startTime, int winSize) {
        System.out.println(r+" "+startTime+" "+winSize+" "+r.getDynPropertyPointValue("full-status", startTime + winSize/2));
        return (Integer) r.getProperty("length");
//        return (Integer) r.getDynPropertyRangeValue("full-status", startTime, startTime + winSize, new RangeQueryCallBack() {
//            int result=0;
//            @Override
//            public void onCall(Slice value) {
//                int status = value.getInt(0);
//		System.out.print(status+",");
//                switch (status) {
//                    case 2:
//                        result++;
//                        break;
//                    case 3:
//                        result += 3;
//                        break;
//                }
//            }
//            @Override
//            public Slice onReturn() {
//                Slice value = new Slice(4);
//                value.setInt(0,result);
//                return value;
//            }
//        });
    }

    public void render(Item item, RenderTarget target, PreviewProperties properties) {
        if (target instanceof G2DTarget) {
            edgeRenderer.render(item, target, properties);
        }else{
            // will not implement.
        }
    }
//
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
                        "specific window size (minute) of statistic",
                        PreviewProperty.CATEGORY_EDGES
                ).setValue(60),
                PreviewProperty.createProperty(
                        this,
                        START_TIME,
                        Integer.class,
                        "window size",
                        "begin timestamp(seconds) of statistic",
                        PreviewProperty.CATEGORY_EDGES
                ).setValue(1288581160)
        };
    }

    public boolean isRendererForitem(Item item, PreviewProperties properties) {
        return item instanceof EdgeItem &&
                properties.getBooleanValue(TGRAPH_STATISTIC) &&
                !properties.getBooleanValue(PreviewProperty.MOVING);
    }

    public boolean needsItemBuilder(ItemBuilder itemBuilder, PreviewProperties properties) {
        return itemBuilder instanceof EdgeBuilder &&
                properties.getBooleanValue(TGRAPH_STATISTIC) &&
                !properties.getBooleanValue(PreviewProperty.MOVING);
    }

    public CanvasSize getCanvasSize(Item item, PreviewProperties previewProperties) {
        return edgeRenderer.getCanvasSize(item,previewProperties);
    }
}
