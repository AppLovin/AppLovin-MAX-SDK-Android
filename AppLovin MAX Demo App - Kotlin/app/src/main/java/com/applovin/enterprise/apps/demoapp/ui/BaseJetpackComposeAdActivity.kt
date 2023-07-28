package com.applovin.enterprise.apps.demoapp.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


/**
 * Base for activities showing ads using Jetpack Compose.
 * <p>
 * Created by Matthew Nguyen on 2023-07-27.
 */
abstract class BaseJetpackComposeAdActivity : AppCompatActivity()
{
    private val callbacks = mutableStateListOf<String>()

    /**
     * Log ad callbacks in the LazyColumn.
     * Uses the name of the function that calls this one in the log.
     */
    fun logCallback()
    {
        val callbackName = Throwable().stackTrace[1].methodName
        callbacks.add(callbackName)
    }

    /**
     * Composable function that lists out callbacks from logCallback() in a LazyColumn.
     * LazyColumn is synonymous with a RecyclerView.
     */
    @Composable
    fun ListCallbacks()
    {
        LazyColumn {
            items(callbacks) { currentCallback ->
                Text(
                    text = currentCallback,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                )
                Divider()
            }
        }
    }

}
