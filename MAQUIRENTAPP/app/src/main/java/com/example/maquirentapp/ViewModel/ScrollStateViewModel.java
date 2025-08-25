package com.example.maquirentapp.ViewModel;

import androidx.lifecycle.ViewModel;

import java.util.HashMap;

public class ScrollStateViewModel extends ViewModel {
    private final HashMap<String, Integer> scrollPositions = new HashMap<>();

    public void saveScrollPosition(String fragmentKey, int scrollY) {
        scrollPositions.put(fragmentKey, scrollY);
    }

    public int getScrollPosition(String fragmentKey) {
        return scrollPositions.getOrDefault(fragmentKey, 0);
    }
}
