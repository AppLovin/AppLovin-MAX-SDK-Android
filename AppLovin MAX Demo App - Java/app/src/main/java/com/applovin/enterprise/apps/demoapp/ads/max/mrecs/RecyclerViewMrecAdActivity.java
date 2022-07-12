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
{
    private final int               AD_VIEW_COUNT = 5;
    private final int               AD_INTERVAL   = 10;
    private final ArrayList<String> sampleData    = new ArrayList<>( Arrays.asList( "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split( "" ) ) );
    private       List<MaxAdView>   adViews       = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_mrec_recycler_view );
        setTitle( R.string.activity_recycler_view_mrecs );

        // Configure recycler view
        RecyclerView recyclerView = findViewById( R.id.mrec_recycler_view );
        recyclerView.setLayoutManager( new LinearLayoutManager( RecyclerViewMrecAdActivity.this ) );
        recyclerView.setAdapter( new CustomRecyclerAdapter( RecyclerViewMrecAdActivity.this, sampleData ) );

        configureAdViews( AD_VIEW_COUNT );
    }

    private void configureAdViews(int count)
    {
        // TODO: Insert ads into stream rather than overwrite data

        for ( int i = 0; i < count; i++ )
        {
            MaxAdView adView = new MaxAdView( "YOUR_AD_UNIT_ID", MaxAdFormat.MREC, this );
            adView.setListener( new MaxAdViewAdListener()
            {
                // MAX Ad Listener
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
            } );

            // Set this extra parameter to work around SDK bug that ignores calls to stopAutoRefresh()
            adView.setExtraParameter( "allow_pause_auto_refresh_immediately", "true" );
            adView.stopAutoRefresh();

            // Load the ad
            adView.loadAd();
            adViews.add( adView );
        }
    }

    public class CustomRecyclerAdapter
            extends RecyclerView.Adapter
    {
        private final int            AD_VIEW_TYPE     = 0;
        private final int            CUSTOM_VIEW_TYPE = 1;
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
            // TODO: Figure out a better way to get viewType
            switch ( viewType )
            {
                case AD_VIEW_TYPE:
                    return new CustomRecyclerAdapter.AdViewHolder( inflater.inflate( R.layout.activity_mrec_ad_view_holder, parent, false ) );
                case CUSTOM_VIEW_TYPE:
                    return new CustomRecyclerAdapter.CustomViewHolder( inflater.inflate( R.layout.activity_mrec_custom_view_holder, parent, false ) );
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

                // Configure cell with an ad
                ( (AdViewHolder) holder ).configureWith( adView );
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

            ViewGroup viewGroup = (ViewGroup) holder.itemView;

            for ( int i = viewGroup.getChildCount(); i >= 0; i-- )
            {
                View subview = viewGroup.getChildAt( i );

                if ( subview instanceof MaxAdView )
                {
                    viewGroup.removeViewAt( i );
                }
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
            if ( position % AD_INTERVAL == 0 )
            {
                return AD_VIEW_TYPE;
            }

            return CUSTOM_VIEW_TYPE;
        }

        public class AdViewHolder
                extends RecyclerView.ViewHolder
        {
            MaxAdView adView;

            public AdViewHolder(View itemView)
            {
                super( itemView );
            }

            public void configureWith(MaxAdView adView)
            {
                this.adView = adView;
                this.adView.startAutoRefresh();

                // MREC width and height are 300 and 250 respectively, on phones and tablets
                int widthPx = AppLovinSdkUtils.dpToPx( RecyclerViewMrecAdActivity.this, 300 );
                int heightPx = AppLovinSdkUtils.dpToPx( RecyclerViewMrecAdActivity.this, 250 );

                // Set background or background color for MRECs to be fully functional
                this.adView.setBackgroundColor( Color.BLACK );
                this.adView.setLayoutParams( new FrameLayout.LayoutParams( widthPx, heightPx, Gravity.CENTER ) );
                ( (ViewGroup) itemView ).addView( adView );
            }

            public void stopAutoRefresh()
            {
                this.adView.setExtraParameter( "allow_pause_auto_refresh_immediately", "true" );
                this.adView.stopAutoRefresh();
            }
        }

        public class CustomViewHolder
                extends RecyclerView.ViewHolder
        {
            TextView textView;

            public CustomViewHolder(View itemView)
            {
                super( itemView );

                this.textView = itemView.findViewById( R.id.textView );
            }
        }
    }
}
