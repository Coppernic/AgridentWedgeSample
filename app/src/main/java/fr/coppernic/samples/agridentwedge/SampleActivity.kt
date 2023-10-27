package fr.coppernic.samples.agridentwedge

import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import fr.coppernic.samples.agridentwedge.databinding.ActivitySampleBinding
import fr.coppernic.sdk.agrident.AgridentMessage
import fr.coppernic.sdk.agrident.Ascii
import fr.coppernic.sdk.agrident.Commands
import fr.coppernic.sdk.agrident.CompactCoding
import fr.coppernic.sdk.agrident.DataBlock
import fr.coppernic.sdk.agrident.ISO11784
import fr.coppernic.sdk.agrident.MessageType
import fr.coppernic.sdk.agrident.OnDataReceivedListener
import fr.coppernic.sdk.agrident.Parameters
import fr.coppernic.sdk.agrident.Reader
import fr.coppernic.sdk.agrident.ReaderFactory
import fr.coppernic.sdk.agrident.ReaderInformation
import fr.coppernic.sdk.core.Defines
import fr.coppernic.sdk.power.PowerManager
import fr.coppernic.sdk.power.api.PowerListener
import fr.coppernic.sdk.power.api.peripheral.Peripheral
import fr.coppernic.sdk.power.impl.access.AccessPeripheral
import fr.coppernic.sdk.utils.core.CpcBytes
import fr.coppernic.sdk.utils.core.CpcResult.RESULT
import fr.coppernic.sdk.utils.io.InstanceListener
import timber.log.Timber
import java.io.IOException
import java.io.UnsupportedEncodingException

class SampleActivity : AppCompatActivity(), PowerListener, InstanceListener<Reader>,
    OnDataReceivedListener {
    // RFID Reader interface
    private lateinit var reader: Reader
    private var isPortOpened = false
    private var lastTagId = ""
    private var tagReadCount = 0
    private val isIso11784Format = false
    private val isIso28631Format = false
    private val isAsciiFormat = false
    private val isCompactCoding = false
    private var currentParam: Parameters? = null
    var context: Context? = null
    private lateinit var binding: ActivitySampleBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySampleBinding.inflate(
            layoutInflater
        )
        val view: View = binding.root
        setContentView(view)
        PowerManager.get().registerListener(this)
        AccessPeripheral.RFID_AGRIDENT_ABR200_GPIO.on(this)
        context = this
    }

    override fun onDestroy() {
        super.onDestroy()
        // Releases PowerManager
        PowerManager.get().unregisterAll()
        PowerManager.get().releaseResources()
    }

    // Instance listener
    override fun onCreated(reader: Reader) {
        Timber.d("onCreated")
        this.reader = reader
        reader.setOnDataReceivedListener(this)
    }

    override fun onDisposed(reader: Reader) {}
    override fun onPause() {
        closePort()
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart")
        val intent = Intent()
        intent.setPackage("fr.coppernic.tools.cpcagridentwedge")
        intent.component = ComponentName(
            "fr.coppernic.tools.cpcagridentwedge",
            "fr.coppernic.tools.cpcagridentwedge.service.WedgeService"
        )
        intent.action = ACTION_SERVICE_STOP
        startService(intent)
        SystemClock.sleep(1000)
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        AccessPeripheral.RFID_AGRIDENT_ABR200_GPIO.on(this)

        with(binding) {
            btnOpenClose.setOnClickListener { OpenClose() }
            btnClear.setOnClickListener { clear() }
            btnGetFirmware.setOnClickListener { displayFirmware() }
            btnGetSerialNumber.setOnClickListener { displaySN() }
            btnGetAmplitude.setOnClickListener { displayAmplitude() }
            btnGetRssi.setOnClickListener { displayRSSI() }
            btnGetFDXRssi.setOnClickListener { displayFdxRssi() }
            btnGetHDXRssi.setOnClickListener { displayHdxRssi() }
            btnGetHDXFreq.setOnClickListener { displayHdxFreq() }
            btnEditOutput.setOnClickListener { editOutput() }
            btnGetOutput.setOnClickListener { outputFormat }
            btnEditTagType.setOnClickListener { editTagType() }
            btnGetTagType.setOnClickListener { tagType }
            btnEditTimeout.setOnClickListener { editTimeout() }
            btnGetTimeout.setOnClickListener { timeout }
            btnEditBaudrate.setOnClickListener { editBaudrate() }
            btnGetBaudrate.setOnClickListener { baudrate }
            btnEditDelayTime.setOnClickListener { editDelayTime() }
            btnGetDelayTime.setOnClickListener { delayTime }
            swRFField.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean ->
                    SwitchRfChange()
            }
        }
    }

    fun OpenClose() {
        if (reader != null) {
            if (!isPortOpened) {
                val baudRate = Integer.valueOf(binding.spBaudrate.selectedItem.toString())
                val res = reader.open(Defines.SerialDefines.AGRIDENT_READER_PORT, baudRate)
                if (res == RESULT.OK) {
                    binding.btnOpenClose.setText(R.string.close)
                    isPortOpened = true
                    if (RESULT.OK != reader.sendCommand(Commands.GET_RF_ACTIVATION_CMD)) {
                        addLog("Fail to get RFID field state", false)
                    }
                } else {
                    addLog("Open error", false)
                }
            } else {
                reader.close()
                binding.btnOpenClose.setText(R.string.open)
                isPortOpened = false
            }
        }
    }

    fun clear() {
        binding.tvMessage.text = ""
    }

    fun displayFirmware() {
        reader.sendCommand(Commands.FIRMWARE_CMD)
    }

    fun displaySN() {
        reader.sendCommand(Commands.SNR_CMD)
    }

    fun displayAmplitude() {
        reader.sendCommand(Commands.GET_AMPLITUDE_CMD)
    }

    fun displayRSSI() {
        reader.sendCommand(Commands.GET_RSSI_CMD)
    }

    fun displayFdxRssi() {
        reader.sendCommand(Commands.GET_AVERAGE_FDX_RSSI_CMD)
    }

    fun displayHdxRssi() {
        reader.sendCommand(Commands.GET_AVERAGE_HDX_RSSI_CMD)
    }

    fun displayHdxFreq() {
        reader.sendCommand(Commands.GET_AVERAGE_HDX_FRQ_CMD)
    }

    fun editOutput() {
        showDialog(
            Parameters(Parameters.OUTPUT_FORMAT),
            binding.tvOutput,
            OutputFormat.ASCII,
            "Output Format"
        )
    }

    val outputFormat: Unit
        get() {
            currentParam = Parameters(Parameters.OUTPUT_FORMAT)
            reader.getConfig(Parameters(Parameters.OUTPUT_FORMAT))
        }

    fun editTagType() {
        showDialog(Parameters(Parameters.TAG_TYPE), binding.tvTagType, TagTypes.FDX_B, "Tag Type")
    }

    val tagType: Unit
        get() {
            currentParam = Parameters(Parameters.TAG_TYPE)
            reader.getConfig(Parameters(Parameters.TAG_TYPE))
        }

    fun editTimeout() {
        showDialog(
            Parameters(Parameters.TIMING),
            binding.tvTimeout,
            Timing.VARIABLE_TIMING,
            "Timing"
        )
    }

    val timeout: Unit
        get() {
            currentParam = Parameters(Parameters.TIMING)
            reader.getConfig(Parameters(Parameters.TIMING))
        }

    fun editBaudrate() {
        showDialog(
            Parameters(Parameters.BAUDRATE),
            binding.tvBaudrate,
            BaudRate.B9600,
            "BaudRate"
        )
    }

    val baudrate: Unit
        get() {
            currentParam = Parameters(Parameters.BAUDRATE)
            reader.getConfig(Parameters(Parameters.BAUDRATE))
        }

    fun editDelayTime() {
        showDialog(Parameters(Parameters.DELAYTIME), binding.tvDelayTime, null, "Delay Time")
    }

    val delayTime: Unit
        get() {
            currentParam = Parameters(Parameters.DELAYTIME)
            reader.getConfig(Parameters(Parameters.DELAYTIME))
        }

    fun SwitchRfChange() {
        if (binding.swRFField.isChecked) {
            reader.sendCommand(Commands.SET_RF_ON_CMD)
        } else {
            reader.sendCommand(Commands.SET_RF_OFF_CMD)
        }
    }

    fun showDialog(param: Parameters, tv: TextView, enumObject: Any?, title: String?) {
        val builder = AlertDialog.Builder(this)
        val input = EditText(this)
        val spinner = Spinner(this)
        //display spinner depending of parameter
        if (enumObject == null) {
            input.setText(tv.text)
        } else if (enumObject is OutputFormat) {
            val adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_item, OutputFormat.values())
            spinner.adapter = adapter
            try {
                val output = OutputFormat.valueOf(tv.text.toString())
                val spinnerPosition = adapter.getPosition(output)
                if (spinnerPosition >= 0) {
                    spinner.setSelection(spinnerPosition)
                }
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        } else if (enumObject is TagTypes) {
            val adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_item, TagTypes.values())
            spinner.adapter = adapter
            try {
                val tagTypes = TagTypes.valueOf(tv.text.toString())
                val spinnerPosition = adapter.getPosition(tagTypes)
                if (spinnerPosition >= 0) {
                    spinner.setSelection(spinnerPosition)
                }
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        } else if (enumObject is BaudRate) {
            val adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_item, BaudRate.values())
            spinner.adapter = adapter
            try {
                val baudRate = BaudRate.valueOf(tv.text.toString())
                val spinnerPosition = adapter.getPosition(baudRate)
                if (spinnerPosition >= 0) {
                    spinner.setSelection(spinnerPosition)
                }
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        } else if (enumObject is Timing) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, Timing.values())
            spinner.adapter = adapter
            try {
                val timing = Timing.valueOf(tv.text.toString())
                val spinnerPosition = adapter.getPosition(timing)
                if (spinnerPosition >= 0) {
                    spinner.setSelection(spinnerPosition)
                }
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        }
        if (enumObject == null) {
            builder.setView(input)
            builder.setMessage("Enter hexadecimal value")
        } else {
            builder.setView(spinner)
            builder.setMessage("Select value")
        }
        builder.setTitle(title)
        builder.setPositiveButton("OK", DialogInterface.OnClickListener { dialogInterface, i ->
            val result: Byte
            result = if (enumObject == null) {
                try {
                    input.text.toString().toInt(16).toByte()
                } catch (ex: Exception) {
                    Timber.d(ex)
                    Toast.makeText(context, "Enter valid hexadecimal value", Toast.LENGTH_LONG)
                        .show()
                    return@OnClickListener
                }
            } else {
                getByteFromEnum(enumObject, spinner.selectedItem.toString())
            }
            param.value = result
            if (reader.setConfig(param) == RESULT.OK) {
                if (enumObject == null) {
                    tv.text = input.text
                } else {
                    tv.text = spinner.selectedItem.toString()
                }
            } else {
                addLog("Set config NOK", false)
            }
        })
        builder.setNegativeButton("Cancel") { dialogInterface, i -> dialogInterface.cancel() }
        builder.show()
    }

    // Rfid agrident listener
    override fun onTagIdReceived(agridentMessage: AgridentMessage, result: RESULT) {
        runOnUiThread { handleMessage(agridentMessage) }
    }

    override fun onFirmwareReceived(s: String, result: RESULT) {
        updateTextView(binding.tvFirmware, s)
    }

    override fun onSerialNumberReceived(s: String, result: RESULT) {
        updateTextView(binding.tvSerialNumberValue, s)
    }

    override fun onCommandAckReceived(messageType: MessageType, b: Boolean) {}
    override fun onGetConfigReceived(b: Byte, result: RESULT) {
        runOnUiThread {
            if (result == RESULT.OK) {
                currentParam?.value = b
                updateParameterValue()
            } else {
                addLog("Fail to get Setting", false)
            }
        }
    }

    override fun onGetConfigAllReceived(parameters: Array<Parameters>, result: RESULT) {}
    override fun onReaderInformationReceived(readerInformation: ReaderInformation, i: Int) {
        Log.d(
            "SampleACtivity",
            "name : " + readerInformation.name + "value" + readerInformation.toString()
        )
        when (readerInformation.name) {
            "AMPLITUDE" -> updateTextView(binding.tvAmplitude, i.toString() + "mV")
            "RSSI" -> updateTextView(binding.tvRssi, "$i mv")
            "AVERAGE_HDX_FREQ" -> updateTextView(binding.tvHdxFreq, i.toString() + "Hz")
            "AVERAGE_HDX_RSSI" -> updateTextView(binding.tvHdxRssi, i.toString() + "mv")
            "AVERAGE_FDX_RSSI" -> updateTextView(binding.tvFdxRssi, i.toString() + "mv")
        }
    }

    //Power listener
    override fun onPowerUp(res: RESULT, peripheral: Peripheral) {
        // Gets Reader instance
        Timber.d("onPowerUp!!!!!!")
        ReaderFactory.getInstance(this, this)
    }

    override fun onPowerDown(res: RESULT, peripheral: Peripheral) {}
    private fun handleMessage(agridentMsg: AgridentMessage) {
        val msg = agridentMsg.messageType
        if (msg != MessageType.RFID_READ_SUCCESS) {
            lastTagId = ""
        }
        when (msg) {
            MessageType.RFID_READ_SUCCESS -> {
                val sDataRead: DataBlock?
                sDataRead = agridentMsg.tag
                if (null != sDataRead) {
                    if (isIso11784Format) {
                        val myIso11784Data = ISO11784()
                        if (RESULT.OK == myIso11784Data.parse(sDataRead.tagData)) {
                            try {
                                if (lastTagId.contains(myIso11784Data.tagId.toString())) {
                                    tagReadCount++
                                    addLog("Count : $tagReadCount", true)
                                    if (tagReadCount == 999) {
                                        tagReadCount = 0
                                    }
                                } else {
                                    lastTagId = myIso11784Data.tagId.toString()
                                    tagReadCount = 0
                                    addLog("", false)
                                    addLog(
                                        "TAG type : " + CpcBytes.byteArrayToString(
                                            sDataRead.tagType,
                                            sDataRead.tagType.size
                                        ), false
                                    )
                                    addLog(
                                        "Country : " + myIso11784Data.countryCode.toString(),
                                        false
                                    )
                                    addLog("TAG ID : $lastTagId", false)
                                    addLog("ISO11784 standard :", false)
                                    addLog("Count : $tagReadCount", false)
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        } else {
                            addLog("Fail to parse data", false)
                        }
                    } else if (isAsciiFormat) {
                        val myAsciiData = Ascii()
                        if (RESULT.OK == Ascii.parse(sDataRead.tagData)) {
                            var dataRead = ""
                            try {
                                dataRead = String(myAsciiData.id, Charsets.UTF_8)
                            } catch (e: UnsupportedEncodingException) {
                                e.printStackTrace()
                            }
                            if (lastTagId.contains(dataRead)) {
                                tagReadCount++
                                addLog("Count : $tagReadCount", true)
                                if (tagReadCount == 999) {
                                    tagReadCount = 0
                                }
                            } else {
                                lastTagId = dataRead
                                tagReadCount = 0
                                addLog("", false)
                                try {
                                    addLog(
                                        "TAG type : " + String(sDataRead.tagType, Charsets.UTF_8)
                                        , false
                                    )
                                    addLog(
                                        "Country : " + String(myAsciiData.countryCode,
                                            Charsets.UTF_8), false
                                    )
                                } catch (e: UnsupportedEncodingException) {
                                    e.printStackTrace()
                                }
                                addLog("TAG ID : $lastTagId", false)
                                addLog("ASCII Format :", false)
                                addLog("Count : $tagReadCount", false)
                            }
                        } else {
                            addLog("Fail to parse data", false)
                        }
                    } else if (isCompactCoding) {
                        val myCompactCodeData = CompactCoding()
                        if (RESULT.OK == myCompactCodeData.parse(sDataRead.tagData)) {
                            if (lastTagId.contains(
                                    CpcBytes.byteArrayToString(
                                        sDataRead.tagType,
                                        sDataRead.tagType.size
                                    )
                                )
                            ) {
                                tagReadCount++
                                addLog("Count : $tagReadCount", true)
                                if (tagReadCount == 999) {
                                    tagReadCount = 0
                                }
                            } else {
                                lastTagId = CpcBytes.byteArrayToString(
                                    myCompactCodeData.id,
                                    myCompactCodeData.id.size
                                )
                                tagReadCount = 0
                                addLog("", false)
                                addLog(
                                    "TAG type : " + CpcBytes.byteArrayToString(
                                        sDataRead.tagType,
                                        sDataRead.tagType.size
                                    ), false
                                )
                                addLog(
                                    "Country : " + CpcBytes.byteArrayToString(
                                        myCompactCodeData.countryCode,
                                        myCompactCodeData.countryCode.size
                                    ), false
                                )
                                addLog("TAG ID : $lastTagId", false)
                                addLog("Compact Coding Format :", false)
                                addLog("Count : $tagReadCount", false)
                            }
                        } else {
                            addLog("Fail to parse data", false)
                        }
                    } else {
                        if (lastTagId.contains(
                                CpcBytes.byteArrayToString(
                                    sDataRead.tagData,
                                    sDataRead.tagData.size
                                )
                            )
                        ) {
                            tagReadCount++
                            addLog("Count : $tagReadCount", true)
                            if (tagReadCount == 999) {
                                tagReadCount = 0
                            }
                        } else {
                            lastTagId = CpcBytes.byteArrayToString(
                                sDataRead.tagData,
                                sDataRead.tagData.size
                            )
                            tagReadCount = 0
                            addLog("", false)
                            addLog(
                                "TAG type : " + CpcBytes.byteArrayToString(
                                    sDataRead.tagType,
                                    sDataRead.tagType.size
                                ), false
                            )
                            addLog("TAG ID : $lastTagId", false)
                            addLog("Count : $tagReadCount", false)
                        }
                    }
                }
            }

            MessageType.RFID_UNKNOWN -> {
                val unknownFrame = agridentMsg.data
                addLog(
                    "UNKNOWN READER OUTPUT : " + CpcBytes.byteArrayToString(
                        unknownFrame,
                        unknownFrame.size
                    ), false
                )
            }

            MessageType.SWITCH_RF_ON_OFF ->                 //TODO check if RF on or OFF
                if (agridentMsg.isAck) {
                    addLog("RF Field Switched", false)
                } else {
                    addLog("Switch ON/OFF RF Field command failed", false)
                }

            MessageType.GET_RF_STATE_RESULT -> if (agridentMsg.isAck) {
                if (1 == agridentMsg.parameterValue) {
                    binding.swRFField.isChecked = true
                    addLog("RF is Activated", false)
                } else {
                    binding.swRFField.isChecked = false
                    addLog("RF is NOT Activated", false)
                }
            } else {
                addLog("Failed to get RSSI", false)
            }

            MessageType.SET_SETTINGS_RESULT -> if (agridentMsg.isAck) {
                addLog("New Setting set Successfully", false)
            } else {
                addLog("Fail to set new Settings", false)
            }

            MessageType.RESET_CONF_RESULT -> if (agridentMsg.isAck) {
                // etValue.setText(Integer.toHexString(agridentMsg.getParameterValue()));
                addLog("All Settings are reset", false)
            } else {
                addLog("Fail to reset Settings", false)
            }

            else -> {
                addLog("Message $msg not treated in handleMessage", false)
            }
        }
    }

    private fun updateTextView(tv: TextView, value: String) {
        runOnUiThread { tv.text = value }
    }

    private fun updateParameterValue() {
        when (currentParam?.address) {
            Parameters.BAUDRATE -> {
                val baudRates = BaudRate.values()
                for (baudRate in baudRates) {
                    if (baudRate.value == currentParam?.value) {
                        updateTextView(binding.tvBaudrate, baudRate.toString())
                    }
                }
            }

            Parameters.DELAYTIME -> currentParam?.value?.let { byteValue ->
                updateTextView(binding.tvDelayTime, Integer.toHexString(byteValue.toInt()))
            }


            Parameters.OUTPUT_FORMAT -> {
                val outputArray = OutputFormat.values()
                for (output in outputArray) {
                    if (output.value == currentParam!!.value) updateTextView(
                        binding.tvOutput,
                        output.toString()
                    )
                }
            }

            Parameters.TAG_TYPE -> {
                val tagTypes = TagTypes.values()
                for (tagType in tagTypes) {
                    if (tagType.value == currentParam!!.value) updateTextView(
                        binding.tvTagType,
                        tagType.toString()
                    )
                }
            }

            Parameters.TIMING -> {
                val timings = Timing.values()
                for (timing in timings) {
                    if (timing.value == currentParam!!.value) updateTextView(
                        binding.tvTimeout,
                        timing.toString()
                    )
                }
            }
        }
    }

    private fun getByteFromEnum(enumObject: Any, value: String): Byte {
        return if (enumObject is OutputFormat) {
            OutputFormat.valueOf(value).value
        } else if (enumObject is BaudRate) {
            BaudRate.valueOf(value).value
        } else if (enumObject is TagTypes) {
            TagTypes.valueOf(value).value
        } else if (enumObject is Timing) {
            Timing.valueOf(value).value
        } else 0x00
    }

    /**
     * Adds a line in the log Edit Text
     *
     * @param s Text to be added
     */
    private fun addLog(s: String, isTagCount: Boolean) {
        val maxLogLines = 2500
        val linesToRemove = 500
        val log: String
        var previousLog = binding.tvMessage.text.toString()
        if (previousLog.length > maxLogLines) {
            previousLog = previousLog.substring(0, previousLog.length - linesToRemove)
        }

        // If the log is a Tag count incrementation,
        // we remove the last line and add the new counter value.
        if (previousLog.length > 10 && isTagCount) {
            previousLog = previousLog.substring(previousLog.indexOf("\r\n"))
            log = s + previousLog
            binding.tvMessage.text = log
        } else {
            log = """
                $s
                $previousLog
                """.trimIndent()
            binding.tvMessage.text = log
        }
    }

    /**
     * Closes the Com port of the reader and switch off the Reader Power.
     */
    private fun closePort() {
        if (reader != null) {
            reader.close()
            binding.btnOpenClose.setText(R.string.open)
            isPortOpened = false

            // ConePeripheral.RFID_AGRIDENT_ABR200_GPIO.off(this);
        }
    }

    companion object {
        const val ACTION_SERVICE_STOP = "fr.coppernic.intent.action.stop.agrident.service"
        const val ACTION_SERVICE_START = "fr.coppernic.intent.action.start.agrident.service"
    }
}