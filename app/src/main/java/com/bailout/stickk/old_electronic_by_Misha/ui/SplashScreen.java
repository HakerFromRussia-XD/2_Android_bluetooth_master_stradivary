package com.bailout.stickk.old_electronic_by_Misha.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import com.bailout.stickk.R;
import com.bailout.stickk.scan.view.ScanActivity;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        askPermissions();
    }

    void askPermissions(){
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION)
            .withListener(new MultiplePermissionsListener() {
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport report) {
                    if(report.areAllPermissionsGranted()){
                        Intent intent = new Intent(SplashScreen.this, ScanActivity.class);
                        startActivity(intent);
                        Log.d("onPermissionsChecked", "Success");
                        finish();
                    }
                    else{
                        Toast.makeText(SplashScreen.this, R.string.we_need_these_permissions, Toast.LENGTH_SHORT).show();
                        askPermissions();
                        Log.d("onPermissionsChecked", "Fail");
                    }

                }

                @Override
                public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                    token.continuePermissionRequest();
                }
            }).check();

    }
}
