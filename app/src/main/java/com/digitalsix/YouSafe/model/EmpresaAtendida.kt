package com.digitalsix.YouSafe.model

import com.google.gson.annotations.SerializedName

data class EmpresaAtendida(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nome_empresa")
    val nomeEmpresa: String
)