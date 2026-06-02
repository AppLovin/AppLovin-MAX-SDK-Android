package com.applovin.enterprise.apps.demoapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.applovin.sdk.AppLovinSdkUtils
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        waitForSdkThenOpenMain()
    }

    private fun waitForSdkThenOpenMain() {
        val handler = Handler(Looper.getMainLooper())
        val maxWaitMs = TimeUnit.SECONDS.toMillis(10)
        val minSplashMs = TimeUnit.SECONDS.toMillis(1)
        val startTime = System.currentTimeMillis()

        fun check() {
            val app = application as? GlobalApplication
            val elapsed = System.currentTimeMillis() - startTime
            if (app != null && app.isSdkInitialized) {
                openMain()
                return
            }
            if (elapsed >= maxWaitMs) {
                openMain()
                return
            }
            handler.postDelayed({ check() }, 150)
        }

        AppLovinSdkUtils.runOnUiThreadDelayed({ check() }, minSplashMs)
    }

    private fun openMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        finish()
    }
}
