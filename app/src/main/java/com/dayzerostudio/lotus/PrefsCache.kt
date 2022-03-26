package com.dayzerostudio.lotus

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

fun Context.getCache() =
    getSharedPreferences("cache", 0)!!

@Composable
fun rememberStringPreference(
    keyName: String,
    initialValue: String
): MutableState<String> {
    val currentState: MutableState<String> = remember { mutableStateOf(initialValue) }
    val context = LocalContext.current
    val sp = context.getCache()
    sp.getString(keyName, null)?.let {
        currentState.value = it
    }
    return object : MutableState<String> {
        override var value: String
            get() = currentState.value
            set(v) {
                currentState.value = v
                sp.edit().putString(keyName, v).apply()
            }

        override fun component1() = value
        override fun component2(): (String) -> Unit = { value = it }
    }
}