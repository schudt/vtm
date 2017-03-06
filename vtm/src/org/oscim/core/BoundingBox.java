/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2014 Hannes Janetzek
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

import java.util.List;

/**
 * A BoundingBox represents an immutable set of two latitude and two longitude
 * coordinates.
 */
public class BoundingBox {
    /**
     * Conversion factor from degrees to microdegrees.
     */
    private static final double CONVERSION_FACTOR = 1000000000000000d;

    /**
     * The maximum latitude value of this BoundingBox in microdegrees (degrees *
     * 10^15).
     */
    public long maxLatitudeE15;

    /**
     * The maximum longitude value of this BoundingBox in microdegrees (degrees
     * * 10^15).
     */
    public long maxLongitudeE15;

    /**
     * The minimum latitude value of this BoundingBox in microdegrees (degrees *
     * 10^15).
     */
    public long minLatitudeE15;

    /**
     * The minimum longitude value of this BoundingBox in microdegrees (degrees
     * * 10^15).
     */
    public long minLongitudeE15;

    /**
     * @param minLatitudeE15  the minimum latitude in microdegrees (degrees * 10^15).
     * @param minLongitudeE15 the minimum longitude in microdegrees (degrees * 10^15).
     * @param maxLatitudeE15  the maximum latitude in microdegrees (degrees * 10^15).
     * @param maxLongitudeE15 the maximum longitude in microdegrees (degrees * 10^15).
     */
    public BoundingBox(long minLatitudeE15, long minLongitudeE15, long maxLatitudeE15, long maxLongitudeE15) {
        this.minLatitudeE15 = minLatitudeE15;
        this.minLongitudeE15 = minLongitudeE15;
        this.maxLatitudeE15 = maxLatitudeE15;
        this.maxLongitudeE15 = maxLongitudeE15;
    }

    /**
     * @param minLatitude  the minimum latitude coordinate in degrees.
     * @param minLongitude the minimum longitude coordinate in degrees.
     * @param maxLatitude  the maximum latitude coordinate in degrees.
     * @param maxLongitude the maximum longitude coordinate in degrees.
     */
    public BoundingBox(double minLatitude, double minLongitude, double maxLatitude, double maxLongitude) {
        this.minLatitudeE15 = (long) (minLatitude * 1E15);
        this.minLongitudeE15 = (long) (minLongitude * 1E15);
        this.maxLatitudeE15 = (long) (maxLatitude * 1E15);
        this.maxLongitudeE15 = (long) (maxLongitude * 1E15);
    }

    /**
     * @param geoPoints the coordinates list.
     */
    public BoundingBox(List<GeoPoint> geoPoints) {
        long minLat = Long.MAX_VALUE;
        long minLon = Long.MAX_VALUE;
        long maxLat = Long.MIN_VALUE;
        long maxLon = Long.MIN_VALUE;
        for (GeoPoint geoPoint : geoPoints) {
            minLat = Math.min(minLat, geoPoint.latitudeE6);
            minLon = Math.min(minLon, geoPoint.longitudeE6);
            maxLat = Math.max(maxLat, geoPoint.latitudeE6);
            maxLon = Math.max(maxLon, geoPoint.longitudeE6);
        }

        this.minLatitudeE15 = minLat;
        this.minLongitudeE15 = minLon;
        this.maxLatitudeE15 = maxLat;
        this.maxLongitudeE15 = maxLon;
    }

    /**
     * @param geoPoint the point whose coordinates should be checked.
     * @return true if this BoundingBox contains the given GeoPoint, false
     * otherwise.
     */
    public boolean contains(GeoPoint geoPoint) {
        return geoPoint.latitudeE6 <= maxLatitudeE15
                && geoPoint.latitudeE6 >= minLatitudeE15
                && geoPoint.longitudeE6 <= maxLongitudeE15
                && geoPoint.longitudeE6 >= minLongitudeE15;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof BoundingBox)) {
            return false;
        }
        BoundingBox other = (BoundingBox) obj;
        if (maxLatitudeE15 != other.maxLatitudeE15) {
            return false;
        } else if (maxLongitudeE15 != other.maxLongitudeE15) {
            return false;
        } else if (minLatitudeE15 != other.minLatitudeE15) {
            return false;
        } else if (minLongitudeE15 != other.minLongitudeE15) {
            return false;
        }
        return true;
    }

    /**
     * @param boundingBox the BoundingBox which this BoundingBox should be extended if it is larger
     * @return a BoundingBox that covers this BoundingBox and the given BoundingBox.
     */
    public BoundingBox extendBoundingBox(BoundingBox boundingBox) {
        return new BoundingBox(Math.min(this.minLatitudeE15, boundingBox.minLatitudeE15),
                Math.min(this.minLongitudeE15, boundingBox.minLongitudeE15),
                Math.max(this.maxLatitudeE15, boundingBox.maxLatitudeE15),
                Math.max(this.maxLongitudeE15, boundingBox.maxLongitudeE15));
    }

    /**
     * Creates a BoundingBox extended up to <code>GeoPoint</code> (but does not cross date line/poles).
     *
     * @param geoPoint coordinates up to the extension
     * @return an extended BoundingBox or this (if contains coordinates)
     */
    public BoundingBox extendCoordinates(GeoPoint geoPoint) {
        if (contains(geoPoint)) {
            return this;
        }

        double minLat = Math.max(MercatorProjection.LATITUDE_MIN, Math.min(getMinLatitude(), geoPoint.getLatitude()));
        double minLon = Math.max(MercatorProjection.LONGITUDE_MIN, Math.min(getMinLongitude(), geoPoint.getLongitude()));
        double maxLat = Math.min(MercatorProjection.LATITUDE_MAX, Math.max(getMaxLatitude(), geoPoint.getLatitude()));
        double maxLon = Math.min(MercatorProjection.LONGITUDE_MAX, Math.max(getMaxLongitude(), geoPoint.getLongitude()));

        return new BoundingBox(minLat, minLon, maxLat, maxLon);
    }

    /**
     * Creates a BoundingBox that is a fixed degree amount larger on all sides (but does not cross date line/poles).
     *
     * @param verticalExpansion   degree extension (must be >= 0)
     * @param horizontalExpansion degree extension (must be >= 0)
     * @return an extended BoundingBox or this (if degrees == 0)
     */
    public BoundingBox extendDegrees(double verticalExpansion, double horizontalExpansion) {
        if (verticalExpansion == 0 && horizontalExpansion == 0) {
            return this;
        } else if (verticalExpansion < 0 || horizontalExpansion < 0) {
            throw new IllegalArgumentException("BoundingBox extend operation does not accept negative values");
        }

        double minLat = Math.max(MercatorProjection.LATITUDE_MIN, getMinLatitude() - verticalExpansion);
        double minLon = Math.max(MercatorProjection.LONGITUDE_MIN, getMinLongitude() - horizontalExpansion);
        double maxLat = Math.min(MercatorProjection.LATITUDE_MAX, getMaxLatitude() + verticalExpansion);
        double maxLon = Math.min(MercatorProjection.LONGITUDE_MAX, getMaxLongitude() + horizontalExpansion);

        return new BoundingBox(minLat, minLon, maxLat, maxLon);
    }

    /**
     * Creates a BoundingBox that is a fixed margin factor larger on all sides (but does not cross date line/poles).
     *
     * @param margin extension (must be > 0)
     * @return an extended BoundingBox or this (if margin == 1)
     */
    public BoundingBox extendMargin(float margin) {
        if (margin == 1) {
            return this;
        } else if (margin <= 0) {
            throw new IllegalArgumentException("BoundingBox extend operation does not accept negative or zero values");
        }

        double verticalExpansion = (getLatitudeSpan() * margin - getLatitudeSpan()) * 0.5;
        double horizontalExpansion = (getLongitudeSpan() * margin - getLongitudeSpan()) * 0.5;

        double minLat = Math.max(MercatorProjection.LATITUDE_MIN, getMinLatitude() - verticalExpansion);
        double minLon = Math.max(MercatorProjection.LONGITUDE_MIN, getMinLongitude() - horizontalExpansion);
        double maxLat = Math.min(MercatorProjection.LATITUDE_MAX, getMaxLatitude() + verticalExpansion);
        double maxLon = Math.min(MercatorProjection.LONGITUDE_MAX, getMaxLongitude() + horizontalExpansion);

        return new BoundingBox(minLat, minLon, maxLat, maxLon);
    }

    /**
     * Creates a BoundingBox that is a fixed meter amount larger on all sides (but does not cross date line/poles).
     *
     * @param meters extension (must be >= 0)
     * @return an extended BoundingBox or this (if meters == 0)
     */
    public BoundingBox extendMeters(int meters) {
        if (meters == 0) {
            return this;
        } else if (meters < 0) {
            throw new IllegalArgumentException("BoundingBox extend operation does not accept negative values");
        }

        double verticalExpansion = GeoPoint.latitudeDistance(meters);
        double horizontalExpansion = GeoPoint.longitudeDistance(meters, Math.max(Math.abs(getMinLatitude()), Math.abs(getMaxLatitude())));

        double minLat = Math.max(MercatorProjection.LATITUDE_MIN, getMinLatitude() - verticalExpansion);
        double minLon = Math.max(MercatorProjection.LONGITUDE_MIN, getMinLongitude() - horizontalExpansion);
        double maxLat = Math.min(MercatorProjection.LATITUDE_MAX, getMaxLatitude() + verticalExpansion);
        double maxLon = Math.min(MercatorProjection.LONGITUDE_MAX, getMaxLongitude() + horizontalExpansion);

        return new BoundingBox(minLat, minLon, maxLat, maxLon);
    }

    public String format() {
        return new StringBuilder()
                .append(minLatitudeE15 / CONVERSION_FACTOR)
                .append(',')
                .append(minLongitudeE15 / CONVERSION_FACTOR)
                .append(',')
                .append(maxLatitudeE15 / CONVERSION_FACTOR)
                .append(',')
                .append(maxLongitudeE15 / CONVERSION_FACTOR)
                .toString();
    }

    /**
     * @return the GeoPoint at the horizontal and vertical center of this
     * BoundingBox.
     */
    public GeoPoint getCenterPoint() {
        long latitudeOffset = (maxLatitudeE15 - minLatitudeE15) / 2;
        long longitudeOffset = (maxLongitudeE15 - minLongitudeE15) / 2;
        return new GeoPoint(minLatitudeE15 + latitudeOffset, minLongitudeE15
                                                             + longitudeOffset);
    }

    /**
     * @return the latitude span of this BoundingBox in degrees.
     */
    public double getLatitudeSpan() {
        return getMaxLatitude() - getMinLatitude();
    }

    /**
     * @return the longitude span of this BoundingBox in degrees.
     */
    public double getLongitudeSpan() {
        return getMaxLongitude() - getMinLongitude();
    }

    /**
     * @return the maximum latitude value of this BoundingBox in degrees.
     */
    public double getMaxLatitude() {
        return maxLatitudeE15 / CONVERSION_FACTOR;
    }

    /**
     * @return the maximum longitude value of this BoundingBox in degrees.
     */
    public double getMaxLongitude() {
        return maxLongitudeE15 / CONVERSION_FACTOR;
    }

    /**
     * @return the minimum latitude value of this BoundingBox in degrees.
     */
    public double getMinLatitude() {
        return minLatitudeE15 / CONVERSION_FACTOR;
    }

    /**
     * @return the minimum longitude value of this BoundingBox in degrees.
     */
    public double getMinLongitude() {
        return minLongitudeE15 / CONVERSION_FACTOR;
    }

    @Override
    public int hashCode() {
        long result = 7;
        result = 31 * result + maxLatitudeE15;
        result = 31 * result + maxLongitudeE15;
        result = 31 * result + minLatitudeE15;
        result = 31 * result + minLongitudeE15;
        return (int)result;
    }

    /**
     * @param boundingBox the BoundingBox which should be checked for intersection with this BoundingBox.
     * @return true if this BoundingBox intersects with the given BoundingBox, false otherwise.
     */
    public boolean intersects(BoundingBox boundingBox) {
        if (this == boundingBox) {
            return true;
        }

        return getMaxLatitude() >= boundingBox.getMinLatitude() && getMaxLongitude() >= boundingBox.getMinLongitude()
                && getMinLatitude() <= boundingBox.getMaxLatitude() && getMinLongitude() <= boundingBox.getMaxLongitude();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("BoundingBox [minLat=")
                .append(minLatitudeE15)
                .append(", minLon=")
                .append(minLongitudeE15)
                .append(", maxLat=")
                .append(maxLatitudeE15)
                .append(", maxLon=")
                .append(maxLongitudeE15)
                .append("]")
                .toString();
    }
}
