package aykuttasil.com.playgroundsmsretriever

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.HintRequest
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.ConnectionResult
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast
import com.google.android.gms.auth.api.phone.SmsRetriever


class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, MySMSBroadcastReceiver.OTPReceiveListener {

    private val PLAY_SERVICES_RESOLUTION_REQUEST: Int = 1200
    private val RESOLVE_HINT = 100

    private val smsBroadcast = MySMSBroadcastReceiver()

    private lateinit var apiClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Play service kullanılabilir mi?
        if (checkPlayServices()) {
            apiClient = GoogleApiClient.Builder(this)
                    .addApi(Auth.CREDENTIALS_API)
                    .enableAutoManage(this, this)
                    .build()

            // SMS gönderilecek telefon numarasını hızlı ve kolay bir şekilde belirlemek için mevcut play service e tanımlı hesabın
            // telefon numarasını dialog şeklinde gösteren hizmeti kullanıyoruz
            requestHint()

            // SMS receiver kayıt ediliyor
            registerSMSReceiver()

            // SmsRetriever servisi başlatılıyor.
            // 5 dakika boyunca sms dinlemesi yapılacak
            startSMSListener()
        }

        /*
        btnCrash.setOnClickListener({
            FirebaseCrash.logcat(Log.ERROR, "TAG", "crash me clicked")
            FirebaseCrash.report(Exception("im crash"))
        })
        */
    }

    /**
     * SMS receiver kayıt ediliyor
     */
    private fun registerSMSReceiver() {
        smsBroadcast.initOTPListener(this)
        val intentFilter = IntentFilter()
        intentFilter.addAction(SmsRetriever.SMS_RETRIEVED_ACTION)
        applicationContext.registerReceiver(smsBroadcast, intentFilter)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.i("aaa", connectionResult.errorMessage ?: "error")
    }


    private fun checkPlayServices(): Boolean {
        val googleAPI = GoogleApiAvailability.getInstance()
        val result = googleAPI.isGooglePlayServicesAvailable(this)
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show()
            }

            return false
        }
        return true
    }

    // Construct a request for phone numbers and show the picker
    private fun requestHint() {
        val hintRequest = HintRequest.Builder()
                .setPhoneNumberIdentifierSupported(true)
                .build()

        val intent = Auth.CredentialsApi.getHintPickerIntent(apiClient, hintRequest)
        startIntentSenderForResult(intent.intentSender, RESOLVE_HINT, null, 0, 0, 0)
    }

    /**
     * SMS geldiğinde buraya düşer
     */
    override fun onOTPReceived(otp: String?) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(smsBroadcast)

        Toast.makeText(this, otp, Toast.LENGTH_SHORT).show()
        Log.e("aaa", otp)
    }

    /**
     * 5 dakika boyunca sms gelmez ise buraya düşer
     */
    override fun onOTPTimeOut() {
        Log.e("aaa", "SMS retriever API Timeout")
        Toast.makeText(this, " SMS retriever API Timeout", Toast.LENGTH_SHORT).show()
    }


    /**
     * SmsRetriever aktifleştirilir
     */
    private fun startSMSListener() {
        val client = SmsRetriever.getClient(this)
        val task = client.startSmsRetriever()
        task.addOnSuccessListener {
            // Successfully started retriever, expect broadcast intent
            Toast.makeText(this, "SMS Retriever starts", Toast.LENGTH_LONG).show()
        }

        task.addOnFailureListener {
            Toast.makeText(this, "Error", Toast.LENGTH_LONG).show()
        }
    }


    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        /**
         * Dialog dan telefon numarası seçildiğinde buraya düşer
         */
        if (requestCode == RESOLVE_HINT) {
            if (resultCode == Activity.RESULT_OK) {
                val credential = data.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
                val phoneNumber = credential.id  //<-- will need to process phone number string
                Log.i("aaa", phoneNumber)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // SMS receiver kaydı iptal ediliyor
        applicationContext.unregisterReceiver(smsBroadcast)

        // Google Api Client bağlantısı kesiliyor
        if (apiClient.isConnected) {
            apiClient.stopAutoManage(this)
            apiClient.disconnect()
        }
    }
}
