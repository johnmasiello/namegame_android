package com.willowtreeapps.namegame;


import android.support.test.filters.SmallTest;

import com.willowtreeapps.namegame.network.api.NameGameApi;
import com.willowtreeapps.namegame.network.api.ProfilesRepository;
import com.willowtreeapps.namegame.network.api.model.Person;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SmallTest
public class PeopleRepositoryTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private static final List<Person> PEOPLE;

    static {
        PEOPLE = new ArrayList<>();
        PEOPLE.add(new Person("1", null, null, null, "Bill", "Smith", null, null));
        PEOPLE.add(new Person("2", null, null, null, "Pam", "White", null, null));
        PEOPLE.add(new Person("3", null, null, null, "Fred", "Doe", null, null));
    }

    @Test
    public void should_throw_for_multiple_registration_of_one_listener() throws Exception {
        NameGameApi api = mock(NameGameApi.class);
        when(api.getProfiles()).thenReturn(SynchronousCallAdapter.forSuccess(PEOPLE));
        ProfilesRepository repo = new ProfilesRepository(api);
        ProfilesRepository.Listener listener = mock(ProfilesRepository.Listener.class);
        repo.register(listener);
        thrown.expect(IllegalStateException.class);
        repo.register(listener);
    }

    @Test
    public void should_allow_registration_of_multiple_listeners() throws Exception {
        NameGameApi api = mock(NameGameApi.class);
        when(api.getProfiles()).thenReturn(SynchronousCallAdapter.forSuccess(PEOPLE));
        ProfilesRepository repo = new ProfilesRepository(api);
        ProfilesRepository.Listener one = mock(ProfilesRepository.Listener.class);
        ProfilesRepository.Listener two = mock(ProfilesRepository.Listener.class);
        ProfilesRepository.Listener three = mock(ProfilesRepository.Listener.class);
        repo.register(one);
        repo.register(two);
        repo.register(three);
    }

    @Test
    public void should_notify_new_registrants_on_success() throws Exception {
        NameGameApi api = mock(NameGameApi.class);
        when(api.getProfiles()).thenReturn(SynchronousCallAdapter.forSuccess(PEOPLE));
        ProfilesRepository repo = new ProfilesRepository(api);
        ProfilesRepository.Listener listener = mock(ProfilesRepository.Listener.class);
        repo.register(listener);
        verify(listener, times(1)).onLoadSuccess(ArgumentMatchers.<Person>anyList());
    }

    @Test
    public void should_not_notify_new_registrants_on_load_failure() throws Exception {
        NameGameApi api = mock(NameGameApi.class);
        when(api.getProfiles()).thenReturn(SynchronousCallAdapter.<List<Person>>forError());
        ProfilesRepository repo = new ProfilesRepository(api);
        ProfilesRepository.Listener listener = mock(ProfilesRepository.Listener.class);
        repo.register(listener);

        // Verify the listener was not called for success
        verify(listener, times(0)).onLoadSuccess(ArgumentMatchers.<Person>anyList());

        // Verify the listener was called for error
        verify(listener, times(1)).onError(any(Throwable.class));
    }

    @Test
    public void should_notify_existing_registrants_on_load_failure() throws Exception {
        NameGameApi api = mock(NameGameApi.class);
        when(api.getProfiles()).thenReturn(SynchronousCallAdapter.<List<Person>>forError());
        ProfilesRepository.Listener listener = mock(ProfilesRepository.Listener.class);
        ProfilesRepository repo = new ProfilesRepository(api, listener);
        verify(listener, times(1)).onError(any(IOException.class));
    }
}
