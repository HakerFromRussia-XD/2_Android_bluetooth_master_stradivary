package me.Romans.motorica.ui.chat.view.service_settings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
import me.Romans.motorica.ui.chat.view.ChartView;
import me.Romans.motorica.ui.chat.view.Massages;
import me.Romans.motorica.utils.ConstantManager;

import static android.support.constraint.Constraints.TAG;

public class FragmentServiceSettings extends Fragment implements ChartView {
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
    @BindView(R.id.buttonEtEClaib) Button buttonEtECalibration;
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
    @BindView(R.id.editTextAddr) EditText editTextAddress;
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
    @BindView(R.id.checkboxSpeedIncrement) CheckBox checkboxSpeedIncrement;
    @BindView(R.id.checkboxDisableAngleControl) CheckBox checkboxDisableAngleControl;
    public View view;
    private ChartActivity chatActivity;
    Massages mMassages = new Massages();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_service_settings, container, false);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(FragmentServiceSettings.this))
                .build().inject(FragmentServiceSettings.this);
        ButterKnife.bind(this, view);

        if (getActivity() != null) {chatActivity = (ChartActivity) getActivity();}
        chatActivity.graphThreadFlag = false;
        chatActivity.updateServiceSettingsThreadFlag = true;
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
                Toast.makeText(parent.getContext(), getString(R.string.select_channel)+ sNumberOfChannel, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        if(chatActivity.getFlagUseHDLCProtocol()){
            buttonOPN.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.OPEN_STOP_CLOSE_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                                                                    ConstantManager.WRITE, 0x00, (byte) 0x00));
                    }
                }
            });
            buttonSTP.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.OPEN_STOP_CLOSE_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.WRITE, 0x02, (byte) 0x00));
                    }
                }
            });
            buttonCLS.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.OPEN_STOP_CLOSE_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.WRITE, 0x01, (byte) 0x00));
                    }
                }
            });
            checkboxSpeedIncrement.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        System.err.println("CLICK SPEED INCREMENT " + checkboxSpeedIncrement.isChecked());
                        if (checkboxSpeedIncrement.isChecked()){
                            chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.SPEED_INCREMENT_TYPE, (byte) chatActivity.numberOfChannel,
                                     ConstantManager.WRITE,  (byte) 0x01, (byte) 0x00));
                        } else {
                            chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.SPEED_INCREMENT_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.WRITE,  (byte) 0x00, (byte) 0x00));
                        }
                    }
                }
            });
            checkboxDisableAngleControl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.err.println("CLICK DISABLE ANGEL CONTROL " + checkboxDisableAngleControl.isChecked());
                    if (checkboxDisableAngleControl.isChecked()){
                        chatActivity.presenter.onHelloWorld(
                            mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.DISABLE_ANGLE_CONTROL_TYPE, (byte) chatActivity.numberOfChannel,
                                    ConstantManager.WRITE,  (byte) 0x01, (byte) 0x00));
                    } else {
                        chatActivity.presenter.onHelloWorld(
                            mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.DISABLE_ANGLE_CONTROL_TYPE, (byte) chatActivity.numberOfChannel,
                                    ConstantManager.WRITE,  (byte) 0x00, (byte) 0x00));
                    }
                }
            });
            buttonSet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int temp;
                    if(!editTextAddress.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                        temp =  Integer.parseInt(editTextAddress.getText().toString());
                        if (getActivity() != null) {
                            System.err.println("CLICK SET " + temp);
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.SET_ADDR_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE,  temp, (byte) 0x00));
                        }
                    }
                }
            });
            buttonGSetup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        System.err.println("CLICK G SETUP");
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.TEMP_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.READ,  0x00, (byte) 0x00));
                    }
                }
            });
            buttonSSetup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int temp;
                    if(!editTextTemp.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                        temp = Integer.parseInt(editTextTemp.getText().toString());
                        if (getActivity() != null) {
                            System.err.println("CLICK S SETUP " + temp);
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.TEMP_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE,  temp, (byte) 0x00));}
                    }
                }
            });
            switchCurrentControl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int temp;
                    if(switchCurrentControl.isChecked()){
                        if(!editTextMaxCurrentValue.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                            temp = Integer.parseInt(editTextMaxCurrentValue.getText().toString());
                            if (getActivity() != null) {
                                System.err.println("CLICK CURRENT CONTROL " + temp);
                                chatActivity.presenter.onHelloWorld(
                                        mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.CURRENT_CONTROL_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                                ConstantManager.WRITE,  temp, (byte) 0x01));
                            }
                        }
                    } else {
                        if (getActivity() != null) {
                            System.err.println("CLICK CURRENT CONTROL OFF");
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.CURRENT_CONTROL_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE,  0x00, (byte) 0x00));
                        }
                    }
                }
            });
            editTextCurrTimeOut.addTextChangedListener(new TextWatcher(){
                @Override
                public void afterTextChanged(Editable s) {
                    if(!editTextCurrTimeOut.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                        int temp;
                            temp = Integer.parseInt(editTextCurrTimeOut.getText().toString());
                        if (getActivity() != null) {
                            System.err.println("CLICK CURRENT TIMEOUT " + temp);
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.CURRENT_TIMEOUT_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE,  temp, (byte) 0x00));
                        }
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
                    if (getActivity() != null) {
                        System.err.println("CLICK CURRENTS");
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.CURRENTS_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.READ,  0x00, (byte) 0x00));
                    }
                }
            });

            buttonEtECalibration.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        System.err.println("CLICK ETE CALIB");
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.ETE_CALIBRATION_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.WRITE, 0x05, (byte) 0x00));
                    }
                }
            });

            buttonEEPROMSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        System.err.println("CLICK EEPROM SAVE");
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.EEPROM_SAVE_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.WRITE, 0x01, (byte) 0x00));
                    }
                }
            });

            buttonAngleFIX.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        System.err.println("CLICK ANGLE FIX");
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.ANGLE_FIX_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.WRITE, 0x01, (byte) 0x00));
                    }
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
                    if (getActivity() != null) {
                        System.err.println("CLICK G1");
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.OPEN_ANGEL_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.READ,  0x00, (byte) 0x00));
                    }
                }
            });
            buttonG2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        System.err.println("CLICK G2");
                    }
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
                    int temp;
                    if(!editTextOpenAngle.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                        temp = Integer.parseInt(editTextOpenAngle.getText().toString());
                        if (getActivity() != null) {
                            System.err.println("CLICK S1 " + temp);
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.OPEN_ANGEL_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE,  temp, (byte) 0x00));
                        }
                    }
                }
            });
            buttonS2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int temp;
                    if(!editTextCloseAngle.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                        temp = Integer.parseInt(editTextCloseAngle.getText().toString());
                        if (getActivity() != null) {
                            System.err.println("CLICK S2 " + temp);
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.CLOSE_ANGEL_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE,  temp, (byte) 0x00));
                        }
                    }
                }
            });
            buttonS3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int temp;
                    if(!editTextWideAngle.getText().toString().matches("") && (editTextCurrTimeOut.getText().toString().length() < 10)){
                        temp = Integer.parseInt(editTextWideAngle.getText().toString());
                        if (getActivity() != null) {
                            System.err.println("CLICK S3 " + temp);
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.WIDE_ANGEL_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE,  temp, (byte) 0x00));
                        }
                    }
                }
            });
            switchMagnetInvert.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(switchMagnetInvert.isChecked()){
                        if (getActivity() != null) {
                            System.err.println("CLICK MAGNET INVERT");
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.MAGNET_INVERT_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE,  0x01, (byte) 0x00));
                        }
                    } else {
                        if (getActivity() != null) {
                            System.err.println("CLICK MAGNET INVERT OFF");
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.MAGNET_INVERT_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE,  0x00, (byte) 0x00));
                        }
                    }

                }
            });
            switchReversMotor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(switchReversMotor.isChecked()){
                        if (getActivity() != null) {
                            System.err.println("CLICK REVERS MOTOR");
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.REVERS_MOTOR_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE,  0x01, (byte) 0x00));
                        }
                    } else {
                        if (getActivity() != null) {
                            System.err.println("CLICK REVERS MOTOR OFF");
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.REVERS_MOTOR_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE,  0x00, (byte) 0x00));
                        }
                    }

                }
            });
            switchZeroCrossing.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(switchZeroCrossing.isChecked()){
                        if (getActivity() != null) {
                            System.err.println("CLICK ZERO CROSSING");
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.ZERO_CROSSING_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE,  0x01, (byte) 0x00));
                        }
                    } else {
                        if (getActivity() != null) {
                            System.err.println("CLICK ZERO CROSSING OFF");
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.ZERO_CROSSING_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE,  0x00, (byte) 0x00));
                        }
                    }

                }
            });
            buttonE.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        System.err.println("CLICK E");
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.E_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.WRITE,  0x01, (byte) 0x00));
                    }
                }
            });
            buttonU.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        System.err.println("CLICK U");
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.U_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.READ,  0x00, (byte) 0x00));
                    }
                }
            });
            buttonO.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        System.err.println("CLICK O");
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.O_S_C_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.WRITE,  0x00, (byte) 0x00));
                    }
                }
            });
            buttonS4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        System.err.println("CLICK S4");
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.O_S_C_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.WRITE,  0x02, (byte) 0x00));
                    }
                }
            });
            buttonC.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        System.err.println("CLICK C");
                        chatActivity.presenter.onHelloWorld(
                                mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.O_S_C_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                        ConstantManager.WRITE,  0x01, (byte) 0x00));
                    }
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
                    if(chatActivity.getFlagUseHDLCProtocol()){
                        if (getActivity() != null) {
                            System.err.println("CLICK SPEED: "+ seekBar.getProgress());
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.SPEED_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                                                                        ConstantManager.WRITE, seekBar.getProgress(), (byte) 0x00));
                        }
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
                    if(chatActivity.getFlagUseHDLCProtocol()){
                        if (getActivity() != null) {
                            System.err.println("CLICK ANGLE: "+ seekBar.getProgress());
                            chatActivity.presenter.onHelloWorld(
                                    mMassages.CompileMassageSettingsCalibrationHDLC(ConstantManager.ANGLE_CALIB_TYPE, (byte) chatActivity.numberOfChannel,
                                            ConstantManager.WRITE, seekBar.getProgress(), (byte) 0x00));
                        }
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
                    chatActivity.updateServiceSettingsThreadFlag = false;
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
                byte roughness = (byte) (((byte) seekBar.getProgress()) + 1);
                if(chatActivity.getFlagUseHDLCProtocol()){
                    chatActivity.presenter.onHelloWorld(mMassages.CompileMassageRoughnessHDLC(roughness));
                } else {
                    chatActivity.presenter.onHelloWorld(mMassages.CompileMassageRoughness(roughness));
                }
                chatActivity.setReceiveRoughnessOfSensors(roughness);
                chatActivity.saveVariable( chatActivity.deviceName+"receiveRoughnessOfSensors", (int) roughness);
            }
        });

        switchInvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.err.println("FragmentServiceSettings-------------->");
                if (switchInvert.isChecked()){
                    chatActivity.invert = 0x01;
                    if(!chatActivity.getFlagUseHDLCProtocol()){
                        chatActivity.presenter.onHelloWorld(mMassages.CompileMassageCurrentSettingsAndInvert(chatActivity.current, chatActivity.invert));
                    }
                    chatActivity.invertChannel = true;
                    chatActivity.saveVariable(ChartActivity.deviceName +"invertChannel", 0x01);
                } else {
                    chatActivity.invert = 0x00;
                    if(!chatActivity.getFlagUseHDLCProtocol()){
                        chatActivity.presenter.onHelloWorld(mMassages.CompileMassageCurrentSettingsAndInvert(chatActivity.current, chatActivity.invert));
                    }
                    chatActivity.invertChannel = false;
                    chatActivity.saveVariable(ChartActivity.deviceName +"invertChannel", 0x00);
                }
                int temp = chatActivity.intValueCH1on;
                chatActivity.seekBarCH1on2.setProgress((int) (chatActivity.intValueCH2on/(chatActivity.multiplierSeekBar -0.1)));//-0.5
                chatActivity.seekBarCH2on2.setProgress((int) (temp/(chatActivity.multiplierSeekBar -0.1)));//-0.5
            }
        });

        switchNotUseInternalADC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchNotUseInternalADC.isChecked()){
                    if(chatActivity.getFlagUseHDLCProtocol()){
                        System.err.println("FragmentServiceSettings--------------> CompileMassegeSettingsNotUseInternalADCHDLC 0");
                        chatActivity.presenter.onHelloWorld(mMassages.CompileMassageSettingsNotUseInternalADCHDLC((byte) 0x00));
                        chatActivity.saveVariable(ChartActivity.deviceName +"InternalADC", 0x00);
                    }
                } else {
                    if(chatActivity.getFlagUseHDLCProtocol()){
                        System.err.println("FragmentServiceSettings--------------> CompileMassegeSettingsNotUseInternalADCHDLC 1");
                        chatActivity.presenter.onHelloWorld(mMassages.CompileMassageSettingsNotUseInternalADCHDLC((byte) 0x01));
                        chatActivity.saveVariable(ChartActivity.deviceName +"InternalADC", 0x01);
                    }
                }
            }
        });

        seekBarRoughness.setProgress(chatActivity.loadVariable(ChartActivity.deviceName +"receiveRoughnessOfSensors"));

        chatActivity.invertChannel = chatActivity.loadVariable(ChartActivity.deviceName + "invertChannel") == 0x01;
//      тоже что и в строчке выше  if (chatActivity.loadVariable(ChartActivity.deviceName +"invertChannel") == 0x01) {
//            chatActivity.invertChannel = true;
//        } else {
//            chatActivity.invertChannel = false;
//        }

        if (chatActivity.loadVariable(ChartActivity.deviceName +"InternalADC") == 0x00) {
            switchNotUseInternalADC.setChecked(true);
        } else {
            switchNotUseInternalADC.setChecked(false);
        }
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
            chatActivity.updateServiceSettingsThreadFlag = false;
            chatActivity.layoutSensors.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG,"FragmentServiceSettings------> "+ chatActivity.loadVariable(ChartActivity.deviceName +"receiveRoughnessOfSensors"));
//        seekBarRoughness.setProgress(chatActivity.loadVariable(chatActivity.deviceName+"receiveRoughnessOfSensors"));
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
    public void enableInterface(boolean enabled) {

    }

    @Override
    public void showToast(String message) {

    }

    @Override
    public void showToastWithoutConnection() {

    }

    @Override
    public void onGestureClick(int position) {

    }

    @Override
    public void setGeneralValue(int receiveCurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) {

    }

    @Override
    public void setStartParameters(Integer receiveCurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication, Byte receiveRoughnessOfSensors) {

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
    public void setStartParametersCurrent(Integer receiveCurrent) {

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
