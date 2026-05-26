package com.bailout.stickk.ubi4.ui.fragments.account.customerServiceFragmentUBI4

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.Ubi4FragmentPersonalAccountCustomerServiceBinding
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.utils.EncryptionManagerUtils
import com.bailout.stickk.ubi4.contract.NavigatorUBI4
import com.bailout.stickk.ubi4.data.network.RequestsUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.google.gson.Gson
import com.simform.refresh.SSPullToRefreshLayout
import kotlin.properties.Delegates


class AccountFragmentCustomerServiceUBI4 : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivityUBI4? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: AccountCustomerServiceAdapterUbi4? = null

    private var token = ""
    private var gson: Gson? = null
    private var encryptionManager: EncryptionManagerUtils? = null
    private var encryptionResult: String? = null
    private var testSerialNumber = "FEST-F-05670"
    private var myRequests: RequestsUBI4? = null

    private var _binding: Ubi4FragmentPersonalAccountCustomerServiceBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = Ubi4FragmentPersonalAccountCustomerServiceBinding.inflate(inflater, container, false)
//        WDApplication.component.inject(this)
        Log.d("AccountFragment", "Activity: $activity, is NavigatorUBI4: ${activity is NavigatorUBI4}")
        if (activity != null) { main = activity as MainActivityUBI4? }
        this.mContext = context
        testSerialNumber = main?.mDeviceName.toString()
        System.err.println("TEST SERIAL NUMBER $testSerialNumber")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gson = Gson()
        myRequests = RequestsUBI4()
        //TODO Узнать у Ромы нужно ли перенести encryptionManager = EncryptionManagerUtils.instance в UBI4
        // ответ от Ромы: можно использовать старый из UBI3 как тут и сделано
        encryptionManager = EncryptionManagerUtils.instance
        encryptionResult = encryptionManager?.encrypt(testSerialNumber)
        System.err.println("Aesserial $encryptionResult")

        accountCustomerServiceList = ArrayList()
//        requestToken()
        //TODO  так же проверить что данные от UBI4
        // ответ от Ромы: А почему тут requestToken выпилен? Полезно же при рефреше перезапрашивать
        // его во избежание ситуации, которая у нас была при отправке файлов для старта обучения
        // модели на сервере (тогда тоже сначала не перезапрашивали токен)
        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener {
//            requestToken()
            binding.refreshLayout.setRefreshing(false)
        }

        //TODO изменить константы val dateOfReceipt: String = main?.loadText(PreferenceKeys.ACCOUNT_DATE_TRANSFER_PROSTHESIS).toString()//"14.03.2021"
        val dateOfReceipt: String = main?.loadText(PreferenceKeys.ACCOUNT_DATE_TRANSFER_PROSTHESIS).toString()//"14.03.2021"
        var warrantyDate: String? = null
        if (dateOfReceipt.length > 7 ) {
            val year = dateOfReceipt.takeLast(4).toInt()
            System.err.println("year test: $year")
            warrantyDate = dateOfReceipt.take(6) + (year+3).toString()
        }


        accountCustomerServiceList.clear()
        accountCustomerServiceList.add(
            AccountCustomerServiceItemUBI4(
                dateOfReceiptOfProsthesis = dateOfReceipt,
                warrantyExpirationDate = warrantyDate.toString(),
                yourManager = main?.loadText(PreferenceKeys.ACCOUNT_MANAGER_FIO).toString(),
                yourManagerPhone = main?.loadText(PreferenceKeys.ACCOUNT_MANAGER_PHONE).toString(),
                prosthesisStatus = main?.loadText(PreferenceKeys.ACCOUNT_STATUS_PROSTHESIS).toString())
        )

        initializeUI()
    }

    //    private fun requestToken() {
//        CoroutineScope(Dispatchers.Main).launch {
//            myRequests!!.getRequestToken(
//                { token ->
//                    this@AccountFragmentCustomerServiceUBI4.token = token
//                    requestUserData()
//                },
//                { error -> main?.runOnUiThread { Toast.makeText(mContext, "AccountFragmentCustomerService requestToken $error", Toast.LENGTH_SHORT).show()}},
//                "Aesserial $encryptionResult")
//        }
//    }
    private fun initAdapter(accountRv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(mContext)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        accountRv.layoutManager = linearLayoutManager
        adapter =
            AccountCustomerServiceAdapterUbi4(
                object : OnAccountCustomerServiceUBI4ClickListener {
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
//        binding.titleClickBlockBtn.setOnClickListener {  }
        binding.titleClickBlockBtnUbi4.setOnClickListener{}
        initAdapter(binding.accountCustomerServiceRv)

        binding.backBtn.setOnClickListener {
            Log.d("AccountFragment", "Clicked: activity = $activity, is NavigatorUBI4: ${activity is NavigatorUBI4}")
            (activity as? NavigatorUBI4)?.goingBackUbi4() ?:
            println("Activity не реализует NavigatorUBI4")
        }
    }

    override fun onDestroyView() {
        binding.accountCustomerServiceRv.adapter = null
        adapter = null
        linearLayoutManager = null
        myRequests = null
        gson = null
        encryptionManager = null
        encryptionResult = null
        main = null
        mContext = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        var accountCustomerServiceList by Delegates.notNull<ArrayList<AccountCustomerServiceItemUBI4>>()
    }
}
