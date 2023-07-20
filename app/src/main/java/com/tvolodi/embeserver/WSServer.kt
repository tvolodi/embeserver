package com.tvolodi.embeserver

import android.content.Context
import android.widget.Toast
import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.lang.Exception


class WSServer(socketAddress: InetSocketAddress, val context: Context ) : WebSocketServer(socketAddress) {

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        // Some actions
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        // Some actions
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        var recMsg = "Server got message: " + message
        Toast.makeText(context, "WS Service got message" + message, Toast.LENGTH_LONG).show()
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        // Some actions
    }

    override fun onStart() {
        // Some actions
    }
}