package com.bailout.stickk.ubi4.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.local.OpticTrainingStruct
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class InferenceFragment : Fragment() {
    private lateinit var contentTextView: TextView
    private val disposables = CompositeDisposable()
    private val rxUpdateMainEvent = RxUpdateMainEventUbi4.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.ubi4_fragment_inference, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contentTextView = view.findViewById(R.id.inferenceContentTv)
//        main.bleCommandWithQueue(
//            BLECommands.requestTransferFlow(1),
//            MAIN_CHANNEL_CHARACTERISTIC,
//            WRITE
//        ) {}
        lifecycleScope.launch(Dispatchers.IO) {
            dataProcessing()
        }
    }

    private fun dataProcessing() {
        val parameterUpdateDisposable = rxUpdateMainEvent.uiOpticTrainingObservable
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(
                { parameterRef: ParameterRef ->
                    try {
                        val parameter = ParameterProvider.getParameter(
                            parameterRef.addressDevice,
                            parameterRef.parameterID
                        )
                        val dataStringRaw = parameter.data

                        if (dataStringRaw.isBlank() || dataStringRaw == "None")
                            return@subscribe

                        val opticTrainingStruct = Json.decodeFromString<OpticTrainingStruct>("\"${dataStringRaw}\"")
                        val dataString = opticTrainingStruct.data.joinToString(separator = " ") { floatValue ->
                            String.format("%.2f", floatValue)
                        }

                        val prbCount = 8
                        val afterPrb = 6
                        val dataStringArr = dataString.split(" ")
                        val prbArr = dataStringArr.dropLast(afterPrb).takeLast(prbCount)

                        lifecycleScope.launch(Dispatchers.Main) {
                            if (isAdded && view != null)
                                contentTextView.text = prbArr.toString()
                        }
                    }
                    catch (e: Exception) {
                        Log.e("InferenceFragment", "Error processing data: ${e.message}", e)
                    }
                },
                { error: Throwable ->
                    Log.e("InferenceFragment", "Error in subscription: ${error.message}", error)
                }
            )
        disposables.add(parameterUpdateDisposable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposables.clear()
    }
}