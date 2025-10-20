package com.digitalsix.YouSafe.network

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("senha")
    val senha: String
)