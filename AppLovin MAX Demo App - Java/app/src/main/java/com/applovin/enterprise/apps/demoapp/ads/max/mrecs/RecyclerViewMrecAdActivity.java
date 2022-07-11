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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewMrecAdActivity
        extends AppCompatActivity
{
    private final ArrayList<String> sampleData  = new ArrayList<>( Arrays.asList( "ABCDEFGHIJKL".split( "" ) ) );
    private       Queue<MaxAdView>  adViewQueue = new LinkedList<>();

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_mrec_recycler_view );
        setTitle( R.string.activity_recycler_view_mrecs );

        CustomRecyclerAdapter adapter = new CustomRecyclerAdapter( RecyclerViewMrecAdActivity.this, sampleData );

        // Configure recycler view
        RecyclerView recyclerView = findViewById( R.id.mrec_recycler_view );
        recyclerView.setLayoutManager( new LinearLayoutManager( RecyclerViewMrecAdActivity.this ) );
        recyclerView.setAdapter( adapter );

        configureAdViews( 5 );
    }

    private void configureAdViews(int count)
    {
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
            adViewQueue.offer( adView );
        }
    }

    public class CustomRecyclerAdapter
            extends RecyclerView.Adapter<CustomRecyclerAdapter.ViewHolder>
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
        public CustomRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType)
        {
            View view = inflater.inflate( R.layout.activity_mrec_recycler_view_holder, parent, false );

            return new ViewHolder( view );
        }

        @Override
        public void onBindViewHolder(@NonNull final CustomRecyclerAdapter.ViewHolder holder, final int position)
        {
            if ( position % 4 == 0 && !adViewQueue.isEmpty() )
            {
                MaxAdView adView = adViewQueue.poll();
                adView.startAutoRefresh();
                holder.setAdView( adView );
                holder.configure(); // Configure view holder with an ad
                adViewQueue.offer( adView );
            }
            else
            {
                holder.textView.setText( data.get( position ) ); // Configure custom views
            }
        }

        @Override
        public void onViewRecycled(@NonNull final ViewHolder holder)
        {
            super.onViewRecycled( holder );

            holder.textView.setText( null );
            ViewGroup viewGroup = (ViewGroup) holder.itemView;

            for ( int i = viewGroup.getChildCount(); i >= 0; i-- )
            {
                View subview = viewGroup.getChildAt( i );
                if ( subview instanceof MaxAdView )
                {
                    // Set this extra parameter to work around SDK bug that ignores calls to stopAutoRefresh()
                    ( (MaxAdView) subview ).setExtraParameter( "allow_pause_auto_refresh_immediately", "true" );
                    ( (MaxAdView) subview ).stopAutoRefresh();

                    viewGroup.removeViewAt( i );
                }
            }
        }

        @Override
        public int getItemCount()
        {
            return sampleData.size();
        }

        public class ViewHolder
                extends RecyclerView.ViewHolder
        {
            TextView  textView;
            MaxAdView adView;

            ViewHolder(View itemView)
            {
                super( itemView );
                textView = itemView.findViewById( R.id.textView );
            }

            public void configure()
            {
                // MREC width and height are 300 and 250 respectively, on phones and tablets
                int widthPx = AppLovinSdkUtils.dpToPx( RecyclerViewMrecAdActivity.this, 300 );
                int heightPx = AppLovinSdkUtils.dpToPx( RecyclerViewMrecAdActivity.this, 250 );

                // Set background or background color for MRECs to be fully functional
                adView.setBackgroundColor( Color.WHITE );
                adView.setLayoutParams( new FrameLayout.LayoutParams( widthPx, heightPx, Gravity.CENTER ) );
                ( (ViewGroup) itemView ).addView( adView );
            }

            private void setAdView(final MaxAdView adView)
            {
                this.adView = adView;
            }
        }
    }
}
