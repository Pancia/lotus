package com.dayzerostudio.lotus

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dayzerostudio.lotus.ui.theme.LotusTheme
import kotlin.math.floor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LotusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val vm: LotusViewModel by viewModels {
                        object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return LotusViewModel(applicationContext) as T
                            }
                        }
                    }
                    LotusTimer(vm)
                }
            }
        }
    }
}

class LotusViewModel(private val context: Context) : ViewModel() {
    val loopSeconds = mutableStateOf(120)
    var secondsLeft: MutableState<Int?> = mutableStateOf(null)
    var timer: CountDownTimer? = null
    val lapCount = mutableStateOf(0)

    fun addMinute() {
        if (timer == null)
            loopSeconds.value += 60
    }

    fun subMinute() {
        if (timer == null && loopSeconds.value > 60)
            loopSeconds.value -= 60
    }

    private val tts = TextToSpeech(context) {}

    fun playSound() {
        Log.d("DBG", "playSound")
        MediaPlayer.create(context, R.raw.gong).apply {
            isLooping = false
            this.start()
        }
        tts.speak("${lapCount.value + 1}", TextToSpeech.QUEUE_ADD, Bundle.EMPTY, "")
    }

    fun start() {
        if (timer == null) {
            secondsLeft.value = loopSeconds.value
            timer = object : CountDownTimer(loopSeconds.value * 1000L, 1000) {
                override fun onTick(millisLeft: Long) {
                    secondsLeft.value = floor(millisLeft / 1000f).toInt()
                }
                override fun onFinish() {
                    playSound()
                    lapCount.value += 1
                    start()
                }
            }.start()
        }
    }

    fun stop() {
        timer?.cancel()
        timer = null
        secondsLeft.value = null
        lapCount.value = 0
    }
}

fun secondsToDigitalTime(seconds: Int) =
    "${seconds / 60}:%02d".format(seconds % 60)

@Composable
fun LotusTimer(vm: LotusViewModel) {
    val typography = MaterialTheme.typography
    Column(
        modifier = Modifier.fillMaxSize(1f),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            secondsToDigitalTime(vm.secondsLeft.value ?: vm.loopSeconds.value),
            style = typography.h3
        )
        if (vm.lapCount.value >= 1) {
            Text("laps: ${vm.lapCount.value}", style = typography.h4)
        }
        Row(
            Modifier.fillMaxWidth(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { vm.subMinute() }, Modifier.size(60.dp)) {
                Text("-", style = typography.h4)
            }
            Button(onClick = { vm.addMinute() }, Modifier.size(60.dp)) {
                Text("+", style = typography.h4)
            }
        }
        Row(
            Modifier.fillMaxWidth(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { vm.start() }, Modifier.size(60.dp)) {
                Icon(Icons.Default.PlayArrow, "start", Modifier.size(30.dp))
            }
            Button(onClick = { vm.stop() }, Modifier.size(60.dp)) {
                Icon(Icons.Default.Close, "stop", Modifier.size(30.dp))
            }
        }
    }
}