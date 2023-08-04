package kz.ascoa.embeserver

import android.content.Context
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        Handler(Looper.getMainLooper()).postDelayed({
            this.start()
        }, 1000)
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
            WSCommands.TEST -> conn?.send("Test passed")

            WSCommands.GET_TAG_DISTANCE -> {
                var distance = reader?.getTagDistance()
                var message  = "got_tag_distance${FIELD_DIVIDER}${distance}"
                conn?.send(message)
            }

            WSCommands.READ_TAG -> {
                isContinueReading = true
                reader?.isContinueReading = isContinueReading
                reader?.readEPC(0)
                isContinueReading = false
                reader?.isContinueReading = isContinueReading
                Thread.sleep(50)
                sendTagReadResult(conn)
            }

            WSCommands.READ_TAG_CONTINUOUS -> {
                isContinueReading = true
                reader?.isContinueReading = isContinueReading
                reader?.readEPC(1)
                readTagsUntilCancel(conn)
            }

            WSCommands.SET_POWER -> {
                var ratio = parsedMessage?.get(1)?.toFloat()
                val result = reader?.setSignalPower(ratio)
                if(result != 0) {
                    conn?.send("Error to set antenna power. Code = ${result}")
                }
            }

            WSCommands.STOP_READING -> {
                isContinueReading = false
                reader?.isContinueReading = isContinueReading
            }

            else -> conn?.send("${message} was re-sent")
        }
    }

    private fun readTagsUntilCancel(conn: WebSocket?){
        GlobalScope.launch {
            while(isContinueReading){
                sendTagReadResult(conn)
                delay(50)
            }
            reader?.deviceDisconnect()
        }
    }

    private fun sendTagReadResult(conn: WebSocket?) {
        var resultTagList = reader?.getAndClearTagList()
        // Select EPC from each list item and concatenate into string
        val epcList = resultTagList?.map { it?._EPC }
        val tagString = epcList?.joinToString( separator = VALUE_DIVIDER)
        var resultString =
            "${Responses.GOT_EPC}${FIELD_DIVIDER}${tagString}"
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

object WSCommands {
    const val TEST = "test"
    const val GET_TAG_DISTANCE = "get_tag_distance"
    const val READ_TAG = "read_tag"
    const val READ_TAG_CONTINUOUS = "read_tag_continuous"
    const val SET_POWER = "set_power"
    const val STOP_READING = "stop_reading"
}

object Responses {
    val GOT_TAG_DISTANCE = "got_tag_distance"
    const val GOT_EPC = "got_epc"
}