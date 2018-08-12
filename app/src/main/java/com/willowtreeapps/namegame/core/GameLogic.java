package com.willowtreeapps.namegame.core;

import android.support.annotation.NonNull;
import android.text.TextUtils;

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
    private boolean mFullyRevealItems;
    private int mMode;
    private PeopleLogic peopleLogic;
    private ArrayList<Listener> listeners = new ArrayList<>(1);

    private final static String ERROR_MESSAGE_PROFILES =
            "Unable to begin game; data is stale or inaccessible";

    public static final class Mode {
        public static final int UNDEFINED   = -1; // Used to signify there is no current game in progress
        public static final int STANDARD    = 0;
        public static final int NO_GHOST    = 1;
        public static final int MAT         = 2;
        public static final int CHEAT       = 3;
        public static final int REVERSE     = 4;
    }

    public GameLogic(@NonNull ListRandomizer listRandomizer,
                     ProfilesRepository profilesRepository) {
        this.mListRandomizer = listRandomizer;
        this.mProfilesRepository = profilesRepository;
        mReadyToLoadGame = false;
        mMode = Mode.UNDEFINED;
        mProfilesRepository.register(this);
    }

    /**
     *
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

    public int getMode() {
        return mMode;
    }

    // Keep the implementation simple to avoid side-effects
    protected final void setMode(int mode) {
        switch (mMode = mode) {
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
     * @return
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
        mFullyRevealItems = false;

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
        protected final int NUMBER_OF_THUMBS  = 6;
        protected final int NUMBER_OF_NAMES   = 1;

        protected List<Person> mThumbs;
        protected List<Person> mNames = new ArrayList<>(NUMBER_OF_NAMES);
        protected int mNumberOfPeople;
        protected int correctIndex;
        protected boolean correct;
        {
            mNames.add(null);
        }

        @Override
        public void next() {
            mThumbs = mListRandomizer.pickN(mProfilesRepository.getProfiles(), NUMBER_OF_THUMBS);
            mNumberOfPeople = mThumbs.size();
            mNames.set(0, mThumbs.get(correctIndex = mListRandomizer.nextInt(mNumberOfPeople)));
            mFullyRevealItems = false;
        }

        @Override
        public void onItemSelected(int index) {
            if (TextUtils.equals(mThumbs.get(index).getId(), mNames.get(0).getId())) {
                // Update any additional state
                correct = mFullyRevealItems = true;
            } else {
                correct = false;
            }
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
            return mNames;
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
            mThumbs = mListRandomizer.pickN(mProfilesRepository.getProfiles(),
                    NUMBER_OF_THUMBS,
                    noGhost);
            mNumberOfPeople = mThumbs.size();
            mNames.set(0, mThumbs.get(correctIndex = mListRandomizer.nextInt(mNumberOfPeople)));
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
            mThumbs = mListRandomizer.pickN(mProfilesRepository.getProfiles(),
                    NUMBER_OF_THUMBS,
                    mat);
            mNumberOfPeople = mThumbs.size();
            mNames.set(0, mThumbs.get(correctIndex = mListRandomizer.nextInt(mNumberOfPeople)));
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
            mNames.set(0, mThumbs.get(correctIndex = 0));
        }
    }

    private class ReverseModePeopleLogic implements PeopleLogic {
        protected final int NUMBER_OF_THUMBS  = 1;
        protected final int NUMBER_OF_NAMES   = 5;

        protected List<Person> mThumbs = new ArrayList<>(NUMBER_OF_THUMBS);
        protected List<Person> mNames;
        protected int mNumberOfPeople;
        protected int correctIndex;
        protected boolean correct;

        {
            mThumbs.add(null);
        }

        @Override
        public void next() {
            mNames = mListRandomizer.pickN(mProfilesRepository.getProfiles(), NUMBER_OF_NAMES);
            mNumberOfPeople = mNames.size();
            mThumbs.set(0, mThumbs.get(correctIndex = mListRandomizer.nextInt(mNumberOfPeople)));
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
            return mThumbs;
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


