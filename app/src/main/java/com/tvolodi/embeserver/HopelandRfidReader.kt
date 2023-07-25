package com.tvolodi.embeserver

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import android.widget.Toast
import com.pda.rfid.EPCModel
import com.pda.rfid.IAsynchronousMessage
import com.pda.rfid.uhf.UHF
import com.pda.rfid.uhf.UHFReader
import com.port.Adapt
import java.lang.Exception

class HopelandRfidReader (val context: Context)  : IAsynchronousMessage {

    var isConnected: Boolean = false
    val deviceName = "Hopeland HY820"
    var maxPowerValue: Int = -1
    var minPowerValue: Int = -1

    var uhfReader: UHF? = null

    lateinit var wsServer: WSServer

    var outPutEpcList: List<EPCModel> = listOf()
    val outPutEpcListLock: Object = Object()

    var currentAntNumber = 1
    var readerAntCount = 1

    val toneGenerator: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 99)

    fun deviceDisconnect() {
        // uhfReader?.CloseConnect()
        UHFReader._Config.CloseConnect()
        // UHFReader.
        isConnected = false
    }



    fun deviceConnect(readerDevice: Object?) : ConnectResults {
        try {
            if(isConnected)
                return ConnectResults.Success

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
            // Thread.sleep(50)

            // readEPC()

            return ConnectResults.Success

        } catch (e: Exception){
            Log.d("Error", "Connect to UHF device: " + e.stackTrace.toString())
            Toast.makeText(context, "Error to connect to device: ${e.stackTrace.toString()}", Toast.LENGTH_SHORT).show()
            return  ConnectResults.UnknownError
        }
    }


    override fun OutPutEPC(p0: EPCModel?) {
        val epcStr = p0?._EPC
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
        // Toast.makeText(context, "EPC: ${someDataStr}", Toast.LENGTH_SHORT).show()
        wsServer.got_epc(epcStr)
    }

    fun readEPC() : String {
        var result : String = ""
        UHFReader._Tag6C.GetEPC(1,0)
        return  result
    }
}

enum class ConnectResults {
    Success,
    ReaderRegionNotConfigured,
    ConnectionPasswordError,
    BatchModeInProgress,
    UnknownError
}