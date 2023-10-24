package com.applovin.enterprise.apps.demoapp.ads.max.mrecs;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdkUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewMrecAdActivity
        extends AppCompatActivity
        implements MaxAdViewAdListener
{
    private final int                   AD_VIEW_COUNT = 5;
    private final int                   AD_INTERVAL   = 10;
    private final ArrayList<String>     sampleData    = new ArrayList<>( Arrays.asList( "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".split( "" ) ) );
    private final List<MaxAdView>       adViews       = new ArrayList<>( AD_VIEW_COUNT );
    private       CustomRecyclerAdapter adapter;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_mrec_recycler_view );
        setTitle( R.string.activity_recycler_view_mrecs );

        adapter = new CustomRecyclerAdapter( this, sampleData );

        // Configure recycler view
        RecyclerView recyclerView = findViewById( R.id.mrec_recycler_view );
        recyclerView.setLayoutManager( new LinearLayoutManager( this ) );
        recyclerView.setAdapter( adapter );

        configureAdViews();
    }

    private void configureAdViews()
    {
        // Insert rows at each interval to be used to display an ad
        for ( int i = 0; i < sampleData.size(); i += AD_INTERVAL )
        {
            sampleData.add( i, "" );
            adapter.notifyItemInserted( i );
        }

        for ( int i = 0; i < AD_VIEW_COUNT; i++ )
        {
            MaxAdView adView = new MaxAdView( "YOUR_AD_UNIT_ID", MaxAdFormat.MREC, this );
            adView.setListener( this );

            // Set this extra parameter to work around SDK bug that ignores calls to stopAutoRefresh()
            adView.setExtraParameter( "allow_pause_auto_refresh_immediately", "true" );
            adView.stopAutoRefresh();

            // Load the ad
            adView.loadAd();
            adViews.add( adView );
        }
    }

    //region RecyclerAdapter and ViewHolder

    private enum ViewHolderType
    {
        AD_VIEW,
        CUSTOM_VIEW
    }

    public class CustomRecyclerAdapter
            extends RecyclerView.Adapter
    {
        private final LayoutInflater inflater;
        private final List<String>   data;

        public CustomRecyclerAdapter(Activity activity, List<String> data)
        {
            this.inflater = activity.getLayoutInflater();
            this.data = data;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType)
        {
            ViewHolderType viewHolderType = ViewHolderType.values()[viewType];

            switch ( viewHolderType )
            {
                case AD_VIEW:
                    return new AdViewHolder( inflater.inflate( R.layout.activity_mrec_ad_view_holder, parent, false ) );
                case CUSTOM_VIEW:
                    return new CustomViewHolder( inflater.inflate( R.layout.activity_text_recycler_view_holder, parent, false ) );
                default:
                    return null;
            }
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position)
        {
            if ( holder instanceof AdViewHolder )
            {
                // Select an ad view to display
                MaxAdView adView = adViews.get( ( position / AD_INTERVAL ) % AD_VIEW_COUNT );

                // Configure view holder with an ad
                ( (AdViewHolder) holder ).configure( adView );
            }
            else if ( holder instanceof CustomViewHolder )
            {
                ( (CustomViewHolder) holder ).textView.setText( data.get( position ) );
            }
        }

        @Override
        public void onViewRecycled(@NonNull final RecyclerView.ViewHolder holder)
        {
            super.onViewRecycled( holder );

            if ( holder instanceof AdViewHolder )
            {
                AdViewHolder adViewHolder = (AdViewHolder) holder;
                ( (ViewGroup) adViewHolder.itemView ).removeView( adViewHolder.adView );
                adViewHolder.adView = null;
            }
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull final RecyclerView.ViewHolder holder)
        {
            super.onViewDetachedFromWindow( holder );

            if ( holder instanceof AdViewHolder )
            {
                ( (AdViewHolder) holder ).stopAutoRefresh();
            }
        }

        @Override
        public int getItemCount()
        {
            return data.size();
        }

        @Override
        public int getItemViewType(final int position)
        {
            return ( position % AD_INTERVAL == 0 ) ? ViewHolderType.AD_VIEW.ordinal() : ViewHolderType.CUSTOM_VIEW.ordinal();
        }

        public class AdViewHolder
                extends RecyclerView.ViewHolder
        {
            MaxAdView adView;

            public AdViewHolder(View itemView)
            {
                super( itemView );
            }

            public void configure(MaxAdView adView)
            {
                this.adView = adView;
                this.adView.startAutoRefresh();

                // MREC width and height are 300 and 250 respectively, on phones and tablets
                int widthPx = AppLovinSdkUtils.dpToPx( RecyclerViewMrecAdActivity.this, 300 );
                int heightPx = AppLovinSdkUtils.dpToPx( RecyclerViewMrecAdActivity.this, 250 );

                // Set background or background color for MRECs to be fully functional
                this.adView.setBackgroundColor( Color.BLACK );
                this.adView.setLayoutParams( new FrameLayout.LayoutParams( widthPx, heightPx, Gravity.CENTER ) );
                ( (ViewGroup) itemView ).addView( this.adView );
            }

            public void stopAutoRefresh()
            {
                adView.setExtraParameter( "allow_pause_auto_refresh_immediately", "true" );
                adView.stopAutoRefresh();
            }
        }

        public class CustomViewHolder
                extends RecyclerView.ViewHolder
        {
            TextView textView;

            public CustomViewHolder(View itemView)
            {
                super( itemView );

                this.textView = itemView.findViewById( R.id.text_view );
            }
        }
    }

    //endregion

    //region MAX Ad Listener

    @Override
    public void onAdLoaded(final MaxAd maxAd) { }

    @Override
    public void onAdLoadFailed(final String adUnitId, final MaxError error) { }

    @Override
    public void onAdDisplayFailed(final MaxAd maxAd, final MaxError error) { }

    @Override
    public void onAdClicked(final MaxAd maxAd) { }

    @Override
    public void onAdExpanded(final MaxAd maxAd) { }

    @Override
    public void onAdCollapsed(final MaxAd maxAd) { }

    @Override
    public void onAdDisplayed(final MaxAd maxAd) { /* DO NOT USE - THIS IS RESERVED FOR FULLSCREEN ADS ONLY AND WILL BE REMOVED IN A FUTURE SDK RELEASE */ }

    @Override
    public void onAdHidden(final MaxAd maxAd) { /* DO NOT USE - THIS IS RESERVED FOR FULLSCREEN ADS ONLY AND WILL BE REMOVED IN A FUTURE SDK RELEASE */ }

    //endregion
}
