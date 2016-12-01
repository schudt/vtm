package org.oscim.renderer.other;

import org.oscim.core.GeoPoint;
import org.oscim.theme.styles.TextStyle;


public class VTMTextItemWrapper
{
    public String    text;
    public TextStyle style;
    public GeoPoint  p;

    public VTMTextItemWrapper(String text, TextStyle style, GeoPoint p)
    {
        this.text = text;
        this.style = style;
        this.p = p;
    }
}
