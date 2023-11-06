package kz.ascoa.embeserver

interface IReadDevice {
    var isContinueReading: Boolean
    val deviceName: String;
    var wsServer: WSServer
    var operationType: String
    var epcFilterList: MutableList<String>
    var locatingEpc: String

    fun connectDevice(deviceName: String = ""): String
    fun disconnectDevice()
    fun getAndClearTagList(): MutableList<RfTagData>
    fun getAvailableDeviceNameList(): MutableList<String>
    fun getConnectedDeviceName(): String
    fun getTagDistance(): Int
    fun LocateTag(locatingEpc: String)
    fun readEPC(mode: Int, epcFilter: MutableList<String>, )
    fun reconnectDevice(deviceName: String = ""): String
    fun rewriteTag(oldEpc: String, newEpc: String )
    fun setConnectedDevice(deviceName: String): String
    fun setOperationType(operationType: String, locatingEpc: String)
    fun setSignalPower(ratio: Float? = 100f) : Int



}