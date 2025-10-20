package com.digitalsix.YouSafe

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.digitalsix.YouSafe.model.EmpresaAtendida
import com.digitalsix.YouSafe.network.*
import com.digitalsix.YouSafe.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        private const val TAG = "MainActivityNFC"
        // ✅ CHAVES PARA LIMPAR DADOS PERSISTENTES
        private const val PREF_PARTIAL_DATA = "partial_aula_data"
        private const val KEY_PARTIAL_AULA_ID = "partial_aula_id"
        private const val KEY_PARTIAL_NFC_IDS = "partial_nfc_ids"
    }

    // ✅ VARIÁVEIS DE INSTÂNCIA (NÃO ESTÁTICAS) - RESOLVE PROBLEMA DE PERSISTÊNCIA
    private var nfcAdapter: NfcAdapter? = null
    private var listaNfcIds = mutableListOf<String>()
    private var isLeituraAtiva = false
    private var aulaAtual: AulaInfo? = null
    private var empresaSelecionada: EmpresaAtendida? = null
    private var isEnviandoDados = false

    // Session Manager
    private lateinit var sessionManager: SessionManager

    // Views da interface
    private lateinit var layoutCriacaoAula: LinearLayout
    private lateinit var layoutLeitura: LinearLayout
    private lateinit var textViewBemVindo: TextView
    private lateinit var spinnerEmpresa: Spinner
    private lateinit var editTextDescricaoAula: EditText
    private lateinit var buttonIniciarLeitura: Button
    private lateinit var textViewDescricaoAtual: TextView
    private lateinit var textViewContador: TextView
    private lateinit var textViewListaIds: TextView
    private lateinit var buttonEncerrarEnviar: Button
    private lateinit var buttonAbortarAula: Button

    // ✅ MÉTODO PARA LIMPAR DADOS PERSISTENTES
    private fun limparDadosParciais() {
        val prefs = getSharedPreferences(PREF_PARTIAL_DATA, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.clear()
        editor.commit()

        Log.d(TAG, "Dados parciais limpos das SharedPreferences")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //supportActionBar?.hide()

        // ✅ LIMPAR ABSOLUTAMENTE TODOS OS DADOS PERSISTENTES
        limparDadosParciais()

        // ✅ FORÇAR LIMPEZA TOTAL DE VARIÁVEIS DE INSTÂNCIA
        listaNfcIds.clear()
        isLeituraAtiva = false
        aulaAtual = null
        empresaSelecionada = null

        // Inicializar Session Manager
        sessionManager = SessionManager(this)

        // Verificar se está logado
        if (!sessionManager.isLoggedIn()) {
            irParaLogin()
            return
        }

        bindViews()
        setupNFC()
        setupInterface()
        setupClickListeners()

        // ✅ GARANTIR QUE SEMPRE COMECE LIMPO
        resetarParaEstadoInicial()

        // ✅ LOG PARA DEBUG
        Log.d(TAG, "=== ONCREATE - ESTADO INICIAL LIMPO ===")
        Log.d(TAG, "isLeituraAtiva: $isLeituraAtiva")
        Log.d(TAG, "listaNfcIds size: ${listaNfcIds.size}")
        Log.d(TAG, "aulaAtual: $aulaAtual")
        Log.d(TAG, "========================================")
    }

    private fun bindViews() {
        layoutCriacaoAula = findViewById(R.id.layoutCriacaoAula)
        layoutLeitura = findViewById(R.id.layoutLeitura)
        textViewBemVindo = findViewById(R.id.textViewBemVindo)
        spinnerEmpresa = findViewById(R.id.spinnerEmpresa)
        editTextDescricaoAula = findViewById(R.id.editTextDescricaoAula)
        buttonIniciarLeitura = findViewById(R.id.buttonIniciarLeitura)
        textViewDescricaoAtual = findViewById(R.id.textViewDescricaoAtual)
        textViewContador = findViewById(R.id.textViewContador)
        textViewListaIds = findViewById(R.id.textViewListaIds)
        buttonEncerrarEnviar = findViewById(R.id.buttonEncerrarEnviar)
        buttonAbortarAula = findViewById(R.id.buttonAbortarAula)
    }

    private fun setupNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "Este dispositivo não suporta NFC.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    private fun setupInterface() {
        // Mostrar nome do professor
        val professor = sessionManager.getProfessor()
        textViewBemVindo.text = "Olá, ${professor?.nome ?: "Professor"}!"

        // Configurar spinner de empresas
        val empresas = sessionManager.getEmpresas()
        val empresasNomes = empresas.map { it.nomeEmpresa }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, empresasNomes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEmpresa.adapter = adapter
    }

    private fun setupClickListeners() {
        buttonIniciarLeitura.setOnClickListener {
            val descricao = editTextDescricaoAula.text.toString().trim()
            if (descricao.isEmpty()) {
                Toast.makeText(this, "Por favor, insira uma descrição para a aula.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val empresas = sessionManager.getEmpresas()
            if (empresas.isEmpty()) {
                Toast.makeText(this, "Nenhuma empresa disponível.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            empresaSelecionada = empresas[spinnerEmpresa.selectedItemPosition]
            criarAula(descricao)
        }

        buttonEncerrarEnviar.setOnClickListener {
            if (listaNfcIds.isEmpty()) {
                Toast.makeText(this, "Nenhum participante registrado.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            finalizarAula()
        }

        buttonAbortarAula.setOnClickListener {
            mostrarDialogoAbortarAula()
        }
    }

    // ✅ MÉTODO CRIAR AULA CORRIGIDO COM LIMPEZA TOTAL
    private fun criarAula(descricao: String) {
        // ✅ NÃO CRIAR AULA NO SERVIDOR - APENAS PREPARAR DADOS LOCALMENTE

        val empresas = sessionManager.getEmpresas()
        if (empresas.isEmpty()) {
            Toast.makeText(this, "Nenhuma empresa disponível.", Toast.LENGTH_SHORT).show()
            return
        }

        empresaSelecionada = empresas[spinnerEmpresa.selectedItemPosition]

        // ✅ CRIAR OBJETO AULA LOCAL (SEM ID do servidor ainda)
        aulaAtual = AulaInfo(
            id = 0, // ID temporário
            descricao = descricao,
            dataHoraInicio = obterHorarioAtualSaoPaulo(),
            empresaAtendidaId = empresaSelecionada!!.id
        )

        iniciarModoLeitura()
        Toast.makeText(this, "Preparando aula: $descricao", Toast.LENGTH_SHORT).show()
    }

    // ✅ MÉTODO INICIAR MODO LEITURA CORRIGIDO
    private fun iniciarModoLeitura() {
        // ✅ GARANTIR QUE A LISTA ESTÁ LIMPA AO INICIAR LEITURA
        listaNfcIds.clear()
        Log.d(TAG, "Lista limpa ao iniciar modo leitura: ${listaNfcIds.size}")

        isLeituraAtiva = true
        layoutCriacaoAula.visibility = View.GONE
        layoutLeitura.visibility = View.VISIBLE

        aulaAtual?.let { aula ->
            val horaInicio = obterHorarioAtualSaoPaulo()
            textViewDescricaoAtual.text = "${aula.descricao}\nIniciada às $horaInicio"
        }

        atualizarContadorELista()
    }

    // ✅ MeTODO FINALIZAR AULA COM LIMPEZA GARANTIDA
    private fun finalizarAula() {
        val token = sessionManager.getTokenWithBearer()
        val aula = aulaAtual

        if (token == null || aula == null || empresaSelecionada == null) {
            irParaLogin()
            return
        }

        if (listaNfcIds.isEmpty()) {
            Toast.makeText(this, "Nenhum participante registrado.", Toast.LENGTH_SHORT).show()
            return
        }

        buttonEncerrarEnviar.isEnabled = false
        buttonEncerrarEnviar.text = "Criando aula e enviando presenças..."

        lifecycleScope.launch {
            try {
                // ✅ PASSO 1: CRIAR AULA NO SERVIDOR
                val criarAulaRequest = CriarAulaRequest(
                    descricaoAula = aula.descricao,
                    empresaAtendidaId = empresaSelecionada!!.id
                )

                val criarAulaResponse = RetrofitInstance.api.criarAula(token, criarAulaRequest)

                if (criarAulaResponse.isSuccessful) {
                    val aulaResposta = criarAulaResponse.body()
                    if (aulaResposta != null) {
                        val aulaIdReal = aulaResposta.aula.id

                        // ✅ PASSO 2: REGISTRAR PRESENÇAS COM O ID REAL DA AULA
                        val presencaRequest = RegistrarPresencaRequest(listaNfcIds)
                        val presencaResponse = RetrofitInstance.api.registrarPresencas(
                            token,
                            aulaIdReal,
                            presencaRequest
                        )

                        if (presencaResponse.isSuccessful) {
                            val resultado = presencaResponse.body()
                            if (resultado != null) {
                                mostrarResultadoFinal(resultado)
                            }
                        } else {
                            throw Exception("Erro ao registrar presenças: ${presencaResponse.code()}")
                        }
                    } else {
                        throw Exception("Resposta inválida ao criar aula")
                    }
                } else {
                    throw Exception("Erro ao criar aula: ${criarAulaResponse.code()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao finalizar aula", e)
                Toast.makeText(this@MainActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                buttonEncerrarEnviar.isEnabled = true
                buttonEncerrarEnviar.text = "Encerrar e Enviar Dados"
            }
        }
    }



    private fun mostrarResultadoFinal(resultado: RegistrarPresencaResponse) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Aula Finalizada!")

        val mensagem = StringBuilder()
        mensagem.append("✅ Presenças registradas: ${resultado.presencasRegistradas}\n")
        mensagem.append("📊 Total processados: ${resultado.totalProcessados}\n")

        if (resultado.idsNaoEncontrados.isNotEmpty()) {
            mensagem.append("\n⚠️ IDs não encontrados:\n")
            resultado.idsNaoEncontrados.forEach {
                mensagem.append("• $it\n")
            }
        }

        builder.setMessage(mensagem.toString())
        builder.setPositiveButton("OK") { _, _ ->
            resetarParaEstadoInicial()
        }

        builder.setCancelable(false)
        builder.show()
    }

    private fun mostrarDialogoAbortarAula() {
        if (!isLeituraAtiva) {
            Toast.makeText(this, "Nenhuma aula em andamento", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Abortar Aula")
            .setMessage("Deseja realmente abortar esta aula? Todos os dados serão perdidos.")
            .setPositiveButton("Sim, Abortar") { _, _ ->
                abortarAulaAtual()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ✅ MÉTODO ABORTAR AULA CORRIGIDO
    private fun abortarAulaAtual() {
        // ✅ COMO A AULA AINDA NÃO FOI CRIADA NO SERVIDOR, SÓ LIMPAR LOCALMENTE

        Log.d(TAG, "Abortando aula local: ${aulaAtual?.descricao}")

        // Limpar dados locais imediatamente
        listaNfcIds.clear()
        isLeituraAtiva = false
        aulaAtual = null
        empresaSelecionada = null

        resetarParaEstadoInicial()
        Toast.makeText(this, "Aula abortada - dados descartados", Toast.LENGTH_SHORT).show()
    }


    // NFC - READER MODE
    override fun onTagDiscovered(tag: Tag?) {
        if (!isLeituraAtiva) return

        tag?.let {
            val tagIdHex = bytesToHexString(it.id)
            runOnUiThread {
                if (!listaNfcIds.contains(tagIdHex)) {
                    listaNfcIds.add(tagIdHex)
                    atualizarContadorELista()
                    Toast.makeText(this, "✅ Participante adicionado!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "⚠️ Este participante já foi registrado.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B,
            null
        )
    }

    // ✅ MÉTODO ONPAUSE PARA CAPTURAR FECHAMENTO DO APP
    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)

        // ✅ SE APP FOR PAUSADO DURANTE LEITURA, LIMPAR DADOS PARCIAIS
        if (isLeituraAtiva) {
            Log.d(TAG, "App pausado durante leitura - limpando dados parciais")
            limparDadosParciais()
        }
    }

    // ✅ MÉTODO ONDESTROY CORRIGIDO
    override fun onDestroy() {
        super.onDestroy()

        // ✅ LIMPAR COMPLETAMENTE AO SAIR
        limparDadosParciais()
        listaNfcIds.clear()

        Log.d(TAG, "onDestroy - Activity sendo destruída e dados limpos")
    }

    // ✅ MeTODO RESETAR CORRIGIDO COM LIMPEZA COMPLETA
    private fun resetarParaEstadoInicial() {
        Log.d(TAG, "=== ANTES DO RESET ===")
        Log.d(TAG, "isLeituraAtiva: $isLeituraAtiva")
        Log.d(TAG, "listaNfcIds: $listaNfcIds")
        Log.d(TAG, "aulaAtual: $aulaAtual")
        Log.d(TAG, "isEnviandoDados: $isEnviandoDados")

        // ✅ LIMPAR DADOS PERSISTENTES
        limparDadosParciais()

        // ✅ LIMPAR ABSOLUTAMENTE TUDO
        isLeituraAtiva = false
        isEnviandoDados = false // ✅ RESETAR CONTROLE DE ENVIO
        listaNfcIds.clear()
        listaNfcIds.clear() // ✅ LIMPAR DUAS VEZES PARA GARANTIR
        aulaAtual = null
        empresaSelecionada = null

        // Limpar campos da interface
        editTextDescricaoAula.text.clear()

        // Resetar interface para estado inicial
        layoutCriacaoAula.visibility = View.VISIBLE
        layoutLeitura.visibility = View.GONE

        // Resetar botões
        buttonEncerrarEnviar.isEnabled = true
        buttonEncerrarEnviar.text = "Encerrar e Enviar Dados"
        buttonIniciarLeitura.isEnabled = true
        buttonIniciarLeitura.text = "Iniciar Leitura de Presença"

        // Atualizar displays
        atualizarContadorELista()

        Log.d(TAG, "=== APÓS O RESET ===")
        Log.d(TAG, "isLeituraAtiva: $isLeituraAtiva")
        Log.d(TAG, "listaNfcIds: $listaNfcIds")
        Log.d(TAG, "aulaAtual: $aulaAtual")
        Log.d(TAG, "isEnviandoDados: $isEnviandoDados")
        Log.d(TAG, "====================")
    }

    private fun atualizarContadorELista() {
        textViewContador.text = listaNfcIds.size.toString()
        val builder = StringBuilder("IDs Registrados:\n")

        if (listaNfcIds.isEmpty()) {
            builder.append("Nenhum participante registrado ainda.")
        } else {
            listaNfcIds.forEachIndexed { index, id ->
                builder.append("${index + 1}. $id\n")
            }
        }

        textViewListaIds.text = builder.toString()
    }

    private fun bytesToHexString(src: ByteArray?): String {
        return src?.joinToString("") { "%02X".format(it) } ?: ""
    }

    private fun irParaLogin() {
        sessionManager.logout()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun obterHorarioAtualSaoPaulo(): String {
        return try {
            val timeZoneSP = TimeZone.getTimeZone("America/Sao_Paulo")
            val calendar = Calendar.getInstance(timeZoneSP)
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
        } catch (e: Exception) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
    }

    // MENU DE OPÇÕES
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            R.id.action_info -> {
                mostrarInfo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Deseja realmente sair?")
            .setPositiveButton("Sim") { _, _ ->
                sessionManager.logout()
                irParaLogin()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarInfo() {
        val professor = sessionManager.getProfessor()
        val empresas = sessionManager.getEmpresas()

        val info = StringBuilder()
        info.append("👤 Professor: ${professor?.nome}\n")
        info.append("📧 Email: ${professor?.email}\n")
        info.append("🏢 Empresa: ${professor?.nomeEmpresa}\n\n")
        info.append("📋 Empresas Atendidas:\n")

        empresas.forEach {
            info.append("• ${it.nomeEmpresa}\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Informações da Conta")
            .setMessage(info.toString())
            .setPositiveButton("OK", null)
            .show()
    }
}
