package kz.ascoa.embeserver

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.OutputStream

//suspend fun HttpClient.downloadFile(file: OutputStream, url: String) : Flow<DownloadResult>? {
//
//    var httpClient = this;
//
//        try {
//            val response = httpClient.get("https://www.vt-ptm.org/files/app-release.apk")
//            //{
////                 onDownload{ bytesTotal, contentLength ->
//            //}
//            val fileBodyBytes = response.body<ByteArray>()
//            val apkFile = File.createTempFile("", "Downloads")
//            apkFile.writeBytes(fileBodyBytes)
//
//        } catch (e: TimeoutCancellationException) {
////            emit(DownloadResult.Error("Connection timed out", e))
//        } catch (t: Throwable) {
////            emit(DownloadResult.Error("Failed to connect"))
//        }
//
//    return null
//
//}