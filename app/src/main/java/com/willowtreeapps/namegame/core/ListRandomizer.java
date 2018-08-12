package com.willowtreeapps.namegame.core;

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
    public <T> List<T> pickN(@NonNull List<T> list, int n) { return pickN(list, n, NO_FILTER); }

    @NonNull
    public <T> List<T> pickN(@NonNull List<T> list, int n, ListFilter<T> filter) {
        if (list.size() == n) return list;
        if (n == 0) return Collections.emptyList();
        List<T> pickFrom = new ArrayList<>(list);
        List<T> picks = new ArrayList<>(n);
        int index;
        for (int i = 0; i < n; i++) {
            index = random.nextInt(pickFrom.size());

            SELECT_ITEM:
            while (true) {
                for (int j = index; j > -1; j--) {
                    if (filter.accept(pickFrom.get(j))) {
                        picks.add(pickFrom.remove(j));
                        break SELECT_ITEM;
                    }
                }
                for (int j = index + 1; j < pickFrom.size(); j++) {
                    if (filter.accept(pickFrom.get(j))) {
                        picks.add(pickFrom.remove(j));
                        break SELECT_ITEM;
                    }
                }
                break;
            }
        }
        return picks;
    }

    public int nextInt(int size) { return random.nextInt(size); }

    interface ListFilter<T> {
        boolean accept(T item);
    }
}
