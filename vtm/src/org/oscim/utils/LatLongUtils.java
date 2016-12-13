package org.oscim.utils;

/**
 * Created by sbrandt on 28.11.16.
 */

import org.oscim.core.GeoPoint;
import org.oscim.core.PointF;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A utility class to convert, calculate with, parse and validate geographical latitude/longitude coordinates.
 */
public final class LatLongUtils
{
    /**
     * The equatorial radius as defined by the <a href="http://en.wikipedia.org/wiki/World_Geodetic_System">WGS84
     * ellipsoid</a>. WGS84 is the reference coordinate system used by the Global Positioning System.
     */
    public static final double EQUATORIAL_RADIUS = 6378137.0;

    /**
     * Maximum possible latitude coordinate.
     */
    public static final double LATITUDE_MAX = 90;

    /**
     * Minimum possible latitude coordinate.
     */
    public static final double LATITUDE_MIN = -LATITUDE_MAX;

    /**
     * Maximum possible longitude coordinate.
     */
    public static final double LONGITUDE_MAX = 180;

    /**
     * Minimum possible longitude coordinate.
     */
    public static final double LONGITUDE_MIN = -LONGITUDE_MAX;

    /**
     * Conversion factor from degrees to microdegrees.
     */
    private static final double CONVERSION_FACTOR = 1000000.0;

    /** The Constant DELIMITER. */
    private static final String DELIMITER = ",";

    /**
     * Converts a coordinate from degrees to microdegrees (degrees * 10^6). No validation is performed.
     *
     * @param coordinate
     *         the coordinate in degrees.
     *
     * @return the coordinate in microdegrees (degrees * 10^6).
     */
    public static int degreesToMicrodegrees(double coordinate){
        return (int) (coordinate * CONVERSION_FACTOR);
    }

    /**
     * Creates a new GeoPoint from a comma-separated string of coordinates in the order latitude, longitude. All
     * coordinate values must be in degrees.
     *
     * @param GeoPointString
     *         the string that describes the GeoPoint.
     *
     * @return a new GeoPoint with the given coordinates.
     */
    public static GeoPoint fromString(String GeoPointString){
        double[] coordinates = parseCoordinateString(GeoPointString, 2);
        return new GeoPoint(coordinates[0], coordinates[1]);
    }

    /**
     * Parses a given number of comma-separated coordinate values from the supplied string.
     *
     * @param coordinatesString
     *         a comma-separated string of coordinate values.
     * @param numberOfCoordinates
     *         the expected number of coordinate values in the string.
     *
     * @return the coordinate values in the order they have been parsed from the string.
     *
     * @author mapsforge Team
     */
    public static double[] parseCoordinateString(String coordinatesString, int numberOfCoordinates){
        StringTokenizer stringTokenizer = new StringTokenizer(coordinatesString, DELIMITER, true);
        boolean isDelimiter = true;
        List<String> tokens = new ArrayList<String>(numberOfCoordinates);

        while (stringTokenizer.hasMoreTokens()){
            String token = stringTokenizer.nextToken();
            isDelimiter = !isDelimiter;
            if (isDelimiter){
                continue;
            }

            tokens.add(token);
        }

        if (isDelimiter){
            throw new IllegalArgumentException("invalid coordinate delimiter: " + coordinatesString);
        }
        else if (tokens.size() != numberOfCoordinates){
            throw new IllegalArgumentException("invalid number of coordinate values: " + coordinatesString);
        }

        double[] coordinates = new double[numberOfCoordinates];
        for (int i = 0; i < numberOfCoordinates; ++i){
            coordinates[i] = Double.parseDouble(tokens.get(i));
        }
        return coordinates;
    }

    /**
     * Calculates the amount of degrees of latitude for a given distance in meters.
     *
     * @param meters
     *         distance in meters
     *
     * @return latitude degrees
     *
     * @author mapsforge Team
     */
    public static double latitudeDistance(double meters){
        return (meters * 360) / (2 * Math.PI * EQUATORIAL_RADIUS);
    }

    /**
     * Calculates the amount of degrees of longitude for a given distance in meters.
     *
     * @param meters
     *         distance in meters
     * @param latitude
     *         the latitude at which the calculation should be performed
     *
     * @return longitude degrees
     *
     * @author mapsforge Team
     */
    public static double longitudeDistance(double meters, double latitude){
        return (meters * 360) / (2 * Math.PI * EQUATORIAL_RADIUS * Math.cos(Math.toRadians(latitude)));
    }

    public static double[] rotatePoint(double centerX, double centerY, double pointX, double pointY, double bearing) {
        return new double[]{centerX + (pointX - centerX) * Math.cos(bearing) - (pointY - centerY) * Math.sin(bearing),
                centerY + (pointX - centerX)*Math.sin(bearing) + (pointY - centerY)*Math.cos(bearing)};
    }


    /**
     * Converts a coordinate from microdegrees (degrees * 10^6) to degrees. No validation is performed.
     *
     * @param coordinate
     *         the coordinate in microdegrees (degrees * 10^6).
     *
     * @return the coordinate in degrees.
     */
    public static double microdegreesToDegrees(int coordinate){
        return coordinate / CONVERSION_FACTOR;
    }

    /**
     * Converts Metrics from Deg2Rad
     */
    public static double rad(double deg){
        return deg * Math.PI / 180;
    }

    /**
     * equiv. to {@code Math.abs()} but Sign-Safe.
     *
     * @param i
     *         - the Number to get Abs from
     *
     * @return - Abs.
     */
    public static int abs(int i){
        return i < 0 ? i * (-1) : i;
    }


    /**
     * equiv. to {@code Math.abs()} but really Sign-Safe.
     *
     * @param i
     *         - the Number to get Abs from
     *
     * @return - Abs.
     */
    public static double abs(double i){
        return i < 0 ? i * (-1) : i;
    }

    /**
     * Csonverts Metrics from Rad2Deg
     */
    public static double deg(double rad){
        return rad * 180 / Math.PI;
    }

    /**
     * Sign-Safe Modulo Operation.
     *
     * @param d
     *         the d
     * @param var2
     *         the var2
     *
     * @return the int
     */
    public static int mod(double d, int var2){
        int x1 = (int) Math.floor((double) d / (double) var2);
        return Math.abs((int) d - (var2 * x1));
    }

    /**
     * Calculates the distance between two points as an Orthodrome
     */
    public static double distance(double lon1, double lat1, double lon2, double lat2){
        double r = LatLongUtils.EQUATORIAL_RADIUS;
        return Math.acos(Math.sin(rad(lat1)) * Math.sin(rad(lat2)) +
                         Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.cos(rad(lon2 - lon1))) * r;
    }

    /**
     * Calculates a new Position as GeoPoint from a current Position, a Distance and a Bearing.
     */
    public static GeoPoint getPosFromBearingAndDistance(GeoPoint currentPos, double distance, double bearing){
        double dist = distance / 1000d;
        double lat1 = rad(currentPos.getLatitude());
        double lon1 = rad(currentPos.getLongitude());
        dist = dist / (EQUATORIAL_RADIUS / 1000); // Earth's radius in km
        double brng = rad(bearing);

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dist) + Math.cos(lat1) * Math.sin(dist) * Math.cos(brng));
        double lon2 = lon1 + Math.atan2(Math.sin(brng) * Math.sin(dist) * Math.cos(lat1),
                Math.cos(dist) - Math.sin(lat1) * Math.sin(lat2));
        lon2 = deg(((lon2 + 3 * Math.PI) % (2 * Math.PI)) - Math.PI);
        lat2 = deg(lat2);
        return new GeoPoint(lat2, lon2);
    }

    /**
     * Tests whether a Point is in a defined Circle or not
     *
     * @param center
     *         The Center of the Circle
     * @param r
     *         The Radius of the Circle
     * @param p
     *         The Point.
     *
     * @return true if in or on the Circle, false if not.
     */
    public static boolean hittestCircleWithPoint(GeoPoint center, double r, GeoPoint p){
        return distance(center, p) <= r;
    }

    /**
     * Calculates the Distance between to Positions in Meters.
     */
    public static double distance(GeoPoint node1, GeoPoint node2){
        return distance(node1.getLongitude(), node1.getLatitude(), node2.getLongitude(), node2.getLatitude());
    }

    /**
     * Calculates the Intersection of two small Circles.
     *
     * @param p1
     *         Center-Point of the first Circle
     * @param r1
     *         Radius of the first Circle
     * @param p2
     *         Center-Point of the second Circle
     * @param r2
     *         Radius of the second Circle
     *
     * @return null, if there are no intersections or an Array of GeoPoints with 1 or 2 entries depending on the number
     * of intersections.
     */
    public static GeoPoint[] scxsc(GeoPoint p1, double r1, GeoPoint p2, double r2){

        double dx = (p2.getLatitude() - p1.getLatitude());
        double dy = (p2.getLongitude() - p1.getLongitude());
        double d = distance(p1, p2);
        if (d > r1 + r2){
            return null; // In this Case we have no Intersection
        }
        else if (d < r1 - r2){
            return null; // The one Circle is contained in the Other.
        }
        else if (d == 0 && r1 == r2){
            return null; // Coincident Circles. NaN Solution.
        }
        else {
            GeoPoint[] resultSet = new GeoPoint[2];
            double a = (Math.pow(r1, 2) - Math.pow(r2, 2) + Math.pow(d, 2)) / (2.0d * d);
            double h = Math.sqrt(abs(Math.pow(r1, 2) - Math.pow(a, 2)));
            GeoPoint p3 = new GeoPoint(p1.getLatitude() + (dx * a / d), p1.getLongitude() + (dy * a / d));
            if (d == r1 + r2){
                resultSet[0] = p3; // The Circles are Touching, we have exactly
                // one Intersection
            }
            else {
                double x2 = p1.getLatitude() + (dx * a / d);
                double y2 = p1.getLongitude() + (dy * a / d);
                double rx = -dy * (h / d);
                double ry = dx * (h / d);
                resultSet[0] = new GeoPoint(x2 + rx, y2 + ry);
                resultSet[1] = new GeoPoint(x2 - rx, y2 - ry);
            }
            return resultSet;
        }
    }

    /**
     * Compute the distance from AB to C if isSegment is true, AB is a segment, not a line.
     */
    public static double linePointDist(GeoPoint A, GeoPoint B, GeoPoint C, boolean isSegment){
        double dist = cross(A, B, C) / distance(A, B);
        if (isSegment){
            double dot1 = dot(A, B, C);
            if (dot1 > 0){ return distance(B, C); }
            double dot2 = dot(B, A, C);
            if (dot2 > 0){ return distance(A, C); }
        }
        return abs(dist);
    }

    /**
     * Compute the cross product AB x AC
     */
    public static double cross(GeoPoint A, GeoPoint B, GeoPoint C){
        double ab_lat = B.getLatitude() - A.getLatitude();
        double ab_lon = B.getLongitude() - A.getLongitude();
        double ac_lat = C.getLatitude() - A.getLatitude();
        double ac_lon = C.getLongitude() - A.getLongitude();
        return ab_lat * ac_lon - ab_lon * ac_lat;
    }

    /**
     * Compute the dot product AB ⋅ BC
     */
    public static double dot(GeoPoint A, GeoPoint B, GeoPoint C){
        double ab_lat = B.getLatitude() - A.getLatitude();
        double ab_lon = B.getLongitude() - A.getLongitude();
        double bc_lat = C.getLatitude() - B.getLatitude();
        double bc_lon = C.getLongitude() - B.getLongitude();
        return ab_lat * bc_lat + ab_lon * bc_lon;
    }

    /**
     * Calculate new Point on given Vector with a known distance.
     */
    public static GeoPoint PointOnVectorWithDistance(GeoPoint A, GeoPoint B, double d){
        double ABx = B.getLatitude() - A.getLatitude();
        double ABy = B.getLongitude() - A.getLongitude();
        double L = Math.sqrt(Math.pow(ABx, 2) + Math.pow(ABy, 2));
        double ABNx = ABx / L;
        double ABNy = ABy / L;
        return new GeoPoint(A.getLatitude() + (ABNx * d), A.getLongitude() + (ABNy * d));
    }

    /**
     * Calculate the Bearing between two Points
     */
    public static double getBearing(GeoPoint pos1, GeoPoint pos2){
        double latitude1 = Math.toRadians(pos1.getLatitude());
        double latitude2 = Math.toRadians(pos2.getLatitude());
        double longDiff = Math.toRadians(pos2.getLongitude() - pos1.getLongitude());
        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) -
                   Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);
        return mod((Math.toDegrees(Math.atan2(y, x)) + 360), 360);
    }

    /**
     * Calculates the arithmetical middle point between the two given ones.
     *
     * @param pointA
     *         - {@code GeoPoint} point A
     * @param pointB
     *         - {@code GeoPoint} point B
     *
     * @return {@code GeoPoint} in the middle of point A and B
     */
    public static GeoPoint middle(GeoPoint pointA, GeoPoint pointB){
        double lat = (pointA.getLatitude() + pointB.getLatitude()) / 2;
        double lon = (pointA.getLongitude() + pointB.getLongitude()) / 2;
        return new GeoPoint(lat, lon);
    }

    /**
     * Calculates the Center of a Polygon defined by GeoPoints by: Convert each lat/long pair into a unit-length 3D
     * vector. Sum each of those vectors Normalise the resulting vector Convert back to spherical coordinates
     *
     * @param nodes
     *         The GeoPoints that are defining the Polygon.
     *
     * @return The center Position
     */
    public static GeoPoint centerOfPolygon(GeoPoint... nodes){
        int total = nodes.length;
        if (total == 0){ return null; }
        double x = 0d;
        double y = 0d;
        double z = 0d;
        for (GeoPoint node : nodes){
            if (node == null ){ continue; }
            x += Math.cos(rad(node.getLatitude())) * Math.cos(rad(node.getLongitude()));
            y += Math.cos(rad(node.getLatitude())) * Math.sin(rad(node.getLongitude()));
            z += Math.sin(rad(node.getLatitude()));
        }
        x /= total;
        y /= total;
        z /= total;
        return new GeoPoint(deg(Math.atan2(z, Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)))),
                deg(Math.atan2(y, x)));
    }


    public static GeoPoint centerOfPolygon(List<GeoPoint> points) {
        return centerOfPolygon(points.toArray(new GeoPoint[]{}));
    }

    /**
     * Calculates the Turn from the Northpole to the Edge between to Points
     */
    public static double turn2Pole(GeoPoint pos1, GeoPoint pos2){
        // difference in longitudinal coordinates
        double dLon = rad(pos2.getLongitude()) - rad(pos1.getLongitude());

        // difference in the phi of latitudinal coordinates
        double dPhi = Math.log(Math.tan(rad(pos2.getLatitude()) / 2 + Math.PI / 4) /
                               Math.tan(rad(pos1.getLatitude()) / 2 + Math.PI / 4));

        // we need to recalculate $dLon if it is greater than pi
        if (Math.abs(dLon) > Math.PI){
            if (dLon > 0){
                dLon = (2 * Math.PI - dLon) * -1;
            }
            else {
                dLon = 2 * Math.PI + dLon;
            }
        }
        // return the angle, normalized
        return (((deg(Math.atan2(dLon, dPhi)) + 360) % 360) + 180) % 360;
    }

    /**
     * Calculates the Minimal Angle betweet to given Bearings!
     */
    public static double smallestAngle(double bearingA, double bearingB){
        return abs(mod(((bearingA - bearingB) + 180), 360) - 180);
    }

    /**
     * Validate latitude.
     *
     * @param latitude
     *         the latitude coordinate in degrees which should be validated.
     */
    public static void validateLatitude(double latitude){
        if (Double.isNaN(latitude) || latitude < LATITUDE_MIN || latitude > LATITUDE_MAX){
            throw new IllegalArgumentException("invalid latitude: " + latitude);
        }
    }

    /**
     * Validate longitude.
     *
     * @param longitude
     *         the longitude coordinate in degrees which should be validated.
     */
    public static void validateLongitude(double longitude){
        if (Double.isNaN(longitude) || longitude < LONGITUDE_MIN || longitude > LONGITUDE_MAX){
            throw new IllegalArgumentException("invalid longitude: " + longitude);
        }
    }

    /**
     * Instantiates a new lat long utils.
     */
    private LatLongUtils(){
        throw new IllegalStateException();
    }


    /**
     * Gets the center of a polygone of given GeoPoints
     * @param GeoPoints
     * @return
     */
    public static GeoPoint getPolygoneMiddleFromPoints(GeoPoint[] GeoPoints){
        ArrayList<SortableGeoPoint> sortableGeoPoints = new ArrayList<>();
        Arrays.sort(GeoPoints);
        GeoPoint base = GeoPoints[GeoPoints.length-1];

        for(GeoPoint l : GeoPoints){
            sortableGeoPoints.add(new SortableGeoPoint(base, l));
        }
        Collections.sort(sortableGeoPoints);

        GeoPoints = new GeoPoint[sortableGeoPoints.size()];
        int i=0;
        for(SortableGeoPoint l : sortableGeoPoints){
            GeoPoints[i] = l.GeoPoint;
            i++;
        }

        return centerOfPolygon(GeoPoints);
    }

    /**
     * Class needed for polar angle sorting based on a given base point.
     */
    private static class SortableGeoPoint implements Comparable<SortableGeoPoint>{
        public GeoPoint GeoPoint;
        private GeoPoint base;

        public SortableGeoPoint(GeoPoint base, GeoPoint GeoPoint){
            this.GeoPoint = GeoPoint;
            this.base = base;
        }

        @Override
        public int compareTo(SortableGeoPoint another){
            double self = turn2Pole(base,GeoPoint);
            double other = turn2Pole(base,another.GeoPoint);

            if(self>other)
                return 1;
            else if (self<other)
                return -1;
            else
                return 0;
        }
    }

    public static boolean hittestPolygon(GeoPoint[] points, GeoPoint p) {
        int i;
        int j;
        double px=p.getLongitude() + 180;
        double py=p.getLatitude() + 90;

        boolean result = false;

        for (i = 0, j = points.length - 1; i < points.length; j = i++) {

            double ipx=points[i].getLongitude() + 180;
            double ipy=points[i].getLatitude() + 90;

            double jpx=points[j].getLongitude() + 180;
            double jpy=points[j].getLatitude() + 90;



            if ((ipy > py) != (jpy > py) &&
                (px < (jpx - ipx) * (py - ipy) / (jpy - ipy) + ipx)) {
                result = !result;
            }
        }
        return result;
    }
}