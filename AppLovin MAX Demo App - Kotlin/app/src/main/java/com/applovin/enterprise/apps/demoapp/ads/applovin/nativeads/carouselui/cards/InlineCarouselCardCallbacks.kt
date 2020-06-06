package com.applovin.apps.kotlindemoapp.nativeads.carouselui.cards

/**
 * Represents callbacks for a view pager.
 * This is not part of the support library's stock pager (SdkViewPager),
 * You should set an OnPageChangeListener on the view pager and invoke these callbacks yourself.
 */
interface InlineCarouselCardCallbacks
{
    /**
     * Called when a page becomes the foreground view.
     */
    fun onCardActivated()

    /**
     * Called when a page loses foreground focus.
     */
    fun onCardDeactivated()
}
