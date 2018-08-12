package com.willowtreeapps.namegame.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.willowtreeapps.namegame.network.api.model.Person;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.willowtreeapps.namegame.network.api.ProfilesRepository.ResponseOutcome.FAIL;
import static com.willowtreeapps.namegame.network.api.ProfilesRepository.ResponseOutcome.IN_PROGRESS;
import static com.willowtreeapps.namegame.network.api.ProfilesRepository.ResponseOutcome.SUCCESS;

public class ProfilesRepository {

    @NonNull
    private final NameGameApi api;
    @NonNull
    private List<Listener> listeners = new ArrayList<>(1);
    @Nullable
    private List<Person> profiles;

    private int responseOutcome = IN_PROGRESS;

    public static final class ResponseOutcome {
        public static final int SUCCESS         = 0;
        public static final int FAIL            = 1;
        public static final int IN_PROGRESS     = 2;
    }

    public ProfilesRepository(@NonNull NameGameApi api, Listener... listeners) {
        this.api = api;
        if (listeners != null) {
            this.listeners = new ArrayList<>(Arrays.asList(listeners));
        }
        load();
    }

    private void load() {
        // Retrofit2
        this.api.getProfiles().enqueue(new Callback<List<Person>>() {
            @Override
            public void onResponse(Call<List<Person>> call, Response<List<Person>> response) {
                responseOutcome = SUCCESS;
                profiles = response.body();
                for (Listener listener : listeners)
                    listener.onLoadSuccess(profiles);
            }

            @Override
            public void onFailure(Call<List<Person>> call, Throwable t) {
                responseOutcome = FAIL;

                for (Listener listener : listeners)
                    listener.onError(t);
            }
        });
    }

    public void refresh() {
        load();
    }

    public int getResponseOutcome() {
        return responseOutcome;
    }

    @Nullable
    public List<Person> getProfiles() { return profiles; }

    public void register(@NonNull Listener listener) {
        if (listeners.contains(listener))
            throw new IllegalStateException("Listener is already registered.");
        listeners.add(listener);
        // Update listener if the data is alive
        switch (responseOutcome) {
            case ResponseOutcome.SUCCESS:
                assert profiles != null;
                listener.onLoadSuccess(profiles);
                break;

            case ResponseOutcome.FAIL:
                listener.onError(new Throwable("Network Request failed"));
                break;
        }
    }

    public void unregister(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public interface Listener {
        void onLoadSuccess(@NonNull List<Person> people);
        void onError(@NonNull Throwable error);
    }
}
