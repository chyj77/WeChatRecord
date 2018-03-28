package com.wanzi.wechatrecord.util

import org.litepal.LitePal
import org.litepal.LitePalDB

/**
 * Created by WZ on 2018-01-29.
 */
object DBUtils {

    /**
     * 切换数据库
     */
    fun switchDBUser(dbName: String) {
        val db = LitePalDB.fromDefault(dbName)
        LitePal.use(db)
    }
}