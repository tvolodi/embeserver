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
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.AppBarConfiguration
import com.dcastalia.localappupdate.DownloadApk
import kz.ascoa.embeserver.databinding.ActivityMainBinding
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.java_websocket.client.WebSocketClient
import java.io.File
import java.net.URI


class MainActivity : AppCompatActivity() {

    private var isSingleDoubleClick = 0
    private var clickOneTime: Long = 0L
    private var clickTwoTime: Long = 0L

    private val REQUEST_READ_PHONE_STATE = 1
    private val REQUEST_WRITE_EXTERNAL_STORAGE = 1
    private val REQUEST_INSTALL_PACKAGES = 1
    private val REQUEST_REQUEST_INSTALL_PACKAGES = 1


//    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var mainActivity: ActivityMainBinding

//    private var mWebSocketClient: WebSocketClient? = null

//    private var testWebSocketClient: WSClient = WSClient(URI("ws://127.0.0.1:38301/"), this)

    val activityContext = this

//    lateinit var hopelandRfidReader: HopelandRfidReader

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        // return super.onKeyUp(keyCode, event)

        // If trigger is
        if (keyCode == 139
            && event?.action == KeyEvent.ACTION_UP
            && isSingleDoubleClick == 2
        ) {
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

        val preferences = PreferenceManager.getDefaultSharedPreferences (this)

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
        doDriverAction(ActionType.START)

        // ATTENTION: This was auto-generated to handle app links.
        val appLinkIntent: Intent = intent
        val appLinkAction: String? = appLinkIntent.action
        val appLinkData: Uri? = appLinkIntent.data

        var appUpdateUrl = preferences.getString("AppUpdateUrl", "")
        if(appUpdateUrl == "") {

        }

    }

    fun updateAction() {

        val requestInstallPackagePermission =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.REQUEST_INSTALL_PACKAGES)
        if (requestInstallPackagePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.REQUEST_INSTALL_PACKAGES),
                REQUEST_REQUEST_INSTALL_PACKAGES
            )
        }

        val url = "https://www.vt-ptm.org/files/app-release.apk"
        val downloadApk = DownloadApk(this@MainActivity)
        downloadApk.startDownloadingApk(url);

    }

    private fun checkPermission() {

        val statePermission =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        if (statePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_READ_PHONE_STATE
            )
        }
    }

    private fun initView() {
    }

    private fun doDriverAction(action: ActionType) {

        val statePermission =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        if (statePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.READ_PHONE_STATE),
                REQUEST_READ_PHONE_STATE
            )
        }

        Intent(this, DriverService::class.java).also {
            it.action = action.name
            startService(it)
        }
    }

}