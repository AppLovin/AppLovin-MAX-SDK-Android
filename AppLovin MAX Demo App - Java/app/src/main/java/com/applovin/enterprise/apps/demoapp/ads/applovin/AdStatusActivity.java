package com.applovin.enterprise.apps.demoapp.ads.applovin;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public abstract class AdStatusActivity
        extends AppCompatActivity
{
    protected TextView adStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );

        getSupportActionBar().setBackgroundDrawable( new ColorDrawable( 0xDD0A83AA ) );
        getSupportActionBar().show();
    }

    protected void log(final String message)
    {
        if ( adStatusTextView != null )
        {
            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    adStatusTextView.setText( message );
                }
            } );
        }
        System.out.println( message );
    }
}
