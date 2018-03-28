package com.wanzi.wechatrecord

import android.app.Application
import android.content.Context
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import org.litepal.LitePal

/**
 * Created by WZ on 2018-03-27.
 */
class CustomApplication : Application() {

    var refWatcher: RefWatcher? = null

    override fun onCreate() {
        super.onCreate()
        LitePal.initialize(this)

        refWatcher = setupLeakCanary()
    }

    private fun setupLeakCanary(): RefWatcher {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return RefWatcher.DISABLED
        }
        return LeakCanary.install(this)
    }

    companion object {
        fun getRefWatcher(context: Context): RefWatcher {
            val application = context.applicationContext as CustomApplication
            return application.refWatcher!!
        }
    }

}