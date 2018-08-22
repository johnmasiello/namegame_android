package com.willowtreeapps.namegame.core;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.willowtreeapps.namegame.core.gamelogic.ListRandomizer;
import com.willowtreeapps.namegame.network.api.ProfilesRepository;
import com.willowtreeapps.namegame.network.api.model.Person;

import java.util.ArrayList;
import java.util.Collections;
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
    private boolean mFullyRevealItems;
    private int mMode;
    private PeopleLogic peopleLogic;
    private final ArrayList<Listener> listeners = new ArrayList<>(1);

    private final static String ERROR_MESSAGE_PROFILES =
            "Unable to begin game; data is stale or inaccessible";

    /**
     * Class that describes the variant, or mode of play: UNDEFINED, STANDARD, NO_GHOST, MAT, CHEAT,
     * REVERSE
     */
    public static final class Mode {
        public static final int UNDEFINED   = -1; // Used to signify there is no current game in progress
        public static final int STANDARD    = 0;
        public static final int NO_GHOST    = 1;
        public static final int MAT         = 2;
        public static final int CHEAT       = 3;
        static final int REVERSE     = 4;
    }

    GameLogic(@NonNull ListRandomizer listRandomizer,
                     ProfilesRepository profilesRepository) {
        this.mListRandomizer = listRandomizer;
        this.mProfilesRepository = profilesRepository;
        mReadyToLoadGame = false;
        mMode = Mode.UNDEFINED;
        mProfilesRepository.register(this);
    }

    /**
     * @param mode Mode of play, defined by {@link GameLogic.Mode}
     * @return whether the game was successfully initialized to a new game. If the data is not yet
     * live, then the game state must defer its reset
     */
    public boolean newGame(int mode) {
        setMode(mode);
        if (mode == Mode.UNDEFINED) {
            // The game is undefined, so state need not be set/defined
            return false;
        }

        mFullyRevealItems = false;

        if (mReadyToLoadGame) {
            peopleLogic.next();

            // Emit the change of state to the listeners
            for (Listener listener : listeners)
                listener.onGameLogicLoadSuccess(peopleLogic);
            return true;
        } else {
            // Emit the change of state to the listeners
            for (Listener listener : listeners)
                listener.onGameLogicLoadFail(new Throwable("Unable to create new game"));
            return false;
        }
    }

    /**
     *
     * @return Mode of play, defined by {@link GameLogic.Mode}
     */
    public int getMode() {
        return mMode;
    }

    // Keep the implementation simple to avoid side-effects

    /**
     *
     * @param mode Mode of play, defined by {@link GameLogic.Mode}
     */
    private void setMode(int mode) {
        mMode = mode;
        switch (mode) {
            case Mode.STANDARD:
                peopleLogic = new StandardModePeopleLogic();
                break;

            case Mode.NO_GHOST:
                peopleLogic = new NoGhostPeopleLogic();
                break;

            case Mode.MAT:
                peopleLogic = new MatPeopleLogic();
                break;

            case Mode.CHEAT:
                peopleLogic = new CheatPeopleLogic();
                break;

            case Mode.REVERSE:
                peopleLogic = new ReverseModePeopleLogic();
                break;
        }
    }

    /**
     * <p>Pre: {@link #networkLoadSuccess()} == true</p>
     * <p>Post: Do not hold on to a reference to people logic</p>
     * @return An implementation of peopleLogic, which can be used to update the state of the game
     */
    public PeopleLogic getPeopleLogic() {
        return peopleLogic;
    }

    public boolean fullyRevealItems() { return mFullyRevealItems; }

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
                if (peopleLogic != null)
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
        if (peopleLogic == null) {
            mReadyToLoadGame = true;
            return;
        }
        // The case the game was not already started
        if (!mReadyToLoadGame) {
            // Begin the game now...
            peopleLogic.next();
        }
        mReadyToLoadGame = true;

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
        final int NUMBER_OF_THUMBS  = 6;

        List<Person> mThumbs;
        Person mName;
        int mNumberOfPeople;
        int correctIndex;
        boolean correct;

        @Override
        public void next() {
            if (mProfilesRepository.getProfiles() == null)
                return;
            mThumbs = mListRandomizer.pickN(mProfilesRepository.getProfiles(), NUMBER_OF_THUMBS);
            mNumberOfPeople = mThumbs.size();
            correctIndex = mListRandomizer.nextInt(mNumberOfPeople);
            mName = mThumbs.get(correctIndex);
            mFullyRevealItems = false;
        }

        @Override
        public void onItemSelected(int index) {
            correct = index < mNumberOfPeople &&
                    TextUtils.equals(mThumbs.get(index).getId(),
                            mName.getId());
            mFullyRevealItems = true;
        }

        @Override
        public int correctItemIndex() {
            return correctIndex;
        }

        @Override
        public boolean isCorrect() {
            return correct;
        }

        @Override
        public List<Person> currentThumbs() {
            return mThumbs;
        }

        @Override
        public List<Person> currentNames() {
            return Collections.singletonList(mName);
        }

        @Override
        public int numberOfPeople() {
            return mNumberOfPeople;
        }
    }

    private class NoGhostPeopleLogic extends StandardModePeopleLogic {
        private final ListRandomizer.ListFilter<Person> noGhost = new ListRandomizer.ListFilter<Person>() {
            @Override
            public boolean accept(Person item) {
                return item.getHeadshot().getUrl() != null && !item.getHeadshot().getUrl().contains("TEST");
            }
        };

        @Override
        public void next() {
            if (mProfilesRepository.getProfiles() == null)
                return;
            mThumbs = mListRandomizer.pickN(mProfilesRepository.getProfiles(),
                    NUMBER_OF_THUMBS,
                    noGhost);
            mNumberOfPeople = mThumbs.size();
            correctIndex = mListRandomizer.nextInt(mNumberOfPeople);
            mName = mThumbs.get(correctIndex);
            mFullyRevealItems = false;
        }
    }

    private class MatPeopleLogic extends StandardModePeopleLogic {
        private final ListRandomizer.ListFilter<Person> mat = new ListRandomizer.ListFilter<Person>() {
            @Override
            public boolean accept(Person item) {
                return item.getFirstName().toLowerCase().contains("mat");
            }
        };

        @Override
        public void next() {
            if (mProfilesRepository.getProfiles() == null)
                return;
            mThumbs = mListRandomizer.pickN(mProfilesRepository.getProfiles(),
                    NUMBER_OF_THUMBS,
                    mat);
            mNumberOfPeople = mThumbs.size();
            correctIndex = mListRandomizer.nextInt(mNumberOfPeople);
            mName = mThumbs.get(correctIndex);
            mFullyRevealItems = false;
        }
    }

    /**
     * An enhanced alternative would be to keep the behavior of the base class intact,
     * and adapt the 'cheating' behavior in the UI instead
     */
    private class CheatPeopleLogic extends StandardModePeopleLogic {
        @Override
        public void next() {
            super.next();
            // The answer is always the top-left corner
            correctIndex = 0;
            mName = mThumbs.get(correctIndex);
        }
    }

    private class ReverseModePeopleLogic implements PeopleLogic {
        final int NUMBER_OF_NAMES   = 5;

        Person mThumb;
        List<Person> mNames;
        int mNumberOfPeople;
        int correctIndex;
        boolean correct;

        @Override
        public void next() {
            if (mProfilesRepository.getProfiles() == null)
                return;
            mNames = mListRandomizer.pickN(mProfilesRepository.getProfiles(), NUMBER_OF_NAMES);
            mNumberOfPeople = mNames.size();
            correctIndex = mListRandomizer.nextInt(mNumberOfPeople);
            mThumb = mNames.get(correctIndex);
        }

        @Override
        public void onItemSelected(int index) {}

        @Override
        public int correctItemIndex() {
            return correctIndex;
        }

        @Override
        public boolean isCorrect() {
            return correct;
        }

        @Override
        public List<Person> currentThumbs() {
            return Collections.singletonList(mThumb);
        }

        @Override
        public List<Person> currentNames() {
            return mNames;
        }

        @Override
        public int numberOfPeople() {
            return mNumberOfPeople;
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

        /**
         * Item could be a person or a name
         * @param index the index of the item relative to the sublist of names or thumbs
         *
         */
        void onItemSelected(int index);
        int correctItemIndex();
        boolean isCorrect();

        List<Person> currentThumbs();
        List<Person> currentNames();

        int numberOfPeople();
    }

    public interface Listener {
        void onGameLogicLoadSuccess(@NonNull PeopleLogic peopleLogic);
        void onGameLogicLoadFail(@NonNull Throwable error);
    }
}


