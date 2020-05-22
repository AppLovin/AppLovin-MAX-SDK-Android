package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards

/**
 * This class tracks the display and playback state of an individual card view within a carousel.
 */
class InlineCarouselCardState
{
    var isVideoStarted: Boolean = false
    var isVideoCompleted: Boolean = false
    var isImpressionTracked: Boolean = false
    var isVideoStartTracked: Boolean = false
    var isFirstPlay: Boolean = true
    var isPreviouslyActivated: Boolean = false
    var isCurrentlyActive: Boolean = false
    var lastMediaPlayerPosition: Int = 0
    var isReplayOverlayVisible: Boolean = false
    var muteState: MuteState = MuteState.UNSPECIFIED

    /**
     * Created by mszaro on 4/24/15.
     */
    enum class MuteState
    {
        UNSPECIFIED,
        UNMUTED,
        MUTED
    }
}
