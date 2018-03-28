package com.wanzi.wechatrecord.util

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

/**
 * Created by WZ on 2018-01-26.
 */
object ShellCommand {

    /**
     * Shell命令
     *
     * @param command 命令语句
     */
    fun shellCommand(command: String): Boolean {
        var process: Process? = null
        var os: DataOutputStream? = null
        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process!!.outputStream)
            os.writeBytes(command + "\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
        } catch (e: Exception) {
            throw Exception(e.message)
        } finally {
            try {
                if (os != null) {
                    os.close()
                }
                process!!.destroy()
            } catch (e: Exception) {
                throw Exception(e.message)
            }
        }
        return true
    }

    /**
     * 判断APP是否拥有Root权限
     *
     * @param pkgCodePath
     * @return
     */
    fun checkRoot(pkgCodePath: String): Boolean {
        var process: Process? = null
        try {
            val cmd = "ls -l " + pkgCodePath
            process = Runtime.getRuntime().exec(cmd)

            val mReader = BufferedReader(InputStreamReader(process!!.inputStream))
            val mRespBuff = StringBuffer()
            val buff = CharArray(1024 * 10)
            var ch = mReader.read(buff)
            while (ch != -1) {
                mRespBuff.append(buff, 0, ch)
                ch = mReader.read(buff)
            }
            mReader.close()

            val regx = "-rwxr[\\s\\S]+"//root后权限的表达式
            return Pattern.matches(regx, mRespBuff.toString())
        } catch (e: Exception) {
            throw Exception(e.message)
        } finally {
            try {
                process!!.destroy()
            } catch (e: Exception) {
                throw Exception(e.message)
            }
        }
    }
}