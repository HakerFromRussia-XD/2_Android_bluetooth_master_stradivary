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
import com.bailout.stickk.ubi4.data.network.getModelVersionsFileName
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.network.ModelVersions
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.showToast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

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
        val modelVersion = loadModelVersions()
        lifecycleScope.launch(Dispatchers.IO) {
            if (modelVersion != null)
                dataProcessing(modelVersion)
            else {
                withContext(Dispatchers.Main) {
                    showToast("${getModelVersionsFileName()} file not found")
                }
            }
        }
    }

    private fun dataProcessing(modelVersions: ModelVersions) {
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

                        var prbCount = 8
                        val afterPrb = 2
                        if (modelVersions.modelVersion.takeLast(1) == "1")
                            prbCount = 11
                        val dataStringArr = dataString.split(" ")
                        val prbArr = dataStringArr.dropLast(afterPrb).takeLast(prbCount)

                        lifecycleScope.launch(Dispatchers.Main) {
                            if (isAdded && view != null)
                                contentTextView.text = "${prbArr}\n${dataString}"
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

    private fun loadModelVersions(): ModelVersions? {
        try {
            val externalDir = requireContext().getExternalFilesDir(null)
            if (externalDir != null) {
                val jsonFile = File(externalDir, getModelVersionsFileName())
                if (jsonFile.exists()) {
                    val jsonString = jsonFile.readText()
                    val json = Json { ignoreUnknownKeys = true }
                    val modelVersions = json.decodeFromString<ModelVersions>(jsonString)
                    return modelVersions
                }
            }
            else {
                Log.e("InferenceFragment", "External files directory is null")
            }
        }
        catch (e: Exception) {
            Log.e("InferenceFragment", "Error loading model versions: ${e.message}", e)
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposables.clear()
    }
}