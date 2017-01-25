package com.fenchtose.mapsdemo.db;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Jay Rambhia on 8/2/16.
 */
public class LocationDbItem {

    private final double latitude;
    private final double longitude;
    private final int id;

    public static LocationDbItem newInstance(@NonNull LatLng latLng) {
        return new LocationDbItem(latLng.hashCode(), latLng.latitude, latLng.longitude);
    }

    protected LocationDbItem(int id, double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getId() {
        return id;
    }
}
