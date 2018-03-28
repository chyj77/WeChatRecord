package com.wanzi.wechatrecord.util

import java.security.MessageDigest

/**
 * Created by WZ on 2018-01-26.
 */
object MD5 {


    fun getMD5Str(str: String): String {
        var messageDigest: MessageDigest? = null
        try {
            messageDigest = MessageDigest.getInstance("MD5")
            messageDigest!!.reset()
            messageDigest.update(str.toByteArray(charset("UTF-8")))
        } catch (e: Exception) {

        }
        val byteArray = messageDigest!!.digest()
        val md5StrBuff = StringBuffer()
        for (i in byteArray.indices) {
            if (Integer.toHexString(0xFF and byteArray[i].toInt()).length == 1)
                md5StrBuff.append("0").append(
                        Integer.toHexString(0xFF and byteArray[i].toInt()))
            else
                md5StrBuff.append(Integer.toHexString(0xFF and byteArray[i].toInt()))
        }
        return md5StrBuff.toString()
    }
}