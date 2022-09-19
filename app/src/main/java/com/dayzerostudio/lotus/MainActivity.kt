package com.dayzerostudio.lotus

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.FloatRange
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dayzerostudio.lotus.ui.theme.LotusTheme
import java.time.temporal.ValueRange
import kotlin.math.floor
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    @ExperimentalFoundationApi
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
    val showDialog = mutableStateOf(false)
    val volume = mutableStateOf(0.5f)

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
        MediaPlayer.create(context, R.raw.gong).apply {
            isLooping = false
            setVolume(volume.value, volume.value)
            this.start()
        }
        val params = bundleOf(TextToSpeech.Engine.KEY_PARAM_VOLUME to volume.value)
        tts.speak("${lapCount.value + 1}", TextToSpeech.QUEUE_ADD, params, "")
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

    private var player: MediaPlayer? = null

    private fun stopPlaying() {
        player?.release()
        player = null
    }

    private fun startPlaying(uri: Uri) {
        player = MediaPlayer().apply {
            try {
                setDataSource(context, uri)
                prepare()
                setVolume(volume.value, volume.value)
                isLooping = true
                start()
            } catch (e: Exception) {
                Log.e("DBG", "playing recording failed", e)
            }
        }
    }

    fun openVolumeSlider() {
        showDialog.value = true
    }

    fun dismissVolumeSlider() {
        showDialog.value = false
    }

    fun setVolume(v: Float) {
        volume.value = v
        if (player?.isPlaying == true) {
            player?.setVolume(v, v)
        }
    }
}

fun secondsToDigitalTime(seconds: Int) =
    "${seconds / 60}:%02d".format(seconds % 60)

@ExperimentalFoundationApi
@Composable
fun LotusTimer(vm: LotusViewModel) {
    val typography = MaterialTheme.typography

    val noiseURL = rememberStringPreference(
        keyName = "noiseURL",
        initialValue = ""
    )
    val requestNoise = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { path: Uri ->
        noiseURL.value = "$path"
    }
    if (vm.showDialog.value) {
        Dialog(onDismissRequest = { vm.dismissVolumeSlider() }) {
            Text("Volume: ${vm.volume.value.toString().take(3)}")
            Slider(
                value = vm.volume.value,
                onValueChange = vm::setVolume,
                steps = 8,
                valueRange = 0f.rangeTo(1f)
            )
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(1f),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth(1f),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { vm.openVolumeSlider() },
                Modifier.size(60.dp)
            ) {
                Text("🔉", style = typography.h5)
            }
        }
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