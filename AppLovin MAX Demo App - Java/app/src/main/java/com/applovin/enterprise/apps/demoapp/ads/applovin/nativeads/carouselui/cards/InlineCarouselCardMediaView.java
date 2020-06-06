package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.AppLovinCarouselViewSettings;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.support.AspectRatioTextureView;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util.LayoutUtils;
import com.applovin.nativeAds.AppLovinNativeAd;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;

/**
 * This class is used to render a native ad's main image and video component into a layout.
 */
public class InlineCarouselCardMediaView
        extends FrameLayout
        implements TextureView.SurfaceTextureListener
{
    private static String TAG = "VideoAdView";

    private AppLovinSdk      sdk;
    private AppLovinNativeAd ad;
    private Handler          uiHandler;

    private boolean                         initialized;
    private AspectRatioTextureView          textureView;
    private ImageView                       fallbackImageView;
    private ImageView                       muteButtonImageView;
    private InlineCarouselCardState         cardState;
    private InlineCarouselCardReplayOverlay replayOverlay;

    // Media player tracking
    private boolean     videoCreated;
    private boolean     autoplayRequested;
    private boolean     mediaPlayerPrepared;
    private MediaPlayer mediaPlayer;
    private Surface     surface;

    public InlineCarouselCardMediaView(Context context)
    {
        super( context );
    }

    public InlineCarouselCardMediaView(Context context, AttributeSet attrs)
    {
        super( context, attrs );
    }

    public InlineCarouselCardMediaView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super( context, attrs, defStyleAttr );
    }

    public InlineCarouselCardMediaView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super( context, attrs, defStyleAttr, defStyleRes );
    }

    public AppLovinSdk getSdk()
    {
        return sdk;
    }

    public void setSdk(AppLovinSdk sdk)
    {
        this.sdk = sdk;
    }

    public AppLovinNativeAd getAd()
    {
        return ad;
    }

    public void setAd(AppLovinNativeAd ad)
    {
        this.ad = ad;
    }

    public void setUiHandler(Handler uiHandler)
    {
        this.uiHandler = uiHandler;
    }

    public InlineCarouselCardState getCardState()
    {
        return cardState;
    }

    public void setCardState(InlineCarouselCardState cardState)
    {
        this.cardState = cardState;
    }

    public void setUpView()
    {
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        inflater.inflate( R.layout.applovin_card_media_view, this, true );

        bindViews();
        initializeView();
    }

    private void bindViews()
    {
        fallbackImageView = (ImageView) findViewById( R.id.applovin_media_image );
        replayOverlay = (InlineCarouselCardReplayOverlay) findViewById( R.id.applovin_media_replay_overlay );
    }

    private void initializeView()
    {
        if ( !initialized )
        {
            initialized = true;

            setBackgroundColor( getResources().getColor( AppLovinCarouselViewSettings.VIDEO_VIEW_BACKGROUND_COLOR ) );

            if ( ad.isVideoPrecached() && AppLovinCarouselViewSettings.USE_VIDEO_SCREENSHOTS_AS_IMAGES )
            {
                updateScreenshot();
            }

            AppLovinSdkUtils.safePopulateImageView( fallbackImageView, Uri.parse( ad.getImageUrl() ),
                                                    AppLovinSdkUtils.dpToPx( getContext(), AppLovinCarouselViewSettings.MAIN_IMAGE_MAX_SCALE_SIZE ) );

            // Create mute and replay views programmatically as they're added selectively at runtime.
            muteButtonImageView = new ImageView( getContext() );

            final int muteSize = AppLovinSdkUtils.dpToPx( getContext(), 20 );
            final int muteMargin = AppLovinSdkUtils.dpToPx( getContext(), 20 );

            final FrameLayout.LayoutParams muteParams = new FrameLayout.LayoutParams( muteSize, muteSize );
            muteParams.gravity = Gravity.LEFT | Gravity.BOTTOM;

            muteButtonImageView.setLayoutParams( muteParams );

            muteButtonImageView.setOnClickListener( new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    toggleMuteState();
                }
            } );

            setAppropriateMuteImage( AppLovinCarouselViewSettings.VIDEO_MUTED_BY_DEFAULT );

            replayOverlay.setVisibility( cardState.isReplayOverlayVisible() ? VISIBLE : GONE );

            replayOverlay.setReplayClickListener( new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    replay();
                }
            } );

            replayOverlay.setLearnMoreClickListener( new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    ad.launchClickTarget( getContext() );
                }
            } );

            replayOverlay.setUpView();
        }
    }

    void updateScreenshot()
    {
        final Bitmap screenshot = getVideoFrame( Math.max( 200, cardState.getLastMediaPlayerPosition() ) );
        if ( screenshot != null )
        {
            fallbackImageView.setImageBitmap( screenshot );
        }
    }

    public void createVideo()
    {
        if ( AppLovinSdkUtils.isValidString( ad.getVideoUrl() ) )
        {
            if ( !videoCreated )
            {
                videoCreated = true;
                textureView = new AspectRatioTextureView( getContext() );
                textureView.setLayoutParams( LayoutUtils.createFrameParams( LayoutUtils.MATCH_PARENT, LayoutUtils.MATCH_PARENT, Gravity.CENTER ) );
                textureView.setSurfaceTextureListener( this );

                final FrameLayout layoutRef = this;
                textureView.setOnMeasureCompletionListener( new AspectRatioTextureView.OnMeasureCompletionListener()
                {
                    @Override
                    public void onMeasureCompleted(int adjustedWidth, int adjustedHeight)
                    {
                        final int xDelta = layoutRef.getWidth() - adjustedWidth; // Difference between layout and adjusted video width.
                        final int yDelta = layoutRef.getHeight() - adjustedHeight;

                        // Move the mute button to overlay the video.
                        final FrameLayout.LayoutParams muteParams = (FrameLayout.LayoutParams) muteButtonImageView.getLayoutParams();
                        final int padding = AppLovinSdkUtils.dpToPx( getContext(), 5 );
                        muteParams.leftMargin = ( xDelta / 2 ) + padding;
                        muteParams.bottomMargin = ( yDelta / 2 ) + padding;
                    }
                } );

                addView( textureView );
                bringChildToFront( textureView );

                // Bump the mute button to the front
                addView( muteButtonImageView );
                bringChildToFront( muteButtonImageView );

                invalidate();
                requestLayout();

                if ( textureView.isAvailable() )
                {
                    onSurfaceTextureAvailable( textureView.getSurfaceTexture(), textureView.getWidth(), textureView.getHeight() );
                }
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        // Once Android has prepared the GL texture, start MediaPlayer setup
        if ( mediaPlayer == null )
        {
            try
            {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource( getContext(), Uri.parse( ad.getVideoUrl() ) );
                this.surface = new Surface( surface );
                mediaPlayer.setSurface( this.surface );
                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener( new MediaPlayer.OnPreparedListener()
                {
                    @Override
                    public void onPrepared(MediaPlayer mp)
                    {
                        try
                        {
                            mediaPlayerPrepared = true;

                            final int videoWidth = mp.getVideoWidth();
                            final int videoHeight = mp.getVideoHeight();

                            textureView.setVideoSize( videoWidth, videoHeight );

                            final int lastPosition = cardState.getLastMediaPlayerPosition();
                            if ( lastPosition > 0 )
                            {
                                mp.seekTo( lastPosition );
                                playVideo( mp );
                            }
                            else if ( autoplayRequested && !cardState.isReplayOverlayVisible() )
                            {
                                playVideo( mp );
                            }
                        }
                        catch ( Exception ex )
                        {
                            Log.e( TAG, "Unable to perform post-preparation setup", ex );
                        }
                    }
                } );

                mediaPlayer.setOnCompletionListener( new MediaPlayer.OnCompletionListener()
                {
                    @Override
                    public void onCompletion(MediaPlayer mp)
                    {

                        int percentViewed = calculatePercentViewed( mp );
                        Log.d( TAG, "OnCompletion invoked at " + percentViewed );

                        // Some Android devices report 0 on completion. So if we've both started and ended organically, this is a success case.
                        if ( percentViewed == 0 )
                        {
                            percentViewed = 100;
                        }

                        // If we've reached the end of the video, toggle 'replay' mode.
                        if ( percentViewed >= 98 )
                        {
                            setBackgroundColor( getResources().getColor( AppLovinCarouselViewSettings.VIDEO_VIEW_BACKGROUND_COLOR ) );
                            cardState.setVideoCompleted( true );
                            prepareForReplay();
                        }

                        // In any case, notify the video end URL.
                        notifyVideoEndUrl( percentViewed );

                        final AlphaAnimation muteFade = new AlphaAnimation( 1f, 0f );
                        muteFade.setDuration( 500 );
                        muteFade.setAnimationListener( new Animation.AnimationListener()
                        {
                            @Override
                            public void onAnimationStart(Animation animation)
                            {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation)
                            {
                                muteButtonImageView.setVisibility( INVISIBLE );
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation)
                            {

                            }
                        } );
                        muteButtonImageView.startAnimation( muteFade );
                    }
                } );

                mediaPlayer.setOnErrorListener( new MediaPlayer.OnErrorListener()
                {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra)
                    {
                        Log.w( TAG, "MediaPlayer error: (" + what + ", " + extra + ")" );
                        return true;
                    }
                } );

            }
            catch ( Exception ex )
            {
                Log.e( TAG, "Unable to build media player.", ex );
            }
        }
    }

    private void notifyVideoEndUrl(int percentViewed)
    {
        if ( cardState.isVideoStarted() )
        {
            sdk.getPostbackService().dispatchPostbackAsync( ad.getVideoEndTrackingUrl( percentViewed, cardState.isFirstPlay() ), null );
            cardState.setFirstPlay( false );
        }
    }

    public void playVideo(final MediaPlayer mp)
    {
        setBackgroundColor( getResources().getColor( AppLovinCarouselViewSettings.VIDEO_VIEW_BACKGROUND_COLOR_WHILE_PLAYING ) );
        replayOverlay.setVisibility( GONE );
        cardState.setReplayOverlayVisible( false );

        final MediaPlayer mediaPlayer = ( mp != null ) ? mp : this.mediaPlayer;

        Log.d( TAG, "Video play requested..." );
        if ( AppLovinSdkUtils.isValidString( ad.getVideoUrl() ) )
        {
            if ( cardState.getMuteState().equals( InlineCarouselCardState.MuteState.UNSPECIFIED ) )
            {
                setMuteState( AppLovinCarouselViewSettings.VIDEO_MUTED_BY_DEFAULT ? InlineCarouselCardState.MuteState.MUTED : InlineCarouselCardState.MuteState.UNMUTED, false );
            }
            else
            {
                setMuteState( cardState.getMuteState(), false );
            }

            mediaPlayer.start();

            if ( !cardState.isVideoStarted() )
            {
                cardState.setVideoStarted( true );
                sdk.getPostbackService().dispatchPostbackAsync( ad.getVideoStartTrackingUrl(), null );
            }

            final AlphaAnimation muteFade = new AlphaAnimation( 0f, 1f );
            muteFade.setDuration( 500 );
            muteFade.setAnimationListener( new Animation.AnimationListener()
            {
                @Override
                public void onAnimationStart(Animation animation)
                {
                    muteButtonImageView.setVisibility( VISIBLE );
                }

                @Override
                public void onAnimationEnd(Animation animation)
                {

                }

                @Override
                public void onAnimationRepeat(Animation animation)
                {

                }
            } );


            muteButtonImageView.startAnimation( muteFade );

            // If the fallback view is visible, crossfade it with the video.
            if ( fallbackImageView.getVisibility() == VISIBLE )
            {
                final AlphaAnimation imageFade = new AlphaAnimation( fallbackImageView.getAlpha(), 0f );
                imageFade.setDuration( 750 );
                imageFade.setAnimationListener( new Animation.AnimationListener()
                {
                    @Override
                    public void onAnimationStart(Animation animation)
                    {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation)
                    {
                        fallbackImageView.setVisibility( INVISIBLE );
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation)
                    {

                    }
                } );

                fallbackImageView.startAnimation( imageFade );

                final AlphaAnimation videoFade = new AlphaAnimation( 0f, 1f );
                videoFade.setDuration( 500 );
                textureView.startAnimation( videoFade );
            }
        }
    }

    public void onCardActivated()
    {
        autoplayVideo();
    }

    public void autoplayVideo()
    {
        if ( AppLovinSdkUtils.isValidString( ad.getVideoUrl() ) )
        {
            if ( !cardState.isReplayOverlayVisible() && ad.isVideoPrecached() )
            {
                if ( mediaPlayer != null && mediaPlayerPrepared && !mediaPlayer.isPlaying() )
                {
                    playVideo( mediaPlayer );
                }
                else
                {
                    autoplayRequested = true;
                    createVideo();
                }
            }
        }
    }

    public void onCardDeactivated()
    {
        if ( mediaPlayer != null && mediaPlayer.isPlaying() )
        {
            mediaPlayer.pause();
            cardState.setLastMediaPlayerPosition( mediaPlayer.getCurrentPosition() );

            final int percentViewed = calculatePercentViewed( mediaPlayer );
            if ( percentViewed > 0 )
            {
                notifyVideoEndUrl( percentViewed );
            }
        }

        updateScreenshot();

        if ( textureView != null )
        {
            final AlphaAnimation imageFade = new AlphaAnimation( fallbackImageView.getAlpha(), 1f );
            imageFade.setDuration( 500 );
            fallbackImageView.setVisibility( VISIBLE );
            fallbackImageView.startAnimation( imageFade );
            final AlphaAnimation videoFade = new AlphaAnimation( textureView.getAlpha(), 0f );
            videoFade.setDuration( 500 );
            videoFade.setAnimationListener( new Animation.AnimationListener()
            {
                @Override
                public void onAnimationStart(Animation animation)
                {

                }

                @Override
                public void onAnimationEnd(Animation animation)
                {
                    removeView( textureView );
                    textureView = null;
                }

                @Override
                public void onAnimationRepeat(Animation animation)
                {

                }
            } );

            textureView.startAnimation( videoFade );
            removeView( muteButtonImageView );

            if ( mediaPlayer != null )
            {
                if ( mediaPlayer.isPlaying() )
                {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            }

            mediaPlayer = null;
            videoCreated = false;
        }
    }

    public void togglePlayback()
    {
        if ( mediaPlayer != null )
        {
            if ( mediaPlayer.isPlaying() )
            {
                mediaPlayer.pause();
                return;
            }
            else if ( mediaPlayerPrepared )
            {
                playVideo( mediaPlayer );
            }
        }

        if ( !mediaPlayerPrepared )
        {
            autoplayRequested = true;
        }
    }

    private void prepareForReplay()
    {
        cardState.setLastMediaPlayerPosition( 0 );
        cardState.setReplayOverlayVisible( true );

        updateScreenshot();

        final AlphaAnimation replayFade = new AlphaAnimation( 0f, 1f );
        replayFade.setDuration( 500 );
        replayOverlay.setVisibility( VISIBLE );
        replayOverlay.startAnimation( replayFade );

        textureView.setVisibility( INVISIBLE );
    }

    private void replay()
    {
        replayOverlay.setVisibility( INVISIBLE );
        cardState.setReplayOverlayVisible( false );

        if ( textureView != null )
        {
            textureView.setVisibility( VISIBLE );
            playVideo( null );
        }
        else
        {
            autoplayRequested = true;
            createVideo();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {

    }

    public void onVideoPrecached(AppLovinNativeAd ad)
    {
        if ( sdk != null )
        {
            Log.d( TAG, "Video precache complete." );
        }

        if ( cardState != null && cardState.isCurrentlyActive() )
        {
            autoplayVideo();
        }
        else
        {
            autoplayRequested = true;
        }
    }

    private Bitmap getVideoFrame(final int position)
    {
        if ( ad.getVideoUrl() == null )
        {
            return null;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Bitmap bitmap;

        try
        {
            retriever.setDataSource( getContext(), Uri.parse( ad.getVideoUrl() ) );

            final Bitmap rawBitmap = retriever.getFrameAtTime( position );
            final Bitmap scaledBitmap = Bitmap.createScaledBitmap( rawBitmap, textureView.getWidth(), textureView.getHeight(), false );

            rawBitmap.recycle();
            bitmap = scaledBitmap;
        }
        catch ( Exception ex )
        {
            bitmap = null;
            Log.d( TAG, "Unable to grab video frame for: " + Uri.parse( ad.getVideoUrl() ) );
        }
        finally
        {
            retriever.release();
        }

        return bitmap;
    }

    private int calculatePercentViewed(final MediaPlayer mp)
    {
        final float videoDuration = mp.getDuration();
        final float currentPosition = mp.getCurrentPosition();
        // NOTE: Media player bug: calling getCurrentPosition after the video finished playing gives slightly larger value than the total duration of the video.
        if ( currentPosition >= videoDuration )
        {
            // Video fully watched, return 100%.
            return 100;
        }

        final double percentViewed = ( currentPosition / videoDuration ) * 100f;
        return (int) Math.ceil( percentViewed );
    }

    private void toggleMuteState()
    {
        setMuteState( cardState.getMuteState().equals( InlineCarouselCardState.MuteState.UNMUTED ) ? InlineCarouselCardState.MuteState.MUTED : InlineCarouselCardState.MuteState.UNMUTED, true );
    }

    private void setMuteState(final InlineCarouselCardState.MuteState muteState, final boolean fade)
    {
        cardState.setMuteState( muteState );
        final boolean isBeingMuted = muteState.equals( InlineCarouselCardState.MuteState.MUTED );
        setAppropriateMuteImage( isBeingMuted );

        if ( fade && AppLovinCarouselViewSettings.MUTE_FADES_AUDIO )
        {
            final float numSteps = 10;
            final int stepDistance = 20;

            // Fade the audio in / out.
            for ( int i = 0; i < numSteps; i++ )
            {
                final float volume = isBeingMuted ? ( numSteps - i ) / numSteps : i / numSteps;
                final int delay = i * stepDistance;

                uiHandler.postDelayed( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if ( mediaPlayer != null )
                        {
                            mediaPlayer.setVolume( volume, volume );
                        }
                    }
                }, delay );
            }

            // Finally, post a final adjustment to ensure it's at the target volume.
            uiHandler.postDelayed( new Runnable()
            {
                @Override
                public void run()
                {
                    if ( mediaPlayer != null )
                    {
                        final float volume = isBeingMuted ? 0 : 1;
                        mediaPlayer.setVolume( volume, volume );
                    }
                }
            }, (long) ( stepDistance * numSteps ) );
        }
        else
        {
            if ( mediaPlayer != null )
            {
                final float volume = isBeingMuted ? 0 : 1;
                mediaPlayer.setVolume( volume, volume );
            }
        }
    }

    private void setAppropriateMuteImage(final boolean isMuted)
    {
        final int drawable = isMuted ? R.drawable.applovin_card_muted : R.drawable.applovin_card_unmuted;
        AppLovinSdkUtils.safePopulateImageView( getContext(), muteButtonImageView, drawable, AppLovinCarouselViewSettings.ICON_IMAGE_MAX_SCALE_SIZE );
    }

    public void destroy()
    {
        try
        {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;

            removeAllViews();
        }
        catch ( Exception ex )
        {
            // This is not a fatal case as media players may well be destroyed or being destroyed by Android already.
            Log.d( TAG, "Encountered exception when destroying:" + ex );
        }
    }
}