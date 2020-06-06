package com.applovin.apps.kotlindemoapp.nativeads.carouselui.support

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView

/**
 * Provides a TextureView that maintains the aspect ratio of a video contained within.
 */
class AspectRatioTextureView : TextureView
{
    private var mVideoWidth: Int = 0
    private var mVideoHeight: Int = 0
    var onMeasureCompletionListener: OnMeasureCompletionListener? = null

    constructor(context: Context) : super(context)
    {

        mVideoWidth = 0
        mVideoHeight = 0
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    {

        mVideoWidth = 0
        mVideoHeight = 0
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    {

        mVideoWidth = 0
        mVideoHeight = 0
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int)
    {
        if (mVideoWidth <= 0 || mVideoHeight <= 0)
        {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val heightRatio = mVideoHeight.toFloat() / height.toFloat()
        val widthRatio = mVideoWidth.toFloat() / width.toFloat()

        val scaledHeight: Int
        val scaledWidth: Int

        if (heightRatio > widthRatio)
        {
            scaledHeight = Math.ceil((mVideoHeight.toFloat() / heightRatio).toDouble()).toInt()
            scaledWidth = Math.ceil((mVideoWidth.toFloat() / heightRatio).toDouble()).toInt()
        }
        else
        {
            scaledHeight = Math.ceil((mVideoHeight.toFloat() / widthRatio).toDouble()).toInt()
            scaledWidth = Math.ceil((mVideoWidth.toFloat() / widthRatio).toDouble()).toInt()
        }

        setMeasuredDimension(scaledWidth, scaledHeight)

        onMeasureCompletionListener?.onMeasureCompleted(scaledWidth, scaledHeight)
    }

    fun setVideoSize(videoWidth: Int, videoHeight: Int)
    {
        mVideoWidth = videoWidth
        mVideoHeight = videoHeight

        try
        {
            requestLayout()
            invalidate()
        }
        catch (ignore: Exception)
        {
        }

    }

    interface OnMeasureCompletionListener
    {
        fun onMeasureCompleted(adjustedWidth: Int, adjustedHeight: Int)
    }
}
