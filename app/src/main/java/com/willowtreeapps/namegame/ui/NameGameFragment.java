package com.willowtreeapps.namegame.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.willowtreeapps.namegame.R;
import com.willowtreeapps.namegame.core.GameLogic;
import com.willowtreeapps.namegame.core.NameGameApplication;
import com.willowtreeapps.namegame.network.api.model.Person;
import com.willowtreeapps.namegame.util.CircleBorderTransform;
import com.willowtreeapps.namegame.util.Ui;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class NameGameFragment extends Fragment implements GameLogic.Listener {

    private static final Interpolator OVERSHOOT = new OvershootInterpolator();

    @Inject
    GameLogic gameLogic;
    @Inject
    Picasso picasso;

    private TextView prompt;
    private Group container;
    private List<ImageView> faces = new ArrayList<>(6);
    private List<TextView> mNames = new ArrayList<>(6);
    private ProgressBar mProgressBar;
    private int mNumberOfImagesFinishedLoading;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NameGameApplication.get(getActivity()).component().inject(this);
        Log.d("fragment", "content fragment created");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.name_game_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        prompt = view.findViewById(R.id.prompt);
        mProgressBar = view.findViewById(R.id.myProgressBar);
        container = view.findViewById(R.id.face_container);
        view.findViewById(R.id.nextTurn).setOnClickListener(onPersonSelected);

        int[] ids = container.getReferencedIds();
        View person;

        for (int id: ids) {
            // Find the layout that will depict the person model
            person = view.findViewById(id);
            person.setOnClickListener(onPersonSelected);

            ImageView face = person.findViewById(R.id.portrait);
            faces.add(face);

            TextView name = person.findViewById(R.id.firstName);
            // Hide names
            name.setVisibility(View.INVISIBLE);
            mNames.add(name);
        }

        gameLogic.register(this);

        if (!gameLogic.isReady()) {
            // Show a progress bar, so the user knows the app is alive
            showProgressBar(true);
            showPrompt(false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        gameLogic.unregister(this);
    }

    /**
     * A method for setting the images from people into the imageviews
     * <p>Pre: faces.size() == people.size()</p>
     */
    private void setImages(List<ImageView> faces, List<Person> people) {
        int imageSize = getContext().getResources().getDimensionPixelSize(R.dimen.thumbSize);
        int n = people.size();
        String url;

        // Used to sync the Images when loading;
        mNumberOfImagesFinishedLoading = 0;

        // Hide the images until they have all finished loading, and show a progress bar
        showProgressBar(true);

        for (int i = 0; i < n; i++) {
            ImageView face = faces.get(i);

            url = Ui.urlPathWithScheme(people.get(i).getHeadshot().getUrl());
            if (url == null) {
                // The callback gets called when url != null
                // We need to offset the number of times it does not get called,
                // so we know when all of the images are finished being loaded
                mNumberOfImagesFinishedLoading++;
            }

            picasso.load(url)
                    .placeholder(R.drawable.ic_face_white_48dp)
                    .resize(imageSize, imageSize)
                    .centerCrop()
                    .transform(new CircleBorderTransform())
                    .noFade() // We want our own animation, not animation based on load order
                    .into(face, mPicassoCallback);
        }
    }

    private void updatePrompt(Person namedPerson) {
        prompt.setText(String.format(getString(R.string.question),
                namedPerson.getFirstName(),
                namedPerson.getLastName()
        ));
    }


    private void revealNames(List<TextView> names, GameLogic.PeopleLogic peopleLogic) {
        // Get the people backed by the thumbs
        List<Person> people = peopleLogic.currentThumbs();
        int n = people.size();

        // We make the Ui, pretty
        // by making the correct answer stand out
        TextView t;
        Resources res = getContext().getResources();
        int colorNormal = res.getColor(R.color.darkGray);
        int colorHighlight = res.getColor(peopleLogic.isCorrect() ? R.color.alphaGreen :
                R.color.alphaRed);
        int correctItemIndex = peopleLogic.correctItemIndex();

        for (int i = 0; i < n; i++) {
            t = names.get(i);
            t.setText(people.get(i).getFirstName());
            t.setTextColor(i == correctItemIndex ? colorHighlight : colorNormal);
            t.setVisibility(View.VISIBLE);
        }
    }

    private void hideNames(List<TextView> names) {
        for (TextView t : names)
            t.setVisibility(View.INVISIBLE);
    }

    /**
     * <p>Post: show == true -> container.getVisibility() == View.GONE</p>
     * @param show true -> show; false -> hide
     */
    private void showProgressBar(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) container.setVisibility(View.GONE);
    }

    private void showPrompt(boolean show) {
        prompt.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * <p>Synchronizes the loading of images, by displaying the container once all of them have
     * finished loading</p>
     * <p>Post: automatically hides the progress bar, via showProgressBar(false)</p>
     */
    private synchronized void onFinishedLoadingImage() {
        if (++mNumberOfImagesFinishedLoading == gameLogic.getPeopleLogic().numberOfThumbs()) {
            container.setVisibility(View.VISIBLE);
            showPrompt(true);
            showProgressBar(false);
            // TODO show/animate everything in the UI right here
        }
        Log.d("PicassoCallback", "finished loading image "+mNumberOfImagesFinishedLoading);
    }

    /**
     * A method to animate the faces into view
     */
    private void animateFacesIn() {
        prompt.animate().alpha(1).start();
        for (int i = 0; i < faces.size(); i++) {
            ImageView face = faces.get(i);
            face.animate().scaleX(1).scaleY(1).setStartDelay(800 + 120 * i).setInterpolator(OVERSHOOT).start();
        }
    }

    @Override
    public void onGameLogicLoadSuccess(@NonNull GameLogic.PeopleLogic peopleLogic) {
        setImages(faces, peopleLogic.currentThumbs());
        if (gameLogic.fullyRevealItems()) {
            revealNames(mNames, peopleLogic);
        } else {
            hideNames(mNames);
        }
        updatePrompt(peopleLogic.currentNames().get(0));
    }

    @Override
    public void onGameLogicLoadFail(@NonNull Throwable error) {
        Toast.makeText(getContext(), "Network unavailable", Toast.LENGTH_SHORT).show();
        showProgressBar(false);
    }

    final private View.OnClickListener onPersonSelected = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int index;
            GameLogic.PeopleLogic peopleLogic = gameLogic.getPeopleLogic();

            switch (v.getId()) {
                case R.id.one:
                    index = 0;
                    break;
                case R.id.two:
                    index = 1;
                    break;
                case R.id.three:
                    index = 2;
                    break;
                case R.id.four:
                    index = 3;
                    break;
                case R.id.five:
                    index = 4;
                    break;
                case R.id.six:
                    index = 5;
                    break;
                case R.id.nextTurn:
                    if (gameLogic.isReady()) {
                        // Update the state of the game, delegated by the 'p' in "MVP"
                        showPrompt(false);
                        peopleLogic.next();

                        // Update the Ui, the 'V' in "MVP"
                        setImages(faces, peopleLogic.currentThumbs());
                        hideNames(mNames);
                        updatePrompt(peopleLogic.currentNames().get(0));
                    }
                    return;

                default:
                    return;
            }
            if (gameLogic.fullyRevealItems()) return;

            peopleLogic.onItemSelected(index);

            // Update the Ui
            // We use currentThumbs as 2nd parameter, because it contains all of person objects
            // backing the thumbs, which is what we want
            revealNames(mNames, peopleLogic);
        }
    };

    final private Callback mPicassoCallback = new Callback() {
        @Override
        public void onSuccess() {
            onFinishedLoadingImage();
        }

        @Override
        public void onError() {
            onFinishedLoadingImage();
        }
    };
}
