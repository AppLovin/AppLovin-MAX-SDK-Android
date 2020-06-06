package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.support;

import android.view.View;
import android.view.ViewGroup;

public abstract class AppLovinSdkViewPagerAdapter
        extends SdkPagerAdapter
{
    /**
     * Get a View that displays the data at the specified position in the data set.
     *
     * @param position The position of the item within the adapter's data set of the item whose view we want.
     * @param pager    The ViewPager that this view will eventually be attached to.
     *
     * @return A View corresponding to the data at the specified position.
     */
    public abstract View getView(int position, SdkCenteredViewPager pager);

    /**
     * Determines whether a page View is associated with a specific key object as
     * returned by instantiateItem(ViewGroup, int).
     *
     * @param view   Page View to check for association with object
     * @param object Object to check for association with view
     *
     * @return true if view is associated with the key object object.
     */
    @Override
    public boolean isViewFromObject(View view, Object object)
    {
        return view == object;
    }

    /**
     * Create the page for the given position.
     *
     * @param container The containing View in which the page will be shown.
     * @param position  The page position to be instantiated.
     *
     * @return Returns an Object representing the new page. This does not need
     * to be a View, but can be some other container of the page.
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position)
    {
        SdkCenteredViewPager pager = (SdkCenteredViewPager) container;
        View view = getView( position, pager );

        pager.removeView( view );
        pager.addView( view );

        return view;
    }

    /**
     * Remove a page for the given position.
     *
     * @param container The containing View from which the page will be removed.
     * @param position  The page position to be removed.
     * @param view      The same object that was returned by instantiateItem(View, int).
     */
    @Override
    public void destroyItem(ViewGroup container, int position, Object view)
    {
        ( (SdkCenteredViewPager) container ).removeView( (View) view );
    }
}
