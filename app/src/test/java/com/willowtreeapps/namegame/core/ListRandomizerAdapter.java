package com.willowtreeapps.namegame.core;

import android.support.annotation.NonNull;

import com.willowtreeapps.namegame.core.gamelogic.ListRandomizer;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Used to avoid double-testing the ListRandomizer.
 * Used to create a test invariant
 *
 * When I tried mocking ListRandomizer, and using it to PickN when passing it a stubbed list value,
 * it returned an empty list, which is not what the real method should return
 */
public class ListRandomizerAdapter extends ListRandomizer {
    private ListRandomizerAdapter() {
        super(new Random(0));
    }

    public static ListRandomizer natural() {
        return new ListRandomizerAdapter() {
            @NonNull
            @Override
            public <T> T pickOne(@NonNull List<T> list) {
                return list.get(0);
            }

            @NonNull
            @Override
            public <T> List<T> pickN(@NonNull List<T> list, int n) {
                return list.subList(0, n);
            }

            @Override
            public int nextInt(int size) {
                return 0;
            }
        };
    }

    public static ListRandomizer reverse() {
        return new ListRandomizerAdapter() {
            @NonNull
            @Override
            public <T> T pickOne(@NonNull List<T> list) {
                return list.get(list.size() - 1);
            }

            @NonNull
            @Override
            public <T> List<T> pickN(@NonNull List<T> list, int n) {
                List<T> l1 = list.subList(0, n);
                Collections.reverse(l1);
                return l1;
            }

            @Override
            public int nextInt(int size) {
                return size - 1;
            }
        };
    }
}
