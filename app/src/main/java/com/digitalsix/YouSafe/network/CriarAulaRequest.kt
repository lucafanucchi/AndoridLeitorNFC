package com.digitalsix.YouSafe.network

import com.google.gson.annotations.SerializedName

data class CriarAulaRequest(
    @SerializedName("descricao_aula")
    val descricaoAula: String,

    @SerializedName("empresa_atendida_id")
    val empresaAtendidaId: Int,

    @SerializedName("data_hora_inicio")
    val dataHoraInicio: String? = null // Opcional, se null usa horário atual
)