package com.wanzi.wechatrecord.util

import android.content.Context
import android.util.Log

/**
 * Created by WZ on 2018-01-30.
 */
object LogUtils {

    private val TAG = "wanzi"

    fun i(context: Context, msg: String) {
        Log.i(TAG, "${context.javaClass.name} -> $msg")
    }
}