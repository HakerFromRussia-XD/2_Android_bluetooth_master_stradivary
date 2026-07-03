package com.bailout.stickk.ubi4.data.network

import AllOptionsV3
import android.annotation.SuppressLint
import android.content.Context
import com.bailout.stickk.ubi4.models.device.DeviceInfo
import com.bailout.stickk.ubi4.models.deviceList.DeviceInList_DEV
import com.bailout.stickk.ubi4.models.user.User
import com.bailout.stickk.ubi4.models.user.UserV2
import com.google.gson.Gson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class RequestsUBI4 {
    private val settingsProfileSender = Ubi4SettingsProfileSender()

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getRequestToken(
        token: (String) -> Unit,
        error: (String) -> Unit,
        encryptedSerialNumber: String
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                UserRetrofitInstance.api.getToken(encryptedSerialNumber)
            } catch (e: HttpException) {
                error("http error ${e.message}")
                return@launch
            } catch (e: IOException) {
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
    suspend fun getRequestUser(
        clientData: (User) -> Unit,
        error: (String) -> Unit,
        token: String,
        lang: String
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                UserRetrofitInstance.api.getUserInfo("Bearer $token", lang)
            } catch (e: HttpException) {
                error("http error ${e.message}")
                return@launch
            } catch (e: IOException) {
                error("app error ${e.message}")
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                System.err.println("getRequestUser ${response.body()}")
                withContext(Dispatchers.Main) {
                    clientData(response.body()!!)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getRequestUserV2(
        clientData: (UserV2) -> Unit,
        error: (String) -> Unit,
        token: String,
        lang: String
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                UserRetrofitInstance.api.getUserInfoV2("Bearer $token", lang)
            } catch (e: HttpException) {
                error("http error ${e.message}")
                return@launch
            } catch (e: IOException) {
                error("app error ${e.message}")
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                System.err.println("getRequestUser ${response.body()}")
                withContext(Dispatchers.Main) {
                    clientData(response.body()!!)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getRequestDevicesList(
        devicesList: (ArrayList<DeviceInList_DEV>) -> Unit,
        error: (String) -> Unit,
        token: String,
        clientId: Int,
        lang: String
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                UserRetrofitInstance.api.getDevicesList(clientId, "Bearer $token", lang)
            } catch (e: HttpException) {
                error("http error ${e.message}")
                return@launch
            } catch (e: IOException) {
                error("app error ${e.message}")
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                val safeList = response.body()!!.devices.mapNotNull { it }.toCollection(ArrayList())
                withContext(Dispatchers.Main) {
                    devicesList(safeList)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getRequestDeviceInfo(
        devicesInfo: (DeviceInfo) -> Unit,
        error: (String) -> Unit,
        token: String,
        deviceId: Int,
        lang: String
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                UserRetrofitInstance.api.getDeviceInfo(deviceId, "Bearer $token", lang)
            } catch (e: HttpException) {
                error("http error ${e.message}")
                return@launch
            } catch (e: IOException) {
                error("app error ${e.message}")
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {
                    devicesInfo(response.body()!!)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun postRequestSettings(
        error: (String) -> Unit,
        token: String,
        deviceId: String,
        gson: Gson,
        context: Context,
        mDeviceAddress: String
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val result = settingsProfileSender.sendProfile1Settings(
                    deviceId = deviceId,
                    token = token
                )
                System.err.println("Settings profile upload payload: ${result.settingsPayload}")
                System.err.println("Settings profile upload server response: ${result.serverResponse}")
            } catch (e: HttpException) {
                error("http error ${e.message}")
                return@launch
            } catch (e: IOException) {
                error("app error ${e.message}")
                return@launch
            } catch (e: Exception) {
                error("app error ${e.message}")
                return@launch
            }

            System.err.println("Settings profile upload success")
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getRequestProthesisSettings(
        allOptions: (AllOptionsV3) -> Unit,
        error: (String) -> Unit,
        token: String,
        prosthesisId: String
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                UserRetrofitInstance.api.getRequestProthesisSettings(prosthesisId, "Bearer $token")
            } catch (e: HttpException) {
                error("http error ${e.message}")
                return@launch
            } catch (e: IOException) {
                error("app error ${e.message}")
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {
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
