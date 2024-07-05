package kz.ascoa.embeserver.view

// import org.java_websocket.drafts.Draft_10;

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.dcastalia.localappupdate.DownloadApk
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import kz.ascoa.embeserver.DriverService
import kz.ascoa.embeserver.R
import kz.ascoa.embeserver.TestReaderFragment
import kz.ascoa.embeserver.databinding.ActivityMainBinding
import kz.ascoa.embeserver.enums.ActionType
import org.java_websocket.client.WebSocketClient
import org.json.JSONObject
import java.nio.charset.Charset


class MainActivity : AppCompatActivity() {

    private var isSingleDoubleClick = 0
    private var clickOneTime: Long = 0L
    private var clickTwoTime: Long = 0L

    private val REQUEST_READ_PHONE_STATE = 1
    private val REQUEST_REQUEST_INSTALL_PACKAGES = 1


//    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var mainActivity: ActivityMainBinding

    //    private var testWebSocketClient: WSClient = WSClient(URI("ws://127.0.0.1:38301/"), this)

    private val activityContext = this

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

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        mainActivity = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainActivity.root)

        val intent: Intent = intent
        val action: String? = intent.action
        val data: Uri? = intent.data

        val preferences = PreferenceManager.getDefaultSharedPreferences (this)

        checkPermission()

        mainActivity.startDriverBtn.setOnClickListener {
            doDriverAction(ActionType.START)
        }

        mainActivity.stopDriverBtn.setOnClickListener {
            doDriverAction(ActionType.STOP)
        }

        mainActivity.minimizeButton.setOnClickListener {
            moveTaskToBack(true)
        }

        mainActivity.updateBtn.setOnClickListener {
            updateAction()
        }

        mainActivity.settingsButton.setOnClickListener {
            settingsAction()
        }

        mainActivity.testReaderButton.setOnClickListener {
            testReaderAction()
        }

        mainActivity.exitButton.setOnClickListener {
            finishAndRemoveTask()
        }

        checkForNewVersion()

        // Start foreground service
         doDriverAction(ActionType.START)

        // ATTENTION: This was auto-generated to handle app links.
        val appLinkIntent: Intent = intent
        val appLinkAction: String? = appLinkIntent.action
        val appLinkData: Uri? = appLinkIntent.data

    }

    private fun testReaderAction() {
        val testFragment = supportFragmentManager.findFragmentById(R.id.testReaderFragmentContainerView)
        if(testFragment == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<TestReaderFragment>(R.id.testReaderFragmentContainerView)
            }
        } else {
            supportFragmentManager.commit {
                remove(testFragment)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun checkForNewVersion() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this) // this.getPreferences(Context.MODE_PRIVATE)?: return
        val url = preferences.getString("update_app_url", "")
        if(url == ""){
            showAlert("Update URL setting is empty")
            return
        }
        val appInfoUrl = "$url/embeserver.info.json"
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
                    appVersionOnServer = jsonObj.getString("version_code")

                    var currentVersionCode : Long
                    var currentVersionName: String
                    val packageName = activityContext.packageName

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                        currentVersionName = packageInfo.versionName
                        currentVersionCode = packageInfo.longVersionCode
                    } else {
                        val packageInfo =  packageManager.getPackageInfo(packageName, 0)
                        currentVersionName = packageInfo.versionName
                        currentVersionCode = packageInfo.versionCode.toLong()
                    }

                    var versionCodeOnServer = appVersionOnServer.toLongOrNull()
                    if(versionCodeOnServer == null) {
                        runOnUiThread {
                            showAlert("Invalid format for version code on server")
                        }
                    } else {
                        if(versionCodeOnServer > currentVersionCode) {
                            val currButtonText = mainActivity.updateBtn.text
                            runOnUiThread{
                                mainActivity.updateBtn.text = "$currButtonText (Can update. Version: $versionCodeOnServer)"
                            }
                        }
                    }
                } catch (e: Exception) {

                    runOnUiThread{
                        showAlert(e.message)
                    }
                }
            }
        }
    }

    private fun settingsAction() {

        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
        if(fragment == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<SettingsFragment>(R.id.fragmentContainerView)
            }
        } else {
            supportFragmentManager.commit {
                remove(fragment)
            }
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
        try {
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
        } catch (e: Exception) {
            Toast.makeText(this, message.toString(), Toast.LENGTH_LONG).show()
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkPermission() {

        var permissionArr = arrayOf<String>(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val device_model = preferences.getString("device_model", "")
        if ( device_model == "pref_model_zebra_value") {
            permissionArr += Manifest.permission.CAMERA
            permissionArr += Manifest.permission.BLUETOOTH
            permissionArr += Manifest.permission.BLUETOOTH_CONNECT
            permissionArr += Manifest.permission.BLUETOOTH_SCAN
        }

        permissionArr += Manifest.permission.CAMERA
        permissionArr += Manifest.permission.BLUETOOTH
        permissionArr += Manifest.permission.BLUETOOTH_CONNECT
        permissionArr += Manifest.permission.BLUETOOTH_SCAN

        val statePermission =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            ActivityCompat.requestPermissions(
                this,
                permissionArr,
                REQUEST_READ_PHONE_STATE
            )
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