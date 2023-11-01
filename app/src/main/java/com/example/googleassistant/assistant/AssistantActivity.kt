package com.example.googleassistant.assistant

//noinspection SuspiciousImport
import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcel
import android.os.Parcelable
import android.os.StrictMode
import android.provider.ContactsContract
import android.provider.Settings.Global.getString
import android.provider.Telephony
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.googleassistant.data.AssistantDatabase
import com.example.googleassistant.databinding.ActivityAssistantBinding
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class AssistantActivity() : AppCompatActivity(), Parcelable {

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

    constructor(parcel: Parcel) : this() {
        recognizer = parcel.readParcelable(Intent::class.java.classLoader)!!
        keeper = parcel.readString().toString()
        REQUESTCALL = parcel.readInt()
        SENDSMS = parcel.readInt()
        READSMS = parcel.readInt()
        SHAREAFILE = parcel.readInt()
        SHAREATEXTFILE = parcel.readInt()
        READCONTACTS = parcel.readInt()
        CAPTUREPHOTO = parcel.readInt()
        REQUEST_CODE_SELECT_DOC = parcel.readInt()
        REQUEST_ENABLE_BT = parcel.readInt()
        cameraID = parcel.readString().toString()
        imageIndex = parcel.readInt()
        imgUri = parcel.readParcelable(Uri::class.java.classLoader)!!
    }

    class OpenWeatherMapHelper(string: String) {
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(
            com.example.googleassistant.R.anim.non_movable,
            com.example.googleassistant.R.anim.non_movable
        )

        binding = DataBindingUtil.setContentView(
            this,
            com.example.googleassistant.R.layout.activity_assistant
        )

        val application = requireNotNull(this).application
        val dataSource = AssistantDatabase.getInstance(application).assistantDao
        val viewModelFactory = AssistantViewModelFactory(dataSource, application)

        assistantViewModel =
            ViewModelProvider(this, viewModelFactory).get(AssistantViewModel::class.java)
        val adapter = AssistantAdapter()
        binding.recyclerview.adapter = adapter

        assistantViewModel.message.observe(this) { it?.let { adapter.data = it } }
        binding.lifecycleOwner = this
        //animations
        if (savedInstanceState == null) {
            binding.assistantConstraintLayout.visibility = View.INVISIBLE

            val viewTreeObserver: ViewTreeObserver =
                binding.assistantConstraintLayout.getViewTreeObserver()
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        circularRevealActivity()
                        binding.assistantConstraintLayout.getViewTreeObserver()
                            .removeOnGlobalLayoutListener(this)
                    }
                })
            }

        }
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            cameraID = cameraManager.cameraIdList[1]
            //0 back camera, 1 is front
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        ringtone = RingtoneManager.getRingtone(
            applicationContext,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        helper = OpenWeatherMapHelper(getString(R.string.OPEN_WEATHER_MAP_API_KEY))

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result: Int = textToSpeech.setLanguage(Locale.ENGLISH)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(logtts, "Language Not Supported")
                } else {
                    Log.e(logtts, "Language Supported")
                }
            } else {
                Log.e(logtts, "Initialization Failed")
            }
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
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
                    when {
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
                        //keeper.contains("") -> () future functions
                        //keeper.contains("") -> () future func
                        //keeper.contains("") -> ()future func
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

        })
        binding.assistantActionButton.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    speechRecognizer.stopListening()
                }

                MotionEvent.ACTION_DOWN -> {
                    textToSpeech.stop()
                    speechRecognizer.startListening(recognizerIntent)
                }
            }
            false
        }
        checkIfSpeechRecognizerAvailable()
    }

    private fun circularRevealActivity() {
        TODO("Not yet implemented")
    }

    private fun checkIfSpeechRecognizerAvailable() {
        if(SpeechRecognizer.isRecognitionAvailable(this)){
            Log.d(logsr, "yes")
        }
        else{
            Log.d(logsr, "false")
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(recognizer, flags)
        parcel.writeString(keeper)
        parcel.writeInt(REQUESTCALL)
        parcel.writeInt(SENDSMS)
        parcel.writeInt(READSMS)
        parcel.writeInt(SHAREAFILE)
        parcel.writeInt(SHAREATEXTFILE)
        parcel.writeInt(READCONTACTS)
        parcel.writeInt(CAPTUREPHOTO)
        parcel.writeInt(REQUEST_CODE_SELECT_DOC)
        parcel.writeInt(REQUEST_ENABLE_BT)
        parcel.writeString(cameraID)
        parcel.writeInt(imageIndex)
        parcel.writeParcelable(imgUri, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AssistantActivity> {
        override fun createFromParcel(parcel: Parcel): AssistantActivity {
            return AssistantActivity(parcel)
        }

        override fun newArray(size: Int): Array<AssistantActivity?> {
            return arrayOfNulls(size)
        }
    }
    //  *called functions list* //
    fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        assistantViewModel.sendMessageToDatabase(keeper, text)
    }
    fun getDate(){
        val calendar = Calendar.getInstance()
        val formattedDate = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.time)
        val splitDate = formattedDate.split(",").toTypedArray()
        val date = splitDate[1].trim{ it <= ' ' }
        speak("Today's date is $date")
    }
    @SuppressLint("SimpleDateFormat")
    fun getTime(){
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("HH:mm")
        val time = String = format.format(calendar.getTime())
        speak("the current time is $time")
    }
    private fun makeAPhoneCall(){
        val keeperSplit = keeper.replace(" ".toRegex(), "").split("o").toTypedArray()
        val  number = keeperSplit[2]
        if(number.trim { it <= ' ' }.isNotEmpty()){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUESTCALL)
            }
            else{
                val dial = "tel:$number"
                speak("Calling $number")
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse(dial)))
            }
        }
        else{
            Toast.makeText(this, "Enter Phone Number", Toast.LENGTH_SHORT).show()
        }
    }
    private fun sendSMS(){
        Log.d("keeper", "Done0")
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SENDSMS)
            Log.d("keeper", "Done1")
        }
        else{
            Log.d("keeper", "Done2")
            val keeperReplaced = keeper.replace(" ".toRegex(), "")
            val number = keeperReplaced.split("o").toTypedArray()[1].split("t").toTypedArray()[0]
            val message = keeper.split("that").toTypedArray()[1]
            Log.d("chk", number + message)
            val  mySmsManager = SmsManager.getDefault()
            mySmsManager.sendTextMessage(number.trim { it <= ' ' }, null, message.trim { it <= ' ' }, null, null)
            speak("Message sent that $message")
        }
        speak("Is there anything else you would like me to do?")
    }
    @SuppressLint("NewApi", "Recycle")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun readSMS(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), READSMS)
        }
        else{
            val cursor = contentResolver.query(Uri.parse("content://sms"), null, null, null)
            cursor!!.moveToFirst()
            speak("Your last message was " + cursor.getString(12))
        }
    }
    private fun openMessages(){
        val intent = packageManager.getLaunchIntentForPackage(Telephony.Sms.getDefaultSmsPackage(this))
        intent?.let { startActivity(it) }
        speak("Opening Messages")
    }
    private fun openFacebook(){
        val intent = packageManager.getLaunchIntentForPackage("com.facebook.katana")
        intent?.let { startActivity(it) }
        speak("Opening Facebook")
    }
    private fun openGmail(){
        val intent = packageManager.getLaunchIntentForPackage("com.google.gm")
        intent?.let { startActivity(it) }
        speak("Opening Gmail")
    }
    private fun shareAFile(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), SHAREAFILE)
        }
        else{
            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            val myFileIntent.type = "application/pdf"
            startActivityForResult(myFileIntent, REQUEST_CODE_SELECT_DOC)
        }
        speak("Sharing file")
    }
    private fun shareATextMessage(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), SHAREATEXTFILE)
        }
        else{
            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            val message = keeper.split("that").toTypedArray()[1]
            val intentShare = Intent(Intent.ACTION_SEND)
            intentShare.type = "text/plain"
            intentShare.putExtra(Intent.EXTRA_TEXT, message)
            startActivity(Intent.createChooser(intentShare, "Sharing Text"))
        }
        speak("Sharing text")
    }
    private fun callContact() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS), READCONTACTS)
        }
        else {
            val name = keeper.split("call").toTypedArray()[1].trim{ it <= ' ' }
            Log.d("chk", name)
            try {
                val cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE), "DISPLAY_NAME = '$name'", null, null)
                cursor!!.moveToFirst()
                val number = cursor.getString(0)
                if(number.trim { it <= ' ' }.isNotEmpty()){
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUESTCALL)
                    }
                    else{
                        val dial = "tel:$number"
                        startActivity(Intent(Intent.ACTION_CALL, Uri.parse(dial)))
                    }
                }
                else{
                    Toast.makeText(this, "Enter Phone Number", Toast.LENGTH_SHORT).show()
                }
            }
            catch (e: Exception){
                e.printStackTrace()
                speak("Something went wrong, please try again")
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun turnOnBluetooth(){
        if(!bluetoothAdapter.isEnabled()){
            speak("Turning on bluetooth service")
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, REQUEST_ENABLE_BT)
        }
        else{
            speak("Bluetooth service is already On")
        }
    }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun turnOffBluetooth(){
        if(bluetoothAdapter.isEnabled()){
            bluetoothAdapter.disable()
            speak("Turning off bluetooth service")
        }
        else{
            speak("Bluetooth service is already Off")
        }
    }
    private fun getAllPairedDevices(){
        if(bluetoothAdapter.isEnabled()){
            speak("Paired Bluetooth Devices are ")
            var text = ""
            var count = 1
            val devices: Set<BluetoothDevice> = bluetoothAdapter.getBondedDevices()
            for(device in devices){
                text += "\nDevice: $count ${device.name}, $device"
                count += 1
            }
            speak(text)
        }
        else{
            speak("Please turn on bluetooth to get paired devices")
        }
    }
    private fun turnOnFlash(){
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                cameraManager.setTorchMode(cameraID, true)
                speak("Flash turned on")
            }
        }
        catch (e: java.lang.Exception){
            e.printStackTrace()
            speak("Error Occurred")
        }
    }
    private fun turnOffFlash(){
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                cameraManager.setTorchMode(cameraID, false)
                speak("Flash turned off")
            }
        }
        catch (e: java.lang.Exception){
            e.printStackTrace()
            speak("Error Occurred")
        }
    }
    fun clipBoardCopy(){
        val data = keeper.split("that").toTypedArray()[1].trim{ it <= ' ' }
        if(!data.isEmpty()){
            val clipData = ClipData.newPlainText("text", data)
            clipboardManager.setPrimaryClip(clipData)
            speak("Data copied to clipboard that is $data")
        }
    }
}





