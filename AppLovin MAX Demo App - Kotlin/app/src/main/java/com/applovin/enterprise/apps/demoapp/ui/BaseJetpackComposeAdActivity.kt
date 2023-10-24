package com.applovin.enterprise.apps.demoapp.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.applovin.enterprise.apps.demoapp.R

/**
 * Base for activities showing ads using Jetpack Compose.
 * <p>
 * Created by Matthew Nguyen on 2023-07-27.
 */
abstract class BaseJetpackComposeAdActivity : AppCompatActivity() {
    private val callbacks = mutableStateListOf<String>()

    /**
     * Log ad callbacks in the LazyColumn.
     * Uses the name of the function that calls this one in the log.
     */
    fun logCallback() {
        val callbackName = Throwable().stackTrace[1].methodName
        callbacks.add(callbackName)
    }

    /**
     * Composable function that lists out callbacks from logCallback() in a LazyColumn.
     * LazyColumn is synonymous with a RecyclerView.
     */
    @Composable
    fun ListCallbacks() {
        LazyColumn(Modifier.padding(10.dp)) {
            items(callbacks) { currentCallback ->
                Text(
                    text = currentCallback,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 8.dp,
                            horizontal = 16.dp
                        ),
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Divider()
            }
        }
    }

    @Composable
    fun LoadButton(onClick: () -> Unit, modifier: Modifier)
    {
        TextButton(
            onClick = onClick,
            modifier = modifier
        )
        {
            Text(
                text = "Load",
                color = colorResource(R.color.colorPrimary),
                style = TextStyle(
                    fontSize = 22.sp,
                    fontFamily = FontFamily.SansSerif
                )
            )
        }
    }

    @Composable
    fun ShowAdButton(onClick: () -> Unit, modifier: Modifier) {
        Button(
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(Color.LightGray),
            onClick = onClick,
            modifier = modifier
        )
        {
            Text(
                text = "SHOW AD",
                color = Color.Black,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}