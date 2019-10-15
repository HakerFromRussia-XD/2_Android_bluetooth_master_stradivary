package me.Romans.motorica.ui.chat.view.Service_settings;

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

public class FragmentServiceSettings extends Fragment implements ChatView {
    @BindView(R.id.save_service_settings) Button save_service_settings;
    public View view;
    private ChatActivity chatActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_service_settings, container, false);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(FragmentServiceSettings.this))
                .build().inject(FragmentServiceSettings.this);
        ButterKnife.bind(this, view);

        if (getActivity() != null) {chatActivity = (ChatActivity) getActivity();}
        chatActivity.graphThreadFlag = false;

        save_service_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    chatActivity.fragmentManager.beginTransaction()
                            .remove(chatActivity.fragmentServiceSettings)
                            .commit();
                    chatActivity.navigation.clearAnimation();
                    chatActivity.navigation.animate().translationY(0).setDuration(200);
                    chatActivity.graphThreadFlag = true;
                    chatActivity.startGraphEnteringDataThread();
                    chatActivity.myMenu.setGroupVisible(R.id.service_settings, true);
                    chatActivity.myMenu.setGroupVisible(R.id.modes, false);
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

    }

    @Override
    public void setGeneralValue(int receiveСurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) {

    }
    @Override
    public void setStartParameters(Integer receiveСurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication) {

    }

    @Override
    public void setStartParametersInGraphActivity() {

    }
}
