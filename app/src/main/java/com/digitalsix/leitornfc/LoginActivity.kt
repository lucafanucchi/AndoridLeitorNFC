package com.digitalsix.leitornfc

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.digitalsix.leitornfc.network.LoginRequest
import com.digitalsix.leitornfc.network.RetrofitInstance
import com.digitalsix.leitornfc.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextSenha: EditText
    private lateinit var buttonLogin: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar SessionManager
        sessionManager = SessionManager(this)

        // Verificar se já está logado
        if (sessionManager.isLoggedIn()) {
            irParaMainActivity()
            return
        }

        bindViews()
        setupClickListeners()
    }

    private fun bindViews() {
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextSenha = findViewById(R.id.editTextSenha)
        buttonLogin = findViewById(R.id.buttonLogin)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val senha = editTextSenha.text.toString().trim()

            if (email.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha email e senha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            realizarLogin(email, senha)
        }
    }

    private fun realizarLogin(email: String, senha: String) {
        progressBar.visibility = View.VISIBLE
        buttonLogin.isEnabled = false
        buttonLogin.text = "Entrando..."

        lifecycleScope.launch {
            try {
                val request = LoginRequest(email, senha)
                Log.d("LOGIN_DEBUG", "🔄 Fazendo requisição para: ${RetrofitInstance.api}")

                val response = RetrofitInstance.api.login(request)
                Log.d("LOGIN_DEBUG", "📡 Response code: ${response.code()}")
                Log.d("LOGIN_DEBUG", "📡 Response success: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    Log.d("LOGIN_DEBUG", "📦 Response body null? ${loginResponse == null}")

                    if (loginResponse != null) {
                        Log.d("LOGIN_DEBUG", "✅ Professor: ${loginResponse.professor.nome}")
                        Log.d("LOGIN_DEBUG", "✅ Token: ${loginResponse.token.take(10)}...")

                        // ✅ VERIFICAÇÃO SEGURA DA LISTA
                        val empresas = loginResponse.empresas_atendidas ?: emptyList()
                        Log.d("LOGIN_DEBUG", "✅ Empresas: ${empresas.size}")
                        Log.d("LOGIN_DEBUG", "✅ Empresas são null? ${loginResponse.empresas_atendidas == null}")

                        sessionManager.salvarLogin(
                            token = loginResponse.token,
                            professor = loginResponse.professor,
                            empresas = empresas  // ✅ Usar lista segura
                        )

                        Toast.makeText(
                            this@LoginActivity,
                            "Bem-vindo, ${loginResponse.professor.nome}!",
                            Toast.LENGTH_LONG
                        ).show()

                        irParaMainActivity()
                    } else {
                        Log.e("LOGIN_DEBUG", "❌ Response body é NULL!")
                        mostrarErro("Resposta inválida do servidor")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("LOGIN_DEBUG", "❌ Error: ${response.code()} - $errorBody")

                    val errorMessage = when (response.code()) {
                        401 -> "Email ou senha incorretos"
                        400 -> "Dados inválidos"
                        500 -> "Erro no servidor. Tente novamente."
                        else -> "Erro de conexão. Verifique sua internet."
                    }
                    mostrarErro(errorMessage)
                }
            } catch (e: Exception) {
                Log.e("LOGIN_DEBUG", "💥 Exception: ${e.message}", e)
                mostrarErro("Falha na conexão: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                buttonLogin.isEnabled = true
                buttonLogin.text = "Entrar"
            }
        }
    }



    private fun mostrarErro(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
    }

    private fun irParaMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Finalizar LoginActivity para não voltar com botão back
    }
}