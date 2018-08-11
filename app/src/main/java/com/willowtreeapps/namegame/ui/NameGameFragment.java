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
import android.widget.TextView;
import android.widget.Toast;

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

    // TODO remove toast as progressbar
    private Toast progressBar;

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
            // TODO show a progress bar, so the user knows the app is alive
            progressBar = Toast.makeText(getContext(), "In Progress", Toast.LENGTH_LONG);
            progressBar.show();
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

        for (int i = 0; i < n; i++) {
            ImageView face = faces.get(i);

            url = Ui.urlPathWithScheme(people.get(i).getHeadshot().getUrl());
            picasso.load(url)
                    .placeholder(R.drawable.ic_face_white_48dp)
                    .resize(imageSize, imageSize)
                    .transform(new CircleBorderTransform())
                    .into(face);
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



        // TODO remove progress bar
        if (progressBar != null) progressBar.cancel();
    }

    @Override
    public void onGameLogicLoadFail(@NonNull Throwable error) {
        Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();

        // TODO remove progress bar
        if (progressBar != null) progressBar.cancel();
    }

    private View.OnClickListener onPersonSelected = new View.OnClickListener() {
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
}
