package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.applovin.enterprise.apps.demoapp.R;


/**
 * Created by mszaro on 4/21/15.
 */
public class InlineCarouselCardReplayOverlay
        extends LinearLayout
{
    private OnClickListener replayClickListener;
    private OnClickListener learnMoreClickListener;

    private ImageView replayImage;
    private ImageView learnMoreImage;
    private TextView  replayText;
    private TextView  learnMoreText;

    public InlineCarouselCardReplayOverlay(Context context)
    {
        super( context );
    }

    public InlineCarouselCardReplayOverlay(Context context, AttributeSet attrs)
    {
        super( context, attrs );
    }

    public InlineCarouselCardReplayOverlay(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super( context, attrs, defStyleAttr );
    }

    public InlineCarouselCardReplayOverlay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super( context, attrs, defStyleAttr, defStyleRes );
    }

    public OnClickListener getReplayClickListener()
    {
        return replayClickListener;
    }

    public void setReplayClickListener(OnClickListener replayClickListener)
    {
        this.replayClickListener = replayClickListener;
    }

    public OnClickListener getLearnMoreClickListener()
    {
        return learnMoreClickListener;
    }

    public void setLearnMoreClickListener(OnClickListener learnMoreClickListener)
    {
        this.learnMoreClickListener = learnMoreClickListener;
    }

    public void setUpView()
    {
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        inflater.inflate( R.layout.applovin_card_replay_overlay, this, true );

        bindViews();
        initializeView();
    }

    private void bindViews()
    {
        replayImage = findViewById( R.id.applovin_card_overlay_replay_image );
        replayText = findViewById( R.id.applovin_card_overlay_replay_text );
        learnMoreImage = findViewById( R.id.applovin_card_overlay_learn_more_image );
        learnMoreText = findViewById( R.id.applovin_card_overlay_learn_more_text );
    }

    private void initializeView()
    {
        replayImage.setOnClickListener( replayClickListener );
        replayText.setOnClickListener( replayClickListener );
        learnMoreImage.setOnClickListener( learnMoreClickListener );
        learnMoreText.setOnClickListener( learnMoreClickListener );
    }
}
