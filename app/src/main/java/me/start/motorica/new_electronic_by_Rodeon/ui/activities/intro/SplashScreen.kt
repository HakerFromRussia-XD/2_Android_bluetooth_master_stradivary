package me.start.motorica.new_electronic_by_Rodeon.ui.activities.intro

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import me.start.motorica.R
import me.start.motorica.scan.view.ScanActivity

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
//        System.err.println(" LOLOLOEFWEF --->  SplashScreen onCreate")

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.navigationBarColor = this.resources.getColor(R.color.color_primary, theme)
        window.statusBarColor = this.resources.getColor(R.color.blue_status_bar, theme)

        askPermissions()
    }

    private fun askPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Handler().postDelayed({
                requestMultiplePermissions.launch(arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT))
                launchScanActivity()
            }, 300)

        }
        else{
//            System.err.println(" LOLOLOEFWEF --->  askPermissions()  Build.VERSION_CODES.S  false")
            Dexter.withActivity(this).withPermissions(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.areAllPermissionsGranted()) {
//                            System.err.println(" LOLOLOEFWEF --->  onPermissionsChecked true")
                            val intent = Intent(this@SplashScreen, ScanActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
//                            System.err.println(" LOLOLOEFWEF --->  onPermissionsChecked false")
                            Toast.makeText(this@SplashScreen, R.string.we_need_these_permissions, Toast.LENGTH_SHORT
                            ).show()
                            askPermissions()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: List<PermissionRequest>,
                        token: PermissionToken
                    ) {
                        token.continuePermissionRequest()
                    }
                }).check()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        System.err.println(" LOLOLOEFWEF --->  onRequestPermissionsResult")
        launchScanActivity()
    }

    private fun launchScanActivity() {
        if ((ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)) {
//            System.err.println(" LOLOLOEFWEF --->  launchScanActivity true")
            val intent = Intent(this@SplashScreen, ScanActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    @SuppressLint("LogNotTimber")
    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
//                System.err.println("LOLOLOEFWEF --->  ${it.key} = ${it.value}")
            }
        }
}