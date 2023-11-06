package kz.ascoa.embeserver

import android.content.Context
import android.media.ToneGenerator
import com.clou.uhf.G3Lib.CLReader
import com.clou.uhf.G3Lib.CLReader.CloseConn
import com.clou.uhf.G3Lib.CLReader.GetBT4DeviceStrList
import com.clou.uhf.G3Lib.ClouInterface.IAsynchronousMessage
import com.clou.uhf.G3Lib.Enumeration.eRF_Range
import com.clou.uhf.G3Lib.Enumeration.eReadType
import com.clou.uhf.G3Lib.Protocol.Tag_Model
//import com.clou.uhf.G3Lib.ClouInterface.IAsynchronousMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.locks.ReentrantLock
import kotlin.Exception
import kotlin.math.pow
import kotlin.math.roundToInt

class HopelandRfidBluetoothReader (val context: Context,
                          val toneGenerator: ToneGenerator,
                          override val deviceName: String,
                          override var operationType: String,
                          override var epcFilterList: MutableList<String>,
                          override var locatingEpc: String
)  : IAsynchronousMessage, IReadDevice {

    var readingMode: Int = 0 // 0 - single, 1 - continuous
    var isConnected: Boolean = false
    override var isContinueReading: Boolean = false
//    val deviceName = "Hopeland HY820"
    var maxPowerValue: Int = -1
    var minPowerValue: Int = -1

    var readerDeviceName: String = ""

    override lateinit var wsServer: WSServer

    // var outPutEpcList: List<EPCModel> = listOf()
    // val outPutEpcListLock: Object = Object()

    var listLock = ReentrantLock()
    var tagList: MutableList<RfTagData> = mutableListOf()

    var currentAntNumber = 1
    private var readerAntCount : Int = 1

    val lToneGenerator: ToneGenerator = toneGenerator

    init{
        operationType = OperationTypes.INVENTORY
    }

    override fun disconnectDevice() {
        CLReader.CloseConn(deviceName)
        isConnected = false
    }

    override fun connectDevice(deviceName: String): String {
        try {
            if(deviceName != "") readerDeviceName = deviceName

            if (isConnected) return ""

            var connRes = CLReader.CreateBT4Conn(readerDeviceName, this)
            Thread.sleep(50)
            isConnected = connRes == true
            if (connRes == false ) {
                throw Exception("Cannot connect device")
            }

            CLReader._Config.SetReaderRestoreFactory(readerDeviceName);
            CLReader.SetBeep(readerDeviceName, 1);
            CLReader._Config.SetReaderRF(readerDeviceName, eRF_Range.ETSI_866_to_868MHz);
//            ChangeReaderSettingForSingleReading(true);

            var propertyStr = CLReader.GetReaderProperty(readerDeviceName);
            var propertyArr = propertyStr.split("|");
            var hmPower = mapOf(1 to 1, 2 to 3, 3 to 7, 4 to 15)
            maxPowerValue = propertyArr[1].toInt()
            minPowerValue = 0
            readerAntCount = propertyArr[2].toInt()
            currentAntNumber = hmPower[readerAntCount]!!
//            var maxValue = propertyArr[1].toInt()
//            var minValue = Math.Round(MaxValue * 0.1, MidpointRounding.AwayFromZero);
//            _readerAntennasCount = int.Parse(propertyArr[2]);
//            _currentAntennaNumber = hmPower[_readerAntennasCount];

//            if(uhfReader == null)
//                return "Cannot connect to reader device"
//
//            var res = uhfReader?.OpenConnect(false, this)
//            Thread.sleep(500)
//            uhfReader!!.SetFrequency("4")
            // Thread.sleep(50)



//            val toneGenerator: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 99)
//            toneGenerator.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 750)

            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)

            // Thread.sleep(50)
//            if(readerDeviceName == "") readerDeviceName = "Hopeland RFID BT"
            wsServer.sendMessage("${Responses.GOT_CONNECTED_DEVICE_NAME}${Dividers.FIELD_DIVIDER}${readerDeviceName}")

        } catch (e: Exception){
//            Log.d("Error", "Connect to UHF device: " + e.stackTrace.toString())
//            val errorMsg = "Error to connect to device: ${e.stackTrace.toString()}"
//            Toast.makeText(context, "Error to connect to device: ${e.stackTrace.toString()}", Toast.LENGTH_SHORT).show()
            wsServer.sendErrorMessage("connectDevice", e.message, e.stackTraceToString())
            return "Cannot connect"
        }
        return ""
    }

    // @Synchronized
    override fun OutPutTags(p0: Tag_Model?) {
        val epcStr = p0?._EPC
//        if(operationType == OperationTypes.INVENTORY) {
//            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
//        }

        // Toast.makeText(context, "EPC: ${someDataStr}", Toast.LENGTH_SHORT).show()
        // wsServer?.got_epc(epcStr)
        listLock.lock()
        var newTag = RfTagData(p0?._EPC, p0?._RSSI,)
        tagList += newTag
        listLock.unlock()
    }

    override fun getAndClearTagList(): MutableList<RfTagData> {
        listLock.lock()
        var result = tagList
        tagList = mutableListOf()
        listLock.unlock()
        return result
    }

    override fun getAvailableDeviceNameList(): MutableList<String> {
        var result = CLReader.GetBT4DeviceStrList()
        return result
    }

    override fun getConnectedDeviceName(): String {
        return ""
    }

    /**
     * Set antenna power. Value - %% from max power. 0 - MIN (0dBm), 100 - MAX (30dBm).
     */
    override fun setSignalPower(ratio: Float?) : Int {
        var lRatio = ratio?: 100f
        var value = (maxPowerValue * lRatio).roundToInt()

        var result = 0
        var setPowerResult: String = "0"
        connectDevice()

        // Power params format: 1,50&2,50&3,50
        // 1,2,3 - antenna numbers
        // 50 - power value

        // Set power for the first antenna
        var params = "$currentAntNumber,$value"

        var i = 2
        while(i <= readerAntCount) {
            params += "&$i,$value"
            i++
        }
        setPowerResult = CLReader.SetPower(deviceName, params)
        if(!setPowerResult.startsWith("0")) result = -1

        return result
    }

    override fun rewriteTag(oldEpc: String, newEpc: String) {
        TODO("Not yet implemented")
    }

    override fun setConnectedDevice(deviceName: String): String {
        return ""
    }

    override fun setOperationType(operationTypePar: String, locatingEpcPar: String) {
        operationType = operationTypePar
        locatingEpc = locatingEpcPar
    }

    /*
    Start device read process.
    On read event function OutPutEPC is called as an event handler. It is defined in IAsynchronousMessage interface from Hopeland native library
    Reading result is taken by WSServer itself by calling getAndClearTagList.
     */
    override fun readEPC(mode: Int, epcFilter: MutableList<String>): Unit = runBlocking {

        readingMode = mode

        var hlReadMode = eReadType.Inventory
        if(mode == 0) hlReadMode = eReadType.Single

        operationType = OperationTypes.INVENTORY
        epcFilterList = epcFilter

        launch {
            try {
                connectDevice()
            } catch (e: Exception) {
                wsServer.sendErrorMessage("readEpc", e.message, e.stackTraceToString())
                return@launch
            }

            var result = CLReader._Tag6C.GetEPC(deviceName, 1, hlReadMode)
            while(isContinueReading){
                try {
                    delay(50)

                    listLock.lock()
                    val tagArray = tagList
                    wsServer.sendReadingResult(tagList)
                    tagList.clear()
                    listLock.unlock()

                    if(readingMode == 0) break
                } catch (e: Exception) {
                    wsServer.sendErrorMessage("readEpc", e.message, e.stackTraceToString())
                    break
                }
            }
            CLReader.Stop(deviceName)
            // disconnectDevice()
        }
    }

    override fun reconnectDevice(deviceName: String): String {
        CLReader.CloseConn(deviceName)
        CLReader.CreateBT4Conn(deviceName, this)
        return ""
    }

    override fun getTagDistance(): Int {
        isContinueReading = true
        epcFilterList = mutableListOf(locatingEpc)
        readEPC(0, epcFilterList)
        isContinueReading = false
        Thread.sleep(50)
        var tagList = getAndClearTagList()
        if(tagList.count() == 0) return 0

        var tagInfo = tagList.last()
        var result = calcDistanceByTagRssi(tagInfo?.rssi)
        return result
    }

    override fun LocateTag(epcCode: String) {
        readingMode = 1

        operationType = OperationTypes.LOCATION
        epcFilterList.clear()
        epcFilterList += epcCode

        var toneGeneratorRunnable = ToneGeneratorRunContainer(toneGenerator)
        val toneGenThread = Thread(toneGeneratorRunnable)
        toneGenThread.start()

        GlobalScope.launch {
            try {
                connectDevice()
            } catch (e: Exception) {
                wsServer.sendErrorMessage("readEpc", e.message, e.stackTraceToString())
                return@launch
            }

            var result = CLReader._Tag6C.GetEPC_MatchEPC(deviceName, currentAntNumber, eReadType.Inventory, epcCode)
            while(isContinueReading){
                try {
                    delay(100)

                    listLock.lock()
                    val tagArray = tagList
                    wsServer.sendReadingResult(tagList)

                    // rssi == -1000 -> no beep
                    // if no tag read -> no beep
                    if(tagList.count() == 0 ) toneGeneratorRunnable.rssi = 10
                    else {
                        val maxRssiItem = tagList.maxBy { it.rssi!!.toByte() }
                        if(maxRssiItem != null) {
                            toneGeneratorRunnable.rssi = maxRssiItem.rssi!!.toInt()
                        } else {
                            toneGeneratorRunnable.rssi = 10
                        }
                    }
                    tagList.clear()
                    listLock.unlock()

                    if(readingMode == 0) break
                } catch (e: Exception) {
                    wsServer.sendErrorMessage("readEpc", e.message, e.stackTraceToString())
                    toneGeneratorRunnable.shutdown()
                    break
                }
            }
            CLReader.Stop(deviceName)
//            disconnectDevice()
            toneGeneratorRunnable.shutdown()
        }
    }

    private fun beepLocationSound() {
        val maxRssiItem = tagList.maxBy{
            it.rssi!!.toByte()
        }
    }

    private fun calcDistanceByTagRssi(rssi: Byte?) : Int {
        if (rssi == null) return 0
        try
        {
            var rssiDbl = rssi.toDouble()
            if (rssiDbl == 0.0) return 1;

            var ratio = rssiDbl / maxPowerValue;
            if (ratio < 1.0) return ratio.pow(10).toInt()

            var distance =  0.89976 * ratio.pow( 7.7095) + 0.111;
            return (distance / 60).toInt();
        }
        catch (e: Exception)
        {
            return 1;
        }
    }

    class ToneGeneratorRunContainer (val toneGenerator: ToneGenerator): Runnable {

        var isToRun: Boolean = true
        var beepPeriod: Long = 1000L
        var rssi = 10

        fun shutdown() {
            isToRun = false
        }

//        fun setRssi(rssiPar: Int){
//            rssi = rssiPar
//        }

        override fun run() {
            try {
                while(isToRun) {
                    if (rssi > 10 ) {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                        beepPeriod = (-11 * rssi + 1095).toLong()
                        Thread.sleep(beepPeriod)
                    }
                }
            } catch (e: Exception) {
                return
            }

        }
    }

    override fun WriteDebugMsg(p0: String?) {
        wsServer.sendErrorMessage("Write debug msg", p0, "")
    }

    override fun WriteLog(p0: String?) {
        wsServer.sendErrorMessage("Write log", p0, "")
    }

    override fun PortConnecting(p0: String?) {
        isConnected = true
    }

    override fun PortClosing(p0: String?) {
        TODO("Not yet implemented")
    }

//    override fun OutPutTags(p0: Tag_Model?) {
//        TODO("Not yet implemented")
//    }

    override fun OutPutTagsOver() {
        TODO("Not yet implemented")
    }

    override fun GPIControlMsg(p0: Int, p1: Int, p2: Int) {
        TODO("Not yet implemented")
    }

    override fun OutPutScanData(p0: ByteArray?) {
        TODO("Not yet implemented")
    }
}

