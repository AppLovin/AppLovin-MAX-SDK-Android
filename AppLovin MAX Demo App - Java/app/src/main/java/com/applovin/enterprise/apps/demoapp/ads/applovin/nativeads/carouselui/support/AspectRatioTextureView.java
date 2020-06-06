package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.support;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * Provides a TextureView that maintains the aspect ratio of a video contained within.
 */
public class AspectRatioTextureView
        extends TextureView
{
    private int                         mVideoWidth;
    private int                         mVideoHeight;
    private OnMeasureCompletionListener onMeasureCompletionListener;

    public AspectRatioTextureView(Context context)
    {
        super( context );

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    public AspectRatioTextureView(Context context, AttributeSet attrs)
    {
        super( context, attrs );

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    public AspectRatioTextureView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super( context, attrs, defStyleAttr );

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    public OnMeasureCompletionListener getOnMeasureCompletionListener()
    {
        return onMeasureCompletionListener;
    }

    public void setOnMeasureCompletionListener(OnMeasureCompletionListener onMeasureCompletionListener)
    {
        this.onMeasureCompletionListener = onMeasureCompletionListener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        if ( mVideoWidth <= 0 || mVideoHeight <= 0 )
        {
            super.onMeasure( widthMeasureSpec, heightMeasureSpec );
            return;
        }

        float heightRatio = (float) mVideoHeight / (float) getHeight();
        float widthRatio = (float) mVideoWidth / (float) getWidth();

        int scaledHeight;
        int scaledWidth;

        if ( heightRatio > widthRatio )
        {
            scaledHeight = (int) Math.ceil( (float) mVideoHeight / heightRatio );
            scaledWidth = (int) Math.ceil( (float) mVideoWidth / heightRatio );
        }
        else
        {
            scaledHeight = (int) Math.ceil( (float) mVideoHeight / widthRatio );
            scaledWidth = (int) Math.ceil( (float) mVideoWidth / widthRatio );
        }

        setMeasuredDimension( scaledWidth, scaledHeight );

        if ( onMeasureCompletionListener != null )
        {
            onMeasureCompletionListener.onMeasureCompleted( scaledWidth, scaledHeight );
        }
    }

    public void setVideoSize(int videoWidth, int videoHeight)
    {
        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;

        try
        {
            requestLayout();
            invalidate();
        }
        catch ( Exception ignore )
        {
        }
    }

    public interface OnMeasureCompletionListener
    {
        void onMeasureCompleted(final int adjustedWidth, final int adjustedHeight);
    }
}
