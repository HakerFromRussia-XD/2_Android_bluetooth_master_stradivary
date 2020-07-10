package me.Romans.motorica.ui.chat.view.Gesture_settings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.Romans.motorica.MyApp;
import me.Romans.motorica.R;
import me.Romans.motorica.data.GesstureAdapter;
import me.Romans.motorica.data.Gesture_my;
import me.Romans.motorica.ui.chat.data.ChatModule;
import me.Romans.motorica.ui.chat.data.DaggerChatComponent;
import me.Romans.motorica.ui.chat.view.ChartActivity;
import me.Romans.motorica.ui.chat.view.ChatView;

import static android.support.constraint.Constraints.TAG;

public class FragmentGestureSettings3 extends Fragment implements ChatView, GesstureAdapter.OnGestureMyListener {
    @BindView(R.id.gesture_use) public Button gesture_use;
    private RecyclerView recyclerView;
    private GesstureAdapter gestureAdapter;
    private List <Gesture_my> gestureMyList;
    private int GESTURE_NUMBER = 0x0004;

    public View view;
    private ChartActivity chatActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_gesture_settings, container, false);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(FragmentGestureSettings3.this))
                .build().inject(FragmentGestureSettings3.this);
        ButterKnife.bind(this, view);

        if (getActivity() != null) {chatActivity = (ChartActivity) getActivity();}
        Log.e(TAG, "NUMBER_CELL = "+String.valueOf(chatActivity.NUMBER_CELL));

        gestureMyList = new ArrayList<>();
        recyclerView = view.findViewById(R.id.gripper_list);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        chatActivity.firstTapRecyclerView = true;
        chatActivity.graphThreadFlag = false;
        if(chatActivity.isEnable){
            if(chatActivity.getFlagUseHDLCProtocol()){
            } else {
                chatActivity.CompileMassageControlComplexGesture(GESTURE_NUMBER);
                chatActivity.TranslateMassageControlComplexGesture();
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

        gestureAdapter = new GesstureAdapter(getActivity(), gestureMyList, FragmentGestureSettings3.this);
        recyclerView.setAdapter(gestureAdapter);

        gesture_use.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    if(chatActivity.isEnable){
                        chatActivity.CompileMassageControlComplexGesture(GESTURE_NUMBER);
                        chatActivity.TranslateMassageControlComplexGesture();
                    }
                    chatActivity.fragmentManager.beginTransaction()
                            .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                            .remove(chatActivity.fragmentGestureSettings3)
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
                if(chatActivity.getFlagUseHDLCProtocol()){
                } else {
                    chatActivity.CompileMassageControlComplexGesture(GESTURE_NUMBER);
                    chatActivity.TranslateMassageControlComplexGesture();
                }
            }
            chatActivity.fragmentManager.beginTransaction()
                    .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                    .remove(chatActivity.fragmentGestureSettings3)
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
    public void appendMessage(String message) {

    }
    @Override
    public void enableHWButton(boolean enabled) {

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
                    if (chatActivity.NUMBER_CELL == 5 ) chatActivity.NUMBER_CELL = (byte) (chatActivity.NUMBER_CELL - 0x01);
                    chatActivity.fragmentManager.beginTransaction()
                            .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                            .add(R.id.view_pager, chatActivity.fragmentGripperSettings)
                            .addToBackStack("myStack")
                            .commit();
                    for (int j = 0; j<chatActivity.MAX_NUMBER_DETAILS; j++) {
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
                    if (chatActivity.NUMBER_CELL == 4 ) chatActivity.NUMBER_CELL = (byte) (chatActivity.NUMBER_CELL + 0x01);
                    chatActivity.fragmentManager.beginTransaction()
                            .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                            .add(R.id.view_pager, chatActivity.fragmentGripperSettings)
                            .addToBackStack("myStack")
                            .commit();
                    for (int j = 0; j<chatActivity.MAX_NUMBER_DETAILS; j++) {
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
    public void setStartParametersCurrent(Integer receiveСurrent) {

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
