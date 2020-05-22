package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout

import com.applovin.sdk.AppLovinSdkUtils

/**
 * Created by mszaro on 3/10/15.
 */
object LayoutUtils
{

    val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
    val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT

    /**
     * NOTE FOR UNDERSTANDING GRAVITY / LAYOUT GRAVITY "Layout Gravity" affects your position in the superview. "Gravity" affects the position of your subviews within you.

     * Said another way, Layout Gravity positions you yourself while gravity positions your children.
     */

    fun createLinearParams(width: Int, height: Int, layoutGravity: Int): LinearLayout.LayoutParams
    {
        val params = LinearLayout.LayoutParams(width, height)
        params.gravity = layoutGravity
        return params
    }

    fun createFrameParams(width: Int, height: Int, layoutGravity: Int): FrameLayout.LayoutParams
    {
        val params = FrameLayout.LayoutParams(width, height)
        params.gravity = layoutGravity
        return params
    }

    fun createLinearParams(width: Int, height: Int, layoutGravity: Int, margins: Margins): LinearLayout.LayoutParams
    {
        val params = LinearLayout.LayoutParams(width, height)
        params.gravity = layoutGravity
        params.setMargins(margins.left, margins.top, margins.right, margins.bottom)
        return params
    }

    fun createFrameParams(width: Int, height: Int, layoutGravity: Int, margins: Margins): FrameLayout.LayoutParams
    {
        val params = FrameLayout.LayoutParams(width, height)
        params.gravity = layoutGravity
        params.setMargins(margins.left, margins.top, margins.right, margins.bottom)
        return params
    }

    open class Margins(val left: Int, val top: Int, val right: Int, val bottom: Int)

    class DPMargins(context: Context, left: Int, top: Int, right: Int, bottom: Int) : Margins(AppLovinSdkUtils.dpToPx(context, left), AppLovinSdkUtils.dpToPx(context, top), AppLovinSdkUtils.dpToPx(context, right), AppLovinSdkUtils.dpToPx(context, bottom))
}
