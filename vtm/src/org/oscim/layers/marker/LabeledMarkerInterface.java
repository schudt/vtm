package org.oscim.layers.marker;

import org.oscim.renderer.other.VTMTextItemWrapper;

/**
 * Created by sbrandt on 13.12.16.
 */

public interface LabeledMarkerInterface extends MarkerInterface
{
    VTMTextItemWrapper getLabel();
}
