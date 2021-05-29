package com.gpsplus.georef;

import java.io.Serializable;

public class LocationDTO implements Serializable {
    private double mLatitude;
    private double mLongitude;
    private double mAltitude;
    private float mBearing = 0.0f;

    public LocationDTO(double latitude, double longitude, double altitude) {
        mLatitude = latitude;
        mLongitude = longitude;
        mAltitude = altitude;
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
}
