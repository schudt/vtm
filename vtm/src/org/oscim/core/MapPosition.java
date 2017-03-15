/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.core;

import org.oscim.event.eventInterfaces.VTMMapPositionInterface;
import org.oscim.utils.FastMath;

import java.util.LinkedList;

public class MapPosition
{

    /**
     * Projected position x 0..1
     */
    public double x;

    /**
     * Projected position y 0..1
     */
    public double y;

    /**
     * Absolute scale
     * - use setScale() to modify
     */
    public double scale;

    /**
     * Rotation angle
     */
    public float bearing;

    /**
     * Perspective tilt
     */
    public float tilt;

    /**
     * Zoom-level for current scale.
     * - To be removed: FastMath.log2(scale)
     * - use setZoomLevel() to modify
     */
    public int zoomLevel;

    private LinkedList<VTMMapPositionInterface> positionListener = new LinkedList<>();

     public MapPosition() {
        this.scale = 1;
        this.x = 0.5;
        this.y = 0.5;
        this.zoomLevel = 1;
        this.bearing = 0;
        notifyPositionListener();
    }

    public MapPosition(double latitude, double longitude, double scale) {
        setTilt(this.getTilt());
        setPosition(latitude, longitude);
        setScale(scale);
    }

    public void addPositionListener(VTMMapPositionInterface listener) {
        if (!positionListener.contains(listener)) {
            positionListener.add(listener);
        }
    }

    public void removePositionListener(VTMMapPositionInterface listener) {
        positionListener.remove(listener);
    }

    private void notifyPositionListener() {
        for (VTMMapPositionInterface listener: positionListener)
        {
            listener.onBearingChanged(this.bearing);
        }
    }

    public double getX() {
        return x;
    }

    public MapPosition setX(double x) {
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public MapPosition setY(double y) {
        this.y = y;
        return this;
    }

    public float getBearing() {
        return bearing;
    }

    public MapPosition setBearing(float bearing) {
        notifyPositionListener();
        this.bearing = clampBearing(bearing);
        return this;
    }

    public float getTilt() {
        return tilt;
    }

    public MapPosition setTilt(float tilt) {
        this.tilt = tilt;
        return this;
    }

    public double getScale() {
        return scale;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public MapPosition setZoomLevel(int zoomLevel) {
        this.zoomLevel = zoomLevel;
        this.scale = 1 << zoomLevel;
        return this;
    }

    public MapPosition setScale(double scale) {
        this.zoomLevel = FastMath.log2((int) scale);
        this.scale = scale;
        return this;
    }

    public void setPosition(GeoPoint geoPoint) {
        setPosition(geoPoint.getLatitude(), geoPoint.getLongitude());
    }

    public void setPosition(double latitude, double longitude) {
        latitude = MercatorProjection.limitLatitude(latitude);
        longitude = MercatorProjection.limitLongitude(longitude);
        this.x = MercatorProjection.longitudeToX(longitude);
        this.y = MercatorProjection.latitudeToY(latitude);
    }

    public void copy(MapPosition other) {
        this.x = other.x;
        this.y = other.y;

        this.tilt = other.tilt;
        this.bearing = other.bearing;
        this.scale = other.scale;
        this.zoomLevel = other.zoomLevel;
        notifyPositionListener();
    }

    public void set(double x, double y, double scale, float bearing, float tilt) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.bearing = clampBearing(bearing);
        this.tilt = tilt;
        this.zoomLevel = FastMath.log2((int) scale);
        notifyPositionListener();
    }

    public void set(double scale, float bearing, float tilt) {
        this.scale = scale;
        this.bearing = clampBearing(bearing);
        this.tilt = tilt;
        this.zoomLevel = FastMath.log2((int) scale);
        notifyPositionListener();
    }

    private static float clampBearing(float bearing) {
        while (bearing > 180)
            bearing -= 360;
        while (bearing < -180)
            bearing += 360;

        return bearing;
    }

    /**
     * @return scale relative to zoom-level.
     */
    public double getZoomScale() {
        return scale / (1 << zoomLevel);
    }

    public GeoPoint getGeoPoint() {
        return new GeoPoint(MercatorProjection.toLatitude(y),
                MercatorProjection.toLongitude(x));
    }

    public double getLatitude() {
        return MercatorProjection.toLatitude(y);
    }

    public double getLongitude() {
        return MercatorProjection.toLongitude(x);
    }

    public void setByBoundingBox(BoundingBox bbox, int viewWidth, int viewHeight) {
        double minx = MercatorProjection.longitudeToX(bbox.getMinLongitude());
        double miny = MercatorProjection.latitudeToY(bbox.getMaxLatitude());

        double dx = Math.abs(MercatorProjection.longitudeToX(bbox.getMaxLongitude()) - minx);
        double dy = Math.abs(MercatorProjection.latitudeToY(bbox.getMinLatitude()) - miny);
        double zx = viewWidth / (dx * Tile.SIZE);
        double zy = viewHeight / (dy * Tile.SIZE);

        scale = Math.min(zx, zy);
        zoomLevel = FastMath.log2((int) scale);
        x = minx + dx / 2;
        y = miny + dy / 2;
        bearing = 0;
        //tilt = 0;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("[X:").append(x)
                .append(", Y:").append(y)
                .append(", Z:").append(zoomLevel)
                .append("] lat:")
                .append(MercatorProjection.toLatitude(y))
                .append(", lon:")
                .append(MercatorProjection.toLongitude(x))
                .toString();
    }
}
