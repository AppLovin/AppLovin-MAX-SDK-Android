package com.applovin.enterprise.apps.demoapp;

import android.content.Intent;
import android.os.Bundle;

import com.applovin.enterprise.apps.demoapp.ads.ProgrammaticBannerAdActivity;
import com.applovin.enterprise.apps.demoapp.ads.InterstitialAdActivity;
import com.applovin.enterprise.apps.demoapp.ads.LayoutEditorBannerAdActivity;
import com.applovin.enterprise.apps.demoapp.ads.RewardedAdActivity;
import com.applovin.enterprise.apps.demoapp.data.main.AdType;
import com.applovin.enterprise.apps.demoapp.data.main.ListItem;
import com.applovin.enterprise.apps.demoapp.data.main.SectionHeader;
import com.applovin.enterprise.apps.demoapp.ui.MainRecyclerViewAdapter;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * The main {@link android.app.Activity} of this app.
 * <p>
 * Created by santoshbagadi on 2019-09-10.
 */
public class MainActivity
        extends AppCompatActivity
        implements MainRecyclerViewAdapter.OnMainListItemClickListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        Toolbar toolbar = findViewById( R.id.toolbar );
        setSupportActionBar( toolbar );

        final MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter( generateMainListItems(), this, this );
        final LinearLayoutManager manager = new LinearLayoutManager( this );
        final DividerItemDecoration decoration = new DividerItemDecoration( this, manager.getOrientation() );

        final RecyclerView recyclerView = findViewById( R.id.main_recycler_view );
        recyclerView.setHasFixedSize( true );
        recyclerView.setLayoutManager( manager );
        recyclerView.addItemDecoration( decoration );
        recyclerView.setItemAnimator( new DefaultItemAnimator() );
        recyclerView.setAdapter( adapter );

        // Initialize the AppLovin SDK
        AppLovinSdk.getInstance( this ).setMediationProvider( AppLovinMediationProvider.MAX );
        AppLovinSdk.getInstance( this ).initializeSdk( new AppLovinSdk.SdkInitializationListener()
        {
            @Override
            public void onSdkInitialized(final AppLovinSdkConfiguration config)
            {
                // AppLovin SDK is initialized, start loading ads now or later if ad gate is reached
            }
        } );
    }

    private List<ListItem> generateMainListItems()
    {
        final List<ListItem> items = new ArrayList<>();

        items.add( new SectionHeader( "MAX Ads" ) );
        items.add( new AdType( "Interstitial", new Intent( this, InterstitialAdActivity.class ) ) );
        items.add( new AdType( "Rewarded", new Intent( this, RewardedAdActivity.class ) ) );
        items.add( new AdType( "Programmatic Banners / Leaders", new Intent( this, ProgrammaticBannerAdActivity.class ) ) );
        items.add( new AdType( "Layout Editor Banners / Leaders", new Intent( this, LayoutEditorBannerAdActivity.class ) ) );

        return items;
    }

    @Override
    public void onItemClicked(final ListItem item)
    {
        if ( item.getType() == ListItem.TYPE_AD_ITEM )
        {
            final AdType adType = (AdType) item;
            startActivity( adType.getIntent() );
        }
    }
}
