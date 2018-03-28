package com.wanzi.wechatrecord.entry

import java.io.Serializable

/**
 * Created by WZ on 2018-01-29.
 */

data class UserInfo(
        val username: String,  // 账号
        val nickname: String   // 昵称
) : Serializable
