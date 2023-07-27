package com.tvolodi.embeserver

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process
import android.util.Log
import android.widget.Toast
import com.pda.rfid.uhf.UHFReader

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import java.net.InetSocketAddress

private const val name = "SPYSERVICE_KEY"
private const val key = "SPYSERVICE_STATE"

private var ktorServer : NettyApplicationEngine? = null
private var commandName : String? = ""

/**
 * Get service state from shared preferences
 */
fun getServiceState(context: Context): ServiceStateType {
    val sharedPreferences = context.getSharedPreferences(name, 0)
    val value = sharedPreferences.getString(key, ServiceStateType.STOPPED.name)
    val stateValue = ServiceStateType.valueOf(value as String)
    return  stateValue
}

/**
 * Set service state to shcred preferences
 */
fun setServiceState(context: Context, state: ServiceStateType) {
    val sharedPreferences = context.getSharedPreferences(name, 0)
    sharedPreferences.edit().let{
        it.putString(key, state.name)
        it.apply()
    }
}

/**
 * Service to run KTor web-server. It serves as communication layer to native drivers
 */
class DriverService : Service() {

    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null;

    var serviceContext = this

    lateinit var hopelandReader: HopelandRfidReader

    /**
     * Use when service is binding service. We don't use it so return null - no binding.
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null;
    }

    /**
     * On Create event handler (?)
     * Prepare a new thread for our service
     */
    override fun onCreate(){
        super.onCreate()

//        Toast.makeText(serviceContext, "Service on create", Toast.LENGTH_LONG).show()

        try {
            var ht = HandlerThread("KtorProcess", Process.THREAD_PRIORITY_BACKGROUND).apply {
                try{
                    start()
                } catch (e: Exception){
                    Log.d("ERROR", e.stackTrace.toString())
                }


                serviceLooper = looper

                // Prepare service handler for KTor
                serviceHandler = ServiceHandler(looper, serviceContext)
            }
        } catch (e: Exception) {
            Log.d("ERROR", e.stackTrace.toString())
        }


    }

    /**
     * Run on service start point
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        commandName = intent.action
        // var mainActivity = intent.component as MainActivity

        if(intent.action == ActionType.START.name){

            // Create message for handler and run KTor
            serviceHandler?.obtainMessage()?.also { msg ->
                msg.arg1 = startId

                serviceHandler?.sendMessage(msg)
            }

            hopelandReader = HopelandRfidReader(this)
            hopelandReader.deviceConnect()
            // Update service state
            setServiceState(this, ServiceStateType.STARTED)


//            mainActivity.setServiceStateText("Started")
            // Inform OS to restart service at once
            return  START_STICKY
        } else
        {


            // Inform user on stop
            // Toast.makeText(this, "Ktor service stopping", Toast.LENGTH_SHORT).show()

            // If 33 and greater then we have to use const
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(true)
            }

            // ktorServer?.stop(100)

            //
            // hopelandReader.uhfReader.CloseConnect()
            hopelandReader.deviceDisconnect()

            // Update service state
            setServiceState(this, ServiceStateType.STOPPED)

            // mainActivity.setServiceStateText("Stopped")

            return START_NOT_STICKY
        }
    }

    /**
     *On Service Destroy handler
     */

    override fun onDestroy() {
        // Inform user
        Toast.makeText(this, "KTor service done", Toast.LENGTH_SHORT).show()
    }

    //
    private inner class ServiceHandler (looper: Looper, val context: Context): Handler(looper) {

        override fun handleMessage(msg: Message) {

//            Toast.makeText(context, "Ktor service starting", Toast.LENGTH_SHORT).show()
//
//            ktorServer = embeddedServer(Netty, port=8080, host = "0.0.0.0", module = Application::module).start(wait = false)
//            Toast.makeText(context, "Ktor service started", Toast.LENGTH_SHORT).show()

//            Toast.makeText(context, "WS Service starting", Toast.LENGTH_SHORT).show()
//            var webSocketServer : WSServer? = null
//            try{
//
//                var socketAddress = InetSocketAddress("127.0.0.1", 38301)
//                webSocketServer = WSServer(socketAddress, context)
//                webSocketServer.start()
//
//                Toast.makeText(context, "WS Service started", Toast.LENGTH_SHORT).show()
//
//            } catch (e : Exception) {
//                System.out.println(e.stackTrace)
//            } finally {
//                System.out.println(webSocketServer.toString())
//            }

            hopelandReader.deviceConnect()

//            if(commandName == ActionType.START.name)
//
//            else
//                ktorServer?.stop(1000)
        }

    }

    private fun startService() {

    }
}