package com.willowtreeapps.namegame.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.willowtreeapps.namegame.R;
import com.willowtreeapps.namegame.core.GameLogic;
import com.willowtreeapps.namegame.core.NameGameApplication;
import com.willowtreeapps.namegame.network.api.ProfilesRepository;

import javax.inject.Inject;

public class NameGameActivity extends AppCompatActivity {

    private static final String FRAG_TAG = "NameGameFragmentTag";

    @Inject
    ProfilesRepository profilesRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.name_game_activity);
        NameGameApplication.get(this).component().inject(this);

        if (savedInstanceState == null) {
            // Kick the game off into Standard mode
            NameGameFragment nameGameFragment= NameGameFragment.newInstance(GameLogic.Mode.STANDARD);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, nameGameFragment, FRAG_TAG)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int gameMode;
        switch (item.getItemId()) {
            case R.id.refreshData:
                profilesRepository.refresh();
                return true;

            /*
                We define modes here so we reserve the option to use a distinct fragment,
                with a distinct Ui per Mode, if we so choose...
             */
            case R.id.mode_standard:
                gameMode = GameLogic.Mode.STANDARD;
                break;

            case R.id.mode_no_ghost:
                gameMode = GameLogic.Mode.NO_GHOST;
                break;

            case R.id.mode_mat:
                gameMode = GameLogic.Mode.MAT;
                break;

            case R.id.mode_cheat:
                gameMode = GameLogic.Mode.CHEAT;
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        // In the case of a "mode" was clicked on
        item.setChecked(true);

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAG_TAG);
        if (fragment != null &&
                fragment instanceof NameGameUIActionable)
            ((NameGameUIActionable)fragment).setMode(gameMode);
        return true;
    }
}
