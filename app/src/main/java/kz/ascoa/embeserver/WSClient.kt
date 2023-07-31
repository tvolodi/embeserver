package kz.ascoa.embeserver

import android.util.Log
import kz.ascoa.embeserver.Dividers.FIELD_DIVIDER
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class WSClient(uri: URI, var mainActivity: MainActivity) : WebSocketClient(uri) {
    override fun onOpen(serverHandshake: ServerHandshake) {

        send("test")
    }

    override fun onMessage(s: String) {
        var messageContentList = s.split(FIELD_DIVIDER)
        val operationName = messageContentList.get(0)
        when(operationName) {
            "test" -> {

            }
            "got_epc" -> {

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