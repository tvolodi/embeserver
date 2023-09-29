package kz.ascoa.embeserver

import android.app.Activity
import android.util.Log
import android.widget.Toast
import kz.ascoa.embeserver.Dividers.FIELD_DIVIDER
import kz.ascoa.embeserver.view.MainActivity
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import kotlin.math.sign

class WSClient(val uri: URI, private val activity: Activity, val testFragment: TestReaderFragment) : WebSocketClient(uri) {

    fun readTag () {

        var message = "${WSCommands.READ_TAG}${FIELD_DIVIDER}"
        connection.send(message)
    }
    override fun onOpen(serverHandshake: ServerHandshake) {

        send("test")
    }

    override fun onMessage(s: String) {
        var messageContentList = s.split(FIELD_DIVIDER)
        val responseName = messageContentList.get(0)
        when(responseName) {
            Responses.GOT_EPC -> {
                // (activity as TestReaderFragment)
                val epcString = messageContentList[1]
                val epcList = epcString.split(Dividers.VALUE_DIVIDER)
                val firstTagDataList = epcList[0]?.split(Dividers.SUBVALUE_DIVIDER)
                if(firstTagDataList != null){
                    val firstEpcString = firstTagDataList[0]
                    testFragment.uiBinding.tagEpcViewText.text = firstEpcString
                    testFragment.tagEpc = firstEpcString // save for future tests with tag. e.g. tag location
                    if(firstTagDataList.count() >= 2){
                        var signalStrength = firstTagDataList[1]
                        testFragment.uiBinding.tagSignalStrengthTextView.text = signalStrength
                        if(signalStrength == "null") signalStrength = "0.0"
                        val signalStrengthDbm = signalStrength.toDouble();
                        var progress = 2.0 * signalStrengthDbm + 140.0
                        testFragment.uiBinding.tagSignalStrengthPBar.progress = progress.toInt()
                    }
                    if(firstTagDataList.count() >= 3) {
                        var relativeDistance = firstTagDataList[2]
                        if(relativeDistance == "null") relativeDistance = "0.0"
                        testFragment.uiBinding.tagRelativeDistanceTextView.text = relativeDistance
                    }
//                    activity.runOnUiThread{
//                        showAlert(activity, "Got EPC $firstEpcString")
//                    }
                }
            }
            Responses.ERROR -> {
                activity.runOnUiThread{
                    showAlert(activity, "Error \n ${messageContentList[1]}")
                }
            }

            "test" -> {

            }

        }

    }

    override fun onClose(i: Int, s: String, b: Boolean) {
        Log.i("Websocket", "Closed $s")
    }

    override fun onError(e: java.lang.Exception) {
        Log.i("Websocket", "Error " + e.message)
    }
}