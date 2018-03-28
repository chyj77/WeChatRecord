package com.wanzi.wechatrecord.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by WZ on 2018-01-30.
 */
object TimeUtils {

    val TIME_STYLE = "yyyy-MM-dd HH:mm:ss"

    /**
     * 时间转换 将毫秒转换成日期
     *
     * @param millisecond
     * @param style
     * @return
     */
    fun timeFormat(millisecond: Long, style: String): String {
        val simpleDateFormat = SimpleDateFormat(style)
        val date = Date(millisecond)
        return simpleDateFormat.format(date)
    }
}