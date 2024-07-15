package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.ubi3.account.customerServiceFragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.FragmentPersonalAccountCustomerServiceBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.connection.Requests
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.bailout.stickk.new_electronic_by_Rodeon.utils.EncryptionManagerUtils
import com.google.gson.Gson
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class AccountFragmentCustomerService : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: AccountCustomerServiceAdapter? = null

    private var token = ""
    private var gson: Gson? = null
    private var encryptionManager: EncryptionManagerUtils? = null
    private var encryptionResult: String? = null
    private var testSerialNumber = "FEST-EP-05674"
    private var myRequests: Requests? = null

    private lateinit var binding: FragmentPersonalAccountCustomerServiceBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPersonalAccountCustomerServiceBinding.inflate(layoutInflater)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.mContext = context
        testSerialNumber = main?.mDeviceName.toString()
        System.err.println("TEST SERIAL NUMBER $testSerialNumber")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gson = Gson()
        myRequests = Requests()
        encryptionManager = EncryptionManagerUtils.instance
        encryptionResult = encryptionManager?.encrypt(testSerialNumber)
        System.err.println("Aesserial $encryptionResult")

        accountCustomerServiceList = ArrayList()
//        requestToken()
        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener {
//            requestToken()
            binding.refreshLayout.setRefreshing(false)
        }

        initializeUI()


        val dateOfReceipt: String = main?.loadText(PreferenceKeys.ACCOUNT_DATE_TRANSFER_PROSTHESIS).toString()//"14.03.2021"
        var warrantyDate: String? = null
        if (dateOfReceipt.length > 7 ) {
            val year = dateOfReceipt.takeLast(4).toInt()
            System.err.println("year test: $year")
            warrantyDate = dateOfReceipt.take(6) + (year+3).toString()
        }


        accountCustomerServiceList.clear()
        accountCustomerServiceList.add(
            AccountCustomerServiceItem(
                dateOfReceiptOfProsthesis = dateOfReceipt,
                warrantyExpirationDate = warrantyDate.toString(),
                yourManager = main?.loadText(PreferenceKeys.ACCOUNT_MANAGER_FIO).toString(),
                yourManagerPhone = main?.loadText(PreferenceKeys.ACCOUNT_MANAGER_PHONE).toString(),
                prosthesisStatus = main?.loadText(PreferenceKeys.ACCOUNT_STATUS_PROSTHESIS).toString())
        )
        initAdapter(binding.accountCustomerServiceRv)
    }

    private fun requestToken() {
        CoroutineScope(Dispatchers.Main).launch {
            myRequests!!.getRequestToken(
                { token ->
                    this@AccountFragmentCustomerService.token = token
//                    requestUserData()
                },
                { error -> main?.runOnUiThread {Toast.makeText(mContext, "AccountFragmentCustomerService requestToken $error", Toast.LENGTH_SHORT).show()}},
                "Aesserial $encryptionResult")
        }
    }
    private fun initAdapter(accountRv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(mContext)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        accountRv.layoutManager = linearLayoutManager
        adapter =
            AccountCustomerServiceAdapter(
                object : OnAccountCustomerServiceClickListener {
                    override fun onYourMangerClicked() {
                        val intent = Intent(
                            Intent.ACTION_DIAL,
                            Uri.parse(
                                "tel:${
                                    main?.loadText(PreferenceKeys.ACCOUNT_MANAGER_PHONE).toString()
                                }"
                            )
                        )
                        if (intent.resolveActivity(main!!.packageManager) != null) {
                            startActivity(intent)
                        }
                    }
                })
        accountRv.adapter = adapter
    }
    private fun initializeUI() {
        binding.titleClickBlockBtn.setOnClickListener {  }
        initAdapter(binding.accountCustomerServiceRv)

        binding.backBtn.setOnClickListener { navigator().goingBack() }
    }

    companion object {
        var accountCustomerServiceList by Delegates.notNull<ArrayList<AccountCustomerServiceItem>>()
    }
}