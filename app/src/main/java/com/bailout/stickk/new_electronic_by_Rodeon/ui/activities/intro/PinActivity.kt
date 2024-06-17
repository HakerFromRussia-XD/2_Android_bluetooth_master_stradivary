package com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.intro

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bailout.stickk.R
import com.bailout.stickk.databinding.ActivityPinBinding
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.bailout.stickk.scan.view.ScanActivity

class PinActivity : AppCompatActivity() {
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null

    private lateinit var binding: ActivityPinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.navigationBarColor = this.resources.getColor(R.color.color_primary, theme)
        window.statusBarColor = this.resources.getColor(R.color.blue_status_bar, theme)
        mSettings = getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)

        val pinCodeApp = mSettings!!.getString(PreferenceKeys.APP_PIN_CODE, "1234")

        binding.pincodeView.requestToShowKeyboard()
        Handler().postDelayed({
            binding.pincodeView.showKeyboard()
        }, 200)


        binding.pincodeView.setPasscodeEntryListener { passcode ->
            System.err.println("tup enter passcode: $passcode")
            if (passcode == pinCodeApp.toString()) {
                launchScanActivity()
                hideKeyboard(binding.pincodeView)
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.app_pin_entering_varning),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun View.showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }
    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun launchScanActivity() {
        if ((ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)) {
            val intent = Intent(this@PinActivity, ScanActivity::class.java)
            startActivity(intent)
            finish()
        }
        System.err.println("переход внутрь приложения")
    }
}