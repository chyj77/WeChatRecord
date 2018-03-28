package com.wanzi.wechatrecord.entry

import org.litepal.crud.DataSupport
import java.io.Serializable

/**
 * Created by WZ on 2018-01-29.
 */

class Contact : DataSupport(), Serializable {
    var username = ""
    var nickname = ""
    var type = ""
    var conRemark = ""
}
