package me.aflak.libraries.ui.chat.view.Gesture_settings;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import me.aflak.libraries.MyApp;
import me.aflak.libraries.R;
import me.aflak.libraries.data.GesstureAdapter;
import me.aflak.libraries.data.Gesture_my;
import me.aflak.libraries.ui.chat.data.ChatModule;
import me.aflak.libraries.ui.chat.data.DaggerChatComponent;
import me.aflak.libraries.ui.chat.presenter.ChatPresenter;
import me.aflak.libraries.ui.chat.view.ChatView;
import me.aflak.libraries.ui.chat.view.Gripper_settings.GripperSettings;
import me.aflak.libraries.ui.chat.view.Gripper_settings.GripperSettings2;
import me.aflak.libraries.ui.chat.view.Gripper_settings.GripperSettings5;
import me.aflak.libraries.ui.chat.view.Gripper_settings.GripperSettings6;

public class Gesture_settings3 extends AppCompatActivity implements GesstureAdapter.OnGestureMyListener, ChatView {

    private static final String TAG = "Gesture_settings3";

    RecyclerView recyclerView;
    GesstureAdapter gestureAdapter;
    List<Gesture_my> gestureMyList;

    @Inject ChatPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gesture_settings);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(Gesture_settings3.this))
                .build().inject(Gesture_settings3.this);
        ButterKnife.bind(Gesture_settings3.this);

        gestureMyList = new ArrayList<>();
        recyclerView = (RecyclerView) findViewById(R.id.gestures_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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

        gestureAdapter = new GesstureAdapter(this, gestureMyList,this);
//        gestureAdapter = new GesstureAdapter(this, gestureMyList, this);
        recyclerView.setAdapter(gestureAdapter);

        getIncomingIntent();
    }

    private void getIncomingIntent(){
        if (getIntent().hasExtra("image_url") && getIntent().hasExtra("image_name")){
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
        final BluetoothDevice device = getIntent().getExtras().getParcelable("device");
        switch (position) {
            case 0:
                Intent intent = new Intent(this, GripperSettings5.class);
                intent.putExtra("device", device);
                startActivity(intent);
                break;
            case 1:
                Intent intent2 = new Intent(this, GripperSettings6.class);
                intent2.putExtra("device", device);
                startActivity(intent2);
                break;
            default:
                Intent intent_b = new Intent(this, GripperSettings5.class);
                intent_b.putExtra("device", device);
                startActivity(intent_b);
                break;

        }
    }
}
