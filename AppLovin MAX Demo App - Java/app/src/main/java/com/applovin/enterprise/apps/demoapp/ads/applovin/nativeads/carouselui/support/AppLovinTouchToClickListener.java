package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.support;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

public class AppLovinTouchToClickListener
        implements View.OnTouchListener
{
    /**
     * Max allowed duration for a "click", in milliseconds.
     */
    private static final int MAX_CLICK_DURATION = 1000;

    /**
     * Max allowed distance to move during a "click", in DP.
     */
    private static final int MAX_CLICK_DISTANCE = 10;

    private long  pressStartTime;
    private float pressedX;
    private float pressedY;

    private final Context              context;
    private final View.OnClickListener clickListener;

    public AppLovinTouchToClickListener(Context context, View.OnClickListener clickListener)
    {
        this.context = context;
        this.clickListener = clickListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent e)
    {
        switch ( e.getAction() )
        {
            case MotionEvent.ACTION_DOWN:
            {
                pressStartTime = System.currentTimeMillis();
                pressedX = e.getX();
                pressedY = e.getY();
                break;
            }
            case MotionEvent.ACTION_UP:
            {
                long pressDuration = System.currentTimeMillis() - pressStartTime;
                if ( pressDuration < MAX_CLICK_DURATION && distance( pressedX, pressedY, e.getX(), e.getY() ) < MAX_CLICK_DISTANCE )
                {
                    clickListener.onClick( v );
                }
            }
        }
        return true;
    }

    private float distance(float x1, float y1, float x2, float y2)
    {
        float dx = x1 - x2;
        float dy = y1 - y2;
        float distanceInPx = (float) Math.sqrt( dx * dx + dy * dy );
        return pxToDp( distanceInPx );
    }

    private float pxToDp(float px)
    {
        return px / context.getResources().getDisplayMetrics().density;
    }
}
