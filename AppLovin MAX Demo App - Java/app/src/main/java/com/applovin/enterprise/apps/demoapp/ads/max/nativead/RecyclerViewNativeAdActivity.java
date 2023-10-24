package com.applovin.enterprise.apps.demoapp.ads.max.nativead;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.nativeAds.adPlacer.MaxAdPlacer;
import com.applovin.mediation.nativeAds.adPlacer.MaxAdPlacerSettings;
import com.applovin.mediation.nativeAds.adPlacer.MaxRecyclerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecyclerViewNativeAdActivity
        extends AppCompatActivity
{
    private final ArrayList<String>  sampleData = new ArrayList<>( Arrays.asList( "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split( "(?!^)" ) ) );
    private       MaxRecyclerAdapter adAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_native_recycler_view );

        // Create recycler adapter
        CustomRecyclerAdapter originalAdapter = new CustomRecyclerAdapter( this, sampleData );

        // Configure ad adapter
        MaxAdPlacerSettings settings = new MaxAdPlacerSettings( "YOUR_AD_UNIT_ID" );
        settings.addFixedPosition( 2 );
        settings.addFixedPosition( 8 );
        settings.setRepeatingInterval( 6 );

        // If using custom views, you must also set the nativeAdViewBinder on the adapter

        adAdapter = new MaxRecyclerAdapter( settings, originalAdapter, this );
        adAdapter.setListener( new MaxAdPlacer.Listener()
        {
            @Override
            public void onAdLoaded(final int position) {}

            @Override
            public void onAdRemoved(final int position) {}

            @Override
            public void onAdClicked(final MaxAd ad) {}

            @Override
            public void onAdRevenuePaid(final MaxAd ad) {}
        } );

        // Configure recycler view
        RecyclerView recyclerView = findViewById( R.id.recycler_view );
        recyclerView.setAdapter( adAdapter );
        recyclerView.setLayoutManager( new LinearLayoutManager( this ) );

        adAdapter.loadAds();
    }

    @Override
    protected void onDestroy()
    {
        adAdapter.destroy();
        super.onDestroy();
    }

    static class CustomRecyclerAdapter
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
            View view = inflater.inflate( R.layout.activity_text_recycler_view_holder, parent, false );
            return new ViewHolder( view );
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position)
        {
            holder.textView.setText( data.get( position ) );
        }

        @Override
        public int getItemCount()
        {
            return data.size();
        }

        static class ViewHolder
                extends RecyclerView.ViewHolder
        {
            TextView textView;

            ViewHolder(View itemView)
            {
                super( itemView );
                textView = itemView.findViewById( R.id.text_view );
            }
        }
    }
}
