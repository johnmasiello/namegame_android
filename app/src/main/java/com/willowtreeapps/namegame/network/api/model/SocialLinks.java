package com.willowtreeapps.namegame.network.api.model;

import android.os.Parcel;
import android.os.Parcelable;

// Simple POJO to help satisfy the spec for the response body
public class SocialLinks implements Parcelable {
    private final String type;
    private final String callToAction;
    private final String url;

    public SocialLinks(String type,
                       String callToAction,
                       String url) {
        this.type = type;
        this.callToAction = callToAction;
        this.url = url;
    }

    private SocialLinks(Parcel in) {
        this.type = in.readString();
        this.callToAction = in.readString();
        this.url = in.readString();
    }

    public String getType() {
        return type;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public String getUrl() {
        return url;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.type);
        dest.writeString(this.callToAction);
        dest.writeString(this.url);
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
