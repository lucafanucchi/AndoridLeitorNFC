package com.digitalsix.YouSafe.utils

import android.content.Context
import android.content.SharedPreferences
import com.digitalsix.YouSafe.model.Professor
import com.digitalsix.YouSafe.model.EmpresaAtendida
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionManager(context: Context) {

    companion object {
        private const val PREF_NAME = "professor_session"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_PROFESSOR = "professor_data"
        private const val KEY_EMPRESAS = "empresas_atendidas"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun salvarLogin(token: String, professor: Professor, empresas: List<EmpresaAtendida>) {
        // ✅ LIMPAR TUDO ANTES DE SALVAR NOVO LOGIN
        preferences.edit().clear().commit()

        val editor = preferences.edit()
        editor.putString(KEY_TOKEN, token)
        editor.putString(KEY_PROFESSOR, gson.toJson(professor))
        editor.putString(KEY_EMPRESAS, gson.toJson(empresas))
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.commit() // ✅ COMMIT IMEDIATO
    }
    // ✅ ADICIONAR ESTE MÉTODO NO SESSIONMANAGER
    fun limparCacheCompleto() {
        val editor = preferences.edit()

        // Manter apenas dados de login, limpar todo o resto
        val token = getToken()
        val professor = getProfessor()
        val empresas = getEmpresas()
        val isLoggedIn = isLoggedIn()

        // ✅ LIMPAR ABSOLUTAMENTE TUDO
        editor.clear()

        // ✅ RESTAURAR APENAS DADOS DE LOGIN
        if (isLoggedIn && token != null && professor != null) {
            editor.putString(KEY_TOKEN, token)
            editor.putString(KEY_PROFESSOR, gson.toJson(professor))
            editor.putString(KEY_EMPRESAS, gson.toJson(empresas))
            editor.putBoolean(KEY_IS_LOGGED_IN, true)
        }

        editor.commit() // ✅ COMMIT IMEDIATO
    }

    fun getToken(): String? {
        return preferences.getString(KEY_TOKEN, null)
    }

    fun getTokenWithBearer(): String? {
        val token = getToken()
        return if (token != null) "Bearer $token" else null
    }

    fun getProfessor(): Professor? {
        val professorJson = preferences.getString(KEY_PROFESSOR, null)
        return if (professorJson != null) {
            gson.fromJson(professorJson, Professor::class.java)
        } else null
    }

    fun getEmpresas(): List<EmpresaAtendida> {
        val empresasJson = preferences.getString(KEY_EMPRESAS, null)
        return if (empresasJson != null) {
            val type = object : TypeToken<List<EmpresaAtendida>>() {}.type
            gson.fromJson(empresasJson, type)
        } else emptyList()
    }

    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout() {
        preferences.edit().clear().commit() // ✅ COMMIT IMEDIATO
    }

    fun getNomeProfessor(): String {
        return getProfessor()?.nome ?: "Professor"
    }

    fun getEmailProfessor(): String {
        return getProfessor()?.email ?: ""
    }

    // ✅ MÉTODO ULTRA SIMPLES - SÓ MANTÉM LOGIN
    fun limparTodosOsEstados() {
        // Não fazer nada - deixar apenas os dados de login
        // O problema não é aqui
    }
}
