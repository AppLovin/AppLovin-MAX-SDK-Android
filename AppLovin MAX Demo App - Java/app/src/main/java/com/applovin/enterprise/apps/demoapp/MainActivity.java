package com.applovin.enterprise.apps.demoapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.applovin.enterprise.apps.demoapp.ads.applovin.banner.BannerDemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.ads.applovin.eventtracking.EventTrackingActivity;
import com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials.InterstitialDemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.ads.applovin.leaders.LeaderDemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs.MRecDemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.NativeAdDemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.ads.applovin.rewarded.RewardedVideosDemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.ads.max.InterstitialAdActivity;
import com.applovin.enterprise.apps.demoapp.ads.max.RewardedAdActivity;
import com.applovin.enterprise.apps.demoapp.ads.max.banner.BannerAdActivity;
import com.applovin.enterprise.apps.demoapp.ads.max.mrecs.MrecAdActivity;
import com.applovin.enterprise.apps.demoapp.data.main.AdType;
import com.applovin.enterprise.apps.demoapp.data.main.FooterType;
import com.applovin.enterprise.apps.demoapp.data.main.ListItem;
import com.applovin.enterprise.apps.demoapp.data.main.SectionHeader;
import com.applovin.enterprise.apps.demoapp.ui.MainRecyclerViewAdapter;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;

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
    private MenuItem muteToggleMenuItem;

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

        // Check that SDK key is present in Android Manifest
        checkSdkKey();
    }

    private void checkSdkKey()
    {
        final String sdkKey = AppLovinSdk.getInstance( getApplicationContext() ).getSdkKey();
        if ( "YOUR_SDK_KEY".equalsIgnoreCase( sdkKey ) )
        {
            new AlertDialog.Builder( this )
                    .setTitle( "ERROR" )
                    .setMessage( "Please update your sdk key in the manifest file." )
                    .setCancelable( false )
                    .setNeutralButton( "OK", null )
                    .show();
        }
    }

    // Mute Toggling

    /**
     * Toggling the sdk mute setting will affect whether your video ads begin in a muted state or not.
     */
    private void toggleMute()
    {
        AppLovinSdk sdk = AppLovinSdk.getInstance( this );
        sdk.getSettings().setMuted( !sdk.getSettings().isMuted() );
        muteToggleMenuItem.setIcon( getMuteIconForCurrentSdkMuteSetting() );
    }

    private Drawable getMuteIconForCurrentSdkMuteSetting()
    {
        AppLovinSdk sdk = AppLovinSdk.getInstance( this );
        int drawableId = sdk.getSettings().isMuted() ? R.drawable.mute : R.drawable.unmute;

        if ( Build.VERSION.SDK_INT >= 22 )
        {
            return getResources().getDrawable( drawableId, getTheme() );
        }
        else
        {
            return getResources().getDrawable( drawableId );
        }
    }

    private List<ListItem> generateMainListItems()
    {
        final List<ListItem> items = new ArrayList<>();

        items.add( new SectionHeader( "APPLOVIN" ) );
        items.add( new AdType( "Interstitials", new Intent( this, InterstitialDemoMenuActivity.class ) ) );
        items.add( new AdType( "Rewarded", new Intent( this, RewardedVideosDemoMenuActivity.class ) ) );

        // Add "Leaders" menu item for tablets
        if ( AppLovinSdkUtils.isTablet( this ) )
        {
            items.add( new AdType( "Leaders", new Intent( this, LeaderDemoMenuActivity.class ) ) );
        }
        // Add "Banners" menu item for phones
        else
        {
            items.add( new AdType( "Banners", new Intent( this, BannerDemoMenuActivity.class ) ) );
        }

        items.add( new AdType( "MRECs", new Intent( this, MRecDemoMenuActivity.class ) ) );
        items.add( new AdType( "Native Ads", new Intent( this, NativeAdDemoMenuActivity.class ) ) );
        items.add( new AdType( "Event Tracking", new Intent( this, EventTrackingActivity.class ) ) );
        items.add( new SectionHeader( "MAX" ) );
        items.add( new AdType( "Interstitials", new Intent( this, InterstitialAdActivity.class ) ) );
        items.add( new AdType( "Rewarded", new Intent( this, RewardedAdActivity.class ) ) );
        items.add( new AdType( "Banners", new Intent( this, BannerAdActivity.class ) ) );
        items.add( new AdType( "MRECs", new Intent( this, MrecAdActivity.class ) ) );
        items.add( new AdType( "Launch Mediation Debugger", () -> AppLovinSdk.getInstance( getApplicationContext() ).showMediationDebugger() ) );
        items.add( new SectionHeader( "SUPPORT" ) );
        items.add( new AdType( "Visit our Support Site", new Intent( Intent.ACTION_VIEW, Uri.parse( "https://support.applovin.com/support/home" ) ) ) );
        items.add( new FooterType() );
        return items;
    }

    @Override
    public void onItemClicked(final ListItem item)
    {
        if ( item.getType() == ListItem.TYPE_AD_ITEM )
        {
            final AdType adType = (AdType) item;
            if ( adType.getIntent() != null )
            {
                startActivity( adType.getIntent() );
            }
            else if ( adType.getRunnable() != null )
            {
                adType.getRunnable().run();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        getMenuInflater().inflate( R.menu.menu_main, menu );
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        muteToggleMenuItem = menu.findItem( R.id.action_toggle_mute );
        muteToggleMenuItem.setIcon( getMuteIconForCurrentSdkMuteSetting() );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        if ( item.getItemId() == R.id.action_toggle_mute )
        {
            toggleMute();
        }

        return true;
    }
}
