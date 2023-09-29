// It's a snippet for update application function!!!

package kz.ascoa.embeserver.Obsolete

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class Updater {

//    fun updateApp(activityContext: AppCompatActivity, lifecycleScope: Any): String? {
//        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        val apkFile = File(downloadDir, "embeserver.apk")
//        var apkUri = FileProvider.getUriForFile(activityContext, "${activityContext.packageName}.provider", apkFile)
//
////        lifecycleScope.launch {
//            HttpClient().use {
//                try {
////                    val response = it.get("https://www.vt-ptm.org/files/app-release.apk"){
////                        onDownload { bytesSentTotal, contentLength ->
////                            runOnUiThread{
////                                Toast.makeText(activityContext, "bytesSentTotal: ${bytesSentTotal}; contentLength:${contentLength}", Toast.LENGTH_SHORT).show()
////                            }
////                        }
////                    }
////                    // "https://www.vt-ptm.org/files/app-release.apk")
////
////                    val fileBodyBytes = response.body<ByteArray>()
//
////                    val isExists = apkFile.exists()
////                    if (isExists) {
////                        val isDeleteSuccess = apkFile.delete()
////                    }
////
////                    apkFile.writeBytes(fileBodyBytes)
//                } catch (e: Exception) {
//                    return e.message
//                }
//            }
////        }
//
//        val packageManager = activityContext.packageManager
//        val canRequestInstall = packageManager.canRequestPackageInstalls()
//        if (!canRequestInstall) {
//            activityContext.startActivity( Intent(
//                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
//                Uri.parse("package:" + activityContext.packageName
//                    //packageManager.getPackageInfo(activityContext.p)
//                )
//            )
//            )
//            return ""
//        }
//
//        try{
//            val packageInstaller = packageManager.packageInstaller
//            val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
//            val sessionId = packageInstaller.createSession(sessionParams)
//            val session = packageInstaller.openSession(sessionId)
//
//            runBlocking {
//                launch { addApkToInstallSession(activityContext, session, apkUri) }
//            }
//
//            val mainActivityClass = activityContext.javaClass
//            val intent = Intent(activityContext, mainActivityClass);
//            intent.setAction("com.example.android.apis.content.SESSION_API_PACKAGE_INSTALLED")
//            val pendingIntent = PendingIntent.getActivity(
//                activityContext,
//                0,
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
//            )
//            val statusReceiver = pendingIntent.intentSender
//
//            if (statusReceiver != null) {
//                session.commit(statusReceiver)
//            }
//        } catch (e: Exception){
//            return e.message
//        }
//
//        return ""
//    }
//
//    private suspend fun addApkToInstallSession(
//        activityContext: AppCompatActivity,
//        session: PackageInstaller.Session,
//        apkUri: Uri
//    ) {
//        val packageInSessionOS = session.openWrite(activityContext.packageName, 0, -1)
//        val inputStream = activityContext.contentResolver.openInputStream(apkUri)
//        try {
//            if (inputStream != null) {
//                inputStream.copyTo(packageInSessionOS)
//            } else {
//                throw java.lang.Exception("APK input stream is null")
//            }
//        } finally {
//            packageInSessionOS.close()
//            inputStream?.close()
//        }
//    }
}