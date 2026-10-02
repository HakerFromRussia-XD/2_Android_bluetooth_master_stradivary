package com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics

class RequestAccountStatisticsUseCaseV3(private val repository: V3AccountStatisticsRepository) {
    operator fun invoke() = repository.requestStatistics()
}
