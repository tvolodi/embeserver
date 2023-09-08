package kz.ascoa.embeserver

// import org.java_websocket.drafts.Draft_10;

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.dcastalia.localappupdate.BuildConfig
import com.dcastalia.localappupdate.DownloadApk
import com.google.gson.JsonObject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import kz.ascoa.embeserver.databinding.ActivityMainBinding
import org.json.JSONObject
import java.nio.charset.Charset


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

        mainActivity.settingsButton.setOnClickListener { view ->
            settingsAction()
        }

        mainActivity.exitButton.setOnClickListener { view ->
            finish()
        }

        checkPermission()

        checkForNewVersion()

        // Start foreground service
        doDriverAction(ActionType.START)

        // ATTENTION: This was auto-generated to handle app links.
        val appLinkIntent: Intent = intent
        val appLinkAction: String? = appLinkIntent.action
        val appLinkData: Uri? = appLinkIntent.data

    }

    private fun checkForNewVersion() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this) // this.getPreferences(Context.MODE_PRIVATE)?: return
        val url = preferences.getString("update_app_url", "")
        val appInfoUrl = url + "/embeserver.info.json"
        var appVersionOnServer = ""
        lifecycleScope.launch {
            HttpClient().use {
                try {
                    val response = it.get(appInfoUrl){
//                        onDownload { bytesSentTotal, contentLength ->
//                            runOnUiThread{
//                                Toast.makeText(activityContext, "bytesSentTotal: ${bytesSentTotal}; contentLength:${contentLength}", Toast.LENGTH_SHORT).show()
//                            }
//                        }
                    }
                    // "https://www.vt-ptm.org/files/app-release.apk")

                    val fileBodyBytes = response.body<ByteArray>()
                    val jsonBodyString = String(fileBodyBytes, Charset.defaultCharset())

                    val jsonObj = JSONObject(jsonBodyString)
                    appVersionOnServer = jsonObj.getString("version")

                } catch (e: Exception) {
                    runOnUiThread{
                        showAlert(e.message)
                        // Toast.makeText(activityContext, "Error on app version check: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        val packageName = activityContext.packageName
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            packageManager.getPackageInfo(packageName, 0)
        }

        val currAppVersion = packageInfo.versionName
    }

    private fun settingsAction() {
        val fragments = supportFragmentManager.fragments
        if(fragments.count() == 0){
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<SettingsFragment>(R.id.fragmentContainerView)
            }
        } else {
            supportFragmentManager.commit {
                remove(fragments[0])
            }
        }
    }

    private fun validateDownloadUrl() {
        val preferences = this.getPreferences(Context.MODE_PRIVATE)?: return
        val url = preferences.getString("update_app_url", "")
        val appApkUrl = url + "/embeserver.apk"
        val validationResult = Patterns.WEB_URL.matcher(appApkUrl).matches()
        if (validationResult == false) {
            showAlert("Set correct download url")
        }
    }

    private fun updateAction() {

        val requestInstallPackagePermission =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.REQUEST_INSTALL_PACKAGES)
        if (requestInstallPackagePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.REQUEST_INSTALL_PACKAGES),
                REQUEST_REQUEST_INSTALL_PACKAGES
            )
        }

        val preferences = this.getPreferences(Context.MODE_PRIVATE)?: return
        val url = preferences.getString("update_app_url", "")
        val appApkUrl = url + "/embeserver.apk"

        val downloadApk = DownloadApk(this@MainActivity)
        downloadApk.startDownloadingApk(appApkUrl);
    }

    private fun showAlert(message: String?) {
        // Create the object of AlertDialog Builder class
        // Create the object of AlertDialog Builder class
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)

        // Set the message show for the Alert time
        builder.setMessage(message)

        // Set Alert Title
        builder.setTitle("Alert!")

        // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
        builder.setCancelable(false)

        // Set the positive button with yes name Lambda OnClickListener method is use of DialogInterface interface.
        builder.setPositiveButton("OK",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                // When the user click yes button then app will close
                dialog?.cancel();
            } as DialogInterface.OnClickListener)


        // Create the Alert dialog
        val alertDialog: AlertDialog = builder.create()
        // Show the Alert Dialog box
        // Show the Alert Dialog box
        alertDialog.show()
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