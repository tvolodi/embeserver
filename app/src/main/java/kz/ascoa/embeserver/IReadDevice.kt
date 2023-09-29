package kz.ascoa.embeserver

interface IReadDevice {
    var isContinueReading: Boolean
    val deviceName: String;
    var wsServer: WSServer
    var operationType: String
    var epcFilterList: MutableList<String>
    var locatingEpc: String

    fun connectDevice(): String
    fun deviceDisconnect()
    fun getAndClearTagList(): MutableList<RfTagData>
    fun getTagDistance(): Int
    fun LocateTag(locatingEpc: String)
    fun readEPC(mode: Int, epcFilter: MutableList<String>, )
    fun rewriteTag(oldEpc: String, newEpc: String )
    fun setOperationType(operationType: String, locatingEpc: String)
    fun setSignalPower(ratio: Float? = 100f) : Int



}