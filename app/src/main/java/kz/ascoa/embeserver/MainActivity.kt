package kz.ascoa.embeserver

// import org.java_websocket.drafts.Draft_10;

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.AppBarConfiguration
import com.tvolodi.embeserver.databinding.ActivityMainBinding
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.java_websocket.client.WebSocketClient
import java.io.File
import java.net.URI

class MainActivity : AppCompatActivity() {

    private var isSingleDoubleClick = 0
    private var clickOneTime : Long = 0L
    private var clickTwoTime : Long = 0L

    private val REQUEST_READ_PHONE_STATE = 1
    private val REQUEST_WRITE_EXTERNAL_STORAGE = 1
    private val REQUEST_INSTALL_PACKAGES = 1
    private val REQUEST_REQUEST_INSTALL_PACKAGES = 1


    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var mainActivity: ActivityMainBinding

    private var mWebSocketClient: WebSocketClient? = null

    private var testWebSocketClient: WSClient = WSClient(URI("ws://127.0.0.1:38301/"), this)

    val activityContext = this

    lateinit var hopelandRfidReader: HopelandRfidReader

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        // return super.onKeyUp(keyCode, event)

        // If trigger is
        if(keyCode == 139
            && event?.action == KeyEvent.ACTION_UP
            && isSingleDoubleClick == 2) {
            isSingleDoubleClick = 0
        }

        return true // stop propagate event further
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        mainActivity = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainActivity.root)

        val intent: Intent = intent
        val action: String? = intent.action
        val data: Uri? = intent.data

        mainActivity.startDriverBtn.setOnClickListener { view ->
            doDriverAction(ActionType.START)
        }

        mainActivity.stopDriverBtn.setOnClickListener { view ->
            doDriverAction(ActionType.STOP)
        }

        mainActivity.updateBtn.setOnClickListener { view ->
            updateAction()
        }

        checkPermission()

        // Start foreground service
        // doDriverAction(ActionType.START)

        // ATTENTION: This was auto-generated to handle app links.
        val appLinkIntent: Intent = intent
        val appLinkAction: String? = appLinkIntent.action
        val appLinkData: Uri? = appLinkIntent.data
    }

    fun updateAction() {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadDir, "embeserver.apk")
        var apkUri = FileProvider.getUriForFile(activityContext, "${activityContext.packageName}.provider", apkFile)

        lifecycleScope.launch {
            HttpClient().use {
                try {
//                    val response = it.get("https://www.vt-ptm.org/files/app-release.apk"){
//                        onDownload { bytesSentTotal, contentLength ->
//                            runOnUiThread{
//                                Toast.makeText(activityContext, "bytesSentTotal: ${bytesSentTotal}; contentLength:${contentLength}", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    }
//                    // "https://www.vt-ptm.org/files/app-release.apk")
//
//                    val fileBodyBytes = response.body<ByteArray>()

//                    val isExists = apkFile.exists()
//                    if (isExists) {
//                        val isDeleteSuccess = apkFile.delete()
//                    }
//
//                    apkFile.writeBytes(fileBodyBytes)
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(activityContext, "Error: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

        val packageManager = activityContext.packageManager
        val canRequestInstall = packageManager.canRequestPackageInstalls()
        if (!canRequestInstall) {
            activityContext.startActivity( Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + activityContext.packageName
                        //packageManager.getPackageInfo(activityContext.p)
                )
            ))
            return
        }

        try{
            val packageInstaller = packageManager.packageInstaller
            val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = packageInstaller.createSession(sessionParams)
            val session = packageInstaller.openSession(sessionId)

            runBlocking {
                launch { addApkToInstallSession(session, apkUri) }
            }

            val mainActivityClass = activityContext.javaClass
            val intent = Intent(activityContext, mainActivityClass);
            intent.setAction("com.example.android.apis.content.SESSION_API_PACKAGE_INSTALLED")
            val pendingIntent = PendingIntent.getActivity(
                activityContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            val statusReceiver = pendingIntent.intentSender

            if (statusReceiver != null) {
                session.commit(statusReceiver)
            }
        } catch (e: Exception){
            runOnUiThread {
                Toast.makeText(activityContext, "Error: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    }

    private suspend fun addApkToInstallSession(
        session: PackageInstaller.Session,
        apkUri: Uri
    ) {
        val packageInSessionOS = session.openWrite(activityContext.packageName, 0, -1)
        val inputStream = activityContext.contentResolver.openInputStream(apkUri)
        try {
            if (inputStream != null) {
                inputStream.copyTo(packageInSessionOS)
            } else {
                throw java.lang.Exception("APK input stream is null")
            }
        } finally {
            packageInSessionOS.close()
            inputStream?.close()
        }
    }

    private fun checkPermission() {
        val fileWritePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (fileWritePermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE
            )
        }

        val installPackagePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.INSTALL_PACKAGES)
        if (installPackagePermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.INSTALL_PACKAGES),
                REQUEST_INSTALL_PACKAGES
            )
        }

        val requestInstallPackagePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.REQUEST_INSTALL_PACKAGES)
        if (requestInstallPackagePermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.REQUEST_INSTALL_PACKAGES),
                REQUEST_REQUEST_INSTALL_PACKAGES
            )
        }

        val statePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        if(statePermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf<String>(Manifest.permission.READ_PHONE_STATE),
                REQUEST_READ_PHONE_STATE)
        } else {
            initView()
        }
    }

    private fun initView() {
    }

//    private fun doTestWSAction() {
//        connectWebSocket()
//    }

    private fun doDriverAction(action: ActionType){
//        var currentState = getServiceState(this)
//        if ( currentState == ServiceStateType.STOPPED
//            && action == ActionType.STOP) return
//        if (currentState == ServiceStateType.STARTED
//            && action == ActionType.START) return
//
        Intent(this, DriverService::class.java).also {
            it.action = action.name
            startService(it)
        }
    }

//    private fun initWSClient(uriP: URI?) {
//
//        var uri = uriP
//
//        if(uri == null) {
//            uri = try {
//                URI("ws://127.0.0.1:38301/")
//            } catch (e: URISyntaxException) {
//                e.printStackTrace()
//                return
//            }
//        }
//
//        mWebSocketClient = object : WebSocketClient(uri) {
//            override fun onOpen(serverHandshake: ServerHandshake) {
//                runOnUiThread{
//                    Toast.makeText(activityContext, "WS Opened", Toast.LENGTH_SHORT).show()
//                }
//                mWebSocketClient!!.send("test")
//            }
//
//            override fun onMessage(s: String) {
//                runOnUiThread {
//                    Toast.makeText(activityContext, s, Toast.LENGTH_LONG).show()
//
//                }
//            }
//
//            override fun onClose(i: Int, s: String, b: Boolean) {
//                Log.i("Websocket", "Closed $s")
//            }
//
//            override fun onError(e: java.lang.Exception) {
//                Log.i("Websocket", "Error " + e.message)
//            }
//        }
//    }

//    private fun connectWebSocket() {
//
//
//        try {
//            if(testWebSocketClient.isOpen != true) {
//                testWebSocketClient.connect()
//            }
//            testWebSocketClient.send("test")
//
//            // mWebSocketClient?.connect()
//
//        } catch (e: Exception){
//            Toast.makeText(activityContext, "${e.stackTrace.toString()}", Toast.LENGTH_LONG).show()
//        }
//    }

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

//class EmptyClient : WebSocketClient {
//    constructor(serverUri: URI?, draft: Draft?) : super(serverUri, draft) {}
//    constructor(serverURI: URI?) : super(serverURI) {}
//
//    override fun onOpen(handshakedata: ServerHandshake) {
//        try {
//            this.send("Hello, WS World!");
//        } catch (e: Exception) {
//            System.out.println(e.stackTrace)
//        }
//    }
//
//    override fun onClose(code: Int, reason: String, remote: Boolean) {
//        println("closed with exit code $code additional info: $reason")
//    }
//
//    override fun onMessage(message: String) {
//        println("received message: $message")
//    }
//
//    override fun onError(ex: Exception) {
//        System.err.println("an error occurred:$ex")
//    }
//}