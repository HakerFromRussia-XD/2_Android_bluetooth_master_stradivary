package me.Romans.motorica.ui.chat.view.Service_settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.Romans.motorica.MyApp;
import me.Romans.motorica.R;
import me.Romans.motorica.ui.chat.data.ChatModule;
import me.Romans.motorica.ui.chat.data.DaggerChatComponent;
import me.Romans.motorica.ui.chat.view.ChartActivity;
import me.Romans.motorica.ui.chat.view.ChatView;

public class FragmentServiceSettings extends Fragment implements ChatView {
    @BindView(R.id.switchInvert) public Switch switchInvert;
    @BindView(R.id.switchNotUseInternalADC) public Switch switchNotUseInternalADC;
    @BindView(R.id.switchMagnetInvert) Switch switchMagnetInvert;
    @BindView(R.id.switchReversMotor) Switch switchReversMotor;
    @BindView(R.id.switchZeroCrossing) Switch switchZeroCrossing;
    @BindView(R.id.layout_calibration) public RelativeLayout layout_calibration;
    @BindView(R.id.save_service_settings) Button save_service_settings;
    @BindView(R.id.buttonOPN) Button buttonOPN;
    @BindView(R.id.buttonCLS) Button buttonCLS;
    @BindView(R.id.buttonSTP) Button buttonSTP;
    @BindView(R.id.buttonSet) Button buttonSet;
    @BindView(R.id.buttonGSetup) Button buttonGSetup;
    @BindView(R.id.buttonSSetup) Button buttonSSetup;
    @BindView(R.id.buttonCurrents) Button buttonCurrents;
    @BindView(R.id.buttonMIO) Button buttonMIO;
    @BindView(R.id.buttonEtEClaib) Button buttonEtEClaib;
    @BindView(R.id.buttonEEPROMSave) Button buttonEEPROMSave;
    @BindView(R.id.buttonAngleFIX) Button buttonAngleFIX;
    @BindView(R.id.buttonG1) Button buttonG1;
    @BindView(R.id.buttonG2) Button buttonG2;
    @BindView(R.id.buttonG3) Button buttonG3;
    @BindView(R.id.buttonS1) Button buttonS1;
    @BindView(R.id.buttonS2) Button buttonS2;
    @BindView(R.id.buttonS3) Button buttonS3;
    @BindView(R.id.buttonE) Button buttonE;
    @BindView(R.id.buttonO) Button buttonO;
    @BindView(R.id.buttonS4) Button buttonS4;
    @BindView(R.id.buttonC) Button buttonC;
    @BindView(R.id.buttonU) Button buttonU;
    @BindView(R.id.editTextAddr) EditText editTextAddr;
    @BindView(R.id.editTextTemp) EditText editTextTemp;
    @BindView(R.id.editTextMaxCurrentValue) EditText editTextMaxCurrentValue;
    @BindView(R.id.editTextCurrTimeOut) EditText editTextCurrTimeOut;
    @BindView(R.id.editTextOpenAngle) EditText editTextOpenAngle;
    @BindView(R.id.editTextCloseAngle) EditText editTextCloseAngle;
    @BindView(R.id.editTextWideAngle) EditText editTextWideAngle;
    @BindView(R.id.seekBarRoughness) public SeekBar seekBarRoughness;
    @BindView(R.id.seekBarSpeed) SeekBar seekBarSpeed;
    @BindView(R.id.seekBarAngle) SeekBar seekBarAngle;
    @BindView(R.id.switchCurrentControl) Switch switchCurrentControl;
    public View view;
    private ChartActivity chatActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_service_settings, container, false);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule((ChatView) FragmentServiceSettings.this))
                .build().inject(FragmentServiceSettings.this);
        ButterKnife.bind(this, view);

        if (getActivity() != null) {chatActivity = (ChartActivity) getActivity();}
        chatActivity.graphThreadFlag = false;
        chatActivity.updateSeviceSettingsThreadFlag = true;
        chatActivity.startUpdateThread();
        chatActivity.layoutSensors.setVisibility(View.GONE);

        Spinner spinnerNumberOfChannel = view.findViewById(R.id.spinnerNumberOfChannel);
        ArrayAdapter<CharSequence> adapterNumbers = ArrayAdapter.createFromResource(this.getActivity(),
                R.array.numbers,
                android.R.layout.simple_spinner_item);
        adapterNumbers.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNumberOfChannel.setAdapter(adapterNumbers);
        spinnerNumberOfChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sNumberOfChannel = parent.getItemAtPosition(position).toString();
                chatActivity.numberOfChannel = Integer.parseInt(sNumberOfChannel);
                Toast.makeText(parent.getContext(), "Выбран канал "+ sNumberOfChannel, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if(chatActivity.getFlagUseHDLCProcol()){
            buttonOPN.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK OPN");}
                }
            });
            buttonSTP.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK STP");}
                }
            });
            buttonCLS.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK CLS");}
                }
            });
            buttonSet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer temp = 0;
                    if(!editTextAddr.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                        temp =  Integer.parseInt(editTextAddr.getText().toString());
                        if (getActivity() != null) { System.err.println("CLICK SET " + temp); }
                    }
                }
            });
            buttonGSetup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK G SETUP");}
                }
            });
            buttonSSetup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer temp = 0;
                    if(!editTextTemp.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                        temp = Integer.parseInt(editTextTemp.getText().toString());
                        if (getActivity() != null) { System.err.println("CLICK S SETUP " + temp); }
                    }
                }
            });
            switchCurrentControl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer temp = 0;
                    if(switchCurrentControl.isChecked()){
                        if(!editTextMaxCurrentValue.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                            temp = Integer.parseInt(editTextMaxCurrentValue.getText().toString());
                            if (getActivity() != null) { System.err.println("CLICK CURRENT CONTROL " + temp); }
                        }
                    } else {
                        if (getActivity() != null) { System.err.println("CLICK CURRENT CONTROL OFF"); }
                    }
                }
            });
            editTextCurrTimeOut.addTextChangedListener(new TextWatcher(){
                @Override
                public void afterTextChanged(Editable s) {
                    if(!editTextCurrTimeOut.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                        Integer temp = 0;
                            temp = Integer.parseInt(editTextCurrTimeOut.getText().toString());
                        if (getActivity() != null) { System.err.println("CLICK CURRENT TIMEOUT " + temp); }
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });
            buttonCurrents.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK CURRENTS"); }
                }
            });

            buttonEtEClaib.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK ETE CALIB"); }
                }
            });

            buttonEEPROMSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK EEPROM SAVE"); }
                }
            });

            buttonAngleFIX.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK ANGLE FIX"); }
                }
            });
            buttonMIO.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK MIO"); }
                }
            });
            buttonG1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK G1"); }
                }
            });
            buttonG2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK G2"); }
                }
            });
            buttonG3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK G3"); }
                }
            });
            buttonS1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer temp = 0;
                    if(!editTextOpenAngle.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                        temp = Integer.parseInt(editTextOpenAngle.getText().toString());
                        if (getActivity() != null) { System.err.println("CLICK S1 " + temp); }
                    }
                }
            });
            buttonS2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer temp = 0;
                    if(!editTextCloseAngle.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                        temp = Integer.parseInt(editTextCloseAngle.getText().toString());
                        if (getActivity() != null) { System.err.println("CLICK S2 " + temp); }
                    }
                }
            });
            buttonS3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer temp = 0;
                    if(!editTextWideAngle.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                        temp = Integer.parseInt(editTextWideAngle.getText().toString());
                        if (getActivity() != null) { System.err.println("CLICK S3 " + temp); }
                    }
                }
            });
            switchMagnetInvert.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(switchMagnetInvert.isChecked()){
                        if (getActivity() != null) { System.err.println("CLICK MAGNET INVERT");}
                    } else {
                        if (getActivity() != null) { System.err.println("CLICK MAGNET INVERT OFF");}
                    }

                }
            });
            switchReversMotor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(switchReversMotor.isChecked()){
                        if (getActivity() != null) { System.err.println("CLICK REVERS MOTOR"); }
                    } else {
                        if (getActivity() != null) { System.err.println("CLICK REVERS MOTOR OFF"); }
                    }

                }
            });
            switchZeroCrossing.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(switchZeroCrossing.isChecked()){
                        if (getActivity() != null) { System.err.println("CLICK ZERO CROSSING"); }
                    } else {
                        if (getActivity() != null) { System.err.println("CLICK ZERO CROSSING OFF"); }
                    }

                }
            });
            buttonE.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK E"); }
                }
            });
            buttonU.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK U"); }
                }
            });
            buttonO.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK O"); }
                }
            });
            buttonS4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK S4"); }
                }
            });
            buttonC.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) { System.err.println("CLICK C"); }
                }
            });
            seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if(chatActivity.getFlagUseHDLCProcol()){
                        if (getActivity() != null) { System.err.println("CLICK SPEED: "+ seekBar.getProgress());}
                    }
                }
            });
            seekBarAngle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if(chatActivity.getFlagUseHDLCProcol()){
                        if (getActivity() != null) { System.err.println("CLICK ANGLE: "+ seekBar.getProgress());}
                    }
                }
            });
        } else {
            layout_calibration.setVisibility(View.GONE);
        }

        if (chatActivity.invertChannel){
            switchInvert.setChecked(true);
        } else {
            switchInvert.setChecked(false);
        }

        save_service_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    chatActivity.fragmentManager.beginTransaction()
                            .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                            .remove(chatActivity.fragmentServiceSettings)
                            .commit();
                    chatActivity.navigation.clearAnimation();
                    chatActivity.navigation.animate().translationY(0).setDuration(200);
                    chatActivity.graphThreadFlag = true;
                    chatActivity.startGraphEnteringDataThread();
                    chatActivity.myMenu.setGroupVisible(R.id.service_settings, true);
                    chatActivity.myMenu.setGroupVisible(R.id.modes, false);
                    chatActivity.updateSeviceSettingsThreadFlag = false;
                    chatActivity.layoutSensors.setVisibility(View.VISIBLE);
                }
            }
        });

        seekBarRoughness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(chatActivity.getFlagUseHDLCProcol()){
                    chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeRouhnessHDLC((byte) (((byte) seekBar.getProgress()) + 1)));
                } else {
                    chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeRouhness((byte) (((byte) seekBar.getProgress()) + 1)));
                }
            }
        });

        switchInvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.err.println("FragmentServiceSettings-------------->");
                if (switchInvert.isChecked()){
                    chatActivity.invert = 0x01;
                    if(chatActivity.getFlagUseHDLCProcol()){
                        chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeCurentSettingsAndInvertHDLC(chatActivity.curent));
                    } else {
                        chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeCurentSettingsAndInvert(chatActivity.curent, chatActivity.invert));
                    }
                    Integer temp = chatActivity.intValueCH1on;
                    chatActivity.seekBarCH1on2.setProgress((int) (chatActivity.intValueCH2on/(chatActivity.multiplierSeekbar-0.1)));//-0.5
                    chatActivity.seekBarCH2on2.setProgress((int) (temp/(chatActivity.multiplierSeekbar-0.1)));//-0.5
                    chatActivity.invertChannel = true;
                } else {
                    chatActivity.invert = 0x00;
                    if(chatActivity.getFlagUseHDLCProcol()){
                        chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeCurentSettingsAndInvertHDLC(chatActivity.curent));
                    } else {
                        chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeCurentSettingsAndInvert(chatActivity.curent, chatActivity.invert));
                    }
                    Integer temp = chatActivity.intValueCH1on;
                    chatActivity.seekBarCH1on2.setProgress((int) (chatActivity.intValueCH2on/(chatActivity.multiplierSeekbar-0.1)));//-0.5
                    chatActivity.seekBarCH2on2.setProgress((int) (temp/(chatActivity.multiplierSeekbar-0.1)));//-0.5
                    chatActivity.invertChannel = false;
                }
            }
        });

        switchNotUseInternalADC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchNotUseInternalADC.isChecked()){
                    if(chatActivity.getFlagUseHDLCProcol()){
                        System.err.println("FragmentServiceSettings--------------> CompileMassegeSettingsNotUseInternalADCHDLC 0");
                        chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeSettingsNotUseInternalADCHDLC((byte) 0x00));
                    }
                } else {
                    if(chatActivity.getFlagUseHDLCProcol()){
                        System.err.println("FragmentServiceSettings--------------> CompileMassegeSettingsNotUseInternalADCHDLC 1");
                        chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeSettingsNotUseInternalADCHDLC((byte) 0x01));
                    }
                }
            }
        });

        return view;
    }

    public void backPressed() {
        if (getActivity() != null) {
            chatActivity.fragmentManager.beginTransaction()
                    .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                    .remove(chatActivity.fragmentServiceSettings)
                    .commit();
            chatActivity.navigation.clearAnimation();
            chatActivity.navigation.animate().translationY(0).setDuration(200);
            chatActivity.graphThreadFlag = true;
            chatActivity.startGraphEnteringDataThread();
            chatActivity.myMenu.setGroupVisible(R.id.service_settings, true);
            chatActivity.myMenu.setGroupVisible(R.id.modes, false);
            chatActivity.updateSeviceSettingsThreadFlag = false;
            chatActivity.layoutSensors.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        seekBarRoughness.setProgress(chatActivity.receiveRoughnessOfSensors);
    }

    @Override
    public void setStatus(String status) {

    }

    @Override
    public void setStatus(int resId) {

    }

    @Override
    public void setValueCH(int levelCH, int numberChannel) {

    }

    @Override
    public void setErrorReception(boolean incomeErrorReception) {

    }

    @Override
    public void appendMessage(String message) {

    }

    @Override
    public void enableHWButton(boolean enabled) {

    }

    @Override
    public void showToast(String message) {

    }

    @Override
    public void onGestureClick(int position) {

    }

    @Override
    public void setGeneralValue(int receiveСurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) {

    }

    @Override
    public void setStartParameters(Integer receiveСurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication, Byte receiveRoughnessOfSensors) {

    }

    @Override
    public void setStartParametersInChartActivity() {

    }

    @Override
    public boolean getFirstRead() {
        return false;
    }

    @Override
    public void setFlagReceptionExpectation(Boolean flagReceptionExpectation) {

    }

    @Override
    public void setStartParametersTrigCH1(Integer receiveLevelTrigCH1) {

    }

    @Override
    public void setStartParametersTrigCH2(Integer receiveLevelTrigCH2) {

    }

    @Override
    public void setStartParametersCurrrent(Integer receiveСurrent) {

    }

    @Override
    public void setStartParametersBlock(Byte receiveBlockIndication) {

    }

    @Override
    public void setStartParametersRoughness(Byte receiveRoughnessOfSensors) {

    }

    @Override
    public void setStartParametersBattery(Integer receiveBatteryTension) {

    }



}
