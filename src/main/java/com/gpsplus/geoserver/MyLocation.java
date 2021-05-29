package com.gpsplus.geoserver;

import com.gpsplus.georef.LocationDTO;

import java.io.Serializable;
import java.text.DecimalFormat;

public class MyLocation implements Serializable {

    private static final ThreadLocal<BearingDistanceCache> sBearingDistanceCache = ThreadLocal
            .withInitial(BearingDistanceCache::new);

    private double mLatitude;
    private double mLongitude;
    private double mAltitude;
    private float mBearing = 0.0f;

    private int refId;

    public MyLocation(double latitude, double longitude, double altitude, int refId) {
        mLatitude = latitude;
        mLongitude = longitude;
        mAltitude = altitude;
        this.refId = refId;
    }

    public MyLocation(LocationDTO loc, int refId) {
        mLatitude = loc.getLatitude();
        mLongitude = loc.getLongitude();
        mAltitude = loc.getAltitude();
        mBearing = loc.getBearing();
        this.refId = refId;
    }

    public LocationDTO myLocationToDTO() {
        return new LocationDTO(mLatitude, mLongitude, mAltitude);
    }


    private static void computeDistanceAndBearing(double lat1, double lon1,
                                                  double lat2, double lon2, BearingDistanceCache results) {
        // Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf

        int MAXITERS = 20;
        // Convert lat/long to radians
        lat1 *= Math.PI / 180.0;
        lat2 *= Math.PI / 180.0;
        lon1 *= Math.PI / 180.0;
        lon2 *= Math.PI / 180.0;

        double a = 6378137.0; // WGS84 major axis
        double b = 6356752.3142; // WGS84 semi-major axis
        double f = (a - b) / a;
        double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

        double L = lon2 - lon1;
        double A = 0.0;
        double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
        double U2 = Math.atan((1.0 - f) * Math.tan(lat2));

        double cosU1 = Math.cos(U1);
        double cosU2 = Math.cos(U2);
        double sinU1 = Math.sin(U1);
        double sinU2 = Math.sin(U2);
        double cosU1cosU2 = cosU1 * cosU2;
        double sinU1sinU2 = sinU1 * sinU2;

        double sigma = 0.0;
        double deltaSigma = 0.0;
        double cosSqAlpha;
        double cos2SM;
        double cosSigma;
        double sinSigma;
        double cosLambda = 0.0;
        double sinLambda = 0.0;

        double lambda = L; // initial guess
        for (int iter = 0; iter < MAXITERS; iter++) {
            double lambdaOrig = lambda;
            cosLambda = Math.cos(lambda);
            sinLambda = Math.sin(lambda);
            double t1 = cosU2 * sinLambda;
            double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
            double sinSqSigma = t1 * t1 + t2 * t2; // (14)
            sinSigma = Math.sqrt(sinSqSigma);
            cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
            sigma = Math.atan2(sinSigma, cosSigma); // (16)
            double sinAlpha = (sinSigma == 0) ? 0.0 :
                    cosU1cosU2 * sinLambda / sinSigma; // (17)
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
            cos2SM = (cosSqAlpha == 0) ? 0.0 :
                    cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)

            double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
            A = 1 + (uSquared / 16384.0) * // (3)
                    (4096.0 + uSquared *
                            (-768 + uSquared * (320.0 - 175.0 * uSquared)));
            double B = (uSquared / 1024.0) * // (4)
                    (256.0 + uSquared *
                            (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
            double C = (f / 16.0) *
                    cosSqAlpha *
                    (4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
            double cos2SMSq = cos2SM * cos2SM;
            deltaSigma = B * sinSigma * // (6)
                    (cos2SM + (B / 4.0) *
                            (cosSigma * (-1.0 + 2.0 * cos2SMSq) -
                                    (B / 6.0) * cos2SM *
                                            (-3.0 + 4.0 * sinSigma * sinSigma) *
                                            (-3.0 + 4.0 * cos2SMSq)));

            lambda = L +
                    (1.0 - C) * f * sinAlpha *
                            (sigma + C * sinSigma *
                                    (cos2SM + C * cosSigma *
                                            (-1.0 + 2.0 * cos2SM * cos2SM))); // (11)

            double delta = (lambda - lambdaOrig) / lambda;
            if (Math.abs(delta) < 1.0e-12) {
                break;
            }
        }

        results.mDistance = (float) (b * A * (sigma - deltaSigma));
        float initialBearing = (float) Math.atan2(cosU2 * sinLambda,
                cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
        initialBearing *= 180.0 / Math.PI;
        results.mInitialBearing = initialBearing;
        float finalBearing = (float) Math.atan2(cosU1 * sinLambda,
                -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
        finalBearing *= 180.0 / Math.PI;
        results.mFinalBearing = finalBearing;
        results.mLat1 = lat1;
        results.mLat2 = lat2;
        results.mLon1 = lon1;
        results.mLon2 = lon2;
    }

    public static void distanceBetween(double startLatitude, double startLongitude,
                                       double endLatitude, double endLongitude, float[] results) {
        if (results == null || results.length < 1) {
            throw new IllegalArgumentException("results is null or has length < 1");
        }
        BearingDistanceCache cache = sBearingDistanceCache.get();
        computeDistanceAndBearing(startLatitude, startLongitude,
                endLatitude, endLongitude, cache);
        results[0] = cache.mDistance;
        if (results.length > 1) {
            results[1] = cache.mInitialBearing;
            if (results.length > 2) {
                results[2] = cache.mFinalBearing;
            }
        }
    }

    public float distanceTo(MyLocation dest) {
        BearingDistanceCache cache = sBearingDistanceCache.get();
        if (mLatitude != cache.mLat1 || mLongitude != cache.mLon1 ||
                dest.mLatitude != cache.mLat2 || dest.mLongitude != cache.mLon2) {
            computeDistanceAndBearing(mLatitude, mLongitude, dest.mLatitude, dest.mLongitude, cache);
        }
        return cache.mDistance;
    }

    public float bearingTo(MyLocation dest) {
        BearingDistanceCache cache = sBearingDistanceCache.get();
        if (mLatitude != cache.mLat1 || mLongitude != cache.mLon1 ||
                dest.mLatitude != cache.mLat2 || dest.mLongitude != cache.mLon2) {
            computeDistanceAndBearing(mLatitude, mLongitude,
                    dest.mLatitude, dest.mLongitude, cache);
        }
        return cache.mInitialBearing;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }

    public double getAltitude() {
        return mAltitude;
    }

    public void setAltitude(double altitude) {
        mAltitude = altitude;
    }

    public float getBearing() {
        return mBearing;
    }

    public void setBearing(float bearing) {
        while (bearing < 0.0f) {
            bearing += 360.0f;
        }
        while (bearing >= 360.0f) {
            bearing -= 360.0f;
        }
        mBearing = bearing;
    }

    @Override
    public String toString() {
        return "com.gpsplus.geoserver.MyLocation{" +
                "mLatitude=" + mLatitude +
                ", mLongitude=" + mLongitude +
                ", mAltitude=" + mAltitude +
                ", mBearing=" + mBearing +
                '}';
    }

    public int getRefId() {
        return refId;
    }

    public void setRefId(int refId) {
        this.refId = refId;
    }

    private static class BearingDistanceCache implements Serializable {
        private double mLat1 = 0.0;
        private double mLon1 = 0.0;
        private double mLat2 = 0.0;
        private double mLon2 = 0.0;
        private float mDistance = 0.0f;
        private float mInitialBearing = 0.0f;
        private float mFinalBearing = 0.0f;
    }
}
