package com.example.maquirentapp.Utils;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

public class SafeNestedScrollView extends NestedScrollView {

    public SafeNestedScrollView(@NonNull Context context) {
        super(context);
    }

    public SafeNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SafeNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        try {
            super.onSizeChanged(w, h, oldw, oldh);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        try {
            super.requestChildFocus(child, focused);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        try {
            return super.requestChildRectangleOnScreen(child, rectangle, immediate);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }
}