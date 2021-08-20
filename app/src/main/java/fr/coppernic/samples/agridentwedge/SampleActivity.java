package fr.coppernic.samples.agridentwedge;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import fr.coppernic.samples.agridentwedge.databinding.ActivitySampleBinding;
import fr.coppernic.sdk.agrident.AgridentMessage;
import fr.coppernic.sdk.agrident.Ascii;
import fr.coppernic.sdk.agrident.Commands;
import fr.coppernic.sdk.agrident.CompactCoding;
import fr.coppernic.sdk.agrident.DataBlock;
import fr.coppernic.sdk.agrident.ISO11784;
import fr.coppernic.sdk.agrident.MessageType;
import fr.coppernic.sdk.agrident.OnDataReceivedListener;
import fr.coppernic.sdk.agrident.Parameters;
import fr.coppernic.sdk.agrident.Reader;
import fr.coppernic.sdk.agrident.ReaderFactory;
import fr.coppernic.sdk.agrident.ReaderInformation;
import fr.coppernic.sdk.core.Defines;
import fr.coppernic.sdk.power.PowerManager;
import fr.coppernic.sdk.power.api.PowerListener;
import fr.coppernic.sdk.power.api.peripheral.Peripheral;
import fr.coppernic.sdk.power.impl.cone.ConePeripheral;
import fr.coppernic.sdk.utils.core.CpcBytes;
import fr.coppernic.sdk.utils.core.CpcResult;
import fr.coppernic.sdk.utils.io.InstanceListener;
import timber.log.Timber;

public class SampleActivity extends AppCompatActivity implements PowerListener, InstanceListener<Reader>, OnDataReceivedListener {

    // RFID Reader interface
    private Reader reader = null;
    private boolean isPortOpened = false;


    private String lastTagId = "";
    private int tagReadCount = 0;
    private boolean isIso11784Format = false;
    private boolean isIso28631Format = false;
    private boolean isAsciiFormat = false;
    private boolean isCompactCoding = false;

    private Parameters currentParam = null;

    public Context context;

    private ActivitySampleBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PowerManager.get().registerListener(this);
        binding = ActivitySampleBinding.inflate(getLayoutInflater());

        View view = binding.getRoot();
        setContentView(view);

        ConePeripheral.RFID_AGRIDENT_ABR200_GPIO.on(this);
        context = this;

        configureOnClick();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Releases PowerManager
        PowerManager.get().unregisterAll();
        PowerManager.get().releaseResources();
    }

    // Instance listener
    @Override
    public void onCreated(Reader reader) {
        Timber.d("onCreated");
        this.reader = reader;
        reader.setOnDataReceivedListener(this);
    }

    @Override
    public void onDisposed(Reader reader) {

    }


    @Override
    protected void onPause() {
        closePort();
        super.onPause();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Timber.d("onStart");
        Intent intent = new Intent();
        intent.setPackage("fr.coppernic.tools.cpcagridentwedge");
        intent.setComponent(new ComponentName("fr.coppernic.tools.cpcagridentwedge", "fr.coppernic.tools.cpcagridentwedge.service.WedgeService"));
        intent.setAction(Defines.IntentDefines.ACTION_AGRIDENT_SERVICE_STOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        SystemClock.sleep(1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.d("onResume");
        ConePeripheral.RFID_AGRIDENT_ABR200_GPIO.on(this);
    }

    private void configureOnClick() {
        binding.btnOpenClose.setOnClickListener(view -> OpenClose());
        binding.btnClear.setOnClickListener(view -> clear());
        binding.btnGetFirmware.setOnClickListener(view -> displayFirmware());
        binding.btnGetSerialNumber.setOnClickListener(view -> displaySN());
        binding.btnGetAmplitude.setOnClickListener(view -> displayAmplitude());
        binding.btnGetRssi.setOnClickListener(view -> displayRSSI());
        binding.btnGetFDXRssi.setOnClickListener(view -> displayFdxRssi());
        binding.btnGetHDXRssi.setOnClickListener(view -> displayHdxRssi());
        binding.btnGetHDXFreq.setOnClickListener(view -> displayHdxFreq());
        binding.btnEditOutput.setOnClickListener(view -> editOutput());
        binding.btnGetOutput.setOnClickListener(view -> getOutputFormat());
        binding.btnEditTagType.setOnClickListener(view -> editTagType());
        binding.btnGetTagType.setOnClickListener(view -> getTagType());
        binding.btnEditTimeout.setOnClickListener(view -> editTimeout());
        binding.btnGetTimeout.setOnClickListener(view -> getTimeout());
        binding.btnEditBaudrate.setOnClickListener(view -> editBaudrate());
        binding.btnGetBaudrate.setOnClickListener(view -> getBaudrate());
        binding.btnEditDelayTime.setOnClickListener(view -> editDelayTime());
        binding.btnGetDelayTime.setOnClickListener(view -> getDelayTime());
        binding.btnOpenClose.setOnClickListener(view -> OpenClose());
        binding.btnOpenClose.setOnClickListener(view -> OpenClose());
        binding.btnOpenClose.setOnClickListener(view -> OpenClose());
        binding.btnOpenClose.setOnClickListener(view -> OpenClose());
        binding.btnOpenClose.setOnClickListener(view -> OpenClose());
        binding.btnOpenClose.setOnClickListener(view -> OpenClose());

        binding.swRFField.setOnCheckedChangeListener((compoundButton, b) -> SwitchRfChange());
    }

    void OpenClose() {
        if (reader != null) {
            if (!isPortOpened) {
                int baudRate = Integer.parseInt(binding.spBaudrate.getSelectedItem().toString());

                CpcResult.RESULT res = reader.open(Defines.SerialDefines.AGRIDENT_READER_PORT, baudRate);

                if (res == CpcResult.RESULT.OK) {
                    binding.btnOpenClose.setText(R.string.close);
                    isPortOpened = true;
                    if (CpcResult.RESULT.OK != reader.sendCommand(Commands.GET_RF_ACTIVATION_CMD)) {
                        addLog("Fail to get RFID field state", false);
                    }
                } else {
                    addLog("Open error", false);
                }
            } else {
                reader.close();
                binding.btnOpenClose.setText(R.string.open);
                isPortOpened = false;
            }
        }
    }

    void clear() {
        binding.tvMessage.setText("");
    }

    void displayFirmware() {
        reader.sendCommand(Commands.FIRMWARE_CMD);
    }

    void displaySN() {
        reader.sendCommand(Commands.SNR_CMD);
    }

    void displayAmplitude() {
        reader.sendCommand(Commands.GET_AMPLITUDE_CMD);
    }

    void displayRSSI() {
        reader.sendCommand(Commands.GET_RSSI_CMD);
    }

    void displayFdxRssi() {
        reader.sendCommand(Commands.GET_AVERAGE_FDX_RSSI_CMD);
    }

    void displayHdxRssi() {
        reader.sendCommand(Commands.GET_AVERAGE_HDX_RSSI_CMD);
    }

    void displayHdxFreq() {
        reader.sendCommand(Commands.GET_AVERAGE_HDX_FRQ_CMD);
    }

    void editOutput() {
        showDialog(new Parameters(Parameters.OUTPUT_FORMAT), binding.tvOutput, OutputFormat.ASCII, "Output Format");
    }

    void getOutputFormat() {
        currentParam = new Parameters(Parameters.OUTPUT_FORMAT);
        reader.getConfig(new Parameters(Parameters.OUTPUT_FORMAT));
    }

    void editTagType() {
        showDialog(new Parameters(Parameters.TAG_TYPE), binding.tvTagType, TagTypes.FDX_B, "Tag Type");
    }

    void getTagType() {
        currentParam = new Parameters(Parameters.TAG_TYPE);
        reader.getConfig(new Parameters(Parameters.TAG_TYPE));
    }

    void editTimeout() {
        showDialog(new Parameters(Parameters.TIMING), binding.tvTimeout, Timing.VARIABLE_TIMING, "Timing");
    }

    void getTimeout() {
        currentParam = new Parameters(Parameters.TIMING);
        reader.getConfig(new Parameters(Parameters.TIMING));
    }

    void editBaudrate() {
        showDialog(new Parameters(Parameters.BAUDRATE), binding.tvBaudrate, BaudRate.B9600, "BaudRate");
    }

    void getBaudrate() {
        currentParam = new Parameters(Parameters.BAUDRATE);
        reader.getConfig(new Parameters(Parameters.BAUDRATE));
    }

    void editDelayTime() {
        showDialog(new Parameters(Parameters.DELAYTIME), binding.tvDelayTime, null, "Delay Time");
    }

    void getDelayTime() {
        currentParam = new Parameters(Parameters.DELAYTIME);
        reader.getConfig(new Parameters(Parameters.DELAYTIME));
    }

    void SwitchRfChange() {
        if (binding.swRFField.isChecked()) {
            reader.sendCommand(Commands.SET_RF_ON_CMD);
        } else {
            reader.sendCommand(Commands.SET_RF_OFF_CMD);
        }
    }

    void showDialog(final Parameters param, final TextView tv, final Object enumObject, String title) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);
        final EditText input = new EditText(this);


        final Spinner spinner = new Spinner(this);
        //display spinner depending of parameter
        if (enumObject == null) {
            input.setText(tv.getText());
        } else if (enumObject instanceof OutputFormat) {
            ArrayAdapter<OutputFormat> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, OutputFormat.values());
            spinner.setAdapter(adapter);
            try {
                OutputFormat output = OutputFormat.valueOf(tv.getText().toString());
                int spinnerPosition = adapter.getPosition(output);
                if (spinnerPosition >= 0) {
                    spinner.setSelection(spinnerPosition);
                }
            } catch (Exception ex) {
                Timber.d(ex);
            }
        } else if (enumObject instanceof TagTypes) {
            ArrayAdapter<TagTypes> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TagTypes.values());
            spinner.setAdapter(adapter);
            try {
                TagTypes tagTypes = TagTypes.valueOf(tv.getText().toString());
                int spinnerPosition = adapter.getPosition(tagTypes);
                if (spinnerPosition >= 0) {
                    spinner.setSelection(spinnerPosition);
                }
            } catch (Exception ex) {
                Timber.d(ex);
            }
        } else if (enumObject instanceof BaudRate) {
            ArrayAdapter<BaudRate> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, BaudRate.values());
            spinner.setAdapter(adapter);
            try {
                BaudRate baudRate = BaudRate.valueOf(tv.getText().toString());
                int spinnerPosition = adapter.getPosition(baudRate);
                if (spinnerPosition >= 0) {
                    spinner.setSelection(spinnerPosition);
                }
            } catch (Exception ex) {
                Timber.d(ex);
            }
        } else if (enumObject instanceof Timing) {
            ArrayAdapter<Timing> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Timing.values());
            spinner.setAdapter(adapter);
            try {
                Timing timing = Timing.valueOf(tv.getText().toString());
                int spinnerPosition = adapter.getPosition(timing);
                if (spinnerPosition >= 0) {
                    spinner.setSelection(spinnerPosition);
                }
            } catch (Exception ex) {
                Timber.d(ex);
            }
        }

        if (enumObject == null) {
            builder.setView(input);
            builder.setMessage("Enter hexadecimal value");
        } else {
            builder.setView(spinner);
            builder.setMessage("Select value");
        }

        builder.setTitle(title);

        builder.setPositiveButton("OK", (dialogInterface, i) -> {
            byte result;
            if (enumObject == null) {
                try {
                    result = (byte) Integer.parseInt(input.getText().toString(), 16);
                } catch (Exception ex) {
                    Timber.d(ex);
                    Toast.makeText(context, "Enter valid hexadecimal value", Toast.LENGTH_LONG).show();
                    return;
                }
            } else {
                result = getByteFromEnum(enumObject, spinner.getSelectedItem().toString());
            }
            param.setValue(result);
            if (reader.setConfig(param) == CpcResult.RESULT.OK) {
                if (enumObject == null) {
                    tv.setText(input.getText());
                } else {
                    tv.setText(spinner.getSelectedItem().toString());
                }
            } else {
                addLog("Set config NOK", false);
            }
        });
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
        builder.show();
    }

    // Rfid agrident listener
    @Override
    public void onTagIdReceived(final AgridentMessage agridentMessage, CpcResult.RESULT result) {
        runOnUiThread(() -> handleMessage(agridentMessage));
    }

    @Override
    public void onFirmwareReceived(final String s, CpcResult.RESULT result) {
        updateTextView(binding.tvFirmware, s);
    }

    @Override
    public void onSerialNumberReceived(final String s, CpcResult.RESULT result) {
        updateTextView(binding.tvSerialNumberValue, s);
    }

    @Override
    public void onCommandAckReceived(MessageType messageType, boolean b) {

    }

    @Override
    public void onGetConfigReceived(final byte b, final CpcResult.RESULT result) {
        runOnUiThread(() -> {
            if (result == CpcResult.RESULT.OK) {
                currentParam.setValue(b);
                updateParameterValue();
            } else {
                addLog("Fail to get Setting", false);
            }
        });
    }

    @Override
    public void onGetConfigAllReceived(Parameters[] parameters, CpcResult.RESULT result) {

    }

    @Override
    public void onReaderInformationReceived(ReaderInformation readerInformation, int i) {
        Timber.d("name : " + readerInformation.name() + "value" + readerInformation.toString());
        switch (readerInformation.name()) {
            case "AMPLITUDE":
                updateTextView(binding.tvAmplitude, i + "mV");
                break;
            case "RSSI":
                updateTextView(binding.tvRssi, i + " mv");
                break;
            case "AVERAGE_HDX_FREQ":
                updateTextView(binding.tvHdxFreq, i + "Hz");
                break;
            case "AVERAGE_HDX_RSSI":
                updateTextView(binding.tvHdxRssi, i + "mv");
                break;
            case "AVERAGE_FDX_RSSI":
                updateTextView(binding.tvFdxRssi, i + "mv");
                break;
        }
    }

    //Power listener
    @Override
    public void onPowerUp(CpcResult.RESULT res, Peripheral peripheral) {
        // Gets Reader instance
        Timber.d("onPowerUp!!!!!!");
        ReaderFactory.getInstance(this, this);
    }

    @Override
    public void onPowerDown(CpcResult.RESULT res, Peripheral peripheral) {

    }

    private void handleMessage(AgridentMessage agridentMsg) {
        MessageType msg = agridentMsg.getMessageType();

        if (msg != MessageType.RFID_READ_SUCCESS) {
            lastTagId = "";
        }

        switch (msg) {
            case RFID_READ_SUCCESS:
                DataBlock sDataRead;
                sDataRead = agridentMsg.getTag();
                if (null != sDataRead) {
                    if (isIso11784Format) {
                        ISO11784 myIso11784Data = new ISO11784();
                        if (CpcResult.RESULT.OK == myIso11784Data.parse(sDataRead.getTagData())) {
                            try {
                                if (lastTagId.contains(String.valueOf(myIso11784Data.getTagId()))) {
                                    tagReadCount++;
                                    addLog("Count : " + tagReadCount, true);
                                    if (tagReadCount == 999) {
                                        tagReadCount = 0;
                                    }
                                } else {
                                    lastTagId = String.valueOf(myIso11784Data.getTagId());
                                    tagReadCount = 0;
                                    addLog("", false);
                                    addLog("TAG type : " + CpcBytes.byteArrayToString(sDataRead.getTagType(), sDataRead.getTagType().length), false);
                                    addLog("Country : " + myIso11784Data.getCountryCode(), false);
                                    addLog("TAG ID : " + lastTagId, false);
                                    addLog("ISO11784 standard :", false);
                                    addLog("Count : " + tagReadCount, false);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            addLog("Fail to parse data", false);
                        }
                    } else if (isAsciiFormat) {
                        Ascii myAsciiData = new Ascii();
                        if (CpcResult.RESULT.OK == Ascii.parse(sDataRead.getTagData())) {
                            String dataRead = "";
                            try {
                                dataRead = new String(myAsciiData.getId(), "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            if (lastTagId.contains(String.valueOf(dataRead))) {
                                tagReadCount++;
                                addLog("Count : " + tagReadCount, true);
                                if (tagReadCount == 999) {
                                    tagReadCount = 0;
                                }
                            } else {
                                lastTagId = dataRead;
                                tagReadCount = 0;
                                addLog("", false);
                                try {
                                    addLog("TAG type : " + new String(sDataRead.getTagType(), "UTF-8"), false);
                                    addLog("Country : " + new String(myAsciiData.getCountryCode(), "UTF-8"), false);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                addLog("TAG ID : " + lastTagId, false);
                                addLog("ASCII Format :", false);
                                addLog("Count : " + tagReadCount, false);
                            }
                        } else {
                            addLog("Fail to parse data", false);
                        }
                    } else if (isCompactCoding) {
                        CompactCoding myCompactCodeData = new CompactCoding();
                        if (CpcResult.RESULT.OK == myCompactCodeData.parse(sDataRead.getTagData())) {
                            if (lastTagId.contains(CpcBytes.byteArrayToString(sDataRead.getTagType(), sDataRead.getTagType().length))) {
                                tagReadCount++;
                                addLog("Count : " + tagReadCount, true);
                                if (tagReadCount == 999) {
                                    tagReadCount = 0;
                                }
                            } else {
                                lastTagId = CpcBytes.byteArrayToString(myCompactCodeData.getId(), myCompactCodeData.getId().length);
                                tagReadCount = 0;
                                addLog("", false);
                                addLog("TAG type : " + CpcBytes.byteArrayToString(sDataRead.getTagType(), sDataRead.getTagType().length), false);
                                addLog("Country : " + CpcBytes.byteArrayToString(myCompactCodeData.getCountryCode(), myCompactCodeData.getCountryCode().length), false);
                                addLog("TAG ID : " + lastTagId, false);
                                addLog("Compact Coding Format :", false);
                                addLog("Count : " + tagReadCount, false);
                            }
                        } else {
                            addLog("Fail to parse data", false);
                        }
                    } else {
                        if (lastTagId.contains(CpcBytes.byteArrayToString(sDataRead.getTagData(), sDataRead.getTagData().length))) {
                            tagReadCount++;
                            addLog("Count : " + tagReadCount, true);
                            if (tagReadCount == 999) {
                                tagReadCount = 0;
                            }
                        } else {
                            lastTagId = CpcBytes.byteArrayToString(sDataRead.getTagData(), sDataRead.getTagData().length);
                            tagReadCount = 0;
                            addLog("", false);
                            addLog("TAG type : " + CpcBytes.byteArrayToString(sDataRead.getTagType(), sDataRead.getTagType().length), false);
                            addLog("TAG ID : " + lastTagId, false);
                            addLog("Count : " + tagReadCount, false);
                        }
                    }
                }
                break;
            case RFID_UNKNOWN:
                byte[] unknownFrame = agridentMsg.getData();
                addLog("UNKNOWN READER OUTPUT : " + CpcBytes.byteArrayToString(unknownFrame, unknownFrame.length), false);
                break;

            case SWITCH_RF_ON_OFF:
                //TODO check if RF on or OFF
                if (agridentMsg.isAck()) {
                    addLog("RF Field Switched", false);
                } else {
                    addLog("Switch ON/OFF RF Field command failed", false);
                }
                break;
            case GET_RF_STATE_RESULT:
                if (agridentMsg.isAck()) {
                    if (1 == agridentMsg.getParameterValue()) {
                        binding.swRFField.setChecked(true);
                        addLog("RF is Activated", false);
                    } else {
                        binding.swRFField.setChecked(false);
                        addLog("RF is NOT Activated", false);
                    }
                } else {
                    addLog("Failed to get RSSI", false);
                }
                break;

            case SET_SETTINGS_RESULT:
                if (agridentMsg.isAck()) {
                    addLog("New Setting set Successfully", false);
                } else {
                    addLog("Fail to set new Settings", false);
                }
                break;
            case RESET_CONF_RESULT:
                if (agridentMsg.isAck()) {
                    // etValue.setText(Integer.toHexString(agridentMsg.getParameterValue()));
                    addLog("All Settings are reset", false);
                } else {
                    addLog("Fail to reset Settings", false);
                }
                break;
        }
    }

    private void updateTextView(final TextView tv, final String value) {
        runOnUiThread(() -> tv.setText(value));
    }

    private void updateParameterValue() {
        switch (currentParam.getAddress()) {
            case Parameters.BAUDRATE:
                BaudRate[] baudRates = BaudRate.values();
                for (BaudRate baudRate : baudRates) {
                    if (baudRate.getValue() == currentParam.getValue())
                        updateTextView(binding.tvBaudrate, baudRate.toString());
                }
                break;
            case Parameters.DELAYTIME:
                updateTextView(binding.tvDelayTime, Integer.toHexString(currentParam.getValue()));
                break;
            case Parameters.OUTPUT_FORMAT:
                OutputFormat[] outputArray = OutputFormat.values();
                for (OutputFormat output : outputArray) {
                    if (output.getValue() == currentParam.getValue())
                        updateTextView(binding.tvOutput, output.toString());
                }

                break;
            case Parameters.TAG_TYPE:
                TagTypes[] tagTypes = TagTypes.values();
                for (TagTypes tagType : tagTypes) {
                    if (tagType.getValue() == currentParam.getValue())
                        updateTextView(binding.tvTagType, tagType.toString());
                }
                break;
            case Parameters.TIMING:
                Timing[] timings = Timing.values();
                for (Timing timing : timings) {
                    if (timing.getValue() == currentParam.getValue())
                        updateTextView(binding.tvTimeout, timing.toString());
                }
                break;
        }
    }

    private byte getByteFromEnum(Object enumObject, String value) {
        if (enumObject instanceof OutputFormat) {
            return (OutputFormat.valueOf(value)).getValue();
        } else if (enumObject instanceof BaudRate) {
            return (BaudRate.valueOf(value)).getValue();
        } else if (enumObject instanceof TagTypes) {
            return (TagTypes.valueOf(value)).getValue();
        } else if (enumObject instanceof Timing) {
            return (Timing.valueOf(value)).getValue();
        } else
            return 0x00;
    }

    /**
     * Adds a line in the log Edit Text
     *
     * @param s Text to be added
     */
    private void addLog(String s, boolean isTagCount) {

        int maxLogLines = 2500;
        int linesToRemove = 500;
        String log;
        String previousLog = binding.tvMessage.getText().toString();

        if (previousLog.length() > maxLogLines) {
            previousLog = previousLog.substring(0, previousLog.length() - linesToRemove);
        }

        // If the log is a Tag count incrementation,
        // we remove the last line and add the new counter value.
        if ((previousLog.length() > 10) && (isTagCount)) {
            previousLog = previousLog.substring(previousLog.indexOf("\r\n"));
            log = s + previousLog;
            binding.tvMessage.setText(log);
        } else {
            log = s + "\r\n" + previousLog;
            binding.tvMessage.setText(log);
        }
    }


    /**
     * Closes the Com port of the reader and switch off the Reader Power.
     */
    private void closePort() {
        if (reader != null) {
            reader.close();
            binding.btnOpenClose.setText(R.string.open);
            isPortOpened = false;

            // ConePeripheral.RFID_AGRIDENT_ABR200_GPIO.off(this);
        }
    }
}
