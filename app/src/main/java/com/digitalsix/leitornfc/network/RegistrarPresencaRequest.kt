package com.digitalsix.leitornfc.network

import com.google.gson.annotations.SerializedName

data class RegistrarPresencaRequest(
    @SerializedName("ids_nfc_participantes")
    val idsNfcParticipantes: List<String>
)