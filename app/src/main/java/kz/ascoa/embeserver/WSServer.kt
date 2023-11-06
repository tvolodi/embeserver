package kz.ascoa.embeserver

import android.content.Context
import android.media.ToneGenerator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kz.ascoa.embeserver.Dividers.FIELD_DIVIDER
import kz.ascoa.embeserver.Dividers.VALUE_DIVIDER
import java.net.InetSocketAddress

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import kotlin.Exception


class WSServer(
    socketAddress: InetSocketAddress,
    val context: Context,
    val reader: IReadDevice?,
    val toneGenerator: ToneGenerator,
    val driverService: DriverService
) : WebSocketServer(socketAddress,) {

    var clientHandshake: ClientHandshake? = null

    var clientConnectionList : MutableList<WebSocket?> = mutableListOf()

    var isContinueReading = false

    var isOneTagRequired = false

    init{
        isReuseAddr = true;
        reader?.wsServer = this
    }

    var wsConnection: WebSocket? = null
    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {

        wsConnection = conn
        clientHandshake = handshake
        clientConnectionList += conn

        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
    }

//    fun got_epc(epc: String?){
//        val message = "got_epc${FIELD_DIVIDER}${epc}"
//        wsConnection?.send(message)
//    }

    fun got_epc_list(epcList: List<String>){

        val message = "got_epc_list${FIELD_DIVIDER}${epcList.joinToString("$$")}"
        wsConnection?.send(message)
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        clientConnectionList -= conn
        wsConnection = null

    }

    /**
     * Message format: operation_name::
     * operation_name
     * continuous_read_tag
     */
    override fun onMessage(conn: WebSocket?, message: String?) {

        var parsedMessageList = message?.split(FIELD_DIVIDER)
        var operationName = parsedMessageList?.get(0)
        var paramValues: MutableList<String> = mutableListOf()
        if(parsedMessageList?.count()!! >= 2) {
            var operationParamString = parsedMessageList?.get(1)
            if(operationParamString != null) {
                paramValues.addAll(operationParamString.split(Dividers.VALUE_DIVIDER))
            }
        }
        wsConnection = conn
        when (operationName) {
            WSCommands.TEST -> conn?.send("Test passed")

            WSCommands.CONNECT_TO_DEVICE -> {
                GlobalScope.launch {
                    try{
                        var deviceName = ""
                        if(paramValues.count() > 0) {
                            deviceName = paramValues[0]
                        }
                        reader?.connectDevice(deviceName)
                    } catch (e: Exception){
                        if(e.message == ""){

                        }
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
                    var distance = reader?.getTagDistance()
                    var message  = "got_tag_distance${FIELD_DIVIDER}${distance}"
                    conn?.send(message)
                }
            }

            WSCommands.LOCATE_TAG -> {
                val tagEpc = paramValues?.get(0)
                if(tagEpc == "undefined"){
                    sendErrorMessage("WSCommand.LOCATE_TAG", "undefined EPC for location", "")
                    return
                }
                isContinueReading = true
                reader?.isContinueReading = true
                try {
                    reader?.LocateTag(paramValues?.get(0) ?: "")
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
                isContinueReading = true
                isOneTagRequired = false
                reader?.isContinueReading = isContinueReading
                GlobalScope.launch {
                    reader?.readEPC(1, paramValues)
                }
            }

            WSCommands.SET_OPERATION_TYPE -> {
                var paramList = parsedMessageList?.get(1)?.split(Dividers.VALUE_DIVIDER)
                val operationType = paramList?.get(0)
                val epcFilterList: MutableList<String> = mutableListOf()
                if(paramList?.count()!! >= 2 ) {
                    val epcs = paramList?.get(1)
                    val epcList = epcs?.split(Dividers.SUBVALUE_DIVIDER)
                    epcList?.forEach {
                        epcFilterList += it
                    }
                }
                if (operationType != null) {
                    reader?.setOperationType(operationType, epcFilterList[0])
                }
            }

            WSCommands.SET_POWER -> {
                var ratio = parsedMessageList?.get(1)?.toFloat()
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

    // Group data by EPC code => take a reading with the most strong signal
    // If reading of one tag then it takes the EPC with the most strong signal
    fun sendReadingResult(tagList: MutableList<RfTagData>) {
        try {
            // var resultTagList = reader?.getAndClearTagList()
            var result: String = ""

            var grouppedResultsMap: MutableMap<String, RfTagData> = mutableMapOf()

            var tagWithMaxValue: RfTagData = RfTagData("", -128, 0)

            tagList.forEach {
                var epc = it.epc
                if(epc == null) epc = ""
                if(grouppedResultsMap.containsKey(epc)){
                    val rfTagData = grouppedResultsMap[epc]
                    if(rfTagData?.rssi!! < it.rssi!!) {
                        rfTagData.rssi = it.rssi
                        tagWithMaxValue = rfTagData
                    }

                    if(rfTagData?.relativeDistance!! < it.relativeDistance!!) rfTagData.relativeDistance = it.relativeDistance

                } else {
                    val rfTagData = RfTagData(epc, it.rssi, it.relativeDistance)
                    grouppedResultsMap[epc] = rfTagData
                }
                val compResult = tagWithMaxValue.rssi!!.compareTo(it.rssi!!)
                if(compResult < 0) {
                    tagWithMaxValue = it
                }
            }

            var epcList: List<RfTagData>
            if(!isOneTagRequired){
                epcList = grouppedResultsMap.values.toList()
            } else epcList = mutableListOf(tagWithMaxValue)

        //        val epcList = tagList.groupBy { it?.epc }?.mapValues { item -> item.value.map{it?.rssi}.toSet() }
            var epcDataList: List<String> = mutableListOf()
            epcList?.forEach { item ->
                val epc = item.epc
                val maxRssi = item.rssi
                val relatedDistance = item.relativeDistance
                val epcDataString = "${epc}${Dividers.SUBVALUE_DIVIDER}${maxRssi}${Dividers.SUBVALUE_DIVIDER}${relatedDistance}"
                epcDataList += epcDataString
            }
            val tagList = epcDataList.joinToString( separator = VALUE_DIVIDER).toString()
            var message = "${Responses.GOT_EPC}${Dividers.FIELD_DIVIDER}${tagList}"

            connections.forEach{
                it.send(message)
            }

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

//    private fun readTagsUntilCancel(conn: WebSocket?, ){
//        GlobalScope.launch {
//            while(isContinueReading){
//                val tagList = getAndProcessTags(1)
//                var message = "${Responses.GOT_EPC}${Dividers.FIELD_DIVIDER}${tagList}"
//                conn?.send(message)
//                delay(50)
//            }
//            reader?.deviceDisconnect()
//        }
//    }

    /*
    Params:
    mode: 0 - single tag with the strongest signal (by RSSI)
    mode: 1 - continuous - get all read tags
     */
//    private fun getAndProcessTags(mode: Int): String {
//        var resultTagList = reader?.getAndClearTagList()
//        var result: String = ""
//        if(mode == 0) {
//            var maxRssiItem = resultTagList?.maxByOrNull { it?.rssi!! }
//            result = "${maxRssiItem?.epc}${Dividers.SUBVALUE_DIVIDER}${maxRssiItem?.rssi}" // E2000..234;-31
//        } else {
//            val epcList = resultTagList?.groupBy { it?.epc }?.mapValues { item -> item.value.map{it?.rssi}.toSet() }
//            var epcDataList: List<String> = mutableListOf()
//            epcList?.forEach { item ->
//                val epc = item.key
//                val maxRssi = item.value.maxByOrNull { it?.toByte() ?: 0 }
//                val epcDataString = "${epc}${Dividers.SUBVALUE_DIVIDER}${maxRssi}"
//                epcDataList += epcDataString
//            }
//            result = epcDataList.joinToString( separator = VALUE_DIVIDER).toString()
//        }
//
//        return result
//    }

    override fun onError(conn: WebSocket?, ex: Exception?) {

        if(ex?.message == "Address already in use") return
        // Some actions
        connections.forEach {
            it.send(ex?.message)
        }

        driverService.showToastMessage("Error: WSServer ${ex!!.stackTraceToString()}")
    }

    override fun onStart() {

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