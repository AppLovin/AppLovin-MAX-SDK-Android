package com.applovin.enterprise.apps.demoapp.ads.applovin.eventtracking

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView

import com.applovin.sdk.AppLovinEventParameters
import com.applovin.sdk.AppLovinEventService
import com.applovin.sdk.AppLovinEventTypes
import com.applovin.sdk.AppLovinSdk
import android.widget.ListView
import java.lang.Long

import com.applovin.enterprise.apps.demoapp.R

class EventTrackingActivity : AppCompatActivity()
{
    private data class EventItem(val name: String,
                                 val description: String,
                                 val eventType: String,
                                 val parameters: Map<String, String>)
    {
        fun trackEvent(eventService: AppLovinEventService)
        {
            if (eventType == AppLovinEventTypes.USER_COMPLETED_IN_APP_PURCHASE)
            {
                // Note: In a real application, you would call:
                // eventService.trackInAppPurchase(responseIntentFromOnActivityResult, parameters);
                // responseIntentFromOnActivityResult is the Intent returned to you by Google Play upon a purchase within the onActivityResult method, as described in the Android Developer Portal.
            }
            else
            {
                eventService.trackEvent(eventType, parameters)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        setTitle(R.string.title_activity_event_tracking)

        val eventService = AppLovinSdk.getInstance(this).eventService

        val unixTimeInMilliseconds = System.currentTimeMillis() / 1000L

        val events = arrayOf(
                EventItem(getString(R.string.event_name_began_checkout),
                          getString(R.string.event_description_began_checkout),
                          AppLovinEventTypes.USER_BEGAN_CHECKOUT,
                          mapOf(
                                  AppLovinEventParameters.PRODUCT_IDENTIFIER to getString(R.string.event_parameter_product_description),
                                  AppLovinEventParameters.REVENUE_AMOUNT to getString(R.string.event_parameter_price_description),
                                  AppLovinEventParameters.REVENUE_CURRENCY to getString(R.string.event_parameter_currency_description)
                          )
                ),
                EventItem(getString(R.string.event_name_cart),
                          getString(R.string.event_description_cart),
                          AppLovinEventTypes.USER_ADDED_ITEM_TO_CART,
                          mapOf(
                                  AppLovinEventParameters.PRODUCT_IDENTIFIER to getString(R.string.event_parameter_product_description)
                          )
                ),
                EventItem(getString(R.string.event_name_achievement),
                          getString(R.string.event_description_achievement),
                          AppLovinEventTypes.USER_COMPLETED_ACHIEVEMENT,
                          mapOf(
                                  AppLovinEventParameters.COMPLETED_ACHIEVEMENT_IDENTIFIER to getString(R.string.event_parameter_achievement_description)
                          )
                ),
                EventItem(getString(R.string.event_name_completed_checkout),
                          getString(R.string.event_description_completed_checkout),
                          AppLovinEventTypes.USER_COMPLETED_CHECKOUT,
                          mapOf(
                                  AppLovinEventParameters.CHECKOUT_TRANSACTION_IDENTIFIER to getString(R.string.event_parameter_transaction_description),
                                  AppLovinEventParameters.PRODUCT_IDENTIFIER to getString(R.string.event_parameter_product_description),
                                  AppLovinEventParameters.REVENUE_AMOUNT to getString(R.string.event_parameter_amount_description),
                                  AppLovinEventParameters.REVENUE_CURRENCY to getString(R.string.event_parameter_currency_description)
                          )
                ),
                EventItem(getString(R.string.event_name_level),
                          getString(R.string.event_description_level),
                          AppLovinEventTypes.USER_COMPLETED_LEVEL,
                          mapOf(
                                  AppLovinEventParameters.COMPLETED_LEVEL_IDENTIFIER to getString(R.string.event_parameter_level_description)
                          )
                ),
                EventItem(getString(R.string.event_name_reservation),
                          getString(R.string.event_description_reservation),
                          AppLovinEventTypes.USER_CREATED_RESERVATION,
                          mapOf(
                                  AppLovinEventParameters.PRODUCT_IDENTIFIER to getString(R.string.event_parameter_product_description),
                                  AppLovinEventParameters.RESERVATION_START_TIMESTAMP to Long.toString(unixTimeInMilliseconds),
                                  AppLovinEventParameters.RESERVATION_END_TIMESTAMP to Long.toString(unixTimeInMilliseconds)
                          )
                ),
                EventItem(getString(R.string.event_name_in_app_purchase),
                          getString(R.string.event_description_in_app_purchase),
                          AppLovinEventTypes.USER_COMPLETED_IN_APP_PURCHASE,
                          mapOf(
                                  AppLovinEventParameters.REVENUE_AMOUNT to getString(R.string.event_parameter_amount_description),
                                  AppLovinEventParameters.REVENUE_CURRENCY to getString(R.string.event_parameter_currency_description)
                          )
                ),
                EventItem(getString(R.string.event_name_login),
                          getString(R.string.event_description_login),
                          AppLovinEventTypes.USER_LOGGED_IN,
                          mapOf(
                                  AppLovinEventParameters.USER_ACCOUNT_IDENTIFIER to getString(R.string.event_parameter_user_description)
                          )
                ),
                EventItem(getString(R.string.event_name_payment_info),
                          getString(R.string.event_description_payment_info),
                          AppLovinEventTypes.USER_PROVIDED_PAYMENT_INFORMATION,
                          mapOf()
                ),
                EventItem(getString(R.string.event_name_registration),
                          getString(R.string.event_description_registration),
                          AppLovinEventTypes.USER_CREATED_ACCOUNT,
                          mapOf(
                                  AppLovinEventParameters.USER_ACCOUNT_IDENTIFIER to getString(R.string.event_parameter_user_description)
                          )
                ),
                EventItem(getString(R.string.event_name_search),
                          getString(R.string.event_description_search),
                          AppLovinEventTypes.USER_EXECUTED_SEARCH,
                          mapOf(
                                  AppLovinEventParameters.SEARCH_QUERY to getString(R.string.event_parameter_search_description)
                          )
                ),
                EventItem(getString(R.string.event_name_invitation),
                          getString(R.string.event_description_invitation),
                          AppLovinEventTypes.USER_SENT_INVITATION,
                          mapOf()
                ),
                EventItem(getString(R.string.event_name_shared_link),
                          getString(R.string.event_description_shared_link),
                          AppLovinEventTypes.USER_SHARED_LINK,
                          mapOf()
                ),
                EventItem(getString(R.string.event_name_virt_currency),
                          getString(R.string.event_description_virt_currency),
                          AppLovinEventTypes.USER_SPENT_VIRTUAL_CURRENCY,
                          mapOf(
                                  AppLovinEventParameters.VIRTUAL_CURRENCY_AMOUNT to getString(R.string.event_parameter_virt_amount_description),
                                  AppLovinEventParameters.VIRTUAL_CURRENCY_NAME to getString(R.string.event_paramter_virt_currency_description)
                          )
                ),
                EventItem(getString(R.string.event_name_tutorial),
                          getString(R.string.event_description_tutorial),
                          AppLovinEventTypes.USER_COMPLETED_TUTORIAL,
                          mapOf()
                ),
                EventItem(getString(R.string.event_name_viewed_content),
                          getString(R.string.event_description_viewed_content),
                          AppLovinEventTypes.USER_VIEWED_CONTENT,
                          mapOf(
                                  AppLovinEventParameters.CONTENT_IDENTIFIER to getString(R.string.event_parameter_content_description)
                          )
                ),
                EventItem(getString(R.string.event_name_viewed_product),
                          getString(R.string.event_description_viewed_product),
                          AppLovinEventTypes.USER_VIEWED_PRODUCT,
                          mapOf(
                                  AppLovinEventParameters.PRODUCT_IDENTIFIER to getString(R.string.event_parameter_product_description)
                          )
                ),
                EventItem(getString(R.string.event_name_wishlist),
                          getString(R.string.event_description_wishlist),
                          AppLovinEventTypes.USER_ADDED_ITEM_TO_WISHLIST,
                          mapOf(
                                  AppLovinEventParameters.PRODUCT_IDENTIFIER to getString(R.string.event_parameter_product_description)
                          )
                )
        )
        val listView = findViewById<ListView>(R.id.list_view)

        listView.adapter = object : ArrayAdapter<EventItem>(this, android.R.layout.simple_list_item_1, events)
        {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
            {
                val inflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

                val row = convertView ?: inflater.inflate(android.R.layout.simple_list_item_1, parent, false)!!

                val item = events[position]

                val title: TextView = row.findViewById(android.R.id.text1)
                title.text = item.name

                return row
            }
        }


        val itemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val event = events[position]
            event.trackEvent(eventService)

            val eventName = event.name
            title = eventName
        }
        listView.onItemClickListener = itemClickListener
    }
}
