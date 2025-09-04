package com.digitalsix.leitornfc.network

import com.google.gson.annotations.SerializedName

data class CriarAulaCompletaResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("aula_id")
    val aulaId: Int,

    @SerializedName("presencas_registradas")
    val presencasRegistradas: Int,

    @SerializedName("ids_nao_encontrados")
    val idsNaoEncontrados: List<String>,

    @SerializedName("total_processados")
    val totalProcessados: Int
)