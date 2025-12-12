package com.digitalsix.YouSafe.network

import com.google.gson.annotations.SerializedName

/**
 * Modelo para resposta do endpoint GET /empresas
 * Representa uma unidade com seus dados de empresa
 */
data class EmpresaUnidadeResponse(
    @SerializedName("unidade_id")
    val unidadeId: Int,

    @SerializedName("empresa_id")
    val empresaId: Int,

    @SerializedName("nome")
    val nomeEmpresa: String,

    @SerializedName("unidade")
    val nomeUnidade: String,

    @SerializedName("cnpj")
    val cnpj: String
)

/**
 * Classes auxiliares para UI
 */
data class Empresa(
    val id: Int,
    val nome: String
) {
    override fun toString() = nome
}

data class Unidade(
    val id: Int,
    val nome: String
) {
    override fun toString() = nome
}