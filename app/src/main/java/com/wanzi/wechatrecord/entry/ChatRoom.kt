package com.wanzi.wechatrecord.entry

import org.litepal.crud.DataSupport

/**
 * Created by WZ on 2018-02-27.
 */
class ChatRoom : DataSupport() {
    var name = ""                // 微信群名，类似id
    var memberList = ""          // 会员微信号列表
    var roomOwner = ""           // 群主
    var selfDisplayName = ""     // 我在本群昵称
    var modifyTime = 0L          // 修改时间
    var isModify = 0             // 是否修改 0：修改 1：无修改
}