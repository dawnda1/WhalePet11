package com.example.whalepet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.random.Random

class PetService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var rootView: FrameLayout
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var pet: TextView
    private lateinit var bubble: TextView

    private val handler = Handler(Looper.getMainLooper())

    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var isDragging = false

    private val lines = listOf(
        "哼，主人别戳人家尾巴啦！",
        "才、才没有等你呢。",
        "喂我米饭，就给你摸一下。",
        "今天也要好好赚 token 哦。",
        "呜……桌面上好冷，抱抱。",
        "主人再点，人家就夹给你听。"
    )

    private val swimRunnable = object : Runnable {
        override fun run() {
            if (!isDragging && ::params.isInitialized) {
                val dx = Random.nextInt(-3, 4)
                val dy = Random.nextInt(-2, 3)
                params.x = (params.x + dx).coerceAtLeast(0)
                params.y = (params.y + dy).coerceAtLeast(0)
                wm.updateViewLayout(rootView, params)
            }
            handler.postDelayed(this, 1200)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        createPetView()

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 120
            y = 500
        }

        setupTouch()
        wm.addView(rootView, params)
        handler.post(swimRunnable)
    }

    private fun createPetView() {
        rootView = FrameLayout(this)

        pet = TextView(this).apply {
            text = "🐋"
            textSize = 64f
            gravity = Gravity.CENTER
            setPadding(20, 20, 20, 20)
        }

        bubble = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            setPadding(24, 12, 24, 12)
            background = GradientDrawable().apply {
                cornerRadius = 40f
                setColor(Color.parseColor("#F2FFFFFF"))
                setStroke(2, Color.parseColor("#88FFFFFF"))
            }
            visibility = View.GONE
        }

        val bubbleParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = -20
        }

        val petParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            topMargin = 60
        }

        rootView.addView(bubble, bubbleParams)
        rootView.addView(pet, petParams)
    }

    private fun setupTouch() {
        pet.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    isDragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY

                    if (abs(dx) > 12 || abs(dy) > 12) {
                        isDragging = true
                    }

                    if (isDragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        wm.updateViewLayout(rootView, params)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val wasDragging = isDragging
                    isDragging = false

                    if (!wasDragging) {
                        showBubble(lines.random())
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun showBubble(text: String) {
        bubble.text = text
        bubble.visibility = View.VISIBLE
        handler.removeCallbacks(hideBubble)
        handler.postDelayed(hideBubble, 2600)
    }

    private val hideBubble = Runnable {
        if (::bubble.isInitialized) {
            bubble.visibility = View.GONE
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "whale_pet"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "小小鱼桌宠",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        return Notification.Builder(this, channelId)
            .setContentTitle("小小鱼在桌面上游着")
            .setContentText("点她一下试试")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (::rootView.isInitialized) {
            try {
                wm.removeView(rootView)
            } catch (_: Exception) {
            }
        }
    }
}
