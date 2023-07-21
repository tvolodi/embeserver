package com.tvolodi.embeserver

import android.content.Context
import android.widget.Toast
import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.lang.Exception


class WSServer(socketAddress: InetSocketAddress, val context: Context ) : WebSocketServer(socketAddress) {

    var clientHandshake: ClientHandshake? = null

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        clientHandshake = handshake
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        // Some actions
    }

    /**
     * Message format:
     * operation_name;
     * continuous_read_tag
     */
    override fun onMessage(conn: WebSocket?, message: String?) {

        var parsedMessage = message?.split(";")
        var operationName = parsedMessage?.get(0)
        when (operationName) {
            "test" -> conn?.send("Test passed")
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        // Some actions
    }

    override fun onStart() {
        // Some actions
    }
}