package kz.ascoa.embeserver

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import kz.ascoa.embeserver.Dividers.FIELD_DIVIDER
import kz.ascoa.embeserver.Dividers.VALUE_DIVIDER
import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.lang.Exception


class WSServer(
    socketAddress: InetSocketAddress,
    val context: Context,
    val reader: HopelandRfidReader?,
    val toneGenerator: ToneGenerator
) : WebSocketServer(socketAddress) {

    var clientHandshake: ClientHandshake? = null

    var clientConnectionList : List<WebSocket?> = mutableListOf()

    var isContinueReading = false;

    init{
        reader?.wsServer = this
    }

    var wsConnection: WebSocket? = null
    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        wsConnection = conn
        clientHandshake = handshake
        clientConnectionList += conn

        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
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
     * Message format: operation_name::
     * operation_name
     * continuous_read_tag
     */
    override fun onMessage(conn: WebSocket?, message: String?) {

        var parsedMessage = message?.split(FIELD_DIVIDER)
        var operationName = parsedMessage?.get(0)

        wsConnection = conn
        when (operationName) {
            "test" -> conn?.send("Test passed")

            "get_tag_distance" -> {
                var distance = reader?.getTagDistance()
                var message  = "got_tag_distance${FIELD_DIVIDER}${distance}"
                conn?.send(message)
            }

            "read_tag" -> {
                isContinueReading = true
                reader?.isContinueReading = isContinueReading
                reader?.readEPC(0)
                isContinueReading = false
                reader?.isContinueReading = isContinueReading
                sendResult(conn)
            }

            "read_tag_continuous" -> {
                isContinueReading = true
                reader?.isContinueReading = isContinueReading
                reader?.readEPC(1)
                while(isContinueReading){
                    sendResult(conn)
                }
            }

            "set_power" -> {
                var ratio = parsedMessage?.get(1)?.toFloat()
                val result = reader?.setSignalPower(ratio)
                if(result != 0) {
                    conn?.send("Error to set antenna power. Code = ${result}")
                }
            }

            "stop_reading" -> {
                isContinueReading = false
                reader?.isContinueReading = isContinueReading
            }

            else -> conn?.send("${message} was re-sent")
        }
    }

    private fun sendResult(conn: WebSocket?) {
        var resultTagList = reader?.getAndClearTagList()
        var resultString =
            "got_epc${FIELD_DIVIDER}${resultTagList?.joinToString(separator = VALUE_DIVIDER)}"
        conn?.send(resultString)
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        // Some actions
    }

    override fun onStart() {
        // Some actions
    }

    fun toneFrequencyFromDistance () {
        //        var i = 1L
//        while(i < 20){
//
//            var j = (500 / i)
//
//            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, j.toInt())
//            Thread.sleep(j)
//            i++
//        }
    }
}