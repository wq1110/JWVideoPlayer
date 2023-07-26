package com.media.jwvideoplayer.mvx.base;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;

public class ViewStub extends ViewGroup {
        View mV;

        public ViewStub(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (mV != null) {
                measureChild(mV, widthMeasureSpec, heightMeasureSpec);
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            if (mV != null) {
                mV.layout(0, 0, mV.getMeasuredWidth(), mV.getMeasuredHeight());
            }
        }

    @Override
    public void requestLayout() {
        super.requestLayout();
    }

    public void setView(View view) {
            mV = view;
            addView(view, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }

    public View getView() {
        return mV;
    }

    protected <T extends View> T findViewTraversal(@IdRes int id) {
            return mV.findViewById(id);
        }

    }