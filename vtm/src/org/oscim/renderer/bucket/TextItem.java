/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.renderer.bucket;

import org.oscim.core.Point;
import org.oscim.theme.styles.TextStyle;
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.SyncPool;

public class TextItem extends Inlist<TextItem> {
    //static final Logger log = LoggerFactory.getLogger(TextItem.class);
    private final static int MAX_POOL = 250;

    /* new Member von stephan */
    public Point screenPoint;
    public boolean hidden;

    public final static SyncPool<TextItem> pool = new SyncPool<TextItem>(MAX_POOL) {

        @Override
        protected TextItem createItem() {
            return new TextItem();
        }

        @Override
        protected boolean clearItem(TextItem ti) {
            // drop references
            ti.string = null;
            ti.text = null;
            //ti.n1 = null;
            //ti.n2 = null;
            return true;
        }
    };


    public void placeLabelFrom(double w, double h) {
        // set line endpoints relative to view to be able to
        // check intersections with label from other tiles

        this.x1 = this.screenPoint.x - ((w / 2));
        this.y1 = this.screenPoint.y - ((h / 2));
        this.x2 = this.screenPoint.x + ((w / 2));
        this.y2 = this.screenPoint.y + ((h / 2));
    }

    public static TextItem copy(TextItem orig) {

        TextItem ti = pool.get();

        ti.x = orig.x;
        ti.y = orig.y;

        ti.x1 = orig.x1;
        ti.y1 = orig.y1;
        ti.x2 = orig.x2;
        ti.y2 = orig.y2;

        return ti;
    }

    public TextItem set(double x, double y, String string, TextStyle text) {
        this.x = x;
        this.y = y;
        this.string = string;
        this.text = text;
        this.x1 = 0;
        this.y1 = 0;
        this.x2 = 1;
        this.y2 = 0;
        this.width = text.paint.measureText(string);
        return this;
    }

    // center
    public double x, y;

    // label text
    public String string;

    // text style
    public TextStyle text;

    // label width
    public float width;

    // left and right corner of segment
    public double x1, y1, x2, y2;

    // segment length
    public short length;

    // link to next/prev label of the way
    //public TextItem n1;
    //public TextItem n2;

    public byte edges;

    public boolean bboxOverlaps(TextItem other, float add) {
        if (this.y1 < this.y2) {
            if (other.y1 < other.y2)
                return (this.x1 - add < other.x2)
                       && (other.x1 < this.x2 + add)
                       && (this.y1 - add < other.y2)
                       && (other.y1 < this.y2 + add);

            // flip other
            return (this.x1 - add < other.x2)
                   && (other.x1 < this.x2 + add)
                   && (this.y1 - add < other.y1)
                   && (other.y2 < this.y2 + add);
        }

        // flip this
        if (other.y1 < other.y2)
            return (this.x1 - add < other.x2)
                   && (other.x1 < this.x2 + add)
                   && (this.y2 - add < other.y2)
                   && (other.y1 < this.y1 + add);

        // flip both
        return (this.x1 - add < other.x2)
               && (other.x1 < this.x2 + add)
               && (this.y2 - add < other.y1)
               && (other.y2 < this.y1 + add);
    }

    @Override
    public String toString() {
        return x + " " + y + " " + string;
    }
}
