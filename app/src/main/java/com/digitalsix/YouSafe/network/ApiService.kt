package com.digitalsix.YouSafe.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // ==========================================
    // ROTAS DE AUTENTICAÇÃO
    // ==========================================
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * Endpoint /auth/me - Retorna dados atualizados do usuário
     * Usado para refresh de dados e validação de token
     */
    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Any>

    @POST("auth/resetSenha/{id}")
    suspend fun resetPassword(
        @Header("Authorization") token: String,
        @Path("id") userId: Int,
        @Body request: ResetPasswordRequest
    ): Response<ResetPasswordResponse>

    // ==========================================
    // ROTAS DE AULAS
    // ==========================================
    @POST("aulas")
    suspend fun criarAula(
        @Header("Authorization") token: String,
        @Body request: CriarAulaRequest
    ): Response<CriarAulaResponse>

    @POST("aulas/{id}/confirmar")
    suspend fun confirmarAula(
        @Header("Authorization") token: String,
        @Path("id") aulaId: Int,
        @Body request: ConfirmarAulaRequest
    ): Response<ConfirmarAulaResponse>

    @POST("aulas/{id}/abortar")
    suspend fun abortarAula(
        @Header("Authorization") token: String,
        @Path("id") aulaId: Int,
        @Body request: AbortarAulaRequest
    ): Response<AbortarAulaResponse>

    // ==========================================
    // ROTAS DE TIPOS DE AULA
    // ==========================================
    @GET("tipos-aula")
    suspend fun getTiposAula(
        @Header("Authorization") token: String
    ): Response<List<TipoAula>>

    // ==========================================
    // ROTAS DE EMPRESAS E UNIDADES
    // ==========================================

    /**
     * GET /empresas - Lista todas as unidades com seus dados de empresa
     * Retorna: Lista de unidades com empresa_id, nome da empresa, etc
     */
    @GET("empresas")
    suspend fun getEmpresas(): Response<List<EmpresaUnidadeResponse>>

    // ==========================================
    // ROTAS DE FUNCIONÁRIOS
    // ==========================================

    /**
     * Buscar funcionário por NFC
     */
    @POST("funcionarios/nfc")
    suspend fun getFuncionarioByNFC(
        @Body request: GetFuncionarioByNFCRequest
    ): Response<GetFuncionarioByNFCResponse>
}