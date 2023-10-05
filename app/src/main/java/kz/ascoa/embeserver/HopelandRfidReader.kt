package kz.ascoa.embeserver

import android.content.Context
import android.media.ToneGenerator
import com.pda.rfid.EPCModel
import com.pda.rfid.IAsynchronousMessage
import com.pda.rfid.uhf.UHF
import com.pda.rfid.uhf.UHFReader
import com.port.Adapt
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.locks.ReentrantLock
import kotlin.Exception
import kotlin.math.pow
import kotlin.math.roundToInt

class HopelandRfidReader (val context: Context,
                          val toneGenerator: ToneGenerator,
                          override val deviceName: String,
                          override var operationType: String,
                          override var epcFilterList: MutableList<String>,
                          override var locatingEpc: String
)  : IAsynchronousMessage, IReadDevice {

    var readingMode: Int = 0;
    var isConnected: Boolean = false
    override var isContinueReading: Boolean = false
//    val deviceName = "Hopeland HY820"
    var maxPowerValue: Int = -1
    var minPowerValue: Int = -1

    var readerDeviceName: String = ""

    var uhfReader: UHF? = null

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
        // uhfReader?.CloseConnect()
        UHFReader._Config.CloseConnect()
        // UHFReader.
        isConnected = false
    }

    override fun connectDevice(deviceName: String) : String {
        try {
            readerDeviceName = deviceName
            Adapt.init( context)
            // Thread.sleep(50)
            Adapt.enablePauseInBackGround(context)
            // Thread.sleep(50)
            uhfReader = UHFReader.getUHFInstance()

            if(uhfReader == null)
                return "Cannot connect to reader device"

            var res = uhfReader?.OpenConnect(false, this)
            Thread.sleep(500)
            uhfReader!!.SetFrequency("4")
            // Thread.sleep(50)

            var propertyStr = uhfReader!!.GetReaderProperty()
            var propertyList = propertyStr.split("|")
            var hmPower = mapOf<Int, Int>(1 to 1, 2 to 3, 3 to 7, 4 to 15)
            maxPowerValue = propertyList[1].toInt()
            minPowerValue = 0
            readerAntCount = propertyList[2].toInt()
            currentAntNumber = hmPower[readerAntCount]!!

//            val toneGenerator: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 99)
//            toneGenerator.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 750)

            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 3000)

            // Thread.sleep(50)
            if(readerDeviceName == "") readerDeviceName = "Hopeland RFID Handheld"
            wsServer.sendMessage("${Responses.GOT_CONNECTED_DEVICE_NAME}${Dividers.FIELD_DIVIDER}${readerDeviceName}")
            return ""

        } catch (e: Exception){
//            Log.d("Error", "Connect to UHF device: " + e.stackTrace.toString())
//            val errorMsg = "Error to connect to device: ${e.stackTrace.toString()}"
//            Toast.makeText(context, "Error to connect to device: ${e.stackTrace.toString()}", Toast.LENGTH_SHORT).show()
            wsServer.sendErrorMessage("connectDevice", e.message, e.stackTraceToString())
            return ""
        }
    }

    // @Synchronized
    override fun OutPutEPC(p0: EPCModel?) {
        val epcStr = p0?._EPC
        if(operationType == OperationTypes.INVENTORY) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
        }

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
        return mutableListOf("")
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
        var i = 1
        var result: Int = -1
        connectDevice()
        while(i <= readerAntCount) {

            result = UHFReader._Config.SetANTPowerParam(i, value)
            if(result != 0)
                break
            i++
        }
        disconnectDevice()
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

        operationType = OperationTypes.INVENTORY
        epcFilterList = epcFilter

        launch {
            try {
                connectDevice()
            } catch (e: Exception) {
                wsServer.sendErrorMessage("readEpc", e.message, e.stackTraceToString())
                return@launch
            }

            var result = UHFReader._Tag6C.GetEPC(1, mode)
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
            disconnectDevice()
        }

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

        operationType = OperationTypes.INVENTORY
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

            var result = UHFReader._Tag6C.GetEPC_MatchEPC(1, readingMode, epcCode)
            while(isContinueReading){
                try {
                    delay(50)

                    listLock.lock()
                    val tagArray = tagList
                    wsServer.sendReadingResult(tagList)

                    // rssi == -1000 -> no beep
                    // if no tag read -> no beep
                    if(tagList.count() == 0 ) toneGeneratorRunnable.rssi = 1000
                    else {
                        val maxRssiItem = tagList.maxBy { it.rssi!!.toByte() }
                        if(maxRssiItem != null) {
                            toneGeneratorRunnable.rssi = maxRssiItem.rssi!!.toInt()
                        } else {
                            toneGeneratorRunnable.rssi = -1000
                        }
                    }
                    tagList.clear()
                    listLock.unlock()

                    if(readingMode == 0) break
                } catch (e: Exception) {
                    wsServer.sendErrorMessage("readEpc", e.message, e.stackTraceToString())
                    break
                }
            }
            disconnectDevice()
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
        var rssi = -1000

        fun shutdown() {
            isToRun = false
        }

//        fun setRssi(rssiPar: Int){
//            rssi = rssiPar
//        }

        override fun run() {
            while(isToRun) {
                if (rssi > -1000 ) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    beepPeriod = Math.round(-0.016 * rssi - 0.12)
                    Thread.sleep(beepPeriod)
                }
            }
        }
    }
}

