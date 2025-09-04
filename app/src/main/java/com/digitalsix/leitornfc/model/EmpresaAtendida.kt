package com.digitalsix.leitornfc.model

import com.google.gson.annotations.SerializedName

data class EmpresaAtendida(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nome_empresa")
    val nomeEmpresa: String
)