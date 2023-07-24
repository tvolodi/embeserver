package com.tvolodi.embeserver

import android.util.Log
import android.widget.Toast
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class WSClient(uri: URI) : WebSocketClient(uri) {
    override fun onOpen(serverHandshake: ServerHandshake) {

        send("test")
    }

    override fun onMessage(s: String) {

    }

    override fun onClose(i: Int, s: String, b: Boolean) {
        Log.i("Websocket", "Closed $s")
    }

    override fun onError(e: java.lang.Exception) {
        Log.i("Websocket", "Error " + e.message)
    }
}