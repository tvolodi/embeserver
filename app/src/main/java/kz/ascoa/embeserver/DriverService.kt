package kz.ascoa.embeserver

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
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
// import kz.ascoa.embeserver.R

import kz.ascoa.embeserver.enums.ActionType
import kz.ascoa.embeserver.view.MainActivity
import java.net.InetSocketAddress

private const val name = "SPYSERVICE_KEY"
private const val key = "SPYSERVICE_STATE"

//private var ktorServer : NettyApplicationEngine? = null
private var commandName : String? = ""

var webSocketServer : WSServer? = null

/**
 * https://betterprogramming.pub/what-is-foreground-service-in-android-3487d9719ab6
 */

/**
 * Get service state from shared preferences
 */
//fun getServiceState(context: Context): ServiceStateType {
//    val sharedPreferences = context.getSharedPreferences(name, 0)
//    val value = sharedPreferences.getString(key, ServiceStateType.STOPPED.name)
//    val stateValue = ServiceStateType.valueOf(value as String)
//    return  stateValue
//}
//
///**
// * Set service state to shcred preferences
// */
//fun setServiceState(context: Context, state: ServiceStateType) {
//    val sharedPreferences = context.getSharedPreferences(name, 0)
//    sharedPreferences.edit().let{
//        it.putString(key, state.name)
//        it.apply()
//    }
//}

/**
 * Service to run RFID driver with communication through Web socket
 */
class DriverService : Service() {

     private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null;

    var serviceContext = this

    private var reader: IReadDevice? = null
    var wsThread: Thread? = null

    private var iconNotification: Bitmap? = null

    private var notification: Notification? = null
    var mNotificationManager: NotificationManager? = null
    private val mNotificationId = 123

    public val toneGenerator: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    var wsServerRunnable: RunnableContainer? = null

    /**
     * Use when service is binding service. We don't use it so return null - no binding.
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null;
    }

    /**
     * On Create event handler. Called once on service creation (before onStartCommand ?)
     */
    override fun onCreate(){
        super.onCreate()

//        Toast.makeText(serviceContext, "Service on create", Toast.LENGTH_LONG).show()

//        try {
//            var ht = HandlerThread("RfidDriverThread", Process.THREAD_PRIORITY_BACKGROUND).apply {
//                try{
//                    start()
//                } catch (e: Exception){
//                    Log.d("ERROR", e.stackTrace.toString())
//                }
//
//                serviceLooper = looper
//
//                // Prepare service handler for KTor
//                serviceHandler = ServiceHandler(looper, serviceContext)
//            }
//        } catch (e: Exception) {
//            Log.d("ERROR", e.stackTrace.toString())
//        }
    }

    /**
     * Run on service start point
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        commandName = intent.action

        if(intent.action == ActionType.START.name){


            if(reader == null) {
                val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                val device_model = preferences.getString("device_model", "")
                when(device_model) {
                    "pref_model_hopeland_value" -> {
                        reader = HopelandRfidReader(this, toneGenerator, "Hopeland", OperationTypes.INVENTORY,  mutableListOf<String>(), "")
                    }
                    "pref_model_zebra_value" -> {
                        reader = ZebraRfidReader(this, toneGenerator, "Zebra", OperationTypes.INVENTORY,  mutableListOf<String>(), "")
                    }
                    // getString(R.string.pref_model_hl_bt_value) -> {
                    "pref_model_hl_bt_value" -> {
                        reader = HopelandRfidBluetoothReader(this, toneGenerator, "Hopeland BT", OperationTypes.INVENTORY,  mutableListOf<String>(), "")
                    }

                    "" -> {
                        Toast.makeText(this, "Device is not set in settings", Toast.LENGTH_LONG).show()
                        return START_NOT_STICKY
                    }
                }
            }

            // Run Web Socket Server in it's own thread
            val prefManager = PreferenceManager.getDefaultSharedPreferences(this)
            val wsPort = prefManager.getString("web_socket_port", "38301")
            var socketAddress = wsPort?.let { InetSocketAddress("127.0.0.1", it.toInt()) }
            webSocketServer =
                socketAddress?.let { WSServer(it, serviceContext, reader, toneGenerator, this) }
            wsServerRunnable = RunnableContainer(webSocketServer!!)
            val wsThread2 = Thread(wsServerRunnable)
            wsThread2.start()

            // Show notification. Hopeland will now normally work in the service without it
            generateForegroundNotification()

            // Inform OS to restart service at once
//            return  START_NOT_STICKY
            return START_STICKY
        } else
        {
            try {
                reader?.disconnectDevice()
                wsServerRunnable?.shutdown()

//                reader?.deviceDisconnect()
//                reader = null
            } catch (e: Exception) {
                showToastMessage(e.message.toString())
//                showAlert(this, e.message)
            }

            stopForeground(true)
            stopSelf()

            // Inform user on stop
            // Toast.makeText(this, "Ktor service stopping", Toast.LENGTH_SHORT).show()

            // If 33 and greater then we have to use const
//            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
//                stopForeground(STOP_FOREGROUND_REMOVE)
//            } else {
//                stopForeground(true)
//            }

            //
            // hopelandReader.uhfReader.CloseConnect()
//            hopelandReader.deviceDisconnect()

            // Update service state
//            setServiceState(this, ServiceStateType.STOPPED)

            // mainActivity.setServiceStateText("Stopped")

            return START_NOT_STICKY
        }
    }

    fun showErrorAllert(message: String) {

        showAlert(this, message)
    }

    fun showToastMessage(messageText: String) {

        val mainHandler: Handler = Handler(Looper.getMainLooper())
        mainHandler.post(Runnable {
                Toast.makeText(applicationContext, messageText, Toast.LENGTH_LONG).show()
        })
//        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show()
    }

    /**
     *On Service Destroy handler
     */

    override fun onDestroy() {
        // Inform user
        Toast.makeText(this, "Driver service stopped", Toast.LENGTH_SHORT).show()
    }

    private fun generateForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intentMainLanding = Intent(this, MainActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intentMainLanding, 0)
            // iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            if (mNotificationManager == null) {
                mNotificationManager =
                    this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assert(mNotificationManager != null)
                mNotificationManager?.createNotificationChannelGroup(
                    NotificationChannelGroup("chats_group", "Chats")
                )
                val notificationChannel =
                    NotificationChannel(
                        "service_channel", "Service Notifications",
                        NotificationManager.IMPORTANCE_MIN
                    )
                notificationChannel.enableLights(false)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
                mNotificationManager?.createNotificationChannel(notificationChannel)
            }
            val builder = NotificationCompat.Builder(this, "service_channel")

            builder.setContentTitle( "embeserver service is running")
                .setTicker( "embeserver service is running")
                .setContentText("Touch to open") //                    , swipe down for more options.
                .setSmallIcon(R.drawable.ic_service_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setWhen(0)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setOngoing(true)

            iconNotification = BitmapFactory.decodeResource(resources,
                R.drawable.ic_service_notification
            )
            if (iconNotification != null) {
                builder.setLargeIcon(Bitmap.createScaledBitmap(iconNotification!!, 128, 128, false))
            }
            builder.color =  Color.YELLOW // resources.getColor(R.color.purple_200)
            notification = builder.build()
            startForeground(mNotificationId, notification)
        }
    }

        //
    private inner class ServiceHandler(
        looper: Looper,
        val context: Context
    ): Handler(looper) {

        override fun handleMessage(msg: Message) {

        }

    }

    private fun startService() {

    }

    class RunnableContainer (val wsServer: WSServer): Runnable {

        fun shutdown() {
            wsServer?.stop()
        }
        override fun run() {
            wsServer?.start()
        }
    }
}