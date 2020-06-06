package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.applovin.enterprise.apps.demoapp.R

class InlineCarouselCardReplayOverlay : LinearLayout
{
    var replayClickListener: View.OnClickListener? = null
    var learnMoreClickListener: View.OnClickListener? = null

    private var replayImage: ImageView? = null
    private var learnMoreImage: ImageView? = null
    private var replayText: TextView? = null
    private var learnMoreText: TextView? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    fun setUpView()
    {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.applovin_card_replay_overlay, this, true)

        bindViews()
        initializeView()
    }

    private fun bindViews()
    {
        replayImage = findViewById(R.id.applovin_card_overlay_replay_image)
        replayText = findViewById(R.id.applovin_card_overlay_replay_text)
        learnMoreImage = findViewById(R.id.applovin_card_overlay_learn_more_image)
        learnMoreText = findViewById(R.id.applovin_card_overlay_learn_more_text)
    }

    private fun initializeView()
    {
        replayImage!!.setOnClickListener(replayClickListener)
        replayText!!.setOnClickListener(replayClickListener)
        learnMoreImage!!.setOnClickListener(learnMoreClickListener)
        learnMoreText!!.setOnClickListener(learnMoreClickListener)
    }
}
