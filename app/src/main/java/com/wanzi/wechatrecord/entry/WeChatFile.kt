package com.wanzi.wechatrecord.entry

import org.litepal.crud.DataSupport

/**
 * Created by WZ on 2018-02-07.
 */
class WeChatFile : DataSupport() {
    var msgSvrId = ""
    var type = ""
    var path = ""
    var name = ""
    var date = 0L
}