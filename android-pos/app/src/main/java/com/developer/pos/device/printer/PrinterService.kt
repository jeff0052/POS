package com.developer.pos.device.printer

interface PrinterService {
    suspend fun printReceipt(orderNo: String, content: String): PrintResult
    suspend fun reprintReceipt(orderNo: String, content: String): PrintResult
    suspend fun testPrint(): PrintResult
}

data class PrintResult(
    val success: Boolean,
    val code: String? = null,
    val message: String? = null
)
