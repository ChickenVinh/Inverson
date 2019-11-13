package com.mahc.custombottomsheet;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class Antenna implements ClusterItem, Parcelable {
    private final LatLng mPosition;
    private final String mTitle;
    private String mAddress;
    private String mExtTitle;
    private String mRegion;
    private String mProvince;
    private Module[] modules;

    public Antenna(double lat, double lng) {
        mPosition = new LatLng(lat, lng);
        mTitle = null;
        mAddress = null;
        mExtTitle = null;
        mRegion = null;
        mProvince = null;
        modules = new Module[3];
    }
    //for SQL parse
    public Antenna(double lat, double lng, String title, String snippet, String ext_title, String region, String province) {
        mPosition = new LatLng(lat, lng);
        mTitle = title;
        mExtTitle = ext_title;
        mAddress = snippet;
        mRegion=region;
        mProvince=province;
    }
    //for CSV Parse
    public Antenna(double lat, double lng, String title, String snippet, String ext_title) {
        mPosition = new LatLng(lat, lng);
        mTitle = title;
        mExtTitle = ext_title;
        mAddress = snippet;
        mRegion = null;
        mProvince = null;
    }
    protected Antenna(Parcel in) {
        mPosition = in.readParcelable(LatLng.class.getClassLoader());
        mTitle = in.readString();
        mAddress = in.readString();
        mExtTitle = in.readString();
        mRegion=in.readString();
        mProvince=in.readString();
    }

    public static final Creator<Antenna> CREATOR = new Creator<Antenna>() {
        @Override
        public Antenna createFromParcel(Parcel in) {
            return new Antenna(in);
        }

        @Override
        public Antenna[] newArray(int size) {
            return new Antenna[size];
        }
    };

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
    @Override
    public String toString() {
        return mTitle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(mPosition, i);
        parcel.writeString(mTitle);
        parcel.writeString(mAddress);
        parcel.writeString(mExtTitle);
        parcel.writeString(mRegion);
        parcel.writeString(mProvince);
        parcel.writeArray(modules);
    }
}
