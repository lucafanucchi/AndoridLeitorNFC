package com.digitalsix.YouSafe.network

import com.digitalsix.YouSafe.model.Professor
import com.digitalsix.YouSafe.model.EmpresaAtendida
import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("professor")
    val professor: Professor,

    @SerializedName("empresas_atendidas")
    val empresas_atendidas: List<EmpresaAtendida>? = emptyList(),  // ✅ Default para lista vazia

    @SerializedName("token")
    val token: String,

    @SerializedName("expires_in")
    val expiresIn: String
)