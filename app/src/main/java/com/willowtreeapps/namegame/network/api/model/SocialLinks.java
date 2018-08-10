package com.willowtreeapps.namegame.network.api.model;

import android.os.Parcel;
import android.os.Parcelable;

// Simple POJO to help satisfy the spec for the response body
public class SocialLinks implements Parcelable {
    private final String mType;
    private final String mCallToAction;
    private final String mUrl;

    public SocialLinks(String type,
                       String callToAction,
                       String url) {
        this.mType = type;
        this.mCallToAction = callToAction;
        this.mUrl = url;
    }

    private SocialLinks(Parcel in) {
        this.mType = in.readString();
        this.mCallToAction = in.readString();
        this.mUrl = in.readString();
    }

    public String getType() {
        return mType;
    }

    public String getCallToAction() {
        return mCallToAction;
    }

    public String getUrl() {
        return mUrl;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mType);
        dest.writeString(this.mCallToAction);
        dest.writeString(this.mUrl);
    }

    public static final Creator<SocialLinks> CREATOR = new Creator<SocialLinks>() {
        @Override
        public SocialLinks createFromParcel(Parcel in) {
            return new SocialLinks(in);
        }

        @Override
        public SocialLinks[] newArray(int size) {
            return new SocialLinks[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
