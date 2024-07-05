package kz.ascoa.embeserver

import android.content.Context
import android.media.ToneGenerator
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kz.ascoa.embeserver.Dividers.FIELD_DIVIDER
import kz.ascoa.embeserver.Dividers.VALUE_DIVIDER
import java.net.InetSocketAddress

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import kotlin.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class WSServer(
    socketAddress: InetSocketAddress,
    val context: Context,
    private val reader: IReadDevice?,
    private val toneGenerator: ToneGenerator,
    private val driverService: DriverService
) : WebSocketServer(socketAddress,) {

    private val keepAliveExecutor = Executors.newScheduledThreadPool(1)

    private var clientHandshake: ClientHandshake? = null

    private var clientConnectionList : MutableList<WebSocket?> = mutableListOf()

    private var isContinueReading = false

    private var isOneTagRequired = false

    init{
        isReuseAddr = true;
        reader?.wsServer = this
        startKeepAliveRoutine()
    }

    private var wsConnection: WebSocket? = null
    private fun startKeepAliveRoutine() {
        keepAliveExecutor.scheduleAtFixedRate({
            connections.forEach { ws ->
                if (ws.isOpen) {
                    ws.sendPing() // This is a method you would implement to send a WebSocket ping
                }
            }
        }, 0, 30, TimeUnit.SECONDS) // Sends a ping every 30 seconds
    }
    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {

        wsConnection = conn
        clientHandshake = handshake
        var isConnRegistered = clientConnectionList.contains(conn);
        if(!isConnRegistered) clientConnectionList += conn;

        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
//        clientConnectionList -= conn
        wsConnection = null

    }

    /**
     * Message format: operation_name::
     * operation_name
     * continuous_read_tag
     */
    @OptIn(DelicateCoroutinesApi::class)
    override fun onMessage(conn: WebSocket?, message: String?) {

        val parsedMessageList = message?.split(FIELD_DIVIDER)
        val operationName = parsedMessageList?.get(0)
        val paramValues: MutableList<String> = mutableListOf()
        if(parsedMessageList?.count()!! >= 2) {
            val operationParamString = parsedMessageList.get(1)
            paramValues.addAll(operationParamString.split(Dividers.VALUE_DIVIDER))
        }
        wsConnection = conn
    when (operationName) {
        WSCommands.TEST -> conn?.send("Test passed")

        WSCommands.CONNECT_TO_DEVICE -> {
            GlobalScope.launch {
                try{
                    var deviceName = ""
                    if(paramValues.isNotEmpty()) {
                        deviceName = paramValues[0]
                    }
                    reader?.connectDevice(deviceName)
                } catch (e: Exception){
                    sendErrorMessage("WSServer.onMessage.CONNECT_TO_DEVICE", e.message, e.stackTraceToString())
                }
            }
        }

        WSCommands.DISCONNECT_DEVICE -> {
            GlobalScope.launch {
                try{
                    reader?.disconnectDevice()
                    sendMessage("${Responses.GOT_CONNECTED_DEVICE_NAME}${Dividers.FIELD_DIVIDER}${""}")
                } catch (e: Exception) {
                    sendErrorMessage("WSServer.onMessage.Disconnect_Device", e.message, e.stackTraceToString())
                }
            }
        }

        WSCommands.DISCONNECT_WS -> {
            isContinueReading = false
            reader?.isContinueReading = isContinueReading
            conn?.close()
        }

        WSCommands.GET_AVAILABLE_DEVICE_NAME_LIST -> {
            val deviceNameList = reader?.getAvailableDeviceNameList()
            val nameListString = deviceNameList?.joinToString(Dividers.VALUE_DIVIDER)
            conn?.send("${Responses.GOT_AVAILABLE_DEVICE_NAME_LIST}${Dividers.FIELD_DIVIDER}${nameListString}")
        }

        WSCommands.GET_CONNECTED_DEVICE_NAME -> {
            try {
                val connectedDeviceName = reader?.getConnectedDeviceName()
                conn?.send("${Responses.GOT_CONNECTED_DEVICE_NAME}${Dividers.FIELD_DIVIDER}${connectedDeviceName}")
            } catch (e: Exception) {
                sendErrorMessage("WSServer.onMessage.GET_CONNECTED_DEVICE_NAME", e.message, e.stackTraceToString())
            }
        }

        WSCommands.GET_TAG_DISTANCE -> {
            isContinueReading = true
            reader?.isContinueReading = isContinueReading
            while(isContinueReading) {
                val distance = reader?.getTagDistance()
                val message  = "got_tag_distance${FIELD_DIVIDER}${distance}"
                conn?.send(message)
            }
        }

        WSCommands.LOCATE_TAG -> {
            val tagEpc = paramValues[0]
            if(tagEpc == "undefined"){
                sendErrorMessage("WSCommand.LOCATE_TAG", "undefined EPC for location", "")
                return
            }
            isContinueReading = true
            reader?.isContinueReading = true
            try {
                reader?.LocateTag(paramValues[0] ?: "")
            } catch (e:Exception) {
                conn?.send(e.message)
            }
        }

        WSCommands.READ_TAG -> {
            isOneTagRequired = true
            isContinueReading = true
            reader?.isContinueReading = isContinueReading
            GlobalScope.launch {
                try {
                    reader?.readEPC(0, paramValues)
                } catch (e: Exception) {
                    sendErrorMessage("Read_Tag", e.message, e.stackTraceToString())
                }
            }
        }

        WSCommands.READ_TAG_CONTINUOUS -> {
            val paramList = parsedMessageList[1].split(Dividers.VALUE_DIVIDER)
            val power = paramList[0]
            reader?.setSignalPower(power.toFloat())
            Thread.sleep(125)

            isContinueReading = true
            isOneTagRequired = false
            reader?.isContinueReading = isContinueReading
            GlobalScope.launch {
                reader?.readEPC(1, paramValues)
            }
        }

        WSCommands.SET_OPERATION_TYPE -> {
            var paramList = parsedMessageList[1].split(Dividers.VALUE_DIVIDER)
            val operationType = paramList[0]
            val epcFilterList: MutableList<String> = mutableListOf()
            if(paramList.count() >= 2 ) {
                val epics = paramList[1]
                val epcList = epics.split(Dividers.SUBVALUE_DIVIDER)
                epcList.forEach {
                    epcFilterList += it
                }
            }
            reader?.setOperationType(operationType, epcFilterList[0])
        }

        WSCommands.SET_POWER -> {
            try{
                val ratio = parsedMessageList[1].toFloat()
                val result = reader?.setSignalPower(ratio)
                if(result != 0) {
                    conn?.send("Error to set antenna power. Code = $result")
                }}
            catch (e: Exception){
                sendErrorMessage("WSServer.onMessage.SET_POWER", e.message, e.stackTraceToString())
            }
        }

        WSCommands.STOP_READING -> {
            isContinueReading = false
            reader?.isContinueReading = isContinueReading
        }

        else -> conn?.send("$message was re-sent")
    }
}

// Group data by EPC code => take a reading with the most strong signal
// If reading of one tag then it takes the EPC with the most strong signal
fun sendReadingResult(tagList: MutableList<RfTagData>) {
    try {
        // var resultTagList = reader?.getAndClearTagList()

        val groupedResultsMap: MutableMap<String, RfTagData> = mutableMapOf()

        var tagWithMaxValue: RfTagData = RfTagData("", -128, 0)

        tagList.forEach {
            var epc = it.epc
            if(epc == null) epc = ""
            if(groupedResultsMap.containsKey(epc)){
                val rfTagData = groupedResultsMap[epc]
                if(rfTagData?.rssi!! < it.rssi!!) {
                    rfTagData.rssi = it.rssi
                    tagWithMaxValue = rfTagData
                }

                if(rfTagData.relativeDistance!! < it.relativeDistance!!) rfTagData.relativeDistance = it.relativeDistance

            } else {
                val rfTagData = RfTagData(epc, it.rssi, it.relativeDistance)
                groupedResultsMap[epc] = rfTagData
            }
            val compResult = tagWithMaxValue.rssi!!.compareTo(it.rssi!!)
            if(compResult < 0) {
                tagWithMaxValue = it
            }
        }

        val epcList: List<RfTagData> = if(!isOneTagRequired){
            groupedResultsMap.values.toList()
        } else mutableListOf(tagWithMaxValue)

        //        val epcList = tagList.groupBy { it?.epc }?.mapValues { item -> item.value.map{it?.rssi}.toSet() }
        var epcDataList: List<String> = mutableListOf()
        epcList.forEach { item ->
            val epc = item.epc
            val maxRssi = item.rssi
            val relatedDistance = item.relativeDistance
            val epcDataString = "${epc}${Dividers.SUBVALUE_DIVIDER}${maxRssi}${Dividers.SUBVALUE_DIVIDER}${relatedDistance}"
            epcDataList += epcDataString
        }
        val tagList = epcDataList.joinToString( separator = VALUE_DIVIDER).toString()
        val message = "${Responses.GOT_EPC}${Dividers.FIELD_DIVIDER}${tagList}"


        sendMessage(message)
//            clientConnectionList.forEach{
//                it?.send(message)
//            }

    } catch (e: Exception) {
        sendErrorMessage("sendReadingResult", e.message, e.stackTraceToString())
    }
}

fun sendErrorMessage(location: String?, errMessage: String?, stackTrace: String?){
    val errorContent = "${location}${Dividers.VALUE_DIVIDER}${errMessage}${Dividers.VALUE_DIVIDER}${stackTrace}"
    val message = "${Responses.ERROR}${Dividers.FIELD_DIVIDER}${errorContent}"
    connections.forEach {
        it.send(message)
    }
}

fun sendMessage(message: String?){
        connections.forEach {
            it.send(message)
        }
    }


    override fun onError(conn: WebSocket?, ex: Exception?) {

        if(ex?.message == "Address already in use") {
            driverService.showToastMessage("Error: WSServer ${ex.message}")
            return
        }
        // Some actions
        connections.forEach {
            it.send(ex?.message)
        }

        driverService.showToastMessage("Error: WSServer ${ex!!.stackTraceToString()}")
    }

    override fun onStart() {

    }

}

object WSCommands {
    const val TEST = "test"
    const val CONNECT_TO_DEVICE="connect_to_device"
    const val DISCONNECT_DEVICE="disconnect_device"
    const val DISCONNECT_WS="disconnect_ws"
    const val GET_TAG_DISTANCE = "get_tag_distance"
    const val GET_AVAILABLE_DEVICE_NAME_LIST="get_available_device_name_list"
    const val GET_CONNECTED_DEVICE_NAME="get_connected_device_name"
    const val LOCATE_TAG = "locate_tag"
    const val READ_TAG = "read_tag"
    const val READ_TAG_CONTINUOUS = "read_tag_continuous"
    const val SET_POWER = "set_power"
    const val STOP_READING = "stop_reading"
    const val SET_OPERATION_TYPE = "operation_type"
}

object Responses {
    const val GOT_AVAILABLE_DEVICE_NAME_LIST = "got_available_device_name_list"
    const val GOT_CONNECTED_DEVICE_NAME = "got_connected_device_name"
    const val GOT_TAG_DISTANCE = "got_tag_distance"
    const val GOT_EPC = "got_epc"
    const val ERROR = "error"

}

object OperationTypes {
    const val INVENTORY = "inventory"
    const val LOCATION = "location"
}