/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016 Izumi Kawashima
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

package org.oscim.layers.marker;

import org.oscim.core.Box;
import org.oscim.core.GeoPoint;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.renderer.bucket.TextBucket;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.renderer.other.VTMTextItemWrapper;
import org.oscim.scalebar.DistanceUnitAdapter;
import org.oscim.utils.LatLongUtils;
import org.oscim.utils.QuadTree;
import org.oscim.utils.TimSort;
import org.oscim.utils.geom.GeometryUtils;
import org.oscim.utils.math.MathUtils;

import java.util.Comparator;
import java.util.LinkedList;

public class MarkerRenderer extends BucketRenderer {

    protected final MarkerSymbol mDefaultMarker;

    private QuadTree<InternalItem> clusterQuadTree;
    private final SymbolBucket mSymbolLayer;
    private final float[] mBox = new float[8];
    private final MarkerLayer<MarkerInterface> mMarkerLayer;
    private final Point mMapPoint = new Point();

    private boolean markerClustering = true;

    /**
     * increase view to show items that are partially visible
     */
    protected int mExtents = 100;

    /**
     * flag to force update of markers
     */
    private boolean mUpdate;

    private InternalItem[] mItems;
    private TextBucket     mTextLayer;

    static class InternalItem {
        MarkerInterface item;

        boolean didSearch = false;
        boolean wasFound = false;
        boolean visible;
        boolean changes;
        float x, y;
        double px, py;
        float dy;

        @Override
        public String toString() {
            return "\n" + px + ":" + py + " / " + dy + " " + visible + " f:" + wasFound;
        }
    }

    public MarkerRenderer(MarkerLayer<MarkerInterface> markerLayer, MarkerSymbol defaultSymbol) {
        mSymbolLayer = new SymbolBucket();
        mMarkerLayer = markerLayer;
        mDefaultMarker = defaultSymbol;
        clusterQuadTree = new QuadTree<>(4, 4);
    }

    public boolean isMarkerClustering()
    {
        return markerClustering;
    }

    public void setMarkerClustering(boolean markerClustering)
    {
        this.markerClustering = markerClustering;
    }

    private void renderMarkerLabel(InternalItem internalItem) {
        MarkerInterface marker = internalItem.item;
        //Only render if is type of LabeldMarker
        if (!(marker instanceof LabeledMarkerInterface) ) {
            return;
        }
        TextItem textItem = TextItem.pool.get();
        VTMTextItemWrapper ti = ((LabeledMarkerInterface) marker).getLabel();
        // Render Marker Label, if Text is available.
        if (ti == null || ti.text == null)
            return;

        org.oscim.core.Point result = new org.oscim.core.Point(0, 0);
        mMarkerLayer.map().viewport().toScreenPoint(ti.p, result);
        textItem.screenPoint = result;

        int distance = (int)((double)internalItem.item.getMarker().getBitmap().getHeight() / 1.5);
        double[] rotated = LatLongUtils.rotatePoint(internalItem.x, internalItem.y, internalItem.x, internalItem.y + distance, Math.toRadians(mMapPosition.bearing));
        textItem.set(rotated[0], rotated[1], ti.text, ti.style);

        mTextLayer.addText(textItem);

    }

    @Override
    public synchronized void update(GLViewport v) {
        if (!v.changed() && !mUpdate)
            return;

        sort(mItems, 0, mItems.length);

        mTextLayer = new TextBucket();

        mUpdate = false;

        double mx = v.pos.x;
        double my = v.pos.y;
        double scale = Tile.SIZE * v.pos.scale;

        //int changesInvisible = 0;
        //int changedVisible = 0;
        int numVisible = 0;

        mMarkerLayer.map().viewport().getMapExtents(mBox, mExtents);

        long flip = (long) (Tile.SIZE * v.pos.scale) >> 1;

        if (mItems == null) {
            if (buckets.get() != null) {
                buckets.clear();
                compile();
            }
            return;
        }
        double angle = Math.toRadians(v.pos.bearing);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        double groundRes = MercatorProjection.groundResolution(v.pos);
        /* check visibility */
        for (InternalItem it : mItems) {
            it.changes = false;
            it.x = (float) ((it.px - mx) * scale);
            it.y = (float) ((it.py - my) * scale);

            if (it.x > flip)
                it.x -= (flip << 1);
            else if (it.x < -flip)
                it.x += (flip << 1);



            if (!GeometryUtils.pointInPoly(it.x, it.y, mBox, 8, 0)) {
                if (it.visible) {
                    it.changes = true;
                    //changesInvisible++;
                }
                continue;
            }

            it.dy = sin * it.x + cos * it.y;
            if (it.visible && markerClustering) {
                double width = groundRes * it.item.getMarker().getBitmap().getWidth() * 1e-7 / 2.2;
                it.didSearch = true;
                for (InternalItem i : mItems)
                {
                    if (i.visible && !i.didSearch && GeometryUtils.distance(new double[] {it.px, it.py, i.px, i.py}, 0, 2) < width)
                    {
                        i.visible = false;
                        i.wasFound = true;
                    }
                }
            }
            numVisible++;
        }

        //log.debug(numVisible + " " + changedVisible + " " + changesInvisible);

        /* only update when zoomlevel changed, new items are visible
         * or more than 10 of the current items became invisible */
        //if ((numVisible == 0) && (changedVisible == 0 && changesInvisible < 10))
        //    return;
        buckets.clear();

        if (numVisible == 0) {
            compile();
            return;
        }
        /* keep position for current state */
        mMapPosition.copy(v.pos);
        mMapPosition.setBearing(-mMapPosition.bearing);

        //log.debug(Arrays.toString(mItems));
        for (InternalItem it : mItems) {

            it.didSearch = false;

            if (it.wasFound) {
                it.visible = false;
                it.wasFound = false;
                continue;
            }

            if (!it.visible)
            {
                it.visible = true;
                continue;
            }

            if (it.changes) {
                it.visible = false;
                continue;
            }

            renderMarkerLabel(it);

            MarkerSymbol marker = it.item.getMarker();
            if (marker == null)
                marker = mDefaultMarker;

            SymbolItem s = SymbolItem.pool.get();
            if (marker.isBitmap()) {
                s.set(it.x, it.y, marker.getBitmap(), true);
            } else {
                s.set(it.x, it.y, marker.getTextureRegion(), true);
            }
            s.offset = marker.getHotspot();
            s.billboard = marker.isBillboard();
            mSymbolLayer.pushSymbol(s);

        }

        mSymbolLayer.next = mTextLayer;
        buckets.set(mSymbolLayer);

        buckets.prepare();

        compile();
    }

    protected void populate(int size) {
        InternalItem[] tmp = new InternalItem[size];

        for (int i = 0; i < size; i++) {
            InternalItem it = new InternalItem();
            tmp[i] = it;
            it.item = mMarkerLayer.createItem(i);

            /* pre-project points */
            MercatorProjection.project(it.item.getPoint(), mMapPoint);
            it.px = mMapPoint.x;
            it.py = mMapPoint.y;

            clusterQuadTree.insert(new Box(it.px, it.py, it.px, it.py), it);
        }
        synchronized (this) {
            mUpdate = true;
            mItems = tmp;
        }
    }

    public void update() {
        mUpdate = true;
    }

    static TimSort<InternalItem> ZSORT = new TimSort<InternalItem>();

    public static void sort(InternalItem[] a, int lo, int hi) {
        int nRemaining = hi - lo;
        if (nRemaining < 2) {
            return;
        }

        ZSORT.doSort(a, zComparator, lo, hi);
    }

    final static Comparator<InternalItem> zComparator = new Comparator<InternalItem>() {
        @Override
        public int compare(InternalItem a, InternalItem b) {
            if (a.visible && b.visible) {
                /*if (a.dy > b.dy) {
                    return -1;
                }
                if (a.dy < b.dy) {
                    return 1;
                }
                */
                return a.toString().compareTo(b.toString());
            } else if (a.visible) {
                return -1;
            } else if (b.visible) {
                return 1;
            }

            return 0;
        }
    };

    //    /**
    //     * Returns the Item at the given index.
    //     *
    //     * @param position
    //     *            the position of the item to return
    //     * @return the Item of the given index.
    //     */
    //    public final Item getItem(int position) {
    //
    //        synchronized (lock) {
    //            InternalItem item = mItems;
    //            for (int i = mSize - position - 1; i > 0 && item != null; i--)
    //                item = item.next;
    //
    //            if (item != null)
    //                return item.item;
    //
    //            return null;
    //        }
    //    }
}
