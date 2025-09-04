// 1. Professor.kt
package com.digitalsix.leitornfc.model

import com.google.gson.annotations.SerializedName

data class Professor(
    @SerializedName("id")
    val id: Int,

    @SerializedName("email")
    val email: String,

    @SerializedName("nome")
    val nome: String,

    @SerializedName("nome_empresa")
    val nomeEmpresa: String
)
