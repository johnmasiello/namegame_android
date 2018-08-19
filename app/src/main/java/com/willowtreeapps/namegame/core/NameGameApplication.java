package com.willowtreeapps.namegame.core;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.willowtreeapps.namegame.network.NetworkModule;

public class NameGameApplication extends Application {

    private ApplicationComponent component;

    public static NameGameApplication get(@NonNull Context context) {
        return (NameGameApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        component = buildComponent();
    }

    public ApplicationComponent component() {
        return component;
    }

    private ApplicationComponent buildComponent() {
        // The modules are annotated with @Module So that dagger2 knows to include them
        return DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .networkModule(new NetworkModule("https://willowtreeapps.com/"))
                .build();
    }
}
