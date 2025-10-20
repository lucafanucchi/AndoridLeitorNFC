package com.digitalsix.YouSafe.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // A URL base da sua API
    private const val BASE_URL = "https://apipresenca.digitalsix.com.br/"

    // Cria uma instância "lazy" do Retrofit. Ele só será criado na primeira vez que for usado.
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Expõe publicamente a nossa ApiService já criada e configurada
    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}