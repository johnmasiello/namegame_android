package com.willowtreeapps.namegame.ui;

import com.willowtreeapps.namegame.core.GameLogic;

interface NameGameUIActionable {
    /**
     * <p>Sets the playing mode of the game</p>
     * @param mode Mode as defined by {@link GameLogic.Mode}
     */
    void setMode(int mode);
}
