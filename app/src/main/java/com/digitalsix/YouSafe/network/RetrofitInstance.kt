package com.digitalsix.YouSafe.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // ✅ URL CORRETA DA API YOUSAFE GL V2
    private const val BASE_URL = "https://apiyousafeglv2.digitalsix.com.br/"

    // Cria uma instância "lazy" do Retrofit (só será criado na primeira vez que for usado)
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Expõe publicamente a ApiService já criada e configurada
    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}