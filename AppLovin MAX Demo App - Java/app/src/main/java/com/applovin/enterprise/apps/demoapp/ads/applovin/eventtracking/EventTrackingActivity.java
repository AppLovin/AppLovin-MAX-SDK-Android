package com.applovin.enterprise.apps.demoapp.ads.applovin.eventtracking;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.sdk.AppLovinEventParameters;
import com.applovin.sdk.AppLovinEventService;
import com.applovin.sdk.AppLovinEventTypes;
import com.applovin.sdk.AppLovinSdk;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Monica Ong on 6/8/17.
 */
public class EventTrackingActivity
        extends AppCompatActivity
{
    private EventItem[] events;

    private static final class EventItem
    {
        private String              name;
        private String              description;
        private String              eventType;
        private Map<String, String> parameters;

        private EventItem(final String name, final String description, final String appLovinEventType, final Map<String, String> parameters)
        {
            this.name = name;
            this.description = description;
            this.eventType = appLovinEventType;
            this.parameters = parameters;
        }

        String getName()
        {
            return name;
        }

        String getDescription()
        {
            return description;
        }

        void trackEvent(AppLovinEventService eventService)
        {
            if ( eventType.equals( AppLovinEventTypes.USER_COMPLETED_IN_APP_PURCHASE ) )
            {
                // eventService.trackInAppPurchase(responseIntentFromOnActivityResult, parameters);
                // responseIntentFromOnActivityResult is the Intent returned to you by Google Play upon a purchase within the onActivityResult method, as described in the Android Developer Portal.
            }
            else
            {
                eventService.trackEvent( eventType, parameters );
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_list );
        setTitle( R.string.title_activity_event_tracking );

        final AppLovinEventService eventService = AppLovinSdk.getInstance( this ).getEventService();

        events = new EventItem[] {
                new EventItem( getString( R.string.event_name_began_checkout ),
                               getString( R.string.event_description_began_checkout ),
                               AppLovinEventTypes.USER_BEGAN_CHECKOUT,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.PRODUCT_IDENTIFIER, getString( R.string.event_parameter_product_description ) );
                                       put( AppLovinEventParameters.REVENUE_AMOUNT, getString( R.string.event_parameter_price_description ) );
                                       put( AppLovinEventParameters.REVENUE_CURRENCY, getString( R.string.event_parameter_currency_description ) );
                                   }
                               } ),
                new EventItem( getString( R.string.event_name_cart ),
                               getString( R.string.event_description_cart ),
                               AppLovinEventTypes.USER_ADDED_ITEM_TO_CART,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.PRODUCT_IDENTIFIER, getString( R.string.event_parameter_product_description ) );
                                   }
                               } ),
                new EventItem( getString( R.string.event_name_achievement ),
                               getString( R.string.event_description_achievement ),
                               AppLovinEventTypes.USER_COMPLETED_ACHIEVEMENT,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.COMPLETED_ACHIEVEMENT_IDENTIFIER, getString( R.string.event_parameter_achievement_description ) );
                                   }
                               } ),
                new EventItem( getString( R.string.event_name_completed_checkout ),
                               getString( R.string.event_description_completed_checkout ),
                               AppLovinEventTypes.USER_COMPLETED_CHECKOUT,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.CHECKOUT_TRANSACTION_IDENTIFIER, getString( R.string.event_parameter_transaction_description ) );
                                       put( AppLovinEventParameters.PRODUCT_IDENTIFIER, getString( R.string.event_parameter_product_description ) );
                                       put( AppLovinEventParameters.REVENUE_AMOUNT, getString( R.string.event_parameter_amount_description ) );
                                       put( AppLovinEventParameters.REVENUE_CURRENCY, getString( R.string.event_parameter_currency_description ) );
                                   }
                               } ),
                new EventItem( getString( R.string.event_name_level ),
                               getString( R.string.event_description_level ),
                               AppLovinEventTypes.USER_COMPLETED_LEVEL,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.COMPLETED_LEVEL_IDENTIFIER, getString( R.string.event_parameter_level_description ) );
                                   }
                               } ),
                new EventItem( getString( R.string.event_name_reservation ),
                               getString( R.string.event_description_reservation ),
                               AppLovinEventTypes.USER_CREATED_RESERVATION,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.PRODUCT_IDENTIFIER, getString( R.string.event_parameter_product_description ) );
                                       long unixTimeInMilliseconds = System.currentTimeMillis() / 1000L;
                                       put( AppLovinEventParameters.RESERVATION_START_TIMESTAMP, Long.toString( unixTimeInMilliseconds ) );
                                       put( AppLovinEventParameters.RESERVATION_END_TIMESTAMP, Long.toString( unixTimeInMilliseconds ) );
                                   }
                               } ),
                new EventItem( getString( R.string.event_name_in_app_purchase ),
                               getString( R.string.event_description_in_app_purchase ),
                               AppLovinEventTypes.USER_COMPLETED_IN_APP_PURCHASE,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.REVENUE_AMOUNT, getString( R.string.event_parameter_amount_description ) );
                                       put( AppLovinEventParameters.REVENUE_CURRENCY, getString( R.string.event_parameter_currency_description ) );
                                   }
                               } ),
                new EventItem( getString( R.string.event_name_login ),
                               getString( R.string.event_description_login ),
                               AppLovinEventTypes.USER_LOGGED_IN,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.USER_ACCOUNT_IDENTIFIER, getString( R.string.event_parameter_user_description ) );
                                   }
                               } ),
                new EventItem( getString( R.string.event_name_payment_info ),
                               getString( R.string.event_description_payment_info ),
                               AppLovinEventTypes.USER_PROVIDED_PAYMENT_INFORMATION,
                               Collections.EMPTY_MAP ),
                new EventItem( getString( R.string.event_name_registration ),
                               getString( R.string.event_description_registration ),
                               AppLovinEventTypes.USER_CREATED_ACCOUNT,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.USER_ACCOUNT_IDENTIFIER, getString( R.string.event_parameter_user_description ) );
                                   }
                               } ),
                new EventItem( getString( R.string.event_name_search ),
                               getString( R.string.event_description_search ),
                               AppLovinEventTypes.USER_EXECUTED_SEARCH,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.SEARCH_QUERY, getString( R.string.event_parameter_search_description ) );
                                   }
                               } ),
                new EventItem( getString( R.string.event_name_invitation ),
                               getString( R.string.event_description_invitation ),
                               AppLovinEventTypes.USER_SENT_INVITATION,
                               Collections.EMPTY_MAP ),
                new EventItem( getString( R.string.event_name_shared_link ),
                               getString( R.string.event_description_shared_link ),
                               AppLovinEventTypes.USER_SHARED_LINK,
                               Collections.EMPTY_MAP ),
                new EventItem( getString( R.string.event_name_virt_currency ),
                               getString( R.string.event_description_virt_currency ),
                               AppLovinEventTypes.USER_SPENT_VIRTUAL_CURRENCY,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.VIRTUAL_CURRENCY_AMOUNT, getString( R.string.event_parameter_virt_amount_description ) );
                                       put( AppLovinEventParameters.VIRTUAL_CURRENCY_NAME, getString( R.string.event_paramter_virt_currency_description ) );
                                   }
                               } ),
                new EventItem( getString( R.string.event_name_tutorial ),
                               getString( R.string.event_description_tutorial ),
                               AppLovinEventTypes.USER_COMPLETED_TUTORIAL,
                               Collections.EMPTY_MAP ),
                new EventItem( getString( R.string.event_name_viewed_content ),
                               getString( R.string.event_description_viewed_content ),
                               AppLovinEventTypes.USER_VIEWED_CONTENT,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.CONTENT_IDENTIFIER, getString( R.string.event_parameter_content_description ) );
                                   }
                               } ),
                new EventItem( getString( R.string.event_name_viewed_product ),
                               getString( R.string.event_description_viewed_product ),
                               AppLovinEventTypes.USER_VIEWED_PRODUCT,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.PRODUCT_IDENTIFIER, getString( R.string.event_parameter_product_description ) );
                                   }
                               } ),
                new EventItem( getString( R.string.event_name_wishlist ),
                               getString( R.string.event_description_wishlist ),
                               AppLovinEventTypes.USER_ADDED_ITEM_TO_WISHLIST,
                               new HashMap<String, String>()
                               {
                                   {
                                       put( AppLovinEventParameters.PRODUCT_IDENTIFIER, getString( R.string.event_parameter_product_description ) );
                                   }
                               } )
        };

        ListView listView = findViewById( R.id.listView );
        listView.setAdapter( new ArrayAdapter<EventItem>( this, android.R.layout.simple_list_item_1, events )
        {
            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                View row = convertView;
                if ( row == null )
                {
                    LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
                    row = inflater.inflate( android.R.layout.simple_list_item_1, parent, false );
                }

                EventItem item = events[position];

                TextView title = row.findViewById( android.R.id.text1 );
                title.setText( item.getName() );

                return row;
            }
        } );

        listView.setOnItemClickListener( (parent, view, position, id) -> {
            EventItem event = events[position];
            event.trackEvent( eventService );

            String eventName = event.getName();
            setTitle( eventName );
        } );
    }
}
