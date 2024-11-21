package com.yunho.audiorecord

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeechToTextApp()
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SpeechToTextApp() {
        var text by remember { mutableStateOf("") }
        val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(this) }

        val speechRecognizerIntent = remember {
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
        }

        LaunchedEffect(speechRecognizer) {
            checkPermission()

            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(bundle: Bundle) {
                    Log.d("SpeechRecognizer", "onReadyForSpeech called")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("SpeechRecognizer", "onBeginningOfSpeech called")
                }

                override fun onRmsChanged(v: Float) {
                    Log.d("SpeechRecognizer", "onRmsChanged called with value: $v")
                }

                override fun onBufferReceived(bytes: ByteArray) {
                    Log.d(
                        "SpeechRecognizer",
                        "onBufferReceived called with buffer size: ${bytes.size}"
                    )
                }

                override fun onEndOfSpeech() {
                    Log.d("SpeechRecognizer", "onEndOfSpeech called")
                }

                override fun onError(i: Int) {
                    Log.e("SpeechRecognizer", "onError called with error code: $i")
                }

                override fun onResults(bundle: Bundle) {
                    val matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d("SpeechRecognizer", "onResults called, matches: $matches")
                    matches?.let {
                        text = it[0]
                    }
                }

                override fun onPartialResults(bundle: Bundle) {
                    Log.d("SpeechRecognizer", "onPartialResults called")
                }

                override fun onEvent(i: Int, bundle: Bundle) {
                    Log.d("SpeechRecognizer", "onEvent called with event code: $i")
                }
            })
        }


        // UI
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Speech to Text") }, modifier = Modifier.clickable {
                    speechRecognizer.startListening(speechRecognizerIntent)
                })
            },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .height(300.dp), fontSize = 50.sp
                    )

                    Button(
                        onClick = {
                            speechRecognizer.startListening(speechRecognizerIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text("Start Listening")
                    }
                }
            }
        )
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + packageName)
            )
            startActivity(intent)
            finish()
            Toast.makeText(this, "Enable Microphone Permission..!!", Toast.LENGTH_SHORT).show()
        }
    }
}


