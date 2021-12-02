package com.example.demo

import WebsocketClient
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.coinbase.wallet.crypto.extensions.encryptUsingAES256GCM
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.Exception
import java.util.concurrent.TimeUnit
import PersonalDataDialogFragment
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.coinbase.wallet.http.connectivity.Internet
import com.coinbase.walletlink.WalletLink
import com.example.dapp.SendTransactionDialog
import com.example.dapp.SignTypedDataDialog
import java.net.URL

class DAPPActivity : AppCompatActivity() , PersonalDataDialogFragment.LoginInputListener {
    private var builderForCustom: CustomDialog.Builder? = null
    private var mDialog: CustomDialog? = null
    private val sessionIDLength = 32
    private val sessionKeyLength = 64
    private val alphabet: CharRange = ('0'..'9')
    private var sessionID: String = ""
    private var secret: String = ""
    private val metadata = "0x03F6f282373900C2F6CE53B5A9f595b92aC5f5E5"
    private val serialScheduler = Schedulers.single()
    private val disposeBag = CompositeDisposable()
    private var signTypedDataDialogBuiler: SignTypedDataDialog.Builder? = null
    private var signTypedDataDialog: SignTypedDataDialog? = null
    private var sendTransactionbuilder: SendTransactionDialog.Builder? = null
    private var sendTransactionDialog: SendTransactionDialog? = null
    private lateinit var walletLink : WalletLink
    private val notificationUrl = URL("https://walletlink.herokuapp.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initiateCredentials()
        initWalletLink()
    }

    private fun initiateCredentials(){
        sessionID = List(sessionIDLength) { alphabet.random() }.joinToString("")
        secret = List(sessionKeyLength) { alphabet.random() }.joinToString("")
        println("Session ID:$sessionID")
        println("secret: $secret")
    }

    private fun initWalletLink() {
        walletLink = WalletLink(
            notificationUrl = notificationUrl,
            context = this
        )
        println("Wallet Link has been initialized!!")
    }

    private fun barcodeFormatCode(content: String): Bitmap {
        val barcode = BarcodeFormat.QR_CODE
        val matrix = MultiFormatWriter().encode(content, barcode, 1000, 1000, null)
        return matrix2Bitmap(matrix)
    }

    private fun matrix2Bitmap(matrix: BitMatrix): Bitmap {
        val w = matrix.width
        val h = matrix.height
        val rawData = IntArray(w * h)

        for (i in 0 until w) {
            for (j in 0 until h) {
                var color = Color.WHITE
                if (matrix.get(i, j)) {
                    color = Color.BLACK
                }
                rawData[i + (j * w)] = color
            }
        }

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(rawData, 0, w, 0, 0, w, h)
        return bitmap
    }

    private fun showSingleButtonDialog(link:String, onClickListener:View.OnClickListener) {
        mDialog = sessionID.let {
            builderForCustom!!
                .setSingleButton("Cancel", onClickListener).setImage(barcodeFormatCode(link)).createSingleButtonDialog()
        }
        mDialog!!.show()
    }

    private fun encryptData(data: String, secret: String) : String {
        return data.encryptUsingAES256GCM(secret)
    }

    fun sendSignPersonal(view: android.view.View) {
        val fd = PersonalDataDialogFragment()
        fd.show(supportFragmentManager,"")
    }

    fun startConnection(view: android.view.View) {
        walletLink.sendHostSessionRequest(sessionID, secret)
        val url = "www.walletlink.org"
        //var url = "https://www.walletlink.org/#/link?id=5437df9bc6933c09b79afe87683903fc&secret=94cbe052ecbe21da0cb76cf3dba88a87ebfae578625fbb53bf0d7110eea449e4&server=https%3A%2F%2Fwww.walletlink.org&v=1"
        val userId = "1"
        val deeplink = "https://${url}?userId=${userId}&secret=${secret}&sessionId=${sessionID}&metadata=${metadata}"
        //val deeplink = "https://www.walletlink.org/#/link?id=$sessionID&secret=$secret&server=https%3A%2F%2Fwww.walletlink.org&v=1"
        builderForCustom = CustomDialog.Builder(this)
        showSingleButtonDialog(deeplink) {
            mDialog!!.dismiss()
        }
        GlobalScope.launch {
            while(true) {
                sessionID.let { WebsocketClient.sendHeartBeatMessage()}
                TimeUnit.SECONDS.sleep(10L)
            }
        }
    }

    fun startTransaction(view: android.view.View) {
        val jsonString = "{" + "\"type\": \"WEB3_REQUEST\"," +
                "\"id\": \"13a09f7199d388e9\"," +
                "\"request\": {" + "\"method\": \"submitEthereumTransaction\"," +
                "\"params\": {" + "\"signedTransaction\": \"111222333444555\"," +
                "\"chainId\": 8888" + "}" + "}," + "\"origin\": \"https://www.usfca.edu\"" + "}"
        val data = secret.let { encryptData(jsonString, it) }
        println("The encrypted Data is: $data")
        sessionID.let {
            WebsocketClient.sendPublishEventMessage(it,data)
        }
    }

    fun cancelRequest(view: android.view.View) {
        val jsonString = "{" +
                "\"type\": \"WEB3_REQUEST\"," + "\"id\": \"13a09f7199d388e9\"," +
                "\"request\": {" + "\"method\": \"requestCanceled\"" + "}," +
                "\"origin\": \"https://www.usfca.edu\"" + "}"
        val data = secret.let { encryptData(jsonString, it) }
        println("The encrypted Data is: $data")
        sessionID.let {
            WebsocketClient.sendPublishEventMessage(it,data)
        }
    }

    override fun onLoginInputComplete(input: String) {
        val jsonString = "{" +
                "\"type\": \"WEB3_REQUEST\"," + "\"id\": \"13a09f7199d388e9\"," +
                "\"request\": {" + "\"method\": \"signEthereumMessage\"," +
                "\"params\": {" + "\"message\": \"$input\"," +
                "\"address\": \"https://app.compound.finance/images/compound-192.png\"," +
                "\"addPrefix\": false," + "\"typedDataJson\": \"ZiyangLiu\"" +
                "}" + "}," + "\"origin\": \" https ://app.compound.finance\"" + "}"
        val data = encryptData(jsonString, secret)
        sessionID.let {
            WebsocketClient.sendPublishEventMessage(it,data)
        }
    }

    fun showSignTypedDataDialog(view: android.view.View) {
        signTypedDataDialogBuiler = SignTypedDataDialog.Builder(this)
        sendSignTypedDataDialog{
            signTypedDataDialog!!.dismiss()
        }
    }

    private fun sendSignTypedDataDialog(onClickListener:View.OnClickListener) {
        signTypedDataDialog = sessionID.let {
            secret.let { it1 ->
                signTypedDataDialogBuiler!!
                    .setCloseButton(onClickListener)
                    .setSession(it).setSecret(it1)
                    .buildDialog()
            }
        }
        signTypedDataDialog!!.show()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun showSendTransactionDialog(view: android.view.View) {
        sendTransactionbuilder = SendTransactionDialog.Builder(this)
        sendTransactionDialog(){
            sendTransactionDialog!!.dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun sendTransactionDialog(onClickListener:View.OnClickListener) {
        sendTransactionDialog = sendTransactionbuilder!!
            .setCloseButton(onClickListener)
            .createDialog()
        sendTransactionDialog!!.show()
    }
}