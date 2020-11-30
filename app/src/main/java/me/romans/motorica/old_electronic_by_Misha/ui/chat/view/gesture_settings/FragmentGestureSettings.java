package me.romans.motorica.old_electronic_by_Misha.ui.chat.view.gesture_settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.romans.motorica.old_electronic_by_Misha.MyApp;
import me.romans.motorica.R;
import me.romans.motorica.old_electronic_by_Misha.data.GesstureAdapter;
import me.romans.motorica.old_electronic_by_Misha.data.Gesture_my;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.data.ChatModule;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.data.DaggerChatComponent;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.view.ChartActivity;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.view.ChartView;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.view.Massages;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.view.Load3DModel;

public class FragmentGestureSettings extends Fragment implements ChartView, GesstureAdapter.OnGestureMyListener {
    public Button gesture_use;
    private int GESTURE_NUMBER = 0x0000;

    public View view;
    private ChartActivity chatActivity;
    Massages mMassages = new Massages();
    Load3DModel mLoad3DModel;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_gesture_settings, container, false);
        gesture_use = view.findViewById(R.id.gesture_use);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(FragmentGestureSettings.this))
                .build().inject(FragmentGestureSettings.this);
        ButterKnife.bind(this, view);

        if (getActivity() != null) {chatActivity = (ChartActivity) getActivity();}
/*        mLoad3DModel = new Load3DModel();*/

        List<Gesture_my> gestureMyList = new ArrayList<>();
        RecyclerView recyclerView = view.findViewById(R.id.gripper_list);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        chatActivity.firstTapRecyclerView = true;
        chatActivity.graphThreadFlag = false;
        if(chatActivity.isEnable){
            if(chatActivity.getFlagUseHDLCProtocol()){
            } else {
                chatActivity.presenter.onHelloWorld(mMassages.CompileMassageControlComplexGesture(GESTURE_NUMBER));
            }
        }

        gestureMyList.add(
                new Gesture_my(
                        1,
                        R.drawable.olpen,
                        "bla bla bla",
                        getString(R.string.allows_you_to_set_the_position_of_the_maximum_open_state),
                        getString(R.string.control_of_an_open_state),
                        1,
                        60));
        gestureMyList.add(
                new Gesture_my(
                        1,
                        R.drawable.close,
                        "bla bla bla",
                        getString(R.string.allows_you_to_set_the_position_of_the_maximum_closed_state),
                        getString(R.string.control_of_the_closed_state),
                        2,
                        6));

        GesstureAdapter gestureAdapter = new GesstureAdapter(getActivity(), gestureMyList, FragmentGestureSettings.this);
        recyclerView.setAdapter(gestureAdapter);

        gesture_use.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    if(chatActivity.isEnable){
                        if(chatActivity.getFlagUseHDLCProtocol()){
                        } else {
                            chatActivity.presenter.onHelloWorld(mMassages.CompileMassageControlComplexGesture(GESTURE_NUMBER));
                        }
                    }
                    chatActivity.fragmentManager.beginTransaction()
                            .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                            .remove(chatActivity.fragmentGestureSettings)
                            .commit();
                    chatActivity.navigation.clearAnimation();
                    chatActivity.navigation.animate().translationY(0).setDuration(200);
                    chatActivity.graphThreadFlag = true;
                    chatActivity.startGraphEnteringDataThread();
                }
            }
        });
        return view;
    }

    public void backPressed() {
        if (getActivity() != null) {
            if(chatActivity.isEnable){
                chatActivity.presenter.onHelloWorld(mMassages.CompileMassageControlComplexGesture(GESTURE_NUMBER));
            }
            chatActivity.fragmentManager.beginTransaction()
                    .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                    .remove(chatActivity.fragmentGestureSettings)
                    .commit();
            chatActivity.navigation.clearAnimation();
            chatActivity.navigation.animate().translationY(0).setDuration(200);
            chatActivity.graphThreadFlag = true;
            chatActivity.startGraphEnteringDataThread();
        }
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
        switch (position) {
            case 0:
                if(chatActivity.firstTapRecyclerView && chatActivity.isEnable){
                    chatActivity.firstTapRecyclerView = false;
                    if (ChartActivity.NUMBER_CELL == 1 ) ChartActivity.NUMBER_CELL = (byte) (ChartActivity.NUMBER_CELL - 0x01);
                    chatActivity.fragmentManager.beginTransaction()
                            .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                            .add(R.id.view_pager, chatActivity.fragmentGripperSettings)
                            .addToBackStack("myStack")
                            .commit();
                    for (int j = 0; j< Load3DModel.MAX_NUMBER_DETAILS; j++) {
                        try {
                            chatActivity.threadFunction[j].join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    chatActivity.transferThreadFlag = true;
                    chatActivity.startTransferThread();
                }
                break;
            case 1:
                if(chatActivity.firstTapRecyclerView && chatActivity.isEnable){
                    chatActivity.firstTapRecyclerView = false;
                    if (ChartActivity.NUMBER_CELL == 0 ) ChartActivity.NUMBER_CELL = (byte) (ChartActivity.NUMBER_CELL + 0x01);
                    chatActivity.fragmentManager.beginTransaction()
                            .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                            .add(R.id.view_pager, chatActivity.fragmentGripperSettings)
                            .addToBackStack("myStack")
                            .commit();
                    for (int j = 0; j< Load3DModel.MAX_NUMBER_DETAILS; j++) {
                        try {
                            chatActivity.threadFunction[j].join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    chatActivity.transferThreadFlag = true;
                    chatActivity.startTransferThread();
                }
                break;

        }
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
