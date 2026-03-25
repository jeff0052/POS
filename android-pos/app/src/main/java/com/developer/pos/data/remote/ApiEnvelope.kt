package com.developer.pos.data.remote

data class ApiEnvelope<T>(
    val code: Int,
    val message: String,
    val data: T
)
