package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.applovin.sdk.AppLovinSdkUtils;

/**
 * Created by mszaro on 3/10/15.
 */
public class LayoutUtils
{

    public static final int MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT;
    public static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;

    /**
     * NOTE FOR UNDERSTANDING GRAVITY / LAYOUT GRAVITY "Layout Gravity" affects your position in the superview. "Gravity" affects the position of your subviews within you.
     *
     * Said another way, Layout Gravity positions you yourself while gravity positions your children.
     */

    public static LinearLayout.LayoutParams createLinearParams(final int width, final int height, final int layoutGravity)
    {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( width, height );
        params.gravity = layoutGravity;
        return params;
    }

    public static FrameLayout.LayoutParams createFrameParams(final int width, final int height, final int layoutGravity)
    {
        final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams( width, height );
        params.gravity = layoutGravity;
        return params;
    }

    public static LinearLayout.LayoutParams createLinearParams(final int width, final int height, final int layoutGravity, final Margins margins)
    {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( width, height );
        params.gravity = layoutGravity;
        params.setMargins( margins.getLeft(), margins.getTop(), margins.getRight(), margins.getBottom() );
        return params;
    }

    public static FrameLayout.LayoutParams createFrameParams(final int width, final int height, final int layoutGravity, final Margins margins)
    {
        final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams( width, height );
        params.gravity = layoutGravity;
        params.setMargins( margins.getLeft(), margins.getTop(), margins.getRight(), margins.getBottom() );
        return params;
    }

    public static class Margins
    {
        private final int left;
        private final int top;
        private final int right;
        private final int bottom;

        public Margins(int left, int top, int right, int bottom)
        {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public int getLeft()
        {
            return left;
        }

        public int getTop()
        {
            return top;
        }

        public int getRight()
        {
            return right;
        }

        public int getBottom()
        {
            return bottom;
        }
    }

    public static class DPMargins extends Margins
    {
        public DPMargins(final Context context, int left, int top, int right, int bottom)
        {
            super(
                    AppLovinSdkUtils.dpToPx(context, left),
                    AppLovinSdkUtils.dpToPx( context, top ),
                    AppLovinSdkUtils.dpToPx( context, right ),
                    AppLovinSdkUtils.dpToPx( context, bottom ) );
        }
    }
}
