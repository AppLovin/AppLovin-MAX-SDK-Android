package com.applovin.enterprise.apps.demoapp.ads.applovin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.applovin.enterprise.apps.demoapp.MainActivity;
import com.applovin.enterprise.apps.demoapp.R;

public class SplashActivity
        extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_splash );

        getSupportActionBar().hide();

        TextView logoTextView = (TextView) findViewById( R.id.logoTextView );
        logoTextView.setText( Html.fromHtml( "<b>App</b>Lovin" ) );

        Thread timerThread = new Thread()
        {
            @Override
            public void run()
            {
                super.run();

                try
                {
                    sleep( 3000 );
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
                finally
                {
                    Intent intent = new Intent( SplashActivity.this, MainActivity.class );
                    startActivity( intent );
                }
            }
        };
        timerThread.start();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        finish();
    }
}
