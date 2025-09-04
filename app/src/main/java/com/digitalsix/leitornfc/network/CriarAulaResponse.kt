package com.digitalsix.leitornfc.network

import com.google.gson.annotations.SerializedName

data class CriarAulaResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("aula")
    val aula: AulaInfo
)

data class AulaInfo(
    @SerializedName("id")
    val id: Int,

    @SerializedName("descricao")
    val descricao: String,

    @SerializedName("data_hora_inicio")
    val dataHoraInicio: String,

    @SerializedName("empresa_atendida_id")
    val empresaAtendidaId: Int
)