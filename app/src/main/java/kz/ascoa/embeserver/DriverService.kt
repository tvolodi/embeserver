package kz.ascoa.embeserver

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import kz.ascoa.embeserver.enums.ActionType
import kz.ascoa.embeserver.view.MainActivity
import java.net.InetSocketAddress

//private var ktorServer : NettyApplicationEngine? = null
private var commandName: String? = ""

@SuppressLint("StaticFieldLeak")
var webSocketServer: WSServer? = null

class DriverService : Service() {

    private var serviceContext = this

    private var reader: IReadDevice? = null

    private val mNotificationId = 123

    private val toneGenerator: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    private var wsServerRunnable: RunnableContainer? = null

    /**
     * Use when service is binding service. We don't use it so return null - no binding.
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null;
    }

    /**
     * Run on service start point
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        commandName = intent.action
        if (intent.action != ActionType.START.name) {
            try {
                reader?.disconnectDevice()
                wsServerRunnable?.shutdown()
            } catch (e: Exception) {
                showToastMessage(e.message.toString())
            }
            stopSelf()
            return START_NOT_STICKY
        } else {
            if (reader == null) {
                initializeReader()
            }
            if (webSocketServer == null) {
                initializeWebSocketServer()
            }
            generateForegroundNotification()
            return START_STICKY
        }
    }

    private fun initializeWebSocketServer() {
        val prefManager = PreferenceManager.getDefaultSharedPreferences(this)
        val wsPort = prefManager.getString("web_socket_port", "38301") ?: "38301"
        val socketAddress = InetSocketAddress("127.0.0.1", wsPort.toInt())

        webSocketServer = WSServer(socketAddress, serviceContext, reader, toneGenerator, this)
        wsServerRunnable = RunnableContainer(webSocketServer!!)
        val wsThread = Thread(wsServerRunnable).apply {
            start()
        }
    }

    private fun initializeReader() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        when (preferences.getString("device_model", "")) {
            "pref_model_hopeland_value" -> {
                reader = HopelandRfidReader(
                    this,
                    toneGenerator,
                    "Hopeland",
                    OperationTypes.INVENTORY,
                    mutableListOf(),
                    ""
                )
            }
            "pref_model_zebra_value" -> {
                reader = ZebraRfidReader(
                    this,
                    toneGenerator,
                    "Zebra",
                    OperationTypes.INVENTORY,
                    mutableListOf(),
                    ""
                )
            }
            "pref_model_hl_bt_value" -> {
                reader = HopelandRfidBluetoothReader(
                    this,
                    toneGenerator,
                    "Hopeland BT",
                    OperationTypes.INVENTORY,
                    mutableListOf(),
                    ""
                )
            }
            "" -> {
                Toast.makeText(this, "Device is not set in settings", Toast.LENGTH_LONG).show()
                stopSelf()
                return
            }
        }
    }

    fun showToastMessage(messageText: String) {

        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            Toast.makeText(applicationContext, messageText, Toast.LENGTH_LONG).show()
        }
    }

    /**
     *On Service Destroy handler
     */

    override fun onDestroy() {
        Toast.makeText(this, "Driver service stopped", Toast.LENGTH_SHORT).show()
    }

    private fun generateForegroundNotification() {
        val intentMainLanding = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intentMainLanding, PendingIntent.FLAG_IMMUTABLE)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannelGroup(NotificationChannelGroup("chats_group", "Chats"))

        val serviceChannel = NotificationChannel(
            "service_channel", "Service Notifications", NotificationManager.IMPORTANCE_MIN
        ).apply {
            enableLights(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        notificationManager.createNotificationChannel(serviceChannel)

        val iconNotification = BitmapFactory.decodeResource(resources, R.drawable.ic_service_notification)

        val notification = NotificationCompat.Builder(this, "service_channel").apply {
            setContentTitle("embeserver service is running")
            setTicker("embeserver service is running")
            setContentText("Touch to open")
            setSmallIcon(R.drawable.ic_service_notification)
            priority = NotificationCompat.PRIORITY_LOW
            setWhen(0)
            setOnlyAlertOnce(true)
            setContentIntent(pendingIntent)
            setOngoing(true)
            iconNotification?.let {
                setLargeIcon(Bitmap.createScaledBitmap(it, 128, 128, false))
            }
            color= Color.YELLOW
        }.build()

        startForeground(mNotificationId, notification)
    }


    class RunnableContainer(private val wsServer: WSServer) : Runnable {
        fun shutdown() {
            wsServer.stop()
        }

        override fun run() {
            wsServer.start()
        }
    }
}