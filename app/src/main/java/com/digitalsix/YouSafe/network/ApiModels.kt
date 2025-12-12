package com.digitalsix.YouSafe.network

import com.google.gson.annotations.SerializedName

// ==========================================
// MODELOS PARA RESET DE SENHA
// ==========================================

data class ResetPasswordRequest(
    @SerializedName("senha")
    val senha: String,

    @SerializedName("confirmSenha")
    val confirmSenha: String
)

data class ResetPasswordResponse(
    @SerializedName("message")
    val message: String
)

// ==========================================
// MODELOS PARA CRIAÇÃO DE AULA
// ==========================================

data class CriarAulaRequest(
    @SerializedName("descricao")
    val descricao: String,

    @SerializedName("unidade_id")
    val unidadeId: Int,

    @SerializedName("data")
    val data: String? = null  // ISO 8601 format, opcional
)

data class CriarAulaResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("aula")
    val aula: AulaInfo
)

data class AulaInfo(
    @SerializedName("aula_id")
    val aulaId: Int,

    @SerializedName("data")
    val data: String,

    @SerializedName("unidade_id")
    val unidadeId: Int,

    @SerializedName("tipo_aula_id")
    val tipoAulaId: Int?,

    @SerializedName("instrutor_id")
    val instrutorId: Int,

    @SerializedName("status_id")
    val statusId: Int  // 4 = em progresso, 1 = confirmada, 2 = abortada
)

// ==========================================
// MODELOS PARA CONFIRMAR AULA
// ==========================================

data class ConfirmarAulaRequest(
    @SerializedName("participantes")
    val participantes: List<ParticipanteAula>
)

data class ParticipanteAula(
    @SerializedName("nfc")
    val nfc: String,

    @SerializedName("unidade_id")
    val unidadeId: Int,

    @SerializedName("funcionario_id")
    val funcionarioId: Int? = null,  // Opcional se já cadastrado

    @SerializedName("nome")
    val nome: String? = null  // Opcional
)

data class ConfirmarAulaResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("aula")
    val aula: AulaInfo,

    @SerializedName("participantesCadastrados")
    val participantesCadastrados: Int
)

// ==========================================
// MODELOS PARA ABORTAR AULA
// ==========================================

data class AbortarAulaRequest(
    @SerializedName("participantes")
    val participantes: List<ParticipanteAula>? = null  // Opcional
)

data class AbortarAulaResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("aula")
    val aula: AulaInfo
)

// ==========================================
// MODELO PARA LOGIN (já existe, mas garantindo)
// ==========================================

data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("senha")
    val senha: String
)

data class LoginResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("token")
    val token: String,

    @SerializedName("expires_in")
    val expiresIn: String,

    @SerializedName("usuario")
    val usuario: Usuario
)

data class Usuario(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nome")
    val nome: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("unidade_id")
    val unidadeId: Int,

    // ✅ ADICIONE ESTE CAMPO
    @SerializedName("empresa_id")
    val empresaId: Int,

    @SerializedName("primeiro_acesso")
    val primeiroAcesso: Boolean,

    @SerializedName("roles")
    val roles: List<Role>,

    @SerializedName("unidades_atendidas")
    val unidadesAtendidas: List<UnidadeAtendida>
)

data class Role(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nome")
    val nome: String
)

data class UnidadeAtendida(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nome")
    val nome: String,

    @SerializedName("empresa")
    val empresa: String?
)

// ==========================================
// MODELOS PARA TIPO DE AULA
// ==========================================

data class TipoAula(
    @SerializedName("tipo_aula_id")
    val tipoAulaId: Int,

    @SerializedName("descricao")
    val descricao: String
)

// Request para buscar funcionário por NFC
data class GetFuncionarioByNFCRequest(
    @SerializedName("nfc")
    val nfc: String,

    @SerializedName("unidade_id")
    val unidade_id: Int
)

// Response da busca (pode vir com dados ou tudo null)
data class GetFuncionarioByNFCResponse(
    @SerializedName("funcionario_id")
    val funcionario_id: Int?,

    @SerializedName("nome")
    val nome: String?,

    @SerializedName("nfc")
    val nfc: String,

    @SerializedName("ativo")
    val ativo: Boolean?,

    @SerializedName("unidade_id")
    val unidade_id: Int
)