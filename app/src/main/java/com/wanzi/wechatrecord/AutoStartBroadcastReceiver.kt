package com.wanzi.wechatrecord

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AutoStartBroadcastReceiver : BroadcastReceiver() {

    val action_boot = "android.intent.action.BOOT_COMPLETED"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(action_boot)) {
            val startIntent = Intent(context, MainAc::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(startIntent)
        }
    }
}
