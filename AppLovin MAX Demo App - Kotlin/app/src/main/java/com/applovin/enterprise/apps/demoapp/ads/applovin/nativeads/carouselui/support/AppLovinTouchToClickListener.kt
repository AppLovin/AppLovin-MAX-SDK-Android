package com.applovin.apps.kotlindemoapp.nativeads.carouselui.support

import android.content.Context
import android.view.MotionEvent
import android.view.View

class AppLovinTouchToClickListener(private val context: Context, private val clickListener: View.OnClickListener) : View.OnTouchListener
{
    /**
     * Max allowed duration for a "click", in milliseconds.
     */
    private val MAX_CLICK_DURATION = 1000

    /**
     * Max allowed distance to move during a "click", in DP.
     */
    private val MAX_CLICK_DISTANCE = 10

    private var pressStartTime: Long = 0
    private var pressedX: Float = 0.toFloat()
    private var pressedY: Float = 0.toFloat()

    override fun onTouch(v: View, e: MotionEvent): Boolean
    {
        when (e.action)
        {
            MotionEvent.ACTION_DOWN ->
            {
                pressStartTime = System.currentTimeMillis()
                pressedX = e.x
                pressedY = e.y
            }
            MotionEvent.ACTION_UP ->
            {
                val pressDuration = System.currentTimeMillis() - pressStartTime
                if (pressDuration < MAX_CLICK_DURATION && distance(pressedX, pressedY, e.x, e.y) < MAX_CLICK_DISTANCE)
                {
                    clickListener.onClick(v)
                }
            }
        }
        return true
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float
    {
        val dx = x1 - x2
        val dy = y1 - y2
        val distanceInPx = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        return pxToDp(distanceInPx)
    }

    private fun pxToDp(px: Float): Float
    {
        return px / context.resources.displayMetrics.density
    }
}