package com.wanzi.wechatrecord

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.telephony.TelephonyManager
import android.widget.Toast

import org.jsoup.Jsoup
import java.io.File
import android.os.Looper
import com.wanzi.wechatrecord.entry.*
import com.wanzi.wechatrecord.util.*
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteDatabaseHook
import org.litepal.crud.DataSupport
import java.util.*
import kotlin.collections.ArrayList

class CoreService : IntentService("CoreService") {

    private val WX_ROOT_PATH = "/data/data/com.tencent.mm/"                               // 微信根目录
    private val WX_SP_UIN_PATH = "${WX_ROOT_PATH}shared_prefs/auth_info_key_prefs.xml"    // 微信保存uin的目录
    private val WX_DB_DIR_PATH = "${WX_ROOT_PATH}MicroMsg/"                               // 微信保存聊天记录数据库的目录
    private val WX_DB_FILE_NAME = "EnMicroMsg.db"                                         // 微信聊天记录数据库

    private val WX_FILE_PATH = "/storage/emulated/0/Tencent/micromsg/"                    // 微信保存聊天时语音、图片、视频文件的地址

    private val currApkPath = "/data/data/com.dfsc.wechatrecord/"
    private val COPY_WX_DATA_DB = "wx_data.db"

    private var uin = ""
    private var dbPwd = ""                        // 数据库密码
    private lateinit var userInfo: UserInfo       // 用户
    private var uinEnc = ""                       // 加密后的uin

    @SuppressLint("MissingPermission")
    override fun onHandleIntent(intent: Intent?) {

        // 获取数据库密码 数据库密码是IMEI和uin合并后计算MD5值取前7位
        // 获取imei
        val manager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val imei = manager.deviceId
        // 修改微信根目录读写权限
        try {
            ShellCommand.shellCommand("chmod -R 777 $WX_ROOT_PATH")
            // 获取uin
            val doc = Jsoup.parse(File(WX_SP_UIN_PATH), "UTF-8")
            val elements = doc.select("int")
            elements
                    .filter { it.attr("name") == "_auth_uin" }
                    .forEach { uin = it.attr("value") }
            if (uin.isEmpty()) {
                toast("当前没有登录微信，请登录后重试")
                return
            }
            // 获取数据库密码
            dbPwd = MD5.getMD5Str(imei + uin).substring(0, 7)
            log("数据库密码：$dbPwd")
            FileUtils.writeLog(this, "数据库密码：$dbPwd\n")
        } catch (e: Exception) {
            log("破解数据库失败：${e.message}")
            FileUtils.writeLog(this, "破解数据库失败：${e.message}\n")
            toast("破解数据库失败：${e.message}")
        }

        // 获取当前微信登录用户的数据库文件父级文件夹名（MD5("mm"+uin) ）
        uinEnc = MD5.getMD5Str("mm$uin")
        // 递归查询微信本地数据库文件
        val dbDir = File(WX_DB_DIR_PATH + uinEnc)
        val list = FileUtils.searchFile(dbDir, WX_DB_FILE_NAME)
        for (file in list) {
            val copyFilePath = currApkPath + COPY_WX_DATA_DB
            try {
                // 将微信数据库拷贝出来，因为直接连接微信的db，会导致微信崩溃
                FileUtils.copyFile(file.absolutePath, copyFilePath)
                // 打开微信数据库
                openWXDB(File(copyFilePath), dbPwd)
            } catch (e: Exception) {
                log("复制数据库失败：${e.message}")
                FileUtils.writeLog(this, "复制数据库失败：${e.message}\n")
                toast("复制数据库失败：${e.message}")
            }
        }
    }

    private fun openWXDB(file: File, password: String) {
        toast("正在打开微信数据库，请稍候...")
        SQLiteDatabase.loadLibs(this.applicationContext)
        val hook = object : SQLiteDatabaseHook {
            override fun preKey(database: SQLiteDatabase) {}

            override fun postKey(database: SQLiteDatabase) {
                database.rawExecSQL("PRAGMA cipher_migrate;") // 兼容2.0的数据库
            }
        }
        try {
            // 打开数据库连接
            val db = SQLiteDatabase.openOrCreateDatabase(file, password, null, hook)
            openUserInfoTable(db)
            openContactTable(db)
            openMessageTable(db)
            openChatRoomTable(db)
            db.close()
        } catch (e: Exception) {
            log("打开数据库失败：${e.message}")
            FileUtils.writeLog(this, "打开数据库失败：${e.message}\n")
            toast("打开数据库失败：${e.message}")
        }
    }

    // 打开用户信息表
    private fun openUserInfoTable(db: SQLiteDatabase) {
        // 这个数组是保存用户信息，第一次拿到的是账号，第二次是昵称
        val values = ArrayList<String>()
        // 用户信息表
        val cursor = db.rawQuery("select value from userinfo where id = ? or id = ?", arrayOf("2", "4"))
        if (cursor.count > 0) {
            while (cursor.moveToNext()) {
                val value = cursor.getString(cursor.getColumnIndex("value"))
                values.add(value)
            }
        }
        cursor.close()
        // 用户信息
        userInfo = UserInfo(values[0], values[1])
        log("用户信息：$userInfo")
        FileUtils.writeLog(this, "用户信息：$userInfo\n")
        // 切换数据库
        DBUtils.switchDBUser(userInfo.username)
    }

    // 打开联系人表
    private fun openContactTable(db: SQLiteDatabase) {
        // verifyFlag!=0：公众号等类型 type=33：微信功能 type=2：未知 type=4：非好友
        // 一般公众号原始ID开头都是gh_
        // 群ID的结尾是@chatroom
        val cursor = db.rawQuery("select * from rcontact where " +
                "type != ? and " +
                "type != ? and " +
                "type != ? and " +
                "verifyFlag = ? and " +
                "username not like 'gh_%' and " +
                "username not like '%@chatroom' ", arrayOf("2", "33", "4", "0"))
        if (cursor.count > 0) {
            while (cursor.moveToNext()) {
                val username = cursor.getString(cursor.getColumnIndex("username"))
                val nickname = cursor.getString(cursor.getColumnIndex("nickname"))
                val type = cursor.getString(cursor.getColumnIndex("type"))
                val conRemark = cursor.getString(cursor.getColumnIndex("conRemark"))
                // 避免保存重复数据
                val list = DataSupport.where("username = ?", username).find(Contact::class.java)
                if (list.isEmpty()) {
                    val contact = Contact()
                    contact.username = username
                    contact.nickname = nickname
                    contact.type = type
                    contact.conRemark = conRemark
                    contact.save()
                }
            }
        }
        cursor.close()
    }

    // 打开聊天记录表
    private fun openMessageTable(db: SQLiteDatabase) {
        // 一般公众号原始ID开头都是gh_
        val cursor = db.rawQuery("select * from message where talker not like 'gh_%' and msgid > ? ", arrayOf(getLastMsgId(db)))
        if (cursor.count > 0) {
            while (cursor.moveToNext()) {
                val msgSvrId = cursor.getString(cursor.getColumnIndex("msgSvrId"))
                val type = cursor.getString(cursor.getColumnIndex("type"))
                val isSend = cursor.getString(cursor.getColumnIndex("isSend"))
                val createTime = cursor.getLong(cursor.getColumnIndex("createTime"))
                val talker = cursor.getString(cursor.getColumnIndex("talker"))
                var content = cursor.getString(cursor.getColumnIndex("content"))
                if (content == null) content = ""
                var imgPath = cursor.getString(cursor.getColumnIndex("imgPath"))
                if (imgPath == null) imgPath = ""
                // 根据“msgSvrId”来判断聊天记录唯一性
                if (msgSvrId == null) {
                    log("该次记录 msgSvrId 为空，跳过")
                    continue
                }
                val list = DataSupport.where("msgSvrId = ?", msgSvrId).find(Message::class.java)
                if (list.isEmpty()) {
                    val message = Message()
                    message.msgSvrId = msgSvrId
                    message.type = type
                    // 内容不做处理，直接上传
                    message.content = content
                    /*message.content = when (message.type) {
                        "1" -> content
                        "3" -> "[图片]"
                        "34" -> "[语音]"
                        "47" -> "[表情]"
                        "50" -> "[语音/视频通话]"
                        "43" -> "[小视频]"
                        "49" -> "[分享]"
                        "48" -> content          // 位置信息
                        "10000" -> content       // 系统提示信息
                        else -> content          // 其他信息，包含红包、转账等
                    }*/
                    message.isSend = isSend
                    message.createTime = TimeUtils.timeFormat(createTime, TimeUtils.TIME_STYLE)
                    message.talker = talker
                    // 保存图片、语音、小视频文件信息
                    if (type == "3" || type == "34" || type == "43") {
                        val weChatFile = WeChatFile()
                        weChatFile.msgSvrId = msgSvrId
                        weChatFile.type = type
                        weChatFile.date = Date().time
                        when (type) {
                            "3" -> {
                                // 图片文件需要根据msgSvrId在ImgInfo2表中查找
                                val imgInfoCu = db.rawQuery("select bigImgPath from ImgInfo2 where msgSvrId = ? ", arrayOf(msgSvrId))
                                if (imgInfoCu.count > 0) {
                                    while (imgInfoCu.moveToNext()) {
                                        val bigImgPath = imgInfoCu.getString(imgInfoCu.getColumnIndex("bigImgPath"))
                                        weChatFile.name = bigImgPath
                                    }
                                }
                                imgInfoCu.close()
                                weChatFile.path = WX_FILE_PATH + uinEnc + "/image2/" + weChatFile.name.substring(0, 2) + "/" + weChatFile.name.substring(2, 4) + "/" + weChatFile.name
                                // 接收的图片在ImgInfo2表中会有两种bigImgPath，一种是原图，一种是缓存图，缓存图的格式是文件.temp.jpg
                                // 如果有原图，则上传原图，否则上传缓存图
                                if (weChatFile.path.contains(".temp")) {
                                    val originalImgPath = weChatFile.path.replace(".temp", "")
                                    if (File(originalImgPath).exists()) {
                                        weChatFile.name = weChatFile.name.replace(".temp", "")
                                        weChatFile.path = originalImgPath
                                    }
                                }
                                // 过滤一些不是jpg的文件
                                if (weChatFile.name.endsWith(".jpg")) {
                                    message.imgPath = weChatFile.name
                                    weChatFile.save()
                                }
                            }
                            "34" -> {
                                weChatFile.name = "msg_$imgPath.amr"
                                val nameEnc = MD5.getMD5Str(imgPath)
                                weChatFile.path = WX_FILE_PATH + uinEnc + "/voice2/" + nameEnc.substring(0, 2) + "/" + nameEnc.substring(2, 4) + "/" + weChatFile.name
                                message.imgPath = weChatFile.name
                                weChatFile.save()
                            }
                            "43" -> {
                                weChatFile.name = "$imgPath.mp4"
                                weChatFile.path = WX_FILE_PATH + uinEnc + "/video/" + weChatFile.name
                                message.imgPath = weChatFile.name
                                weChatFile.save()
                            }
                        }
                    }
                    message.save()
                }
            }
        }
        cursor.close()
    }

    // 获取最后一条消息ID
    private fun getLastMsgId(db: SQLiteDatabase): String {
        // 查询本地数据库中的最后一条
        var lastMsgId = "0"
        val last = DataSupport.findLast(Message::class.java)
        if (last != null) {
            log("本地数据库中存在最后一条记录，msgSvrid：${last.msgSvrId}")
            val msgCu = db.rawQuery(" select * from message where msgsvrid = ? ", arrayOf(last.msgSvrId))
            if (msgCu.count > 0) {
                while (msgCu.moveToNext()) {
                    lastMsgId = msgCu.getString(msgCu.getColumnIndex("msgId"))
                    log("微信数据库中存在 msgSvrid 为：${last.msgSvrId} 的记录，msgid 为：$lastMsgId")
                }
            }
            msgCu.close()
        }
        log("聊天记录从 msgid 为：$lastMsgId 处开始查询")
        return lastMsgId
    }

    // 打开微信群表
    private fun openChatRoomTable(db: SQLiteDatabase) {
        val cursor = db.rawQuery("select * from chatroom ", arrayOf())
        if (cursor.count > 0) {
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndex("chatroomname"))
                val memberList = cursor.getString(cursor.getColumnIndex("memberlist"))
                val roomOwner = cursor.getString(cursor.getColumnIndex("roomowner"))
                var selfDisplayName = cursor.getString(cursor.getColumnIndex("selfDisplayName"))
                val modifyTime = cursor.getLong(cursor.getColumnIndex("modifytime"))
                if (selfDisplayName == null) {
                    selfDisplayName = ""
                }
                val list = DataSupport.where("name = ?", name).find(ChatRoom::class.java)
                if (list.isEmpty()) {
                    // 新建群信息
                    val chatRoom = ChatRoom()
                    chatRoom.name = name
                    chatRoom.memberList = memberList
                    chatRoom.roomOwner = roomOwner
                    chatRoom.selfDisplayName = selfDisplayName
                    chatRoom.modifyTime = modifyTime
                    chatRoom.save()
                } else {
                    // 修改群信息
                    val first = list[0]
                    if (first.modifyTime != modifyTime) {
                        first.memberList = memberList
                        first.roomOwner = roomOwner
                        first.selfDisplayName = selfDisplayName
                        first.modifyTime = modifyTime
                        first.isModify = 0
                        first.save()
                    }
                }
            }
        }
        cursor.close()
    }

    private fun IntentService.toast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        val handler = Handler(Looper.getMainLooper())
        handler.post({
            Toast.makeText(this, text, duration).show()
        })
    }

    private fun log(msg: String) {
        LogUtils.i(this@CoreService, msg)
    }

}
