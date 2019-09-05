package me.Romans.motorica.ui.chat.view.Gripper_settings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.Romans.motorica.MyApp;
import me.Romans.motorica.R;
import me.Romans.motorica.ui.chat.data.ChatModule;
import me.Romans.motorica.ui.chat.data.DaggerChatComponent;
import me.Romans.motorica.ui.chat.presenter.ChatPresenter;
import me.Romans.motorica.ui.chat.view.ChatActivity;
import me.Romans.motorica.ui.chat.view.ChatView;

public class FragmentGripperSettings2 extends Fragment implements ChatView {
    @BindView(R.id.gesture_use) Button gesture_use;
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
        view = inflater.inflate(R.layout.fragment_gripper_settings, container, false);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(FragmentGripperSettings2.this))
                .build().inject(FragmentGripperSettings2.this);
        ButterKnife.bind(this, view);

        if (getActivity() != null) {chatActivity = (ChatActivity) getActivity();}


        gesture_use.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                   chatActivity.CompileMassegeSwitchGesture((byte) 0x02, (byte) 0x03);
                   chatActivity.TranslateMassegeSwitchGesture();
                   chatActivity.fragmentManager.beginTransaction()
                           .remove(chatActivity.fragmentGripperSettings)
                           .commit();
                   chatActivity.firstTapRcyclerView = true;
                }
            }
        });
        return view;
    }

    @Override
    public void setStatus(String status) { }
    @Override
    public void setStatus(int resId) { }
    @Override
    public void setValueCH(int levelCH, int numberChannel) { }
    @Override
    public void setErrorReception(boolean incomeErrorReception) { }
    @Override
    public void appendMessage(String message) { }
    @Override
    public void enableHWButton(boolean enabled) { }
    @Override
    public void showToast(String message) { }
    @Override
    public void onGestureClick(int position) { }
    @Override
    public void setGeneralValue(int receiveСurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) { }
    @Override
    public void setStartParameters(Integer receiveСurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication) { }
}
