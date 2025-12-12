package com.digitalsix.YouSafe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.digitalsix.YouSafe.network.LoginRequest
import com.digitalsix.YouSafe.network.RetrofitInstance
import com.digitalsix.YouSafe.utils.SessionManager
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

        sessionManager = SessionManager(this)

        // Se já estiver logado, verificar primeiro acesso
        if (sessionManager.isLoggedIn()) {
            verificarPrimeiroAcesso()
            return
        }

        // Inicializar views
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextSenha = findViewById(R.id.editTextSenha)
        buttonLogin = findViewById(R.id.buttonLogin)
        progressBar = findViewById(R.id.progressBar)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val senha = editTextSenha.text.toString().trim()

            if (validarCampos(email, senha)) {
                fazerLogin(email, senha)
            }
        }
    }

    private fun validarCampos(email: String, senha: String): Boolean {
        when {
            email.isEmpty() -> {
                Toast.makeText(this, "Digite o email", Toast.LENGTH_SHORT).show()
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(this, "Email inválido", Toast.LENGTH_SHORT).show()
                return false
            }
            senha.isEmpty() -> {
                Toast.makeText(this, "Digite a senha", Toast.LENGTH_SHORT).show()
                return false
            }
            else -> return true
        }
    }

    private fun fazerLogin(email: String, senha: String) {
        progressBar.visibility = View.VISIBLE
        buttonLogin.isEnabled = false
        buttonLogin.text = "Entrando..."

        lifecycleScope.launch {
            try {
                val request = LoginRequest(email = email, senha = senha)
                val response = RetrofitInstance.api.login(request)

                if (response.isSuccessful) {
                    val loginResponse = response.body()

                    if (loginResponse != null) {
                        // Salvar sessão
                        sessionManager.saveSession(
                            token = loginResponse.token,
                            usuario = loginResponse.usuario
                        )

                        Toast.makeText(
                            this@LoginActivity,
                            "Login realizado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Verificar se é primeiro acesso
                        verificarPrimeiroAcesso()
                    } else {
                        mostrarErro("Resposta vazia do servidor")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    mostrarErro(errorBody ?: "Email ou senha inválidos")
                }
            } catch (e: Exception) {
                mostrarErro("Erro de conexão: ${e.localizedMessage}")
            } finally {
                progressBar.visibility = View.GONE
                buttonLogin.isEnabled = true
                buttonLogin.text = "Entrar"
            }
        }
    }

    private fun mostrarErro(mensagem: String) {
        Toast.makeText(this@LoginActivity, mensagem, Toast.LENGTH_LONG).show()
    }

    /**
     * Verificar se é primeiro acesso e redirecionar adequadamente
     */
    private fun verificarPrimeiroAcesso() {
        val usuario = sessionManager.getUsuario()

        if (usuario != null && usuario.primeiroAcesso) {
            // É primeiro acesso - ir para tela de reset de senha
            irParaResetPassword()
        } else {
            // Não é primeiro acesso - ir para MainActivity
            irParaMainActivity()
        }
    }

    private fun irParaResetPassword() {
        val intent = Intent(this, ResetPasswordActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun irParaMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}