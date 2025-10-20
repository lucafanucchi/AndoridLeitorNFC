package com.digitalsix.YouSafe.network

import com.google.gson.annotations.SerializedName

data class AulaResponse(
    @SerializedName("message")
    val mensagem: String,

    @SerializedName("aula_id")
    val aulaId: Int
)