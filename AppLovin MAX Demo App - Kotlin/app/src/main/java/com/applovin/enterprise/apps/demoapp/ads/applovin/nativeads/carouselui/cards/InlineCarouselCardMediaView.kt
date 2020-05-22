package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView

import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.AppLovinCarouselViewSettings
import com.applovin.apps.kotlindemoapp.nativeads.carouselui.support.AspectRatioTextureView
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util.LayoutUtils
import com.applovin.nativeAds.AppLovinNativeAd
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkUtils

/**
 * This class is used to render a native ad's main image and video component into a layout.
 */
class InlineCarouselCardMediaView : FrameLayout, TextureView.SurfaceTextureListener
{

    var sdk: AppLovinSdk? = null
    var ad: AppLovinNativeAd? = null
    private var uiHandler: Handler? = null

    private var initialized: Boolean = false
    private var textureView: AspectRatioTextureView? = null
    private var fallbackImageView: ImageView? = null
    private var muteButtonImageView: ImageView? = null
    var cardState: InlineCarouselCardState? = null
    private var replayOverlay: InlineCarouselCardReplayOverlay? = null

    // Media player tracking
    private var videoCreated: Boolean = false
    private var autoplayRequested: Boolean = false
    private var mediaPlayerPrepared: Boolean = false
    private var mediaPlayer: MediaPlayer? = null
    private var surface: Surface? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    fun setUiHandler(uiHandler: Handler)
    {
        this.uiHandler = uiHandler
    }

    fun setUpView()
    {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.applovin_card_media_view, this, true)

        bindViews()
        initializeView()
    }

    private fun bindViews()
    {
        fallbackImageView = findViewById<View>(R.id.applovin_media_image) as ImageView
        replayOverlay = findViewById<View>(R.id.applovin_media_replay_overlay) as InlineCarouselCardReplayOverlay
    }

    private fun initializeView()
    {
        if (!initialized)
        {
            initialized = true

            setBackgroundColor(resources.getColor(AppLovinCarouselViewSettings.VIDEO_VIEW_BACKGROUND_COLOR))

            if (ad!!.isVideoPrecached && AppLovinCarouselViewSettings.USE_VIDEO_SCREENSHOTS_AS_IMAGES)
            {
                updateScreenshot()
            }

            AppLovinSdkUtils.safePopulateImageView(fallbackImageView, Uri.parse(ad!!.imageUrl),
                                                   AppLovinSdkUtils.dpToPx(context, AppLovinCarouselViewSettings.MAIN_IMAGE_MAX_SCALE_SIZE))

            // Create mute and replay views programmatically as they're added selectively at runtime.
            muteButtonImageView = ImageView(context)

            val muteSize = AppLovinSdkUtils.dpToPx(context, 20)
            val muteMargin = AppLovinSdkUtils.dpToPx(context, 20)

            val muteParams = FrameLayout.LayoutParams(muteSize, muteSize)
            muteParams.gravity = Gravity.LEFT or Gravity.BOTTOM

            muteButtonImageView!!.layoutParams = muteParams

            muteButtonImageView!!.setOnClickListener { toggleMuteState() }

            setAppropriateMuteImage(AppLovinCarouselViewSettings.VIDEO_MUTED_BY_DEFAULT)

            replayOverlay!!.visibility = if (cardState!!.isReplayOverlayVisible) View.VISIBLE else View.GONE

            replayOverlay!!.replayClickListener = View.OnClickListener { replay() }

            replayOverlay!!.learnMoreClickListener = View.OnClickListener { ad!!.launchClickTarget(context) }

            replayOverlay!!.setUpView()
        }
    }

    internal fun updateScreenshot()
    {
        val screenshot = getVideoFrame(Math.max(200, cardState!!.lastMediaPlayerPosition))
        if (screenshot != null)
        {
            fallbackImageView!!.setImageBitmap(screenshot)
        }
    }

    fun createVideo()
    {
        if (AppLovinSdkUtils.isValidString(ad!!.videoUrl))
        {
            if (!videoCreated)
            {
                videoCreated = true
                textureView = AspectRatioTextureView(context)
                textureView!!.layoutParams = LayoutUtils.createFrameParams(LayoutUtils.MATCH_PARENT, LayoutUtils.MATCH_PARENT, Gravity.CENTER)
                textureView!!.surfaceTextureListener = this

                val layoutRef = this
                textureView!!.onMeasureCompletionListener = object : AspectRatioTextureView.OnMeasureCompletionListener
                {
                    override fun onMeasureCompleted(adjustedWidth: Int, adjustedHeight: Int)
                    {
                        val xDelta = layoutRef.width - adjustedWidth // Difference between layout and adjusted video width.
                        val yDelta = layoutRef.height - adjustedHeight

                        // Move the mute button to overlay the video.
                        val muteParams = muteButtonImageView!!.layoutParams as LayoutParams
                        val padding = AppLovinSdkUtils.dpToPx(context, 5)
                        muteParams.leftMargin = xDelta / 2 + padding
                        muteParams.bottomMargin = yDelta / 2 + padding
                    }
                }

                addView(textureView)
                bringChildToFront(textureView)

                // Bump the mute button to the front
                addView(muteButtonImageView)
                bringChildToFront(muteButtonImageView)

                invalidate()
                requestLayout()

                if (textureView!!.isAvailable)
                {
                    onSurfaceTextureAvailable(textureView!!.surfaceTexture, textureView!!.width, textureView!!.height)
                }
            }
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int)
    {
        // Once Android has prepared the GL texture, start MediaPlayer setup
        if (mediaPlayer == null)
        {
            try
            {
                mediaPlayer = MediaPlayer()
                mediaPlayer!!.setDataSource(context, Uri.parse(ad!!.videoUrl))
                this.surface = Surface(surface)
                mediaPlayer!!.setSurface(this.surface)
                mediaPlayer!!.prepareAsync()

                mediaPlayer!!.setOnPreparedListener { mp ->
                    try
                    {
                        mediaPlayerPrepared = true

                        val videoWidth = mp.videoWidth
                        val videoHeight = mp.videoHeight

                        textureView!!.setVideoSize(videoWidth, videoHeight)

                        val lastPosition = cardState!!.lastMediaPlayerPosition
                        if (lastPosition > 0)
                        {
                            mp.seekTo(lastPosition)
                            playVideo(mp)
                        }
                        else if (autoplayRequested && !cardState!!.isReplayOverlayVisible)
                        {
                            playVideo(mp)
                        }
                    }
                    catch (ex: Exception)
                    {
                        Log.e(TAG, "Unable to perform post-preparation setup", ex)
                    }
                }

                mediaPlayer!!.setOnCompletionListener { mp ->
                    var percentViewed = calculatePercentViewed(mp)
                    Log.d(TAG, "OnCompletion invoked at " + percentViewed)

                    // Some Android devices report 0 on completion. So if we've both started and ended organically, this is a success case.
                    if (percentViewed == 0)
                    {
                        percentViewed = 100
                    }

                    // If we've reached the end of the video, toggle 'replay' mode.
                    if (percentViewed >= 98)
                    {
                        setBackgroundColor(resources.getColor(AppLovinCarouselViewSettings.VIDEO_VIEW_BACKGROUND_COLOR))
                        cardState!!.isVideoCompleted = true
                        prepareForReplay()
                    }

                    // In any case, notify the video end URL.
                    notifyVideoEndUrl(percentViewed)

                    val muteFade = AlphaAnimation(1f, 0f)
                    muteFade.duration = 500
                    muteFade.setAnimationListener(object : Animation.AnimationListener
                                                  {
                                                      override fun onAnimationStart(animation: Animation)
                                                      {

                                                      }

                                                      override fun onAnimationEnd(animation: Animation)
                                                      {
                                                          muteButtonImageView!!.visibility = View.INVISIBLE
                                                      }

                                                      override fun onAnimationRepeat(animation: Animation)
                                                      {

                                                      }
                                                  })
                    muteButtonImageView!!.startAnimation(muteFade)
                }

                mediaPlayer!!.setOnErrorListener { mp, what, extra ->
                    Log.w(TAG, "MediaPlayer error: ($what, $extra)")
                    true
                }

            }
            catch (ex: Exception)
            {
                Log.e(TAG, "Unable to build media player.", ex)
            }

        }
    }

    private fun notifyVideoEndUrl(percentViewed: Int)
    {
        if (cardState!!.isVideoStarted)
        {
            sdk!!.postbackService.dispatchPostbackAsync(ad!!.getVideoEndTrackingUrl(percentViewed, cardState!!.isFirstPlay), null)
            cardState!!.isFirstPlay = false
        }
    }

    fun playVideo(mp: MediaPlayer?)
    {
        setBackgroundColor(resources.getColor(AppLovinCarouselViewSettings.VIDEO_VIEW_BACKGROUND_COLOR_WHILE_PLAYING))
        replayOverlay!!.visibility = View.GONE
        cardState!!.isReplayOverlayVisible = false

        val mediaPlayer = mp ?: this.mediaPlayer

        Log.d(TAG, "Video play requested...")
        if (AppLovinSdkUtils.isValidString(ad!!.videoUrl))
        {
            if (cardState!!.muteState == InlineCarouselCardState.MuteState.UNSPECIFIED)
            {
                setMuteState(if (AppLovinCarouselViewSettings.VIDEO_MUTED_BY_DEFAULT) InlineCarouselCardState.MuteState.MUTED else InlineCarouselCardState.MuteState.UNMUTED, false)
            }
            else
            {
                setMuteState(cardState!!.muteState, false)
            }

            mediaPlayer!!.start()

            if (!cardState!!.isVideoStarted)
            {
                cardState!!.isVideoStarted = true
                sdk!!.postbackService.dispatchPostbackAsync(ad!!.videoStartTrackingUrl, null)
            }

            val muteFade = AlphaAnimation(0f, 1f)
            muteFade.duration = 500
            muteFade.setAnimationListener(object : Animation.AnimationListener
                                          {
                                              override fun onAnimationStart(animation: Animation)
                                              {
                                                  muteButtonImageView!!.visibility = View.VISIBLE
                                              }

                                              override fun onAnimationEnd(animation: Animation)
                                              {

                                              }

                                              override fun onAnimationRepeat(animation: Animation)
                                              {

                                              }
                                          })


            muteButtonImageView!!.startAnimation(muteFade)

            // If the fallback view is visible, crossfade it with the video.
            if (fallbackImageView!!.visibility == View.VISIBLE)
            {
                val imageFade = AlphaAnimation(fallbackImageView!!.alpha, 0f)
                imageFade.duration = 750
                imageFade.setAnimationListener(object : Animation.AnimationListener
                                               {
                                                   override fun onAnimationStart(animation: Animation)
                                                   {

                                                   }

                                                   override fun onAnimationEnd(animation: Animation)
                                                   {
                                                       fallbackImageView!!.visibility = View.INVISIBLE
                                                   }

                                                   override fun onAnimationRepeat(animation: Animation)
                                                   {

                                                   }
                                               })

                fallbackImageView!!.startAnimation(imageFade)

                val videoFade = AlphaAnimation(0f, 1f)
                videoFade.duration = 500
                textureView!!.startAnimation(videoFade)
            }
        }
    }

    fun onCardActivated()
    {
        autoplayVideo()
    }

    fun autoplayVideo()
    {
        if (AppLovinSdkUtils.isValidString(ad!!.videoUrl))
        {
            if (!cardState!!.isReplayOverlayVisible && ad!!.isVideoPrecached)
            {
                if (mediaPlayer != null && mediaPlayerPrepared && !mediaPlayer!!.isPlaying)
                {
                    playVideo(mediaPlayer)
                }
                else
                {
                    autoplayRequested = true
                    createVideo()
                }
            }
        }
    }

    fun onCardDeactivated()
    {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying)
        {
            mediaPlayer!!.pause()
            cardState!!.lastMediaPlayerPosition = mediaPlayer!!.currentPosition

            val percentViewed = calculatePercentViewed(mediaPlayer!!)
            if (percentViewed > 0)
            {
                notifyVideoEndUrl(percentViewed)
            }
        }

        updateScreenshot()

        if (textureView != null)
        {
            val imageFade = AlphaAnimation(fallbackImageView!!.alpha, 1f)
            imageFade.duration = 500
            fallbackImageView!!.visibility = View.VISIBLE
            fallbackImageView!!.startAnimation(imageFade)
            val videoFade = AlphaAnimation(textureView!!.alpha, 0f)
            videoFade.duration = 500
            videoFade.setAnimationListener(object : Animation.AnimationListener
                                           {
                                               override fun onAnimationStart(animation: Animation)
                                               {

                                               }

                                               override fun onAnimationEnd(animation: Animation)
                                               {
                                                   removeView(textureView)
                                                   textureView = null
                                               }

                                               override fun onAnimationRepeat(animation: Animation)
                                               {

                                               }
                                           })

            textureView!!.startAnimation(videoFade)
            removeView(muteButtonImageView)

            if (mediaPlayer != null)
            {
                if (mediaPlayer!!.isPlaying)
                {
                    mediaPlayer!!.stop()
                }
                mediaPlayer!!.release()
            }

            mediaPlayer = null
            videoCreated = false
        }
    }

    fun togglePlayback()
    {
        if (mediaPlayer != null)
        {
            if (mediaPlayer!!.isPlaying)
            {
                mediaPlayer!!.pause()
                return
            }
            else if (mediaPlayerPrepared)
            {
                playVideo(mediaPlayer)
            }
        }

        if (!mediaPlayerPrepared)
        {
            autoplayRequested = true
        }
    }

    private fun prepareForReplay()
    {
        cardState!!.lastMediaPlayerPosition = 0
        cardState!!.isReplayOverlayVisible = true

        updateScreenshot()

        val replayFade = AlphaAnimation(0f, 1f)
        replayFade.duration = 500
        replayOverlay!!.visibility = View.VISIBLE
        replayOverlay!!.startAnimation(replayFade)

        textureView!!.visibility = View.INVISIBLE
    }

    private fun replay()
    {
        replayOverlay!!.visibility = View.INVISIBLE
        cardState!!.isReplayOverlayVisible = false

        if (textureView != null)
        {
            textureView!!.visibility = View.VISIBLE
            playVideo(null)
        }
        else
        {
            autoplayRequested = true
            createVideo()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int)
    {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean
    {
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture)
    {

    }

    fun onVideoPrecached(ad: AppLovinNativeAd)
    {
        if (sdk != null)
        {
            Log.d(TAG, "Video precache complete.")
        }

        if (cardState != null && cardState!!.isCurrentlyActive)
        {
            autoplayVideo()
        }
        else
        {
            autoplayRequested = true
        }
    }

    private fun getVideoFrame(position: Int): Bitmap?
    {
        if (ad!!.videoUrl == null)
        {
            return null
        }

        val retriever = MediaMetadataRetriever()
        var bitmap: Bitmap?

        try
        {
            retriever.setDataSource(context, Uri.parse(ad!!.videoUrl))

            val rawBitmap = retriever.getFrameAtTime(position.toLong())
            val scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, textureView!!.width, textureView!!.height, false)

            rawBitmap.recycle()
            bitmap = scaledBitmap
        }
        catch (ex: Exception)
        {
            bitmap = null
            Log.d(TAG, "Unable to grab video frame for: " + Uri.parse(ad!!.videoUrl))
        }
        finally
        {
            retriever.release()
        }

        return bitmap
    }

    private fun calculatePercentViewed(mp: MediaPlayer): Int
    {
        val videoDuration = mp.duration.toFloat()
        val currentPosition = mp.currentPosition.toFloat()
        // NOTE: Media player bug: calling getCurrentPosition after the video finished playing gives slightly larger value than the total duration of the video.
        if ( currentPosition >= videoDuration)
        {
            // Video fully watched, return 100%.
            return 100
        }

        val percentViewed = (currentPosition / videoDuration * 100f).toDouble()
        return Math.ceil(percentViewed).toInt()
    }

    private fun toggleMuteState()
    {
        setMuteState(if (cardState!!.muteState == InlineCarouselCardState.MuteState.UNMUTED) InlineCarouselCardState.MuteState.MUTED else InlineCarouselCardState.MuteState.UNMUTED, true)
    }

    private fun setMuteState(muteState: InlineCarouselCardState.MuteState, fade: Boolean)
    {
        cardState!!.muteState = muteState
        val isBeingMuted = muteState == InlineCarouselCardState.MuteState.MUTED
        setAppropriateMuteImage(isBeingMuted)

        if (fade && AppLovinCarouselViewSettings.MUTE_FADES_AUDIO)
        {
            val numSteps = 10f
            val stepDistance = 20

            // Fade the audio in / out.
            var i = 0
            while (i < numSteps)
            {
                val volume = if (isBeingMuted) (numSteps - i) / numSteps else i / numSteps
                val delay = i * stepDistance

                uiHandler!!.postDelayed({
                                            if (mediaPlayer != null)
                                            {
                                                mediaPlayer!!.setVolume(volume, volume)
                                            }
                                        }, delay.toLong())
                i++
            }

            // Finally, post a final adjustment to ensure it's at the target volume.
            uiHandler!!.postDelayed({
                                        if (mediaPlayer != null)
                                        {
                                            val volume = (if (isBeingMuted) 0 else 1).toFloat()
                                            mediaPlayer!!.setVolume(volume, volume)
                                        }
                                    }, (stepDistance * numSteps).toLong())
        }
        else
        {
            if (mediaPlayer != null)
            {
                val volume = (if (isBeingMuted) 0 else 1).toFloat()
                mediaPlayer!!.setVolume(volume, volume)
            }
        }
    }

    private fun setAppropriateMuteImage(isMuted: Boolean)
    {
        val drawable = if (isMuted) R.drawable.applovin_card_muted else R.drawable.applovin_card_unmuted
        AppLovinSdkUtils.safePopulateImageView(context, muteButtonImageView, drawable, AppLovinCarouselViewSettings.ICON_IMAGE_MAX_SCALE_SIZE)
    }

    fun destroy()
    {
        try
        {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null

            removeAllViews()
        }
        catch (ex: Exception)
        {
            // This is not a fatal case as media players may well be destroyed or being destroyed by Android already.
            Log.d(TAG, "Encountered exception when destroying:" + ex)
        }

    }

    companion object
    {
        private val TAG = "VideoAdView"
    }
}