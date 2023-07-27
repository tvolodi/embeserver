package com.tvolodi.embeserver

import android.content.Context
import com.tvolodi.embeserver.Dividers.FIELD_DIVIDER
import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.lang.Exception


class WSServer(socketAddress: InetSocketAddress, val context: Context, val reader: HopelandRfidReader ) : WebSocketServer(socketAddress) {

    var clientHandshake: ClientHandshake? = null

    init{
        reader.wsServer = this
    }

    var wsConnection: WebSocket? = null
    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        wsConnection = conn
        clientHandshake = handshake
    }

    fun got_epc(epc: String?){
        val message = "got_epc${FIELD_DIVIDER}${epc}"
        wsConnection?.send(message)
    }

    fun got_epc_list(epcList: List<String>){

        val message = "got_epc_list${FIELD_DIVIDER}${epcList.joinToString("$$")}"
        wsConnection?.send(message)
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

        var parsedMessage = message?.split(FIELD_DIVIDER)
        var operationName = parsedMessage?.get(0)
        wsConnection = conn
        when (operationName) {
            "test" -> conn?.send("Test passed")
            "read_tag" -> {
                reader.isContinueReading = true
                reader.readEPC(0)
                reader.isContinueReading = false
            }

            "read_tag_continuous" -> {
                reader.isContinueReading = true
                reader.readEPC(1)
            }
            "stop_reading" -> reader.isContinueReading = false
            else -> conn?.send("${message} was sent")
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        // Some actions
    }

    override fun onStart() {
        // Some actions
    }
}