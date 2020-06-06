package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;

import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.AppLovinCarouselViewSettings;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.support.AppLovinSdkViewPagerAdapter;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.support.SdkCenteredViewPager;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.AppLovinCarouselView;
import com.applovin.nativeAds.AppLovinNativeAd;
import com.applovin.sdk.AppLovinSdk;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * This adapter manages instances of InlineCarouselCardView which are supplied to the view pager contained in AppLovinCarouselView.
 */
public class InlineCarouselAdapter
        extends AppLovinSdkViewPagerAdapter
{
    private static final String TAG = "InlineCarouselAdapter";

    private AppLovinSdk                                        sdk;
    private Context                                            context;
    private AppLovinCarouselView                               parentView;
    private SparseArray<WeakReference<com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards.InlineCarouselCardView>> existingCards;

    public InlineCarouselAdapter(Context context, AppLovinSdk sdk, AppLovinCarouselView parentView)
    {
        this.sdk = sdk;
        this.context = context;
        this.parentView = parentView;
        this.existingCards = new SparseArray<WeakReference<InlineCarouselCardView>>();
    }

    @Override
    public View getView(int newPosition, SdkCenteredViewPager pager)
    {
        Log.d( TAG, "Adapter is creating a card for position " + newPosition );

        final List<AppLovinNativeAd> slots = parentView.getNativeAds();
        if ( slots != null && newPosition < slots.size() )
        {
            final InlineCarouselCardView card = new InlineCarouselCardView( context );
            card.setSdk( sdk );
            card.setAd( slots.get( newPosition ) );
            card.setCardState( parentView.getCardState( newPosition ) );
            card.setUpView();

            final SdkCenteredViewPager.LayoutParams params = new SdkCenteredViewPager.LayoutParams();
            params.width = SdkCenteredViewPager.LayoutParams.MATCH_PARENT;
            params.height = SdkCenteredViewPager.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.CENTER;

            card.setLayoutParams( params );

            existingCards.append( newPosition, new WeakReference<InlineCarouselCardView>( card ) );

            return card;
        }
        else
        {
            Log.e( TAG, "Unable to render widget slot: Requested position does not exist." );
            return new View( context );
        }
    }

    @Override
    public int getCount()
    {
        final List<AppLovinNativeAd> slots = parentView.getNativeAds();

        final int count = ( slots != null ) ? slots.size() : 0;
        if ( count <= 1 )
        {
            Log.e( TAG, "Asked to render a view pager but only one slot is available!" );
            return 0;
        }
        else
        {
            return slots.size();
        }
    }

    public WeakReference<InlineCarouselCardView> getExistingCard(int key)
    {
        return existingCards.get( key );
    }

    @Override
    public float getPageWidth(int position)
    {
        return AppLovinCarouselViewSettings.VIEW_PAGER_CARD_WIDTH;
    }

    public void destroyCards()
    {
        Log.d( TAG, "Destroying all owned cards" );
        for ( int i = 0; i < existingCards.size(); i++ )
        {
            final WeakReference<InlineCarouselCardView> cardRef = existingCards.get( i );
            if ( cardRef != null )
            {
                final InlineCarouselCardView card = cardRef.get();
                if ( card != null )
                {
                    card.destroy();
                }
            }
        }
    }
}
