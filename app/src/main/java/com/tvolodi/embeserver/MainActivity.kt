package com.tvolodi.embeserver

// import org.java_websocket.drafts.Draft_10;

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import com.pda.rfid.uhf.UHFReader
import com.port.Adapt
import com.tvolodi.embeserver.databinding.ActivityMainBinding
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.handshake.ServerHandshake
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val REQUEST_READ_PHONE_STATE = 1

    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var binding: ActivityMainBinding

    private var mWebSocketClient: WebSocketClient? = null

    val activityContext = this

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent: Intent = intent
        val action: String? = intent.action
        val data: Uri? = intent.data


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

        binding.serviceStateText.setText( "Service state: ${getServiceState(this)}")

        binding.startWSBtn.setOnClickListener{
            startWS()
        }

        checkPermission()
    }

    private fun checkPermission() {
        val statePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        if(statePermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf<String>(Manifest.permission.READ_PHONE_STATE),
                REQUEST_READ_PHONE_STATE)
        } else {
            initView()
        }
    }

    fun setServiceStateText(text: String) {
        binding.serviceStateText.setText(text)
    }

    private fun initView() {
//        Adapt.init(this)
//        Adapt.enablePauseInBackGround(this)

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

    fun startWS() {

        val t = Thread{

            var webSocketServer : WSServer? = null
            try{

                var socketAddress = InetSocketAddress("127.0.0.1", 38301)
                webSocketServer = WSServer(socketAddress, activityContext)
                webSocketServer.start()

            } catch (e : Exception) {
                System.out.println(e.stackTrace)
            } finally {
                System.out.println(webSocketServer.toString())
            }
        }
        t.start()
        initWSClient(null)
        connectWebSocket()
    }

    private fun initWSClient(uriP: URI?) {

        var uri = uriP

        if(uri == null) {
            uri = try {
                URI("ws://127.0.0.1:38301/")
            } catch (e: URISyntaxException) {
                e.printStackTrace()
                return
            }
        }

        mWebSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(serverHandshake: ServerHandshake) {
                runOnUiThread{
                    Toast.makeText(activityContext, "WS Opened", Toast.LENGTH_SHORT).show()
                }
                mWebSocketClient!!.send("test")
            }

            override fun onMessage(s: String) {
                runOnUiThread {
                    Toast.makeText(activityContext, s, Toast.LENGTH_LONG).show()

                }
            }

            override fun onClose(i: Int, s: String, b: Boolean) {
                Log.i("Websocket", "Closed $s")
            }

            override fun onError(e: java.lang.Exception) {
                Log.i("Websocket", "Error " + e.message)
            }
        }
    }

    private fun connectWebSocket() {


        try {
            mWebSocketClient?.connect()

        } catch (e: Exception){
            Toast.makeText(activityContext, "${e.stackTrace.toString()}", Toast.LENGTH_LONG).show()
        }




    }

    // Copied from example. Seems it is not used in this class but used in Activity which is used in the example. Delete?
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String?>?,
//        grantResults: IntArray?
//    ) {
//        if (requestCode == REQUEST_READ_PHONE_STATE) {
//            initView()
//        }
//        super.onRequestPermissionsResult(requestCode, permissions!!, grantResults!!)
//    }
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