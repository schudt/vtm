/*
 * Copyright 2012 osmdroid authors: Viesturs Zarins, Martin Pearman
 * Copyright 2012 Hannes Janetzek
 * Copyright 2016 devemux86
 * Copyright 2016 Bezzu
 * Copyright 2016 Pedinel
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

import org.oscim.layers.tile.PolygonRenderer;
import org.oscim.map.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class draws a path line in given color or texture.
 */
public class PolygonLayer extends GenericLayer
{
    static final Logger log = LoggerFactory.getLogger(PolygonLayer.class);

    public PolygonLayer(Map map) {
        super(map, new PolygonRenderer());
    }
}
