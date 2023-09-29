package kz.ascoa.embeserver

import android.media.ToneGenerator
import android.widget.Toast
import com.zebra.rfid.api3.Antennas
import com.zebra.rfid.api3.BATCH_MODE
import com.zebra.rfid.api3.BEEPER_VOLUME
import com.zebra.rfid.api3.ENUM_TRANSPORT
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE
import com.zebra.rfid.api3.INVENTORY_STATE
import com.zebra.rfid.api3.InvalidUsageException
import com.zebra.rfid.api3.MEMORY_BANK
import com.zebra.rfid.api3.OperationFailureException
import com.zebra.rfid.api3.RFIDReader
import com.zebra.rfid.api3.ReaderDevice
import com.zebra.rfid.api3.Readers
import com.zebra.rfid.api3.RfidEventsListener
import com.zebra.rfid.api3.RfidReadEvents
import com.zebra.rfid.api3.RfidStatusEvents
import com.zebra.rfid.api3.SESSION
import com.zebra.rfid.api3.SL_FLAG
import com.zebra.rfid.api3.START_TRIGGER_TYPE
import com.zebra.rfid.api3.STATUS_EVENT_TYPE
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE
import com.zebra.rfid.api3.TagAccess
import com.zebra.rfid.api3.TagDataArray
import com.zebra.rfid.api3.TriggerInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.java_websocket.WebSocket
import java.util.concurrent.locks.ReentrantLock

class ZebraRfidReader(val context: DriverService,
                      val toneGenerator: ToneGenerator,
                      override val deviceName: String,
                      override var operationType: String,
                      override var epcFilterList: MutableList<String>,
                      override var locatingEpc: String, ) : Readers.RFIDReaderEventHandler, IReadDevice {

    public var reader: RFIDReader? = null
    private var readers: Readers? = null
    private var readerDevice: ReaderDevice? = null
    private var availableRFIDReaderList: ArrayList<ReaderDevice>? = null
    private var listLock = ReentrantLock()
    private var tagList: MutableList<RfTagData> = mutableListOf()
    private val readerDeviceName = ""
    private var MAX_POWER = 270

    private var readingMode: Int = 0


    private var eventHandler: EventHandler? = null
    override var isContinueReading: Boolean = false
    override lateinit var wsServer: WSServer


    init {
        operationType = OperationTypes.INVENTORY
        setReaderDevice()
    }

    override fun connectDevice(): String {

        try {
            setReaderDevice()
            val res = tryConnect()
            if (res != ""){
                throw Exception(res)
            }
        } catch (e: Exception) {
            return e.stackTrace.toString()
        }
        return ""
    }

    fun tryConnect(): String {
        var message = ""
        try {
            if(!reader!!.isConnected) {
                reader!!.connect()
                configureReader()
                if (reader!!.isConnected) {
                    return ""
                } else {
                    return "Can't connect to device"
                }
            } else return "" // Reader is connected. Nothing to do.
        } catch (e: Exception) {
            message = "Cannot connect to Zebra RFID device"
            if(e.message != null && e.message != "") message = e.message!!
            wsServer.sendErrorMessage("tryConnect", message, e.stackTrace.toString())
        }
        return message
    }

    private fun setReaderDevice(): String {
        // Based on support available on host device choose the reader type

        val invalidUsageException: InvalidUsageException? = null
        readers = Readers(context, ENUM_TRANSPORT.ALL)
        try {
            availableRFIDReaderList = readers!!.GetAvailableRFIDReaderList()
            if ( availableRFIDReaderList == null) {
                throw Exception("No available readers")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "${e.message}", Toast.LENGTH_LONG).show()
            throw Exception(e.message)
        }

        if (invalidUsageException != null) {
            readers!!.Dispose()
            readers = null
            readers = Readers(context, ENUM_TRANSPORT.ALL)
        }

        if (readers != null) {
            Readers.attach(this)
            try {
                availableRFIDReaderList = readers!!.GetAvailableRFIDReaderList()
                if (availableRFIDReaderList != null) {
                    var listSize = availableRFIDReaderList!!.size
                    if (listSize != 0) {
                        // if single reader is available then connect it
                        if (listSize == 1) {
                            readerDevice = availableRFIDReaderList!!.get(0)
                            reader = readerDevice!!.rfidReader
                        } else {
                            // search reader specified by name
                            for (device in availableRFIDReaderList!!) {
                                if (device.name == readerDeviceName) {
                                    readerDevice = device
                                    reader = readerDevice!!.rfidReader
                                }
                            }
                        }
                    }
                }
            } catch (e: InvalidUsageException) {
                e.printStackTrace()
            } catch (e: OperationFailureException) {
                e.printStackTrace()
                val des = e.results.toString()
                return "Connection failed" + e.vendorMessage + " " + des
            }
        }
        return ""
    }

    private fun configureReader() {
        if (reader!!.isConnected) {
            val triggerInfo = TriggerInfo()
            triggerInfo.StartTrigger.triggerType = START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE
            triggerInfo.StopTrigger.triggerType = STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE
            try {
                // receive events from reader
                if (eventHandler == null) eventHandler = EventHandler()
                var readerEvents = reader!!.Events
                readerEvents.addEventsListener( eventHandler )
                // HH event
                readerEvents.setHandheldEvent(true)
                // tag event with tag data
                readerEvents.setTagReadEvent(true)
                readerEvents.setAttachTagDataWithReadEvent(false)
                // set trigger mode as rfid so scanner beam will not come
                var readerConfig = reader!!.Config

                readerConfig?.beeperVolume = BEEPER_VOLUME.LOW_BEEP
                readerConfig?.setBatchMode(BATCH_MODE.DISABLE)

                readerConfig.setTriggerMode( ENUM_TRIGGER_MODE.RFID_MODE, true )
                // set start and stop triggers
                readerConfig.setStartTrigger(triggerInfo.StartTrigger)
                readerConfig.setStopTrigger(triggerInfo.StopTrigger)
                // power levels are index based so maximum power supported get the last one
                MAX_POWER = reader!!.ReaderCapabilities.getTransmitPowerLevelValues().size - 1
                // set antenna configurations
                val antennaConfig: Antennas.AntennaRfConfig = readerConfig.Antennas.getAntennaRfConfig(1)
                antennaConfig.transmitPowerIndex = MAX_POWER
                antennaConfig.setrfModeTableIndex(0)
                antennaConfig.tari = 0
                readerConfig.Antennas.setAntennaRfConfig(1, antennaConfig)
                // Set the singulation control
                val s1_singulationControl: Antennas.SingulationControl = readerConfig.Antennas.getSingulationControl(1)
                s1_singulationControl.session = SESSION.SESSION_S0
                s1_singulationControl.Action.inventoryState = INVENTORY_STATE.INVENTORY_STATE_A
                s1_singulationControl.Action.slFlag = SL_FLAG.SL_ALL
                readerConfig.Antennas.setSingulationControl(1, s1_singulationControl)
                // delete any prefilters
                reader?.Actions?.PreFilters?.deleteAll()
                //
            } catch (e: InvalidUsageException) {
                e.printStackTrace()
            } catch (e: OperationFailureException) {
                e.printStackTrace()
            }
        }
    }

    override fun setSignalPower(ratio: Float?): Int {
        TODO("Not yet implemented")
    }

    override fun setOperationType(operationType: String, locatingEpcPar: String) {
        this.operationType = operationType
        this.locatingEpc = locatingEpcPar
    }

    override fun readEPC(mode: Int, epcFilter: MutableList<String>): Unit = runBlocking {
        readingMode = mode

        operationType = OperationTypes.INVENTORY
        epcFilterList = epcFilter

        var isReading = false
        try{
            tryConnect()
        } catch (e: Exception) {
            wsServer?.sendErrorMessage("tryConnect",e.message, e.stackTraceToString())
        }

        launch {
            try {
                var tagList2 = mutableListOf<RfTagData>()
                reader?.Actions?.purgeTags()
                reader?.Actions?.Inventory?.perform()
//                val tagInfoList = reader?.Actions?.getReadTags(100)

                while (isContinueReading) {
//                    val tagInfoList = reader?.Actions?.getReadTags(100)
                    delay(100L)
                    val tagArray = reader?.Actions?.getReadTagsEx(1000)
                    if (tagArray != null) {
                        for (tag in tagArray.tags) {
                            if (tag == null) continue
                            if (tag.tagID == null) continue
                            // val relativeDistance = tag.LocationInfo?.relativeDistance;

                            val newTag = RfTagData(epc = tag.tagID, rssi = tag.peakRSSI.toByte(),)
                            listLock.lock()
                            tagList += newTag
                            listLock.unlock()
                        }
                    }
                    launch {
                        listLock.lock()
                        wsServer?.sendReadingResult(tagList)
                        tagList.clear()
                        listLock.unlock()
                    }



                    if (readingMode == 0) {
                        break
                    }
                }
                reader?.Actions?.Inventory?.stop()


//                launch {
//                    wsServer.sendReadingResult(tagList)
//                    tagList.clear()
//                }
            } catch (e: Exception) {
                wsServer?.sendErrorMessage("readEPC", e.message, e.stackTraceToString())
//                try{
//                    reader?.Actions?.Inventory?.stop()
//                } catch(e: Exception){
//                    var errMessage = e.message
//                    if(errMessage == null || errMessage == ""){
//                        errMessage = "Cannot stop reader device"
//                    }
//                    wsServer?.sendErrorMessage(e.message, e.stackTrace.toString())
//                }
            } finally {
            }

//            if (readingMode == 0) {
//                delay(1000L)
//                launch {
//                    wsServer.sendReadingResult(tagList)
//                    tagList.clear()
//                }
//            }
        }
    }

    override fun getTagDistance(): Int {
        TODO("Not yet implemented")
    }

    override fun LocateTag(locatingEpcPar: String): Unit = runBlocking {

        operationType = OperationTypes.LOCATION
        locatingEpc = locatingEpcPar

        var tagDistancesMap: MutableMap<String, MutableSet<Short?>> = mutableMapOf()

        val res = tryConnect()
        if (res != "") {
            throw Exception("Can't connect Zebra")
        }

        launch{
            try {
                reader?.Actions?.TagLocationing?.Perform(locatingEpc, null, null)

                val tagLocationList: MutableList<RfTagData> = mutableListOf()

                while(isContinueReading) {
                    delay(500L)
                    val tagInfoList = reader?.Actions?.getReadTags(100)
                    var minDistance: Short = 0
                    var maxRssi: Byte = -128

                    tagInfoList?.forEach {

                        val lInfo = it.LocationInfo
                        val relativeDistance = lInfo?.relativeDistance;

                        // Distance increase -> relativeDistance decrease. Max relative distance = 100
                        if(minDistance < relativeDistance!!) minDistance = relativeDistance
//                        var epc = it.tagID
                        if(maxRssi < it.peakRSSI) maxRssi = it.peakRSSI.toByte()
                        val rssi = it.peakRSSI
                    }
                    var tagData = RfTagData(locatingEpc, maxRssi, minDistance )
                    tagLocationList += tagData

                    launch {
                        wsServer.sendReadingResult(tagLocationList)
                        tagLocationList.clear()
                    }

                }
            } catch (e: Exception) {
                launch {
                    wsServer?.sendErrorMessage("LocateTag", e.message, e.stackTraceToString())
                }
            } finally {
                reader?.Actions?.TagLocationing?.Stop()
            }
        }

    }

    override fun rewriteTag(oldEpc: String, newEpc: String): Unit = runBlocking {
        try{
            // From API docs
            // WriteAccessParams(MEMORY_BANK m_eMemoryBank, int m_nByteOffset, int m_nWriteDataLength, long m_nAccessPassword, byte[] m_WriteData)
            val epcLength = newEpc.length / 4;
            var writeAccessParams = TagAccess().WriteAccessParams(MEMORY_BANK.MEMORY_BANK_EPC, 2, epcLength, 0L, byteArrayOf())
            writeAccessParams.setWriteData(newEpc)

            GlobalScope.launch {
                // writeWait(java.lang.String tagID, TagAccess.WriteAccessParams writeAccessParams, AntennaInfo antennaInfo, TagData tagData, boolean bPrefilter, boolean bTIDPrefilter)
                // This method is used to write data to the memory bank of a specific tag.
                try{
                    reader?.Actions?.TagAccess?.writeWait(oldEpc, writeAccessParams, null, null, true, false)
                } catch (e: Exception) {
                    wsServer?.sendErrorMessage("rewriteTag", e.message, e.stackTraceToString())
                }
                wsServer?.sendMessage("Tag rewrited")
            }
        } catch (e: Exception){
            wsServer?.sendErrorMessage("rewriteTag", e.message, e.stackTraceToString())
        }
    }

    override fun deviceDisconnect() {

        try {
            synchronized(this) {
                if(reader != null) {
                    reader?.Events?.removeEventsListener(eventHandler)
                    reader?.disconnect()
                    reader = null
                }
            }
        } catch (e: InvalidUsageException) {
            e.printStackTrace()
        }
    }

    override fun getAndClearTagList(): MutableList<RfTagData> {
        listLock.lock()
        var result = tagList
        tagList = mutableListOf()
        listLock.unlock()
        return result
    }

    override fun RFIDReaderAppeared(readerDevice: ReaderDevice?) {
//        connectReader()
    }

    override fun RFIDReaderDisappeared(readerDevice: ReaderDevice?) {
        if (readerDevice!!.name == reader?.hostName) {
//            disconnect()
        }
    }

    inner class EventHandler : RfidEventsListener {
        // Read Event Notification
        override fun eventReadNotify(e: RfidReadEvents) {
            // Recommended to use new method getReadTagsEx for better performance in case of large tag population
            var tagNumber = 1

            GlobalScope.launch {
//                var tagList2 = mutableListOf<RfTagData>()
//            if(readingMode == 0){
//                tagNumber = 1
//            }
                val tagArray: TagDataArray? = reader?.Actions?.getReadTagsEx(tagNumber)
                if (tagArray != null) {
                    for (tag in tagArray.tags) {
                        if (tag == null) continue
                        if(tag.tagID == null) continue
                        // val relativeDistance = tag.LocationInfo?.relativeDistance;

                        val newTag = RfTagData(epc = tag.tagID, rssi = tag.peakRSSI.toByte(), )
                        listLock.lock()
                        tagList += newTag
                        listLock.unlock()

//                    if (tag.opCode === ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ &&
//                        tag.opStatus === ACCESS_OPERATION_STATUS.ACCESS_SUCCESS
//                    ) {
//                    }
                    }

                    // For continuous mode send data as soon as they are available
//                    if(readingMode == 1) {
//                        launch {
//                            wsServer?.sendReadingResult(tagList2)
//                            tagList2.clear()
//                        }
//                    }
//                if(readingMode == 0) isContinueReading = false
                }
            }

        }

        // Status Event Notification
        override fun eventStatusNotify(rfidStatusEvents: RfidStatusEvents) {
            if (rfidStatusEvents.StatusEventData.statusEventType === STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.handheldEvent === HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    if(operationType == OperationTypes.INVENTORY) {
                        isContinueReading = true
                        readEPC(1, epcFilterList,)

                    } else if (operationType == OperationTypes.LOCATION){
                        LocateTag(locatingEpc)
                    }
//                    object : AsyncTask<Void?, Void?, Void?>() {
//                        protected override fun doInBackground(vararg voids: Void): Void? {
//                            context.handleTriggerPress(true)
//                            return null
//                        }
//                    }.execute()
                }
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.handheldEvent === HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
                    isContinueReading = false
//                    object : AsyncTask<Void?, Void?, Void?>() {
//                        protected override fun doInBackground(vararg voids: Void): Void? {
//                            context.handleTriggerPress(false)
//                            return null
//                        }
//                    }.execute()
                }
            }
        }
    }

//    inner class AsyncDataUpdate :
//        AsyncTask<Array<TagData?>?, Void?, Void?>() {
//        override fun doInBackground(vararg params: Array<TagData?>?): Void? {
//            context.handleTagdata(params[0])
//            return null
//        }
//    }



//    interface ResponseHandlerInterface {
//        fun handleTagdata(tagData: Array<TagData?>?)
//        fun handleTriggerPress(pressed: Boolean) //void handleStatusEvents(Events.StatusEventData eventData);
//    }
}