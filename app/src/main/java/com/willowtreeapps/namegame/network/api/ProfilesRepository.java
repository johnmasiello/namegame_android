package com.willowtreeapps.namegame.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.willowtreeapps.namegame.network.api.model.Person;
import com.willowtreeapps.namegame.network.api.model.Profiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfilesRepository {

    @NonNull
    private final NameGameApi api;
    @NonNull
    private List<Listener> listeners = new ArrayList<>(1);
    @Nullable
    private List<Person> profiles;

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
                profiles = response.body();
                for (Listener listener : listeners) {
                    listener.onLoadFinished(profiles);
                }
                Log.d("Repository", "Success fetching profiles");
            }

            @Override
            public void onFailure(Call<List<Person>> call, Throwable t) {
                for (Listener listener : listeners) {
                    listener.onError(t);
                }

                Log.d("Repository", "Failure fetching profiles");
                Log.d("Repository", "Reason: "+t.getMessage());
                Log.d("Repository", "The original url was..." + call.request().url().toString());

                /*
                Failure fetching profiles
                Reason: java.lang.IllegalStateException: Expected BEGIN_OBJECT but was BEGIN_ARRAY at line 1 column 2 path $
                The original url was...https://willowtreeapps.com/api/v1.0/profiles
                 */

                // Using Api as Call<List<Person>> getProfiles();
                /*
                 * com.willowtreeapps.namegame D/Repository: Failure fetching profiles
                 Reason: java.lang.IllegalStateException: Expected a string but was BEGIN_OBJECT at line 1 column 2133 path $[4].socialLinks[0]
                 The original url was...https://willowtreeapps.com/api/v1.0/profiles
                 */

                // -> Solution need to update Social Links To match the spec: [{String, String, String}]
            }
        });
    }

    public void register(@NonNull Listener listener) {
        if (listeners.contains(listener)) throw new IllegalStateException("Listener is already registered.");
        listeners.add(listener);
        if (profiles != null) {
            listener.onLoadFinished(profiles);
        }
    }

    public void unregister(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public interface Listener {
        void onLoadFinished(@NonNull List<Person> people);
        void onError(@NonNull Throwable error);
    }

}
