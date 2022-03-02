package com.kodevko.applicationforcustomer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager

class RemindReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.getSharedPreferences("", 0).edit()
            .putLong("expiration_date", System.currentTimeMillis()).apply()
        sendMessage(context)
    }

    private fun sendMessage(context: Context) {
        try {
            val SENT = "SMS_SENT"

            val sentPI = PendingIntent.getBroadcast(context, 0, Intent(SENT), 0)

            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    context!!.getSharedPreferences("", 0).edit()
                        .putLong("expiration_date", System.currentTimeMillis()).apply()
                }
            }, IntentFilter(SENT))

            val smsMgr = SmsManager.getDefault()
            smsMgr.sendTextMessage(
                context.resources.getStringArray(R.array.service_phone)[4],
                null,
                context.resources.getStringArray(R.array.code_product)[1],
                sentPI,
                null
            )

        } catch (e: Exception) {

        }
    }
}