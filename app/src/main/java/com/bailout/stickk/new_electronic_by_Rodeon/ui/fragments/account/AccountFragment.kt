package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.account

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.FragmentPersonalAccountBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.connection.Requests
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.bailout.stickk.new_electronic_by_Rodeon.utils.EncryptionManagerUtils
import com.google.gson.Gson
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class AccountFragment : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: AccountAdapter? = null

    private var token = ""
    private var gson: Gson? = null
    private var encryptionManager: EncryptionManagerUtils? = null
    private var encryptionResult: String? = null
    private var testSerialNumber = "FEST-EP-05674"
    private var myRequests: Requests? = null

    private lateinit var binding: FragmentPersonalAccountBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPersonalAccountBinding.inflate(layoutInflater)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.mContext = context
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gson = Gson()
        myRequests = Requests()
        encryptionManager = EncryptionManagerUtils.instance
        encryptionResult = encryptionManager?.encrypt(testSerialNumber)

        accountList = ArrayList()
        requestToken()
        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { requestToken() }

        initializeUI()
    }

    private fun requestToken() {
        CoroutineScope(Dispatchers.Main).launch {
            myRequests!!.getRequestToken(
                { token ->
                    this@AccountFragment.token = token
                    requestUserData()
                },
                { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show()},
                "Aesserial $encryptionResult")
        }
    }
    private fun requestUserData() {
        CoroutineScope(Dispatchers.Main).launch {
            myRequests!!.getRequestUser(
                { user ->
                    binding.apply {
                        accountList.clear()
                        accountList.add(AccountItem(
                            avatarUrl = "avatarUrl",
                            name = user.clientData?.fio.toString(),
                            surname = user.clientData?.fio.toString(),
                            patronymic = "Ivanovich",
                            versionDriver = "111111111.111",
                            versionBms = "222222.22222",
                            versionSensors = "33333.33333"))
                        initAdapter(binding.accountRv)
                        binding.refreshLayout.setRefreshing(false)
                    }
                },
                { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show() },
                token = this@AccountFragment.token
            )
        }
    }
    private fun initAdapter(accountRv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        accountRv.layoutManager = linearLayoutManager
        adapter = AccountAdapter(object : OnAccountClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onCustomerServiceClicked() {
//                CoroutineScope(Dispatchers.Main).launch {
//                    myRequests?.postRequestSettings(
//                        { error -> Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show() },
//                        token = token,
//                        prosthesisId = address,
//                        gson = this@MainActivity.gson!!
//                    )
//                }
                main?.showToast("onCustomerServiceClicked")
            }

            override fun onProsthesisInformationClicked() {
                main?.showToast("onProsthesisInformationClicked")
//                CoroutineScope(Dispatchers.Main).launch {
//                    myRequests?.getRequestProthesisSettings(
//                        { allOptions ->
//                            binding.apply {
//                                tvUserId.visibility = View.VISIBLE
//                                progressBar.visibility = View.GONE
//                                tvUserId.text = allOptions.toString()
//                            }
//                        },
//                        { error -> Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show() },
//                        token = token,
//                        prosthesisId = id
//                    )
//                }
            }
        })
        accountRv.adapter = adapter
    }
    private fun initializeUI() {
        binding.titleClickBlockBtn.setOnClickListener {  }
        initAdapter(binding.accountRv)

        binding.backBtn.setOnClickListener { navigator().goingBack() }
    }

    companion object {
        var accountList by Delegates.notNull<ArrayList<AccountItem>>()
    }
}