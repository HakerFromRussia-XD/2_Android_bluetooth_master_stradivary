package com.bailout.stickk.new_electronic_by_Rodeon.connection

import android.annotation.SuppressLint
import android.util.ArrayMap
import com.bailout.stickk.new_electronic_by_Rodeon.models.AllOptions
import com.bailout.stickk.new_electronic_by_Rodeon.models.DeviceInList_DEV
import com.bailout.stickk.new_electronic_by_Rodeon.models.TestModel
import com.bailout.stickk.new_electronic_by_Rodeon.models.deviceInfo.DeviceInfo
import com.bailout.stickk.new_electronic_by_Rodeon.models.user.User
import com.bailout.stickk.new_electronic_by_Rodeon.models.userV2.UserV2
import com.google.gson.Gson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

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
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getRequestUser(clientData: (User) -> Unit, error: (String) -> Unit, token: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getUserInfo("Bearer $token")
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
    suspend fun getRequestUserV2(clientData: (UserV2) -> Unit, error: (String) -> Unit, token: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getUserInfoV2("Bearer $token")
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
    suspend fun getRequestDevicesList(devicesList: (ArrayList<DeviceInList_DEV>) -> Unit, error: (String) -> Unit, token: String, clientId: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getDevicesList(clientId, "Bearer $token")
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
    suspend fun getRequestDeviceInfo(devicesInfo: (DeviceInfo) -> Unit, error: (String) -> Unit, token: String, deviceId: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getDeviceInfo(deviceId, "Bearer $token")
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
    suspend fun postRequestSettings(error: (String) -> Unit, token: String, prosthesisId: String, gson: Gson) {
        GlobalScope.launch(Dispatchers.IO) {
            val jsonParams: MutableMap<String, Any> = ArrayMap()
            jsonParams["settings"] = "some_code"

            val response = try {
                val myClass = AllOptions( lolMy = "test",
                    viewMy = "my view")
                RetrofitInstance.api.createPost(
                    prosthesisId,
                    "Bearer $token",
                    TestModel(gson.toJson(myClass))
                )
            } catch (e: HttpException) {
                error("http error ${e.message}")
                return@launch
            } catch (e: IOException) {
                error("app error ${e.message}")
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                System.err.println("Test post response: ${response.body()!!.lolMy} ${response.body()!!.viewMy}")
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