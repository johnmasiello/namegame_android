package com.willowtreeapps.namegame.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

    private TextView title;
    private ViewGroup container;
    private List<ImageView> faces = new ArrayList<>(5);

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
        title = (TextView) view.findViewById(R.id.title);
        container = (ViewGroup) view.findViewById(R.id.face_container);

        //Hide the views until data loads
//        title.setAlpha(0);

        int n = container.getChildCount();
        for (int i = 0; i < n; i++) {
            ImageView face = (ImageView) container.getChildAt(i).findViewById(R.id.portrait);
            faces.add(face);

            //Hide the views until data loads
//            face.setScaleX(0);
//            face.setScaleY(0);
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
     */
    private void setImages(List<ImageView> faces, List<Person> people) {
        int imageSize = getContext().getResources().getDimensionPixelSize(R.dimen.thumbSize);
        int n = faces.size() < people.size() ? faces.size() : people.size();
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

    /**
     * A method to animate the faces into view
     */
    private void animateFacesIn() {
        title.animate().alpha(1).start();
        for (int i = 0; i < faces.size(); i++) {
            ImageView face = faces.get(i);
            face.animate().scaleX(1).scaleY(1).setStartDelay(800 + 120 * i).setInterpolator(OVERSHOOT).start();
        }
    }

    /**
     * A method to handle when a person is selected
     *
     * @param view   The view that was selected
     * @param person The person that was selected
     */
    private void onPersonSelected(@NonNull View view, @NonNull Person person) {
        //TODO evaluate whether it was the right person and make an action based on that
    }

    @Override
    public void onGameLogicLoadSuccess(@NonNull GameLogic.PeopleLogic peopleLogic) {
        setImages(faces, peopleLogic.currentThumbs());
        Person namedPerson = peopleLogic.currentNames().get(0);
        title.setText(String.format(getString(R.string.question),
                namedPerson.getFirstName(),
                namedPerson.getLastName()
                ));


        // TODO remove progress bar
        if (progressBar != null) progressBar.cancel();
    }

    @Override
    public void onGameLogicLoadFail(@NonNull Throwable error) {
        Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();

        // TODO remove progress bar
        if (progressBar != null) progressBar.cancel();
    }
}
