/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.layers;

import org.oscim.map.Map;
import org.oscim.renderer.LayerRenderer;
import org.oscim.utils.TimSort;

import java.util.Comparator;

public abstract class Layer implements Comparable<Layer>
{

    public Layer(Map map) {
        mMap = map;
    }

    private boolean mEnabled = true;
    protected final Map mMap;
    private int zIndex = 0;

    protected LayerRenderer mRenderer;

    public LayerRenderer getRenderer() {
        return mRenderer;
    }

    /**
     * Enabled layers will be considered for rendering and receive onMapUpdate()
     * calls when they implement MapUpdateListener.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Override to perform clean up of resources before shutdown.
     */
    public void onDetach() {
    }

    public int getzIndex()
    {
        return zIndex;
    }

    public void setzIndex(int zIndex)
    {
        this.zIndex = zIndex;
    }

    public Map map() {
        return mMap;
    }


    @Override
    public int compareTo(Layer layer)
    {
        if (this.isEnabled() && layer.isEnabled()) {
            if (this.getzIndex() > layer.getzIndex()) {
                return 1;
            }
            if (this.getzIndex() < layer.getzIndex()) {
                return -1;
            }
        } else if (this.isEnabled()) {
            return 1;
        } else if (layer.isEnabled()) {
            return -1;
        }

        return 0;
    }

    public static TimSort<Layer> ZSORT = new TimSort<Layer>();

    public final static Comparator<Layer> zComparator = new Comparator<Layer>() {
        @Override
        public int compare(Layer layer, Layer t1)
        {
            if (layer.isEnabled() && t1.isEnabled()) {
                if (layer.getzIndex() > t1.getzIndex()) {
                    return 1;
                }
                if (layer.getzIndex() < t1.getzIndex()) {
                    return -1;
                }
            } else if (layer.isEnabled()) {
                return 1;
            } else if (t1.isEnabled()) {
                return -1;
            }

            return 0;
        }
    };
}
