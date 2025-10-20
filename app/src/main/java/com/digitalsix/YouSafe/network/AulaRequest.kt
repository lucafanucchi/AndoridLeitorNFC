package com.digitalsix.YouSafe.network

import com.google.gson.annotations.SerializedName

data class AulaRequest(
    @SerializedName("descricao_aula")
    val descricao: String,

    @SerializedName("data_hora")
    val dataHora: String,

    @SerializedName("ids_nfc_participantes")
    val idsNfc: List<String>
)