package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

sealed interface V3AccountProfileResult<out T> {
    data class Success<T>(val value: T) : V3AccountProfileResult<T>
    data class Error(val code: Int?, val message: String) : V3AccountProfileResult<Nothing>
}
