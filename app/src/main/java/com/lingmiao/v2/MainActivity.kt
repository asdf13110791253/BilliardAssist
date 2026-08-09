package com.lingmiao.v2

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.lingmiao.v2.service.OverlayService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 因为这是一个纯悬浮窗工具，这里不设置任何 UI 界面
        checkAndStartOverlay()
    }

    private fun checkAndStartOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 1. 检查是否已经拥有悬浮窗权限
            if (!Settings.canDrawOverlays(this)) {
                // 如果没有，跳转到系统设置页让用户手动开启
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                // 2. 如果已经开启了，直接启动悬浮窗服务
                OverlayService.start(this)
                // 启动后直接关闭主界面，省电省内存
                finish() 
            }
        } else {
            // Android 6.0 以下不需要动态申请悬浮窗权限
            OverlayService.start(this)
            finish()
        }
    }

    // 用户从手机设置页开了权限返回 App 时，会触发这个方法
    override fun onResume() {
        super.onResume()
        // 如果权限已经打开，立马拉出悬浮窗
        if (Settings.canDrawOverlays(this)) {
            OverlayService.start(this)
            finish()
        }
    }
}
