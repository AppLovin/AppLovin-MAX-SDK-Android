package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards;

/**
 * This class tracks the display and playback state of an individual card view within a carousel.
 */
public class InlineCarouselCardState
{
    private boolean   videoStarted;
    private boolean   videoCompleted;
    private boolean   impressionTracked;
    private boolean   videoStartTracked;
    private boolean   firstPlay;
    private boolean   previouslyActivated;
    private boolean   currentlyActive;
    private int       lastMediaPlayerPosition;
    private boolean   replayOverlayVisible;
    private MuteState muteState;

    public InlineCarouselCardState()
    {
        muteState = MuteState.UNSPECIFIED;
        firstPlay = true;
    }

    public boolean isVideoCompleted()
    {
        return videoCompleted;
    }

    public void setVideoCompleted(boolean videoCompleted)
    {
        this.videoCompleted = videoCompleted;
    }

    public int getLastMediaPlayerPosition()
    {
        return lastMediaPlayerPosition;
    }

    public void setLastMediaPlayerPosition(int lastMediaPlayerPosition)
    {
        this.lastMediaPlayerPosition = lastMediaPlayerPosition;
    }

    public boolean isVideoStarted()
    {
        return videoStarted;
    }

    public void setVideoStarted(boolean videoStarted)
    {
        this.videoStarted = videoStarted;
    }

    public boolean isPreviouslyActivated()
    {
        return previouslyActivated;
    }

    public void setPreviouslyActivated(boolean previouslyActivated)
    {
        this.previouslyActivated = previouslyActivated;
    }

    public boolean isImpressionTracked()
    {
        return impressionTracked;
    }

    public void setImpressionTracked(boolean impressionTracked)
    {
        this.impressionTracked = impressionTracked;
    }

    public boolean isVideoStartTracked()
    {
        return videoStartTracked;
    }

    public void setVideoStartTracked(boolean videoStartTracked)
    {
        this.videoStartTracked = videoStartTracked;
    }

    public boolean isCurrentlyActive()
    {
        return currentlyActive;
    }

    public void setCurrentlyActive(boolean currentlyActive)
    {
        this.currentlyActive = currentlyActive;
    }

    public boolean isReplayOverlayVisible()
    {
        return replayOverlayVisible;
    }

    public void setReplayOverlayVisible(boolean replayOverlayVisible)
    {
        this.replayOverlayVisible = replayOverlayVisible;
    }

    public MuteState getMuteState()
    {
        return muteState;
    }

    public void setMuteState(MuteState muteState)
    {
        this.muteState = muteState;
    }

    public boolean isFirstPlay()
    {
        return firstPlay;
    }

    public void setFirstPlay(boolean paused)
    {
        this.firstPlay = paused;
    }

    /**
     * Created by mszaro on 4/24/15.
     */
    public static enum MuteState
    {
        UNSPECIFIED,
        UNMUTED,
        MUTED
    }
}
