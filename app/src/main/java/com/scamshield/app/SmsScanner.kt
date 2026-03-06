package com.scamshield.app

import android.content.Context
import android.provider.Telephony

data class SmsMessageData(
    val address: String,
    val body: String,
    val date: Long
)

class SmsScanner(private val context: Context) {

    fun getAllSms(): List<SmsMessageData> {
        val smsList = mutableListOf<SmsMessageData>()

        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            null,
            null,
            null,
            Telephony.Sms.DATE + " DESC"
        )

        cursor?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                val address = it.getString(addressIndex)
                val body = it.getString(bodyIndex)
                val date = it.getLong(dateIndex)

                smsList.add(SmsMessageData(address, body, date))
            }
        }

        return smsList
    }
}
