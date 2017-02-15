package org.oscim.renderer.other;

import org.oscim.core.GeoPoint;
import org.oscim.core.Point;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.TextStyle;
import org.oscim.utils.geom.OBB2D;

import java.lang.ref.WeakReference;


public class VTMTextItemWrapper
{
    public String    text;
    public TextStyle style;
    public GeoPoint  p;

    public boolean hidden = false;

    public TextItem item;

    public VTMTextItemWrapper(String text, TextStyle style, GeoPoint p)
    {
        this.text = text;
        this.style = style;
        this.p = p;
    }
}
