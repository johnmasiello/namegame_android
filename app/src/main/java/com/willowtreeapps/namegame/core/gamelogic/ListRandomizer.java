package com.willowtreeapps.namegame.core.gamelogic;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ListRandomizer {
    private final static ListFilter NO_FILTER = new ListFilter() {
        @Override
        public boolean accept(Object item) {
            return true;
        }
    };

    @NonNull
    private final Random random;

    public ListRandomizer(@NonNull Random random) {
        this.random = random;
    }

    @NonNull
    public <T> T pickOne(@NonNull List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    @NonNull
    public <T> List<T> pickN(@NonNull List<T> list, int n) { //noinspection unchecked
        return pickN(list, n, NO_FILTER); }

    @NonNull
    public <T> List<T> pickN(@NonNull List<T> list, int n, ListFilter<T> filter) {
        if (n == 0) return Collections.emptyList();
        List<T> pickFrom = new ArrayList<>(list);
        List<T> picks = new ArrayList<>(n);
        int index;
        for (int i = 0; i < n; i++) {
            index = random.nextInt(pickFrom.size());

            // Iterate
            for (int j = index; j > -1; j--) {
                if (filter.accept(pickFrom.get(j))) {
                    picks.add(pickFrom.remove(j));

                    // item already found
                    // Move index to the end to prevent the next loop
                    index = pickFrom.size();
                    break;
                }
            }
            for (int j = index + 1; j < pickFrom.size(); j++) {
                if (filter.accept(pickFrom.get(j))) {
                    picks.add(pickFrom.remove(j));
                    break;
                }
            }
        }
        return picks;
    }

    public int nextInt(int size) { return random.nextInt(size); }

    public interface ListFilter<T> {
        boolean accept(T item);
    }
}
