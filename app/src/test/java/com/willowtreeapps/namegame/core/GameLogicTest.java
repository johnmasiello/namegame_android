package com.willowtreeapps.namegame.core;

import android.support.test.filters.SmallTest;

import com.willowtreeapps.namegame.core.gamelogic.ListRandomizer;
import com.willowtreeapps.namegame.network.api.ProfilesRepository;
import com.willowtreeapps.namegame.network.api.model.Person;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SmallTest
public class GameLogicTest {
    private List<Person> people;

    @Before
    public void setUp() {
        people = new ArrayList<>();
        people.add(new Person("1", null, null, null, "Bill", "Smith", null, null));
        people.add(new Person("2", null, null, null, "Pam", "White", null, null));
        people.add(new Person("3", null, null, null, "Fred", "Doe", null, null));
        people.add(new Person("4", null, null, null, "Jeff", "Ward", null, null));
        people.add(new Person("5", null, null, null, "Ashley", "Joost", null, null));
        people.add(new Person("6", null, null, null, "Ben", "Frye", null, null));
    }

    @Test
    public void testIsNotReady() {
        // Mock the dependencies
        ListRandomizer listRandomizer = mock(ListRandomizer.class);
        ProfilesRepository profilesRepository = mock(ProfilesRepository.class);
        // Set the stubs
        when(profilesRepository.getResponseOutcome()).thenReturn(ProfilesRepository.ResponseOutcome.IN_PROGRESS);
        // Create the game logic
        GameLogic gameLogic = new GameLogic(listRandomizer, profilesRepository);
        // Run the test
        Assert.assertEquals("game logic should not be ready",
                false,
                gameLogic.isReady());
    }

    @Test
    public void testIsNotLoadSuccess() {
        // Mock the dependencies
        ListRandomizer listRandomizer = mock(ListRandomizer.class);
        ProfilesRepository profilesRepository = mock(ProfilesRepository.class);
        // Set the stubs
        when(profilesRepository.getResponseOutcome()).thenReturn(ProfilesRepository.ResponseOutcome.FAIL);
        // Create the game logic
        GameLogic gameLogic = new GameLogic(listRandomizer, profilesRepository);
        // Run the test
        Assert.assertEquals("game logic should not be ready",
                false,
                gameLogic.networkLoadSuccess());
    }

    @Test
    public void newListenerNotCalledWhenNameGameApiInProgress() {
        // Mock the dependencies
        ListRandomizer listRandomizer = mock(ListRandomizer.class);
        ProfilesRepository profilesRepository = mock(ProfilesRepository.class);
        // Set the stubs
        when(profilesRepository.getResponseOutcome()).thenReturn(ProfilesRepository.ResponseOutcome.IN_PROGRESS);
        // Create the game logic
        GameLogic gameLogic = new GameLogic(listRandomizer, profilesRepository);
        // Run the test
        GameLogic.Listener gameLogicListener = mock(GameLogic.Listener.class);
        gameLogic.register(gameLogicListener);
        verify(gameLogicListener, times(0)).
                onGameLogicLoadFail(any(Throwable.class));
        verify(gameLogicListener, times(0)).
                onGameLogicLoadSuccess(any(GameLogic.PeopleLogic.class));
    }

    @Test
    public void newListenerOnFailCalledWhenNameGameApiInFailed() {
        // Mock the dependencies
        ListRandomizer listRandomizer = mock(ListRandomizer.class);
        ProfilesRepository profilesRepository = mock(ProfilesRepository.class);
        // Set the stubs
        when(profilesRepository.getResponseOutcome()).thenReturn(ProfilesRepository.ResponseOutcome.FAIL);
        // Create the game logic
        GameLogic gameLogic = new GameLogic(listRandomizer, profilesRepository);
        // Run the test
        GameLogic.Listener gameLogicListener = mock(GameLogic.Listener.class);
        gameLogic.register(gameLogicListener);
        verify(gameLogicListener, times(1)).
                onGameLogicLoadFail(any(Throwable.class));
    }

    @Test
    public void newListenerOnSuccessNotCalledWhenNameGameApiInSucceededButGameNotYetStarted() {
        // Mock the dependencies
        ListRandomizer listRandomizer = mock(ListRandomizer.class);
        ProfilesRepository profilesRepository = mock(ProfilesRepository.class);
        // Set the stubs
        when(profilesRepository.getResponseOutcome()).thenReturn(ProfilesRepository.ResponseOutcome.SUCCESS);
        // Create the game logic
        GameLogic gameLogic = new GameLogic(listRandomizer, profilesRepository);
        // Run the test
        GameLogic.Listener gameLogicListener = mock(GameLogic.Listener.class);
        gameLogic.register(gameLogicListener);
        // PeopleLogic should be unset;
        // thus the callback is withheld
        verify(gameLogicListener, times(0)).
                onGameLogicLoadSuccess(any(GameLogic.PeopleLogic.class));
    }

    @Test
    public void newGameNotCreatedWhenNameGameApiInProgress_And_GameListenersOnFailCalled() {
        // Mock the dependencies
        ListRandomizer listRandomizer = mock(ListRandomizer.class);
        ProfilesRepository profilesRepository = mock(ProfilesRepository.class);
        // Set the stubs
        when(profilesRepository.getResponseOutcome()).thenReturn(ProfilesRepository.ResponseOutcome.IN_PROGRESS);
        // Create the game logic
        GameLogic gameLogic = new GameLogic(listRandomizer, profilesRepository);
        // Run the test
        GameLogic.Listener gameLogicListener = mock(GameLogic.Listener.class);
        gameLogic.register(gameLogicListener);

        boolean newGameCreated = gameLogic.newGame(GameLogic.Mode.STANDARD);
        Assert.assertEquals("New game should not be created; NameGameApi Call still in progress",
                false,
                newGameCreated);
        verify(gameLogicListener, times(1)).onGameLogicLoadFail(any(Throwable.class));
    }

    @Test
    public void newGameCreatedWhenNameGameApiSuccess_And_GameListenersOnSuccessCalled() {
        // Mock the dependencies
        ListRandomizer listRandomizer = ListRandomizerAdapter.reverse();
        ProfilesRepository profilesRepository = mock(ProfilesRepository.class);
        // Set the stubs
        when(profilesRepository.getResponseOutcome()).thenReturn(ProfilesRepository.ResponseOutcome.SUCCESS);
        when(profilesRepository.getProfiles()).thenReturn(people);
        // Create the game logic
        GameLogic gameLogic = new GameLogic(listRandomizer, profilesRepository);
        // Run the test
        GameLogic.Listener gameLogicListener = mock(GameLogic.Listener.class);
        gameLogic.register(gameLogicListener);
        // Trip a flag so the logic layer "thinks" the repo is ready
        gameLogic.onLoadSuccess(Collections.<Person>emptyList());
        // PeopleLogic should be unset;
        // thus the callback is withheld
        verify(gameLogicListener, times(0)).onGameLogicLoadSuccess(any(GameLogic.PeopleLogic.class));

        boolean newGameCreated = gameLogic.newGame(GameLogic.Mode.STANDARD);
        Assert.assertEquals("New game should be created successfully",
                true,
                newGameCreated);
        verify(gameLogicListener, times(1)).onGameLogicLoadSuccess(any(GameLogic.PeopleLogic.class));
    }

    @Test
    public void testSelectedPermutationOfPeople() {
        // Mock the dependencies
        ListRandomizer listRandomizer = ListRandomizerAdapter.reverse();
        ProfilesRepository profilesRepository = mock(ProfilesRepository.class);
        // Set the stubs
        when(profilesRepository.getResponseOutcome()).thenReturn(ProfilesRepository.ResponseOutcome.SUCCESS);
        when(profilesRepository.getProfiles()).thenReturn(people);
        // Create the game logic
        GameLogic gameLogic = new GameLogic(listRandomizer, profilesRepository);
        // Run the test
        // Trip a flag so the logic layer "thinks" the repo is ready
        gameLogic.onLoadSuccess(Collections.<Person>emptyList());
        gameLogic.newGame(GameLogic.Mode.STANDARD);
        List<Person> selectedPermutationOfPeople = gameLogic.getPeopleLogic()
                .currentThumbs();

        Assert.assertTrue("The first person's id should be 6",
                "6".equals(selectedPermutationOfPeople.get(0).getId()));
    }
}
