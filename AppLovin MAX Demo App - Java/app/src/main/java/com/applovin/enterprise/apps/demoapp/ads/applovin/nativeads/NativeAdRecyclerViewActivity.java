package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ads.applovin.AdStatusActivity;
import com.applovin.nativeAds.AppLovinNativeAd;
import com.applovin.nativeAds.AppLovinNativeAdLoadListener;
import com.applovin.nativeAds.AppLovinNativeAdPrecacheListener;
import com.applovin.nativeAds.AppLovinNativeAdService;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;

import java.util.List;

/**
 * This activity simply inflates a default XML file that contains a carousel view, which will load its own ads into it.
 */
public class NativeAdRecyclerViewActivity
        extends AdStatusActivity
{
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_native_ad_recycler_view );
        recyclerView = (RecyclerView) findViewById( R.id.nativeAdsRecyclerView );

        // Load an initial batch of native ads.
        // In a real app, you'd ideally load smaller batches, and load more as the user scrolls.
        final AppLovinSdk sdk = AppLovinSdk.getInstance( this );
        final Activity activityRef = this;

        final AppLovinNativeAdService nativeAdService = sdk.getNativeAdService();
        nativeAdService.loadNativeAds( 10, new AppLovinNativeAdLoadListener()
        {
            @Override
            public void onNativeAdsLoaded(final List /* <AppLovinNativeAd> */ list)
            {
                runOnUiThread( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        renderRecyclerView( list );
                        retrieveImageResources( nativeAdService, list );
                    }
                } );
            }

            @Override
            public void onNativeAdsFailedToLoad(final int errorCode)
            {
                runOnUiThread( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText( activityRef, "Failed to load native ads: " + errorCode, Toast.LENGTH_LONG ).show();
                    }
                } );
            }
        } );
    }

    private void retrieveImageResources(final AppLovinNativeAdService nativeAdService, final List<AppLovinNativeAd> nativeAds)
    {
        final Activity thisRef = this;

        for ( final AppLovinNativeAd nativeAd : nativeAds )
        {
            nativeAdService.precacheResources( nativeAd, new AppLovinNativeAdPrecacheListener()
            {
                @Override
                public void onNativeAdImagesPrecached(final AppLovinNativeAd appLovinNativeAd)
                {
                    runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            final NativeAdRecyclerViewAdapter adapter = (NativeAdRecyclerViewAdapter) recyclerView.getAdapter();
                            adapter.notifyItemChanged( nativeAds.indexOf( appLovinNativeAd ) );
                        }
                    } );
                }

                @Override
                public void onNativeAdVideoPreceached(AppLovinNativeAd appLovinNativeAd)
                {
                    // This example does not implement videos; see NativeAdCarouselUIActivity for an example of a widget which does.
                }

                @Override
                public void onNativeAdImagePrecachingFailed(final AppLovinNativeAd appLovinNativeAd, final int errorCode)
                {
                    runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Toast.makeText( thisRef, "Failed to load images for native ad: " + errorCode, Toast.LENGTH_LONG ).show();
                        }
                    } );
                }

                @Override
                public void onNativeAdVideoPrecachingFailed(AppLovinNativeAd appLovinNativeAd, int i)
                {
                    // This example does not implement videos; see NativeAdCarouselUIActivity for an example of a widget which does.
                }
            } );
        }
    }

    private void renderRecyclerView(final List<AppLovinNativeAd> nativeAds)
    {
        recyclerView.setLayoutManager( new LinearLayoutManager( this ) );
        recyclerView.setAdapter( new NativeAdRecyclerViewAdapter( nativeAds ) );
    }

    private void onRecyclerViewItemClicked(final View clickedView, final List<AppLovinNativeAd> nativeAds)
    {
        final int itemPosition = recyclerView.getChildAdapterPosition( clickedView );
        final AppLovinNativeAd ad = nativeAds.get( itemPosition );
        ad.launchClickTarget( this );
    }

    private class NativeAdRecyclerViewHolder
            extends RecyclerView.ViewHolder
    {
        private TextView  appTitleTextView;
        private TextView  appDescriptionTextView;
        private ImageView appIconImageView;

        public NativeAdRecyclerViewHolder(View itemView)
        {
            super( itemView );

            appTitleTextView = (TextView) itemView.findViewById( R.id.appTitleTextView );
            appDescriptionTextView = (TextView) itemView.findViewById( R.id.appDescriptionTextView );
            appIconImageView = (ImageView) itemView.findViewById( R.id.appIconImageView );
        }

        public TextView getAppTitleTextView()
        {
            return appTitleTextView;
        }

        public TextView getAppDescriptionTextView()
        {
            return appDescriptionTextView;
        }

        public ImageView getAppIconImageView()
        {
            return appIconImageView;
        }
    }

    private class NativeAdRecyclerViewAdapter
            extends RecyclerView.Adapter<NativeAdRecyclerViewHolder>
    {

        private List<AppLovinNativeAd> nativeAds;

        public NativeAdRecyclerViewAdapter(List<AppLovinNativeAd> nativeAds)
        {
            this.nativeAds = nativeAds;
        }

        @Override
        public NativeAdRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            final View prototypeView = LayoutInflater.from( parent.getContext() ).inflate( R.layout.recycler_view_cell_nativead, parent, false );

            prototypeView.setOnClickListener( new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    onRecyclerViewItemClicked( v, nativeAds );
                }
            } );

            return new NativeAdRecyclerViewHolder( prototypeView );
        }

        @Override
        public void onBindViewHolder(NativeAdRecyclerViewHolder holder, int position)
        {
            final AppLovinNativeAd nativeAd = nativeAds.get( position );

            holder.getAppTitleTextView().setText( nativeAd.getTitle() );
            holder.getAppDescriptionTextView().setText( nativeAd.getDescriptionText() );

            final int maxSizeDp = 50; // match the size defined in the XML layout
            AppLovinSdkUtils.safePopulateImageView( holder.getAppIconImageView(), Uri.parse( nativeAd.getImageUrl() ), maxSizeDp );

            // Track impression
            nativeAd.trackImpression();
        }

        @Override
        public int getItemCount()
        {
            return ( nativeAds != null ) ? nativeAds.size() : 0;
        }
    }

}
