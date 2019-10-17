package com.applovin.enterprise.apps.demoapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.sdk.AppLovinSdkUtils
import kotlinx.android.synthetic.main.activity_splash.*
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        logoTextView.text = HtmlCompat.fromHtml("<b>App</b>Lovin", HtmlCompat.FROM_HTML_MODE_LEGACY)

        AppLovinSdkUtils.runOnUiThreadDelayed(
                {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                }, TimeUnit.SECONDS.toMillis(2))
    }
}
