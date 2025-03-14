package com.bailout.stickk.new_electronic_by_Rodeon.connection

import android.annotation.SuppressLint
import android.content.Context
import android.util.ArrayMap
import com.bailout.stickk.new_electronic_by_Rodeon.models.AllOptions
import com.bailout.stickk.new_electronic_by_Rodeon.models.DeviceInList_DEV
import com.bailout.stickk.new_electronic_by_Rodeon.models.TestModel
import com.bailout.stickk.new_electronic_by_Rodeon.models.deviceInfo.DeviceInfo
import com.bailout.stickk.new_electronic_by_Rodeon.models.user.User
import com.bailout.stickk.new_electronic_by_Rodeon.models.userV2.UserV2
import com.bailout.stickk.new_electronic_by_Rodeon.utils.InitAllOptions
import com.google.gson.Gson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import com.bailout.stickk.new_electronic_by_Rodeon.utils.InitAllOptions.Companion as InitAllOptions1

class Requests {

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getRequestToken(token: (String) -> Unit, error: (String) -> Unit, encryptedSerialNumber: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getToken(encryptedSerialNumber)
            }catch (e: HttpException){
                error("http error ${e.message}")
                return@launch
            }catch (e: IOException){
                error("app error ${e.message}")
                return@launch
            }
            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {
                    token(response.body()!!.token)
                }
            } else {
                error("${response.code()}")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getRequestUser(clientData: (User) -> Unit, error: (String) -> Unit, token: String, lang: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getUserInfo("Bearer $token", lang)
            }catch (e: HttpException){
                error("http error ${e.message}")
                return@launch
            }catch (e: IOException){
                error("app error ${e.message}")
                return@launch
            }

            if (response.isSuccessful && response.body() != null){
                System.err.println("getRequestUser ${response.body()}")
                withContext(Dispatchers.Main){
                    clientData(response.body()!!)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getRequestUserV2(clientData: (UserV2) -> Unit, error: (String) -> Unit, token: String, lang: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getUserInfoV2("Bearer $token", lang)
            }catch (e: HttpException){
                error("http error ${e.message}")
                return@launch
            }catch (e: IOException){
                error("app error ${e.message}")
                return@launch
            }

            if (response.isSuccessful && response.body() != null){
                System.err.println("getRequestUser ${response.body()}")
                withContext(Dispatchers.Main){
                    clientData(response.body()!!)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getRequestDevicesList(devicesList: (ArrayList<DeviceInList_DEV>) -> Unit, error: (String) -> Unit, token: String, clientId: Int, lang: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getDevicesList(clientId, "Bearer $token", lang)
            }catch (e: HttpException){
                error("http error ${e.message}")
                return@launch
            }catch (e: IOException){
                error("app error ${e.message}")
                return@launch
            }

            if (response.isSuccessful && response.body() != null){
                withContext(Dispatchers.Main){

                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getRequestDeviceInfo(devicesInfo: (DeviceInfo) -> Unit, error: (String) -> Unit, token: String, deviceId: Int, lang: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getDeviceInfo(deviceId, "Bearer $token", lang)
            }catch (e: HttpException){
                error("http error ${e.message}")
                return@launch
            }catch (e: IOException){
                error("app error ${e.message}")
                return@launch
            }

            if (response.isSuccessful && response.body() != null){
                withContext(Dispatchers.Main){
                    devicesInfo(response.body()!!)
                }
            }
        }
    }


    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun postRequestSettings(error: (String) -> Unit, token: String, deviceId: String, gson: Gson, context: Context, mDeviceAddress: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val jsonParams: MutableMap<String, Any> = ArrayMap()
            jsonParams["settings"] = "some_code"

            val response = try {
                InitAllOptions(context = context, mDeviceAddress = mDeviceAddress)
                val classForReceive = InitAllOptions1.myAllOptions
                System.err.println("mSettings GAME_LAUNCH_RATE ${InitAllOptions1.myAllOptions.gameLaunchRate}")
                System.err.println("mSettings MAXIMUM_POINTS ${InitAllOptions1.myAllOptions.maximumPoints}")
                System.err.println("mSettings NUMBER_OF_CUPS ${InitAllOptions1.myAllOptions.numberOfCups}")

                RetrofitInstance.api.createPost(
                    deviceId,
                    "Bearer $token",
                    TestModel(gson.toJson(classForReceive))
                )
            } catch (e: HttpException) {
                error("http error ${e.message}")
                return@launch
            } catch (e: IOException) {
                error("app error ${e.message}")
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                System.err.println("Test post response: ${response.body()!!.gameLaunchRate} ${response.body()!!.maximumPoints}")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getRequestProthesisSettings(allOptions: (AllOptions) -> Unit, error: (String) -> Unit, token: String, prosthesisId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getRequestProthesisSettings(prosthesisId, "Bearer $token")
            }catch (e: HttpException){
                error("http error ${e.message}")
                return@launch
            }catch (e: IOException){
                error("app error ${e.message}")
                return@launch
            }

            if (response.isSuccessful && response.body() != null){
                withContext(Dispatchers.Main){
                    allOptions(response.body()!!)
                }
            } else {
                withContext(Dispatchers.Main) {
                    error("app error \"NO DATA\"")
                }
            }
        }
    }
}