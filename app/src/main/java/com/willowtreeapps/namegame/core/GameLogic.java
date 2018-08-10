package com.willowtreeapps.namegame.core;

import android.support.annotation.NonNull;

import com.willowtreeapps.namegame.network.api.ProfilesRepository;
import com.willowtreeapps.namegame.network.api.model.Person;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that maintains state and logic of the game. Wraps the ProfileRepository where game logic is
 * concerned. As such the game fragment should listen directly to this class in order for the game
 * state to be "live"
 */
public class GameLogic implements ProfilesRepository.Listener {
    private final ProfilesRepository mProfilesRepository;
    private final ListRandomizer mListRandomizer;
    /**
     * Equivalent to data in ProfileRepository is live
     */
    private boolean mReadyToLoadGame;
    private int mMode;
    private PeopleLogic peopleLogic;
    private ArrayList<Listener> listeners = new ArrayList<>(1);

    private final static String ERROR_MESSAGE_PROFILES =
            "Unable to begin game; data is stale or inaccessible";

    public static final class Mode {
        public static final int STANDARD = 0;
        public static final int REVERSE = 1;
    }

    public GameLogic(@NonNull ListRandomizer listRandomizer,
                     ProfilesRepository profilesRepository) {
        this.mListRandomizer = listRandomizer;
        this.mProfilesRepository = profilesRepository;
        setMode(Mode.STANDARD);
        mReadyToLoadGame = false;
        mProfilesRepository.register(this);
    }

    /**
     *
     * @return whether the game was successfully initialized to a new game. If the data is not yet
     * live, then the game state must defer its reset
     */
    public boolean newGame(int mode) {
        setMode(mode);

        if (mReadyToLoadGame) {
            peopleLogic.next();
            return true;
        }
        return false;
    }

    public int getMode() {
        return mMode;
    }

    // Keep the implementation simple to avoid side-effects
    protected final void setMode(int mode) {
        switch (mMode = mode) {
            case Mode.STANDARD:
                peopleLogic = new StandardModePeopleLogic();
                break;

            case Mode.REVERSE:
                peopleLogic = new ReverseModePeopleLogic();
                break;
        }
    }

    /**
     * <p>Pre: {@link #networkLoadSuccess()} == true</p>
     * @return
     */
    public PeopleLogic getPeopleLogic() {
        return peopleLogic;
    }

    /**
     * @return Whether the list of people has finished loading from the Network Module.
     */
    public boolean isReady() {
        return mProfilesRepository.getResponseOutcome() != ProfilesRepository.ResponseOutcome.IN_PROGRESS;
    }

    /**
     *
     * @return true iff the response of network api was a success
     */
    public boolean networkLoadSuccess() {
        return mProfilesRepository.getResponseOutcome() == ProfilesRepository.ResponseOutcome.SUCCESS;
    }

    // Allow for Listeners to this to be set up. Ui that depends of gameLogic should register here
    public void register(@NonNull Listener listener) {
        if (listeners.contains(listener)) throw new IllegalStateException("Listener is already registered.");
        listeners.add(listener);

        // Update listener if the data is alive
        switch (mProfilesRepository.getResponseOutcome()) {
            case ProfilesRepository.ResponseOutcome.SUCCESS:
                listener.onGameLogicLoadSuccess(peopleLogic);
                break;

            case ProfilesRepository.ResponseOutcome.FAIL:
                listener.onGameLogicLoadFail(new Throwable(ERROR_MESSAGE_PROFILES));
                break;
        }
    }

    public void unregister(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    // Implement Listener interface
    @Override
    public void onLoadSuccess(@NonNull List<Person> people) {
        mReadyToLoadGame = true;
        peopleLogic.next();

        for (Listener listener : listeners)
            listener.onGameLogicLoadSuccess(peopleLogic);
    }

    @Override
    public void onError(@NonNull Throwable error) {
        mReadyToLoadGame = false;

        Throwable g = new Throwable(ERROR_MESSAGE_PROFILES);
        for (Listener listener : listeners)
            listener.onGameLogicLoadFail(g);
    }

    private class StandardModePeopleLogic implements PeopleLogic {
        private final int NUMBER_OF_THUMBS  = 6;
        private final int NUMBER_OF_NAMES   = 1;

        private List<Person> mThumbs;
        private List<Person> mNames = new ArrayList<>(NUMBER_OF_NAMES);

        {
            mNames.add(null);
        }

        @Override
        public void next() {
            mThumbs = mListRandomizer.pickN(mProfilesRepository.getProfiles(), NUMBER_OF_THUMBS);
            mNames.set(0, mThumbs.get(mListRandomizer.nextInt(NUMBER_OF_NAMES)));
        }

        @Override
        public List<Person> currentThumbs() {
            return mThumbs;
        }

        @Override
        public List<Person> currentNames() {
            return mNames;
        }

        @Override
        public int numberOfThumbs() {
            return NUMBER_OF_THUMBS;
        }

        @Override
        public int numberOfNames() {
            return NUMBER_OF_NAMES;
        }
    }

    private class ReverseModePeopleLogic implements PeopleLogic {
        private final int NUMBER_OF_THUMBS  = 1;
        private final int NUMBER_OF_NAMES   = 5;

        private List<Person> mThumbs = new ArrayList<>(NUMBER_OF_THUMBS);
        private List<Person> mNames;

        {
            mThumbs.add(null);
        }

        @Override
        public void next() {
            mNames = mListRandomizer.pickN(mProfilesRepository.getProfiles(), NUMBER_OF_NAMES);
            mThumbs.set(0, mThumbs.get(mListRandomizer.nextInt(NUMBER_OF_THUMBS)));
        }

        @Override
        public List<Person> currentThumbs() {
            return mThumbs;
        }

        @Override
        public List<Person> currentNames() {
            return mNames;
        }

        @Override
        public int numberOfThumbs() {
            return NUMBER_OF_THUMBS;
        }

        @Override
        public int numberOfNames() {
            return NUMBER_OF_NAMES;
        }
    }

    /**
     * A Union of logic for each of the modes
     */
    public interface PeopleLogic {
        /**
         * Updates State to "next", may consist of new names, thumbs
         */
        void next();

        List<Person> currentThumbs();
        List<Person> currentNames();

        int numberOfThumbs();
        int numberOfNames();
    }

    public interface Listener {
        void onGameLogicLoadSuccess(@NonNull PeopleLogic peopleLogic);
        void onGameLogicLoadFail(@NonNull Throwable error);
    }
}


