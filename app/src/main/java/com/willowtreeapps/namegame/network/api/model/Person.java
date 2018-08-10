package com.willowtreeapps.namegame.network.api.model;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Config;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Person implements Parcelable {
    // Pojo
    private final String id;
    private final String type;
    private final String slug;
    private final String jobTitle;
    private final String firstName;
    private final String lastName;
    private final Headshot headshot;
    private final List<SocialLinks> socialLinks;

    public Person(String id,
                  String type,
                  String slug,
                  String jobTitle,
                  String firstName,
                  String lastName,
                  Headshot headshot,
                  List<SocialLinks> socialLinks) {
        this.id = id;
        this.type = type;
        this.slug = slug;
        this.jobTitle = jobTitle;
        this.firstName = firstName;
        this.lastName = lastName;
        this.headshot = headshot;
        this.socialLinks = socialLinks;
    }

    private Person(Parcel in) {
        this.id = in.readString();
        this.type = in.readString();
        this.slug = in.readString();
        this.jobTitle = in.readString();
        this.firstName = in.readString();
        this.lastName = in.readString();
        this.headshot = in.readParcelable(Headshot.class.getClassLoader());

        this.socialLinks = new ArrayList<>();
        Parcelable[] socialLinks = in.readParcelableArray(SocialLinks.class.getClassLoader());
        if (socialLinks != null) {
            for (Parcelable x : socialLinks) {
                try {
                    this.socialLinks.add((SocialLinks)x);
                } catch (ClassCastException ignore) {
                    Log.d("JSON", "Unable to read social links for object with id="+this.id);
                }
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSlug() {
        return slug;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Headshot getHeadshot() {
        return headshot;
    }

    public List<SocialLinks> getSocialLinks() {
        return socialLinks;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.type);
        dest.writeString(this.slug);
        dest.writeString(this.jobTitle);
        dest.writeString(this.firstName);
        dest.writeString(this.lastName);
        dest.writeParcelable(this.headshot, flags);
        dest.writeParcelableArray(this.socialLinks.toArray(new Parcelable[socialLinks.size()]), flags);
    }

    public static final Creator<Person> CREATOR = new Creator<Person>() {
        @Override
        public Person createFromParcel(Parcel source) {
            return new Person(source);
        }

        @Override
        public Person[] newArray(int size) {
            return new Person[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}