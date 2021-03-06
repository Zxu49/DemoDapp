package com.example.demo

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import com.coinbase.wallet.core.util.JSON
import com.coinbase.wallet.crypto.extensions.encryptUsingAES256GCM
import com.coinbase.walletlink.WalletLink
import com.coinbase.walletlink.dtos.JsonRPCRequestTypedDataDTO
import com.coinbase.walletlink.dtos.SignEthereumTransactionParamsRPC
import com.coinbase.walletlink.dtos.Web3RequestTypedData
import com.coinbase.walletlink.models.RequestMethod
import com.example.demo.utils.getTextInput

/**
 * interface that will listen to the actions of  SignTypedData dialog and 
 * close the SignTypedData dialog accordingly
 */
interface SignTypedDataListener {
    fun closeSTD()
}

/**
 * Dialog that allows the users to send Sign Typed Data to the wallet
 */
class SignTypedDataDialog(context: Context) : Dialog(context) {

    /**
     * Builder of the SignTypedData Dialog, it will set up relevant parameters and listeners
     */
    @SuppressLint("InflateParams")
    class Builder(context: Context) {

        private var sessionID: String ? = null
        private var secret: String ? = null
        private var contentView: View? = null
        private var closeButtonClickListener: View.OnClickListener? = null
        private var sendButtonClickListener: View.OnClickListener? = null
        private var listener: SignTypedDataListener? = null

        private val layout: View
        private companion object val dialog: SignTypedDataDialog = SignTypedDataDialog(context)
        private var walletLink : WalletLink ? = null

        private val TAG = "SignTypedDataDialog Builder"

        /**
         * Initialization function of the SignTypedData Dialog Builder
         */
        init {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            inflater.inflate(R.layout.send_typed_data_view, null).also { layout = it }
            dialog.addContentView(layout, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        /**
         * Set the OnClickListener of Close Button when users click it
         *
         * @params listener - OnClickListener that will close the dialog when users click it
         *
         * @return Builder - Builder of the SignTypedData dialog
         */
        fun setCloseButton(listener: View.OnClickListener): Builder {
            this.closeButtonClickListener = listener
            return this
        }

        /**
         * Set the OnClickListener of Send Button when users click it
         *
         * @params listener - OnClickListener that will Send the greeting message to the smart contract
         *  when users click it
         *
         * @return Builder - Builder of the SignTypedData dialog
         */
        fun setSendButton(listener: View.OnClickListener): Builder {
            this.sendButtonClickListener = listener
            return this
        }

        /**
         * Set the session ID of SignTypedData Dialog
         *
         * @params sessionID - String of session ID
         *
         * @return Builder - Builder of the SignTypedData dialog
         */
        fun setSession(sessionID: String?): Builder {
            this.sessionID = sessionID
            return this
        }

        /**
         * Set the secret of SignTypedData Dialog
         *
         * @params sessionID - String of secret
         *
         * @return Builder - Builder of the SignTypedData dialog
         */
        fun setSecret(secret: String?): Builder {
            this.secret = secret
            return this
        }

        /**
         * Set the relevant configuration of the dialog, attach the listener to the button textviews
         * and create a new SignTypedData dialog from builders
         *
         * @return New SignTypedData dialog
         */
        fun buildDialog(): SignTypedDataDialog {
            setSendButton{
                SignTypedData()
                listener?.closeSTD()
            }
            showSingleButton()
            layout.findViewById<View>(R.id.send_typedData_close).setOnClickListener(closeButtonClickListener)
            layout.findViewById<View>(R.id.send_typedData_button).setOnClickListener(sendButtonClickListener)
            create()
            return dialog
        }

        /**
         * Set the WalletLink instance of SignTypedData Dialog
         *
         * @params w - WalletLink instance
         *
         * @return Builder - Builder of the SignTypedData dialog
         */
        fun setWalletLink(w : WalletLink) : Builder {
            this.walletLink = w
            return this
        }

        /**
         * Set listener of SendTransaction Dialog
         *
         * @params listener - SignTypedDataListener that will close the dialog
         *
         * @return Builder - Builder of the SignTypedData dialog
         */
        fun setListener(listener: SignTypedDataListener): Builder {
            this.listener = listener
            return this
        }

        /**
         * Set properties of SignTypedData Dialog
         */
        private fun create() {
            if (contentView != null) {
                (layout?.findViewById<View>(R.id.content) as LinearLayout).removeAllViews()
                (layout?.findViewById<View>(R.id.content) as LinearLayout)
                    .addView(contentView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
            dialog.setContentView(layout)
            dialog.setCancelable(true)
            dialog.setCanceledOnTouchOutside(false)
        }

        /**
         * set the visibility of sendTransactionLayout
         */
        private fun showSingleButton() {
            layout.findViewById<View>(R.id.SignTypedDataButtonsLayout).visibility = View.VISIBLE
        }

        /**
         * Get the parameters of SignTypedData from user's input and send it to the wallet
         */
        private fun SignTypedData() {
            val fromAddress: String? = layout?.let {
                getTextInput("SignTyped fromAddress: ",
                    it.findViewById<EditText>(R.id.from_address_input))
            }
            val toAddress: String? = layout?.let {
                getTextInput("SignTyped toAddress: ",
                    it.findViewById<EditText>(R.id.to_address_title))
            }
            val weiValue: String? = layout?.let {
                getTextInput("SignTyped wei: ",
                    it.findViewById<EditText>(R.id.wei_input))
            }
            val dataInput: String? = layout?.let {
                getTextInput("SignTyped data: ",
                    it.findViewById<EditText>(R.id.data_input))
            }
            val nonce: String? = layout?.let {
                getTextInput("SignTyped nonce: ",
                    it.findViewById<EditText>(R.id.nonce_input))
            }
            val gasPrice: String? = layout?.let {
                getTextInput("SignTyped gas Price: ",
                    it.findViewById<EditText>(R.id.gas_price_input))
            }
            val gasLimit: String? = layout?.let {
                getTextInput("SignTyped gas Limit: ",
                    it.findViewById<EditText>(R.id.gas_limit_input))
            }
            val chainId: String? = layout?.let {
                getTextInput("SignTyped chainId: ",
                    it.findViewById<EditText>(R.id.chain_id_input))
            }
            val shouldSubmit: Boolean =
                layout.findViewById<CheckBox>(R.id.should_submit_input).isChecked

            Log.d(TAG, "shouldSubmit: $shouldSubmit")

            sendSignType()
        }

        /**
         * Send Sign Typed Data to the wallet
         */
        fun sendSignType() {
            val id1 = "13a09f7199d39999"
            val fromAddress1 = "0x568d46f6a798cd75a9beb60a8f57879043a69c3b"
            val toAddress1 = "0x996384c2Bd825D0C4aAbA4bA9475Efb717f0f38A"
            val weiValue1 = "100000000000000000"
            val jsonData1 = "greeting"
            val nonce1 = 1
            val gasPriceInWei1 = "0"
            val gasLimit1 = "0"
            val chainId1 = 3
            val shouldSubmit1 = true
            val origin1 = "https://www.usfca.edu"
            val jsonRPC = JsonRPCRequestTypedDataDTO(id = id1, request = Web3RequestTypedData(method = RequestMethod.SignEthereumTransaction, params = SignEthereumTransactionParamsRPC(
                fromAddress1,
                toAddress1,
                weiValue1,
                jsonData1,
                nonce1,
                gasPriceInWei1,
                gasLimit1,
                chainId1,
                shouldSubmit1
            )
            ), origin = origin1)
            println(JSON.toJsonString(SignEthereumTransactionParamsRPC(
                fromAddress1,
                toAddress1,
                weiValue1,
                jsonData1,
                nonce1,
                gasPriceInWei1,
                gasLimit1,
                chainId1,
                shouldSubmit1
            )))
            val data = secret?.let { JSON.toJsonString(jsonRPC).encryptUsingAES256GCM(it)}
            if (data != null) {
                sessionID?.let { walletLink?.sendSignTypedData(data, it) }
                println(data)
            }
        }

    }
}