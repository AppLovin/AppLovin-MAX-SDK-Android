package com.applovin.enterprise.apps.demoapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import com.applovin.enterprise.apps.demoapp.ads.applovin.banners.BannerDemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.ads.max.InterstitialAdActivity;
import com.applovin.enterprise.apps.demoapp.ads.max.RewardedAdActivity;
import com.applovin.enterprise.apps.demoapp.ads.max.banners.MaxBannerDemoMenuActivity;
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
        items.add( new SectionHeader( "APPLOVIN" ) );
        items.add( new AdType( "Interstitials", new Intent( this, BannerDemoMenuActivity.class ) ) );
        items.add( new AdType( "Rewarded", new Intent( this, BannerDemoMenuActivity.class ) ) );
        items.add( new AdType( "Banner", new Intent( this, BannerDemoMenuActivity.class ) ) );
        items.add( new AdType( "MRECs", new Intent( this, BannerDemoMenuActivity.class ) ) );
        items.add( new AdType( "Native Ads", new Intent( this, BannerDemoMenuActivity.class ) ) );
        items.add( new AdType( "Event Tracking", new Intent( this, BannerDemoMenuActivity.class ) ) );

        items.add( new SectionHeader( "MAX" ) );
        items.add( new AdType( "Interstitial", new Intent( this, InterstitialAdActivity.class ) ) );
        items.add( new AdType( "Rewarded", new Intent( this, RewardedAdActivity.class ) ) );
        items.add( new AdType( "Banners", new Intent( this, MaxBannerDemoMenuActivity.class ) ) );
        items.add( new AdType( "Launch Mediation Debugger", new Intent( this, MaxBannerDemoMenuActivity.class ) ) );

        items.add( new SectionHeader( "SUPPORT" ) );
        items.add( new AdType( "Resources", new Intent( Intent.ACTION_VIEW, Uri.parse( "https://support.applovin.com/support/home" ) ) ) );
        items.add( new AdType( "Contact", getContactIntent() ) );

        return items;
    }

    private Intent getContactIntent()
    {
        Intent intent = new Intent( Intent.ACTION_SENDTO );
        intent.setType( "text/plain" );
        intent.setData( Uri.parse( "mailto:" + "support@applovin.com" ) );
        intent.putExtra( Intent.EXTRA_SUBJECT, "Android SDK support" );
        intent.putExtra( Intent.EXTRA_TEXT, "\n\n\n---\nSDK Version: " + AppLovinSdk.VERSION );
        return intent;
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

    //    @Override
    //    public boolean onCreateOptionsMenu(final Menu menu)
    //    {
    //        getMenuInflater().inflate( R.menu.menu_main, menu );
    //        return true;
    //    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        if ( item.getItemId() == R.id.action_mediation_debugger )
        {
            AppLovinSdk.getInstance( this ).showMediationDebugger();
            return true;
        }

        return super.onOptionsItemSelected( item );
    }
}
