package com.wanzi.wechatrecord.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

/**
 * Created by WZ on 2018-01-26.
 */
object FileUtils {

    val PATH_ROOT = "/WeChatRecord"

    val FILE_LOG = "log.txt"

    private var list = ArrayList<File>()

    private fun createLogFile() {
        val sd = Environment.getExternalStorageDirectory()
        val root = File(sd, PATH_ROOT)
        if (!root.exists()) {
            root.mkdir()
        }
        val log = File(root, FILE_LOG)
        if (!log.exists()) {
            log.createNewFile()
        }
    }

    fun writeLog(context: Context, log: String) {
        createLogFile()
        try {
            val fos = FileOutputStream(File("${Environment.getExternalStorageDirectory()}$PATH_ROOT/$FILE_LOG"), true)
            fos.write("${TimeUtils.timeFormat(Date().time, TimeUtils.TIME_STYLE)}\n".toByteArray())
            fos.write(log.toByteArray())
            fos.close()
        } catch (e: Exception) {
            LogUtils.i(context, "写入日志失败：${e.message}")
        }
    }

    fun searchFile(file: File, fileName: String): List<File> {
        list.clear()
        recursiveSearchFile(file, fileName)
        return list
    }

    /**
     * 递归查询指定目录下的指定文件名的文件列表
     *
     * @param file     目录
     * @param fileName 需要查找的文件名称
     */
    private fun recursiveSearchFile(file: File, fileName: String) {
        if (file.isDirectory) {
            val files = file.listFiles()
            if (files != null) {
                for (childFile in files) {
                    recursiveSearchFile(childFile, fileName)
                }
            }
        } else {
            if (fileName == file.name) {
                list.add(file)
            }
        }
    }

    /**
     * 复制单个文件
     *
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    fun copyFile(oldPath: String, newPath: String) {
        try {
            val oldFile = File(oldPath)
            if (oldFile.exists()) { //文件存在时
                val inStream = FileInputStream(oldPath) //读入原文件
                val fs = FileOutputStream(newPath)
                val buffer = ByteArray(1024)
                var byteRead = inStream.read(buffer)
                while (byteRead != -1) {
                    fs.write(buffer, 0, byteRead)
                    byteRead = inStream.read(buffer)
                }
                inStream.close()
            }
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    /**
     * 打开文件
     */
    fun openFile(context: Context, path: String) {
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "文件不存在！", Toast.LENGTH_SHORT).show()
            return
        }
        val end = file.name.substring(file.name.lastIndexOf(".") + 1, file.name.length).toLowerCase(Locale.getDefault())
        LogUtils.i(context, "end:$end")
        val intent: Intent =
                when (end) {
                    ".mp4" -> {
                        getVideoFileIntent(file)
                    }
                    ".jpg" -> {
                        getImageFileIntent(file)
                    }
                    else -> {
                        getAudioFileIntent(file)
                    }
                }
        context.startActivity(intent)
    }

    /**
     * 图片
     */
    private fun getImageFileIntent(file: File): Intent {
        val intent = Intent("android.intent.action.VIEW")
        intent.addCategory("android.intent.category.DEFAULT")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri = Uri.fromFile(file)
        intent.setDataAndType(uri, "image/*")
        return intent
    }

    /**
     * 视频
     */
    private fun getVideoFileIntent(file: File): Intent {
        val intent = Intent("android.intent.action.VIEW")
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("oneshot", 0)
        intent.putExtra("configchange", 0)
        val uri = Uri.fromFile(file)
        intent.setDataAndType(uri, "video/*")
        return intent
    }

    /**
     * 音频
     */
    private fun getAudioFileIntent(file: File): Intent {
        val intent = Intent("android.intent.action.VIEW")
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("oneshot", 0)
        intent.putExtra("configchange", 0)
        val uri = Uri.fromFile(file)
        intent.setDataAndType(uri, "audio/*")
        return intent
    }
}