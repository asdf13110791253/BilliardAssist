package com.lingmiao.v2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.lingmiao.v2.R

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.startForegroundService(intent)
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
        startForeground(1002, createNotification())
    }

    private fun createOverlay() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.control_panel, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        windowManager.addView(floatingView, params)

        // 1. 整体拖拽逻辑 (按住面板任意位置可以移动)
        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    params.x = event.rawX.toInt() - 100
                    params.y = event.rawY.toInt() - 100
                    windowManager.updateViewLayout(floatingView, params)
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = event.rawX.toInt() - 100
                    params.y = event.rawY.toInt() - 100
                    windowManager.updateViewLayout(floatingView, params)
                }
            }
            true
        }

        // 2. 顶部工具栏点击折叠/展开
        floatingView?.findViewById<TextView>(R.id.btn_toggle_toolbar)?.setOnClickListener {
            val topBar = floatingView?.findViewById<View>(R.id.top_toolbar)
            if (topBar?.visibility == View.VISIBLE) {
                topBar.visibility = View.GONE
                it.text = "▶"
            } else {
                topBar?.visibility = View.VISIBLE
                it.text = "◀"
            }
        }

        // 3. 右下角拖拽缩放面板大小 (按住右下角 ◢ 拉大拉小)
        var startX = 0f
        var startY = 0f
        var startWidth = 0
        var startHeight = 0
        floatingView?.findViewById<TextView>(R.id.resize_handle)?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    startWidth = floatingView?.width ?: 300
                    startHeight = floatingView?.height ?: 500
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startX).toInt()
                    val dy = (event.rawY - startY).toInt()
                    params.width = startWidth + dx
                    params.height = startHeight + dy
                    windowManager.updateViewLayout(floatingView, params)
                }
            }
            true
        }
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    "overlay_channel",
                    "台球悬浮窗",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        return NotificationCompat.Builder(this, "overlay_channel")
            .setContentTitle("台球辅助")
            .setContentText("悬浮窗正在运行")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
        floatingView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
