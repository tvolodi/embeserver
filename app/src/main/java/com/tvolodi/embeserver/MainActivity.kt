package com.tvolodi.embeserver

// import org.java_websocket.drafts.Draft_10;

import android.R
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.tvolodi.embeserver.databinding.ActivityMainBinding
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.net.URISyntaxException


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var mWebSocketClient: WebSocketClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setSupportActionBar(binding.toolbar)

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.startDriverBtn.setOnClickListener { view ->
//            Intent(this, DriverService::class.java).also { intent ->
//                startService(intent)
//            }
            doDriverAction(ActionType.START)
        }

        binding.stopDriverBtn.setOnClickListener { view ->
            doDriverAction(ActionType.STOP)
        }

        binding.testWSBtn.setOnClickListener { view ->
            doTestWSAction()
        }
    }

    private fun doTestWSAction() {
        connectWebSocket()
    }

    private fun doDriverAction(action: ActionType){
        var currentState = getServiceState(this)
        if ( currentState == ServiceStateType.STOPPED
            && action == ActionType.STOP) return
        if (currentState == ServiceStateType.STARTED
            && action == ActionType.START) return

        Intent(this, DriverService::class.java).also {
            it.action = action.name
            startForegroundService(it)
        }

    }

    private fun connectWebSocket() {
        val uri: URI
        uri = try {
            URI("ws://127.0.0.1:38301/")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }
        mWebSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(serverHandshake: ServerHandshake) {
                Log.i("Websocket", "Opened")
                mWebSocketClient!!.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL)
            }

            override fun onMessage(s: String) {
                runOnUiThread {
                    var text = "Got message " + s

                }
            }

            override fun onClose(i: Int, s: String, b: Boolean) {
                Log.i("Websocket", "Closed $s")
            }

            override fun onError(e: java.lang.Exception) {
                Log.i("Websocket", "Error " + e.message)
            }
        }

        try {
            mWebSocketClient?.connect()

        } catch (e: Exception){
            System.out.println(e.stackTrace)
        }


    }
}

class EmptyClient : WebSocketClient {
    constructor(serverUri: URI?, draft: Draft?) : super(serverUri, draft) {}
    constructor(serverURI: URI?) : super(serverURI) {}

    override fun onOpen(handshakedata: ServerHandshake) {
        try {
            this.send("Hello, WS World!");
        } catch (e: Exception) {
            System.out.println(e.stackTrace)
        }
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        println("closed with exit code $code additional info: $reason")
    }

    override fun onMessage(message: String) {
        println("received message: $message")
    }

    override fun onError(ex: Exception) {
        System.err.println("an error occurred:$ex")
    }
}