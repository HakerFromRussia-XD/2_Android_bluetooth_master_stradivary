package com.bailout.stickk.ubi4.data.network

sealed class NetworkResult<out T> {
    data class Success<out T>(val value: T): NetworkResult<T>()
    data class Error(val code: Int? = null, val message: String): NetworkResult<Nothing>()
}