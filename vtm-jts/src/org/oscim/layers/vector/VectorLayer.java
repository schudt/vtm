/*
 * Copyright 2016 devemux86
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.layers.vector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

import org.oscim.backend.canvas.Color;
import org.oscim.core.BoundingBox;
import org.oscim.core.Box;
import org.oscim.core.GeoPoint;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.utils.ScreenUtils;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.PointDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.MeshBucket;
import org.oscim.renderer.bucket.PolygonBucket;
import org.oscim.renderer.bucket.TextBucket;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.renderer.other.VTMTextItemWrapper;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.TextStyle;
import org.oscim.tiling.source.mapfile.Projection;
import org.oscim.utils.FastMath;
import org.oscim.utils.LatLongUtils;
import org.oscim.utils.QuadTree;
import org.oscim.utils.SpatialIndex;
import org.oscim.utils.geom.GeometryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.oscim.core.MercatorProjection.latitudeToY;
import static org.oscim.core.MercatorProjection.longitudeToX;

/* TODO keep bounding box of geometries - only try to render when bbox intersects viewport */

/**
 * Use this layer to draw predefined geometries from layers.vector.geometries
 * package and
 * JTS geometries together with a GeometryStyle
 */
public class VectorLayer extends AbstractVectorLayer<Drawable> implements GestureListener{

    public static final Logger log = LoggerFactory.getLogger(VectorLayer.class);
    public static final int MAX_ZOOM_LEVEL_FOR_ROOM_LABELS = 17;

    protected final SpatialIndex<Drawable> mDrawables = new QuadTree<>(1 << 30, 21);

    protected final List<Drawable> tmpDrawables = new ArrayList<>(128);
    protected final List<Drawable> tmpEventDrawables = new ArrayList<>(128);
    protected           ItemizedLayer.OnItemGestureListener<Geometry> mOnItemGestureListener;
    protected final JtsConverter                              mConverter;
    protected       double                                    mMinX;
    protected       double                                    mMinY;
    private List<VTMTextItemWrapper> textItems = new LinkedList<>();
    private TextBucket mTextLayer;
    private TextStyle mTextStyle;


    private static class GeometryWithStyle implements Drawable {
        final Geometry geometry;
        final Style style;

        GeometryWithStyle(Geometry g, Style s) {
            geometry = g;
            style = s;
        }

        @Override
        public Style getStyle() {
            return style;
        }

        @Override
        public Geometry getGeometry() {
            return geometry;
        }
    }

    public void addText(VTMTextItemWrapper text) {
        textItems.add(text);
    }

    public void setTextStyle(TextStyle textStyle) {
        mTextStyle = textStyle;
    }

    protected Polygon mEnvelope;

    public VectorLayer(Map map, SpatialIndex<Drawable> index) {
        this(map);
    }

    public VectorLayer(Map map) {
        super(map);
        mConverter = new JtsConverter(Tile.SIZE / UNSCALE_COORD);

    }

    public VectorLayer(Map map, ItemizedLayer.OnItemGestureListener<Geometry> gestureListener) {
        super(map);
        mOnItemGestureListener = gestureListener;
        mConverter = new JtsConverter(Tile.SIZE / UNSCALE_COORD);

    }

    private static Box bbox(Geometry geometry, Style style) {
        Envelope e = geometry.getEnvelopeInternal();
        Box bbox = new Box(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());
        //if ("Point".equals(geometry.getGeometryType())){
        //    bbox.
        //}

        //bbox.scale(1E6);
        return bbox;
    }

    /**
     * Adds a drawable to a list of geometries that have to be drawn in the next
     * map update.
     *
     * @param drawable
     */
    public void add(Drawable drawable) {
        mDrawables.insert(bbox(drawable.getGeometry(), drawable.getStyle()), drawable);
    }

    /**
     * Adds a JTS geometry and a style to a list of geometries that have to be
     * drawn in the next map update.
     *
     * @param geometry
     * @param style
     */
    public synchronized void add(Geometry geometry, Style style) {
        mDrawables.insert(bbox(geometry, style), new GeometryWithStyle(geometry, style));
    }

    /**
     * Removes the drawable from the list of drawn geometries.
     *
     * @param drawable
     */
    public synchronized void remove(Drawable drawable) {
        mDrawables.remove(bbox(drawable.getGeometry(), drawable.getStyle()), drawable);
    }

    /**
     * removes the JTS geometry and its style from the list of drawn geometries.
     *
     * @param geometry
     */
    public synchronized void remove(Geometry geometry) {
        Drawable toRemove = null;
        Box bbox = bbox(geometry, null);

        synchronized (this) {
            tmpDrawables.clear();
            mDrawables.search(bbox, tmpDrawables);
            for (Drawable d : tmpDrawables) {
                if (d.getGeometry() == geometry)
                    toRemove = d;
            }
        }

        if (toRemove == null) {
            log.error("Can't find geometry to remove.");
            return;
        }

        mDrawables.remove(bbox, toRemove);
        //mMap.render();
    }

    @Override
    protected void processFeatures(Task t, Box bbox) {
        //log.debug("bbox {}", bbox);
        if (Double.isNaN(bbox.xmin))
            return;

        //    mEnvelope = new GeomBuilder()
        //        .point(bbox.xmin, bbox.ymin)
        //        .point(bbox.xmin, bbox.ymax)
        //        .point(bbox.xmax, bbox.ymax)
        //        .point(bbox.xmax, bbox.ymin)
        //        .point(bbox.xmin, bbox.ymin)
        //        .toPolygon();

        /* reduce lines points min distance */
        mMinX = ((bbox.xmax - bbox.xmin) / mMap.getWidth());
        mMinY = ((bbox.ymax - bbox.ymin) / mMap.getHeight());

        mConverter.setPosition(t.position.x, t.position.y, t.position.scale);

        //bbox.scale(1E6);
        mTextLayer = new TextBucket();
        t.buckets.clear();
        t.buckets.set(mTextLayer);
        int level = 2000;
        Style lastStyle = null;

        // TODO sort by some order...

        /* go through features, find the matching style and draw */
        synchronized (this) {
            //mTextLayer.clear();
            //
            tmpDrawables.clear();
            mDrawables.search(bbox, tmpDrawables);
            for (Drawable d : tmpDrawables) {
                Style style = d.getStyle();
                draw(t, level, d, style);

                if (style != lastStyle)
                    level += 2;

                lastStyle = style;
            }
            if (t.position.zoomLevel > MAX_ZOOM_LEVEL_FOR_ROOM_LABELS)
            {
                addTextItems();
            }
        }
        //
    }




    private void addTextItems()
    {
        for (VTMTextItemWrapper ti : textItems) {
            TextItem textItem = TextItem.pool.get();

            GeometryBuffer mGeom = new GeometryBuffer(1, 2);
            mGeom.startPoints();
            CoordinateSequence c = PackedCoordinateSequenceFactory.DOUBLE_FACTORY.create(new double[]{ti.p.getLongitude(), ti.p.getLatitude()}, 2);
            Point p = new Point(c, new GeometryFactory());
            mConverter.transformPoint(mGeom.clear(), p);
            org.oscim.core.Point resultPoint = mGeom.getPoint(0);
            if (ti.text == null && mTextStyle != null) {
                textItem.text = mTextStyle;
            }

            org.oscim.core.Point result = new org.oscim.core.Point(0, 0);
            mMap.viewport().toScreenPoint(ti.p, result);

            textItem.set(resultPoint.getX(), resultPoint.getY(), ti.text, ti.style);
            textItem.screenPoint = result;
            ti.hidden = false;
            ti.item = textItem;
            mTextLayer.addText(ti.item);
        }
    }

    private void draw(Task task, int level, Drawable d, Style style) {
        Geometry geom = d.getGeometry();

        if (d instanceof LineDrawable) {
            drawLine(task, level, geom, style);
        } else if (d instanceof PointDrawable) {
            drawPoint(task, level, geom, style);
        } else {
            drawPolygon(task, level, geom, style);
        }
    }

    private void drawPoint(Task t, int level, Geometry points, Style style) {

        MeshBucket mesh = t.buckets.getMeshBucket(level);
        if (mesh.area == null) {
            mesh.area = new AreaStyle(Color.fade(style.fillColor,
                    style.fillAlpha));
        }

        LineBucket ll = t.buckets.getLineBucket(level + 1);
        if (ll.line == null) {
            ll.line = new LineStyle(2, style.strokeColor, style.strokeWidth);
        }

        for (int i = 0; i < points.getNumGeometries(); i++) {
            Point p = (Point) points.getGeometryN(i);
            addCircle(mGeom.clear(), t.position, p.getX(), p.getY(), style);

            if (!mClipper.clip(mGeom))
                continue;

            mesh.addConvexMesh(mGeom);
            ll.addLine(mGeom);
        }
    }

    private void drawLine(Task t, int level, Geometry line, Style style) {

        LineBucket ll;
        if (style.stipple == 0 && style.texture == null)
            ll = t.buckets.getLineBucket(level);
        else
            ll = t.buckets.getLineTexBucket(level);
        if (ll.line == null) {
            if (style.stipple == 0 && style.texture == null)
                ll.line = new LineStyle(style.strokeColor, style.strokeWidth, style.cap);
            else
                ll.line = LineStyle.builder()
                        .cap(style.cap)
                        .color(style.strokeColor)
                        .fixed(style.fixed)
                        .level(0)
                        .randomOffset(style.randomOffset)
                        .stipple(style.stipple)
                        .stippleColor(style.stippleColor)
                        .stippleWidth(style.stippleWidth)
                        .strokeWidth(style.strokeWidth)
                        .texture(style.texture)
                        .build();
        }

        if (style.generalization != Style.GENERALIZATION_NONE) {
            line = DouglasPeuckerSimplifier.simplify(line, mMinX * style.generalization);
        }

        //line = line.intersection(mEnvelope);

        for (int i = 0; i < line.getNumGeometries(); i++) {
            mConverter.transformLineString(mGeom.clear(), (LineString) line.getGeometryN(i));
            if (!mClipper.clip(mGeom))
                continue;

            ll.addLine(mGeom);
        }
    }

    private void drawPolygon(Task t, int level, Geometry polygon, Style style) {
        MeshBucket mesh = t.buckets.getMeshBucket(level);
        if (mesh.area == null) {
            mesh.area = new AreaStyle(Color.fade(style.fillColor,
                    style.fillAlpha));
        }

        LineBucket ll = t.buckets.getLineBucket(level + 1);
        if (ll.line == null) {
            ll.line = new LineStyle(2, style.strokeColor, style.strokeWidth);
        }

        if (style.generalization != Style.GENERALIZATION_NONE) {
            polygon = DouglasPeuckerSimplifier.simplify(polygon, mMinX * style.generalization);
        }

        // if (polygon.isRectangle())

        for (int i = 0; i < polygon.getNumGeometries(); i++) {
            mConverter.transformPolygon(mGeom.clear(), (Polygon) polygon.getGeometryN(i));

            if (mGeom.getNumPoints() < 3)
                continue;

            if (!mClipper.clip(mGeom))
                continue;

            mesh.addMesh(mGeom);
            ll.addLine(mGeom);
        }
    }

    private void addCircle(GeometryBuffer g, MapPosition pos,
                           double px, double py, Style style) {

        double scale = pos.scale * Tile.SIZE / UNSCALE_COORD;
        double x = (longitudeToX(px) - pos.x) * scale;
        double y = (latitudeToY(py) - pos.y) * scale;

        /* TODO in the next line I was only able to interpolate a function
         * that makes up for the zoom level. The circle should not grow, it
         * should stickto the map. 0.01 / (1 << startLvl) makes it retain
         * its size. Correction? */
        int zoomScale = (1 << style.scalingZoomLevel);

        /* Keep the circle's size constant in relation to the underlying map */
        double radius = style.buffer;

        if (pos.scale > zoomScale)
            radius = (radius * 0.01) / zoomScale * (scale - zoomScale);

        int quality = (int) (Math.sqrt(radius) * 8);
        quality = FastMath.clamp(quality, 4, 32);

        double step = 2.0 * Math.PI / quality;

        g.startPolygon();
        for (int i = 0; i < quality; i++) {
            g.addPoint((float) (x + radius * Math.cos(i * step)),
                    (float) (y + radius * Math.sin(i * step)));
        }
    }


    @Override
    public boolean onGesture(Gesture g, MotionEvent e)
    {
        if (g instanceof Gesture.Tap)
        {
            return tapGeometries(findSelectedGeometries(e));
        }

        else if (g instanceof Gesture.LongPress)
        {
            return longPressGeometries(findSelectedGeometries(e));
        }

        // Event was not consumed.
        return false;
    }

    private List<Geometry> findSelectedGeometries(MotionEvent e) {
        List<Geometry> geom = new LinkedList<>();

        // Translate ScreenPoint to current LatLon
        GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), (e.getY()));
        Geometry g = new Point(new Coordinate(p.getLongitude(), p.getLatitude()), new PrecisionModel(), 0);

        // Get the BoundingBox of the current ViewPort
        BoundingBox b = map().getBoundingBox(0);
        Box bbox = new Box(b.getMinLongitude(), b.getMinLatitude(), b.getMaxLongitude(), b.getMaxLatitude());

        // Search in Drawables for Items that might be Located in the Area to speed up hittesting.
        tmpEventDrawables.clear();
        mDrawables.search(bbox, tmpEventDrawables);

        // Do local precise Search in the Resultset
        for (Drawable d : tmpEventDrawables) {
            if (d.getGeometry().contains(g)) {
                geom.add(d.getGeometry());
            }
        }

        return geom;
    }

    private boolean tapGeometries(List<Geometry> g) {
        for (int i = 0; i < g.size(); i++) {
            mOnItemGestureListener.onItemSingleTapUp(i, g.get(i));
        }
        return true;
    }

    private boolean longPressGeometries(List<Geometry> g) {
        for (int i = 0; i < g.size(); i++) {
            mOnItemGestureListener.onItemLongPress(i, g.get(i));
        }
        return true;
    }
}
