package com.mahc.custombottomsheet;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class Antenna implements ClusterItem {
    private final LatLng mPosition;
    private final String mTitle;
    private String mAddress;
    private String mExtTitle;

    public Antenna(double lat, double lng) {
        mPosition = new LatLng(lat, lng);
        mTitle = null;
        mAddress = null;
        mExtTitle = null;
    }

    public Antenna(double lat, double lng, String title, String snippet, String ext_title) {
        mPosition = new LatLng(lat, lng);
        mTitle = title;
        mExtTitle = ext_title;
        mAddress = snippet;
    }
    @Override
    public LatLng getPosition() {
        return mPosition;
    }
    @Override
    public String getTitle() {
        return mTitle;
    }
    @Override
    public String getSnippet() {
        return null;
    }

    public String getAddress() {
        return mAddress;
    }

    public String getExtTitle(){
        return mExtTitle;
    }
}
