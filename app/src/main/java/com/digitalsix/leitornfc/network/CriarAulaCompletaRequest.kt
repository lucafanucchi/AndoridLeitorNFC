package com.digitalsix.leitornfc.network

import com.google.gson.annotations.SerializedName

data class CriarAulaCompletaRequest(
    @SerializedName("descricao_aula")
    val descricaoAula: String,

    @SerializedName("empresa_atendida_id")
    val empresaAtendidaId: Int,

    @SerializedName("ids_nfc_participantes")
    val idsNfcParticipantes: List<String>
)