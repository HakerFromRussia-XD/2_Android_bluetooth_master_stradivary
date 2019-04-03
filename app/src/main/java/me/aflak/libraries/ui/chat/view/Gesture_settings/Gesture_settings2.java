package me.aflak.libraries.ui.chat.view.Gesture_settings;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
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
import me.aflak.libraries.ui.chat.view.Gripper_settings.GripperSettings3;
import me.aflak.libraries.ui.chat.view.Gripper_settings.GripperSettings4;

public class Gesture_settings2 extends AppCompatActivity implements GesstureAdapter.OnGestureMyListener, ChatView {

    private static final String TAG = "Gesture_settings2";

    @BindView(R.id.gesture_use) Button gesture_use;
    RecyclerView recyclerView;
    GesstureAdapter gestureAdapter;
    List<Gesture_my> gestureMyList;
    private int indicatorTypeMessage = 0x04;
    private int GESTURE_NUMBER = 0x0001;
    private int GripperNumberStart1 = 0xA000;
    private int mySensorEvent1 = 0xB000;
    private int GripperNumberEnd1 = 0xC001;
    private int GripperNumberStart2 = 0xA001;
    private int mySensorEvent2 = 0xB001;
    private int GripperNumberEnd2 = 0xC000;
    private byte[] TextByteTreeg = new byte[15];

    @Inject ChatPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gesture_settings);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(Gesture_settings2.this))
                .build().inject(Gesture_settings2.this);
        ButterKnife.bind(Gesture_settings2.this);

        gestureMyList = new ArrayList<>();
        recyclerView = (RecyclerView) findViewById(R.id.gestures_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        presenter.onCreate(getIntent());
        getIncomingIntent();
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
        recyclerView.setAdapter(gestureAdapter);

        gesture_use.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CompileMassege(GESTURE_NUMBER, GripperNumberStart1, mySensorEvent1, GripperNumberEnd1, GripperNumberStart2, mySensorEvent2, GripperNumberEnd2);
                presenter.onHelloWorld(TextByteTreeg);
            }
        });

    }

    private void getIncomingIntent(){
        if (getIntent().hasExtra("image_url") && getIntent().hasExtra("image_name")){
        }
    }

    @Override
    public void setStatus(String status) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.onStart(Gesture_settings2.this);
    }

    @Override
    public void onStop() {
        super.onStop();
        presenter.disconnect();
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
        gesture_use.setEnabled(enabled);
    }

    @Override
    public void showToast(String message) {

    }

    private byte[] CompileMassege(int GESTURE_NUMBER, int GripperNumberStart1, int mySensorEvent1, int GripperNumberEnd1, int GripperNumberStart2, int mySensorEvent2, int GripperNumberEnd2){
        TextByteTreeg[0] = (byte) indicatorTypeMessage;
        TextByteTreeg[1] = (byte) (GESTURE_NUMBER >> 8);
        TextByteTreeg[2] = (byte) GESTURE_NUMBER;
        TextByteTreeg[3] = (byte) (GripperNumberStart1 >> 8);
        TextByteTreeg[4] = (byte) GripperNumberStart1;
        TextByteTreeg[5] = (byte) (mySensorEvent1 >> 8);
        TextByteTreeg[6] = (byte) mySensorEvent1;
        TextByteTreeg[7] = (byte) (GripperNumberEnd1 >> 8);
        TextByteTreeg[8] = (byte) GripperNumberEnd1;
        TextByteTreeg[9] = (byte) (GripperNumberStart2 >> 8);;
        TextByteTreeg[10] = (byte) GripperNumberStart2;
        TextByteTreeg[11] = (byte) (mySensorEvent2 >> 8);
        TextByteTreeg[12] = (byte) mySensorEvent2;
        TextByteTreeg[13] = (byte) (GripperNumberEnd2 >> 8);
        TextByteTreeg[14] = (byte) GripperNumberEnd2;

        return TextByteTreeg;
    }

    @Override
    public void onGestureClick(int position) {
        final BluetoothDevice device = getIntent().getExtras().getParcelable("device");
        switch (position) {
            case 0:
                Intent intent = new Intent(this, GripperSettings3.class);
                intent.putExtra("device", device);
                startActivity(intent);
                break;
            case 1:
                Intent intent2 = new Intent(this, GripperSettings4.class);
                intent2.putExtra("device", device);
                startActivity(intent2);
                break;
            default:
                Intent intent_b = new Intent(this, GripperSettings3.class);
                intent_b.putExtra("device", device);
                startActivity(intent_b);
                break;

        }
    }
}