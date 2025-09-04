package com.digitalsix.leitornfc.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Rotas de autenticação
    @POST("professor/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("professor/me")
    suspend fun verificarToken(@Header("Authorization") token: String): Response<Any>

    @POST("professor/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Any>

    // Rotas de aula
    @POST("aulas")
    suspend fun criarAula(
        @Header("Authorization") token: String,
        @Body request: CriarAulaRequest
    ): Response<CriarAulaResponse>

    @POST("aulas/completa")
    suspend fun criarAulaCompleta(
        @Header("Authorization") token: String,
        @Body request: CriarAulaCompletaRequest
    ): Response<CriarAulaCompletaResponse>

    @POST("aulas/{aulaId}/presencas")
    suspend fun registrarPresencas(
        @Header("Authorization") token: String,
        @Path("aulaId") aulaId: Int,
        @Body request: RegistrarPresencaRequest
    ): Response<RegistrarPresencaResponse>

    // ✅ NOVA ROTA PARA EXCLUIR AULA (quando abortar)
    @DELETE("aulas/{aulaId}")
    suspend fun excluirAula(
        @Header("Authorization") token: String,
        @Path("aulaId") aulaId: Int
    ): Response<Any>

    // Rota antiga (manter para compatibilidade)
    @POST("aulas")
    suspend fun registrarAula(@Body request: AulaRequest): Response<Any>
}
