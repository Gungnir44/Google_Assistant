package com.example.googleassistant.assistant

//noinspection SuspiciousImport
import android.R
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.ClipboardManager
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.Global.getString
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.googleassistant.data.AssistantDatabase
import com.example.googleassistant.databinding.ActivityAssistantBinding
import java.util.Locale


class AssistantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssistantBinding
    private lateinit var assistantViewModel: AssistantViewModel

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizer: Intent
    private lateinit var keeper: String

    private var REQUESTCALL = 1
    private var SENDSMS = 2
    private var READSMS = 3
    private var SHAREAFILE = 4
    private var SHAREATEXTFILE = 5
    private var READCONTACTS = 6
    private var CAPTUREPHOTO = 7

    private var REQUEST_CODE_SELECT_DOC: Int = 100
    private var REQUEST_ENABLE_BT = 1000

    private var bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var cameraManager: CameraManager
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var cameraID: String
    private lateinit var ringtone: Ringtone

    private val logtts = "TTS"
    private val logsr = "SR"
    private val logkeeper = "keeper"

    private var imageIndex: Int = 0
    private lateinit var imgUri: Uri
    private lateinit var helper: OpenWeatherMapHelper


    @Suppress("DEPRECATION")
    private val imageDirectory = Environment.getExternalStorageState(Environment.DIRECTORY_PICTURES).toString()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(com.example.googleassistant.R.anim.non_movable, com.example.googleassistant.R.anim.non_movable)

        binding = DataBindingUtil.setContentView(this, com.example.googleassistant.R.layout.activity_assistant)

        val application = requireNotNull(this).application
        val dataSource = AssistantDatabase.getInstance(application).assistantDao
        val viewModelFactory = AssistantViewModelFactory(dataSource, application)

        assistantViewModel = ViewModelProvider(this, viewModelFactory).get(AssistantViewModel::class.java)
        val adapter = AssistantAdapter()
        binding.recyclerview.adapter = adapter

        assistantViewModel.message.observe(this) { it?.Let{ adapter.data = it } }
        binding.lifecycleOwner = this
        //animations
        if(savedInstanceState == null){
            binding.assistantConstraintLayout.visibility = View.INVISIBLE

            val viewTreeObserver: ViewTreeObserver = binding.assistantConstraintLayout.getViewTreeObserver()
            if(viewTreeObserver.isAlive){
                viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{
                    override fun onGlobalLayout() {
                        circularRevealActivity()
                        binding.assistantConstraintLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                    }
                })
            }

        }
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try{
            cameraID = cameraManager.cameraIdList[1]
            //0 back camera, 1 is front
        }
        catch (e: java.lang.Exception){
            e.printStackTrace()
        }
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        ringtone = RingtoneManager.getRingtone(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
        helper = OpenWeatherMapHelper(getString(R.string.OPEN_WEATHER_MAP_API_KEY))

        textToSpeech = TextToSpeech(this) { status ->
            if(status == TextToSpeech.SUCCESS){
                val result: Int = textToSpeech.setLanguage(Locale.ENGLISH)
                if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                    Log.e(logtts, "Language Not Supported")
                }
                else{
                    Log.e(logtts, "Language Supported")
                }
            }
            else{
                Log.e(logtts, "Initialization Failed")
            }
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {

            }

            override fun onBeginningOfSpeech() {
                Log.d("SR", "Started")
            }

            override fun onRmsChanged(p0: Float) {

            }

            override fun onBufferReceived(p0: ByteArray?) {
                TODO("Not yet implemented")
            }

            override fun onEndOfSpeech() {
                Log.d("SR", "Ended")
            }

            override fun onError(p0: Int) {
                TODO("Not yet implemented")
            }

            override fun onResults(p0: Bundle?) {
                val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if(data != null){
                    keeper = data[0]
                    Log.d(logkeeper, keeper)
                    when{
                        keeper.contains("thanks") -> speak("You're welcome, is there anything else I can help with?")
                        keeper.contains("braille") -> speak("How may I assist you?")
                        keeper.contains("clear") -> assistantViewModel.onClear()
                        keeper.contains("date") -> getDate()
                        keeper.contains("time") -> getTime()
                        keeper.contains("make call") -> makeAPhoneCall()
                        keeper.contains("send text") -> sendSMS()
                        keeper.contains("read last text") -> readSMS()
                        keeper.contains("open Gmail") -> openGmail()
                        keeper.contains("open Facebook") -> openFacebook()
                        keeper.contains("open messages") -> openMessages()
                        keeper.contains("share file") -> shareAFile()
                        keeper.contains("call") -> callContact()
                        keeper.contains("turn on bluetooth") -> turnOnBluetooth()
                        keeper.contains("turn off bluetooth") -> turnOffBluetooth()
                        keeper.contains("get bluetooth devices") -> getAllPairedDevices()
                        keeper.contains("turn on flash") -> turnOnFlash()
                        keeper.contains("turn of flash") -> turnOffFlash()
                        keeper.contains("copy to clipboard") -> clipBoardCopy()
                        keeper.contains("read last clipboard") -> clipBoardSpeak()
                        keeper.contains("capture photo") -> capturePhoto()
                        keeper.contains("alarm") -> setAlarm()
                        keeper.contains("weather") -> weather()
                        keeper.contains("horoscope") -> horoscope()
                        keeper.contains("read me") -> readMe()
                        keeper.contains("medical") -> medicalApplication()
                        keeper.contains("joke") -> joke()
                        keeper.contains("question") -> question()
                        keeper.contains("hello") || keeper.contains("hi") || keeper.contains("hey") -> speak("How can I be of service?")
                        //keeper.contains("") -> ()
                        //keeper.contains("") -> ()
                        //keeper.contains("") -> ()
                        else -> speak("I'm sorry, that is invalid or I did not hear you.")
                    }
                }
            }

            override fun onPartialResults(p0: Bundle?) {
                TODO("Not yet implemented")
            }

            override fun onEvent(p0: Int, p1: Bundle?) {
                TODO("Not yet implemented")
            }

        }
    }
    class OpenWeatherMapHelper(string: String) {
    }
}


