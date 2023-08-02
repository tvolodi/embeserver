package kz.ascoa.embeserver

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import android.widget.Toast
import com.pda.rfid.EPCModel
import com.pda.rfid.IAsynchronousMessage
import com.pda.rfid.uhf.Tag6C
import com.pda.rfid.uhf.UHF
import com.pda.rfid.uhf.UHFReader
import com.port.Adapt
import java.lang.Exception
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.pow
import kotlin.math.roundToInt

class HopelandRfidReader (val context: Context, val toneGenerator: ToneGenerator)  : IAsynchronousMessage {

    var isConnected: Boolean = false
    var isContinueReading: Boolean = false
    val deviceName = "Hopeland HY820"
    var maxPowerValue: Int = -1
    var minPowerValue: Int = -1

    var uhfReader: UHF? = null

    var wsServer: WSServer? = null

    // var outPutEpcList: List<EPCModel> = listOf()
    // val outPutEpcListLock: Object = Object()

    var listLock = ReentrantLock()
    var tagList: List<EPCModel?> = mutableListOf()

    var currentAntNumber = 1
    private var readerAntCount : Int = 1

    val lToneGenerator: ToneGenerator = toneGenerator

    fun deviceDisconnect() {
        // uhfReader?.CloseConnect()
        UHFReader._Config.CloseConnect()
        // UHFReader.
        isConnected = false
    }

    fun deviceConnect() : ConnectResults {
        try {
            Adapt.init( context)
            // Thread.sleep(50)
            Adapt.enablePauseInBackGround(context)
            // Thread.sleep(50)
            uhfReader = UHFReader.getUHFInstance()

            if(uhfReader == null)
                return ConnectResults.UnknownError

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

             return ConnectResults.Success

        } catch (e: Exception){
            Log.d("Error", "Connect to UHF device: " + e.stackTrace.toString())
            // TODO
            Toast.makeText(context, "Error to connect to device: ${e.stackTrace.toString()}", Toast.LENGTH_SHORT).show()
            return ConnectResults.UnknownError
        }
    }

    // @Synchronized
    override fun OutPutEPC(p0: EPCModel?) {
        val epcStr = p0?._EPC
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
        // Toast.makeText(context, "EPC: ${someDataStr}", Toast.LENGTH_SHORT).show()
        // wsServer?.got_epc(epcStr)
        listLock.lock()
        tagList += p0
        listLock.unlock()
    }

    fun getAndClearTagList(): List<EPCModel?> {
        listLock.lock()
        var result = tagList
        tagList = mutableListOf()
        listLock.unlock()
        return result
    }

    /**
     * Set antenna power. Value - %% from max power. 0 - MIN (0dBm), 100 - MAX (30dBm).
     */
    fun setSignalPower(ratio: Float? = 100f) : Int {
        var lRatio = ratio?: 100f
        var value = (maxPowerValue * lRatio).roundToInt()
        var i = 1
        var result: Int = -1
        deviceConnect()
        while(i <= readerAntCount) {

            result = UHFReader._Config.SetANTPowerParam(i, value)
            if(result != 0)
                break
            i++
        }
        deviceDisconnect()
        return result
    }

    fun readEPC(mode: Int) {
        deviceConnect()
        while(isContinueReading){
            var result = UHFReader._Tag6C.GetEPC(1, mode)
            Thread.sleep(50)
            if(mode == 0) break
        }
        deviceDisconnect()
    }

    fun getTagDistance(): Int {
        isContinueReading = true
        readEPC(0)
        isContinueReading = false
        Thread.sleep(50)
        var tagList = getAndClearTagList()
        if(tagList.count() == 0)
            return 0
        var tagInfo = tagList.last()
        var result = calcDistanceByTagRssi(tagInfo?._RSSI)
        return result
    }

    private fun calcDistanceByTagRssi(rssi: Byte?) : Int {
        if (rssi == null)
            return 0

        try
        {
            var rssiDbl = rssi.toDouble()
            if (rssiDbl == 0.0) return 1;
            var ratio = rssiDbl / maxPowerValue;
            if (ratio < 1.0)
                return ratio.pow(10).toInt()
            var distance =  0.89976 * ratio.pow( 7.7095) + 0.111;
            return (distance / 60).toInt();
        }
        catch (e: Exception)
        {
            return 1;
        }
    }
}

enum class ConnectResults {
    Success,
    ReaderRegionNotConfigured,
    ConnectionPasswordError,
    BatchModeInProgress,
    UnknownError
}