package com.wanzi.wechatrecord

import android.Manifest
import android.app.Activity
import android.content.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.wanzi.wechatrecord.util.ShellCommand
import android.databinding.DataBindingUtil
import android.view.KeyEvent
import android.widget.Toast
import com.wanzi.wechatrecord.databinding.AcMainBinding
import com.wanzi.wechatrecord.util.LogUtils
import com.tbruyelle.rxpermissions2.RxPermissions

class MainAc : AppCompatActivity() {

    private lateinit var binding: AcMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.ac_main)

        // 检查权限
        val rxPermissions = RxPermissions(this)
        rxPermissions
                .request(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECEIVE_BOOT_COMPLETED
                )
                .subscribe {
                    if (!it) {
                        toast("请打开相关权限")
                        // 如果权限申请失败，则退出
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                }

        binding.btn.setOnClickListener {
            checkRoot()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        CustomApplication.getRefWatcher(this).watch(this)
    }

    private fun checkRoot() {
        try {
            log("准备检测Root权限")
            // 检测是否拥有Root权限
            if (!ShellCommand.checkRoot(packageCodePath)) {
                log("检测到未拥有Root权限")
                // 申请Root权限（弹出申请root权限框）
                val rootCommand = "chmod 777 $packageCodePath"
                ShellCommand.shellCommand(rootCommand)

                MaterialDialog.Builder(this)
                        .title("提示")
                        .content("请授予本APP Root权限")
                        .positiveText("确定")
                        .onPositive { _, _ ->
                            goMIUIPermission()
                        }
                        .show()
            } else {
                startService(Intent(this, CoreService::class.java))
            }
        } catch (e: Exception) {
            toast("检查Root权限失败：${e.message!!}")
        }
    }

    /**
     * 跳转到MIUI的权限管理页面
     */
    private fun goMIUIPermission() {
        val i = Intent("miui.intent.action.APP_PERM_EDITOR")
        val componentName = ComponentName("com.miui.securitycenter", "com.miui.permcenter.MainAcitivty")
        i.component = componentName
        i.putExtra("extra_pkgname", packageName)
        try {
            startActivity(i)
        } catch (e: Exception) {
            toast("跳转权限管理页面失败：${e.message!!}")
        }
    }

    private fun Activity.toast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, text, duration).show()
    }

    fun log(msg: String) {
        LogUtils.i(this, msg)
    }

    /**
     * 返回键只返回桌面
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
