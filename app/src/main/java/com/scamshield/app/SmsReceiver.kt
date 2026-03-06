package com.scamshield.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {

            val bundle: Bundle? = intent.extras
            bundle?.let {

                val pdus = it.get("pdus") as Array<*>
                val format = it.getString("format")

                for (pdu in pdus) {

                    val sms = SmsMessage.createFromPdu(
                        pdu as ByteArray,
                        format
                    )

                    val messageBody = sms.messageBody

                    val detector = ScamDetector()
                    val result = detector.analyze(messageBody)

                    // You can log or notify here
                }
            }
        }
    }
}
