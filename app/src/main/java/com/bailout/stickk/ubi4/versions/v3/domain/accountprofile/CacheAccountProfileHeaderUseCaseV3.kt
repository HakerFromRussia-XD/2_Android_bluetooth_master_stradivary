package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

class CacheAccountProfileHeaderUseCaseV3(private val repository: V3AccountProfileLocalRepository) {
    operator fun invoke(header: V3AccountProfileHeader) = repository.cacheHeader(header)
}
