package kor.app

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

class MainActivity : AppCompatActivity() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getBooleanExtra("FROM_NOTIFICATION", false)) {
            showFloatingWindow()
        }
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        } else {
            showFloatingWindow()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showFloatingWindow() {
        setTheme(R.style.TransparentTheme)
        floatingView = layoutInflater.inflate(R.layout.floating_view, null)
        params = createLayoutParams()

        val closeWindow = floatingView.findViewById<ImageView>(R.id.close_window)
        val hide_window = floatingView.findViewById<ImageView>(R.id.hide_window)
        closeWindow.setOnClickListener {
            windowManager.removeViewImmediate(MyLinearLayout(this@MainActivity))
        }

        hide_window.setOnClickListener {
            windowManager.removeView(floatingView)
            showNotification()
        }

        windowManager.addView(floatingView, params)
        floatingView.setOnTouchListener(FloatingViewTouchListener())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Window",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showNotification() {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        notificationIntent.putExtra("FROM_NOTIFICATION", true)

        contentIntent.run {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            send()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private inner class FloatingViewTouchListener : View.OnTouchListener {
        private var initialX: Int = 0
        private var initialY: Int = 0
        private var initialTouchX: Float = 0.toFloat()
        private var initialTouchY: Float = 0.toFloat()

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val xDiff = (event.rawX - initialTouchX).toInt()
                    val yDiff = (event.rawY - initialTouchY).toInt()
                    if (xDiff < 10 && yDiff < 10) {
                        //Toast.makeText(this@MainActivity, "Clicked!", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
            }
            return false
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
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
            x = 0
            y = 0
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${applicationContext.packageName}")
            )
            val OVERLAY_PERMISSION_REQ_CODE = 1234
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onStart() {
        super.onStart()
        onBackPressed()
    }


    override fun onRestart() {
        super.onRestart()
        onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeViewImmediate(MyLinearLayout(this@MainActivity))
    }

    companion object {
        const val CHANNEL_ID = "floating_window_channel"
        const val NOTIFICATION_ID = 1234
    }
}

class MyLinearLayout(context: Context) : LinearLayout(context) {
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}