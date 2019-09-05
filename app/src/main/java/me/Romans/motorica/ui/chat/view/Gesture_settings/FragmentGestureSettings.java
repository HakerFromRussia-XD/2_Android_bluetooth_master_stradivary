package me.Romans.motorica.ui.chat.view.Gesture_settings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.Romans.motorica.MyApp;
import me.Romans.motorica.R;
import me.Romans.motorica.data.GesstureAdapter;
import me.Romans.motorica.data.Gesture_my;
import me.Romans.motorica.ui.chat.data.ChatModule;
import me.Romans.motorica.ui.chat.data.DaggerChatComponent;
import me.Romans.motorica.ui.chat.presenter.ChatPresenter;
import me.Romans.motorica.ui.chat.view.ChatActivity;
import me.Romans.motorica.ui.chat.view.ChatView;

public class FragmentGestureSettings extends Fragment implements ChatView, GesstureAdapter.OnGestureMyListener {
    @BindView(R.id.gesture_use) Button gesture_use;
    private RecyclerView recyclerView;
    private GesstureAdapter gestureAdapter;
    private List <Gesture_my> gestureMyList;
    private int indicatorTypeMessage = 0x04;
    private int GESTURE_NUMBER = 0x0000;
    private int GripperNumberStart1 = 0xA000;
    private int mySensorEvent1 = 0xB000;
    private int GripperNumberEnd1 = 0xC001;
    private int GripperNumberStart2 = 0xA001;
    private int mySensorEvent2 = 0xB001;
    private int GripperNumberEnd2 = 0xC000;
    private byte[] TextByteTreegSettings = new byte[15];
    private byte[] TextByteTreegControl = new byte[2];

    public View view;
    private ChatActivity chatActivity;

    @Inject ChatPresenter presenter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_gesture_settings, container, false);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(FragmentGestureSettings.this))
                .build().inject(FragmentGestureSettings.this);
        ButterKnife.bind(this, view);

        if (getActivity() != null) {chatActivity = (ChatActivity) getActivity();}

        for (int i = 0; i<chatActivity.MAX_NUMBER_DETAILS; i++) {
            chatActivity.indexCount = chatActivity.verticesArrey[i].length;
            System.err.println("LessonEightActivity--------> количество элементов в массиве №" + (i + 1) + " " + chatActivity.indexCount);
        }

        gestureMyList = new ArrayList<>();
        recyclerView = view.findViewById(R.id.gripper_list);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        chatActivity.firstTapRcyclerView = true;

        gestureMyList.add(
                new Gesture_my(
                        1,
                        R.drawable.olpen,
                        "bla bla bla",
                        "Позволяет настоить положение максимально открытого состояния",
                        "Настройка открытого сотояния",
                        1,
                        60));
        gestureMyList.add(
                new Gesture_my(
                        1,
                        R.drawable.close,
                        "bla bla bla",
                        "Позволяет настоить положение максимально закрытого состояния",
                        "Настройка закрытого сотояния",
                        2,
                        6));

        gestureAdapter = new GesstureAdapter(getActivity(), gestureMyList, FragmentGestureSettings.this);
        recyclerView.setAdapter(gestureAdapter);

        gesture_use.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                   chatActivity.CompileMassegeSwitchGesture((byte) 0x00, (byte) 0x01);
                   chatActivity.TranslateMassegeSwitchGesture();
                   chatActivity.fragmentManager.beginTransaction()
                           .remove(chatActivity.fragmentGestureSettings)
                           .commit();
                   chatActivity.navigation.clearAnimation();
                   chatActivity.navigation.animate().translationY(0).setDuration(200);
                }
            }
        });
        return view;
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
        switch (position) {
            case 0:
                if(chatActivity.firstTapRcyclerView){
                    chatActivity.firstTapRcyclerView = false;
                    chatActivity.fragmentManager.beginTransaction()
                            .add(R.id.view_pager, chatActivity.fragmentGripperSettings)
                            .commit();
                    for (int j = 0; j<chatActivity.MAX_NUMBER_DETAILS; j++) {
                        try {
                            chatActivity.threadFanction[j].join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case 1:
//                chatActivity.fragmentManager.beginTransaction()
//                        .remove(chatActivity.fragmentGestureSettings)
//                        .add(R.id.view_pager, chatActivity.fragmentGripperSettings2)
//                        .commit();
                break;
            default:
//                chatActivity.fragmentManager.beginTransaction()
//                        .remove(chatActivity.fragmentGestureSettings)
//                        .add(R.id.view_pager, chatActivity.fragmentGripperSettings)
//                        .commit();
                break;

        }
    }
    @Override
    public void setGeneralValue(int receiveСurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) {

    }
    @Override
    public void setStartParameters(Integer receiveСurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication) {

    }
}
