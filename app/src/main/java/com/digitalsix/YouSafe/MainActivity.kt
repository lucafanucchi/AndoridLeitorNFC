package com.digitalsix.YouSafe

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.digitalsix.YouSafe.network.*
import com.digitalsix.YouSafe.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.view.Menu
import android.view.MenuItem
import com.digitalsix.YouSafe.network.RetrofitInstance
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.card.MaterialCardView

// ==========================================
// DATA CLASS PARA PARTICIPANTE COM STATUS
// ==========================================
data class ParticipanteComStatus(
    val nfc: String,
    val funcionarioId: Int?,
    val nome: String?,
    val existe: Boolean
)

class MainActivity : AppCompatActivity() {

    // ==========================================
    // PROPRIEDADES
    // ==========================================
    private lateinit var sessionManager: SessionManager
    private var nfcAdapter: NfcAdapter? = null

    // Estado 1 - Criação de Aula
    private lateinit var layoutCriacaoAula: MaterialCardView
    private lateinit var spinnerEmpresa: AutoCompleteTextView
    private lateinit var spinnerUnidade: AutoCompleteTextView
    private lateinit var editTextDescricaoAula: TextInputEditText
    private lateinit var buttonIniciarAula: Button
    private lateinit var textViewBemVindo: TextView

    private var ultimaLeituraNFC: Long = 0
    private val DEBOUNCE_DELAY_MS = 1000L

    // Estado 2 - Leitura NFC
    private lateinit var layoutLeituraNFC: MaterialCardView
    private lateinit var textViewAulaEmAndamento: TextView
    private lateinit var textViewDescricaoAtual: TextView
    private lateinit var textViewUnidadeAtual: TextView
    private lateinit var textViewContador: TextView
    private lateinit var textViewListaNFCs: TextView
    private lateinit var buttonConfirmarAula: Button
    private lateinit var buttonAbortarAula: Button

    // ✅ Controle de Empresas e Unidades
    private var todasUnidadesDoInstrutor = listOf<EmpresaUnidadeResponse>()
    private var empresasMap = mapOf<String, EmpresaComUnidades>()  // empresa_nome -> dados
    private var empresaSelecionadaNome: String? = null
    private var unidadeIdSelecionada: Int? = null

    // Controle de Estado
    private var aulaEmProgressoId: Int? = null
    private var descricaoAulaAtual: String = ""

    private val listaParticipantes = mutableListOf<ParticipanteComStatus>()

    // ==========================================
    // DATA CLASSES AUXILIARES
    // ==========================================
    data class EmpresaComUnidades(
        val empresaNome: String,
        val empresaId: Int,
        val unidades: List<UnidadeInfo>
    )

    data class UnidadeInfo(
        val unidadeId: Int,
        val unidadeNome: String
    )

    // ==========================================
    // LIFECYCLE
    // ==========================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            irParaLogin()
            return
        }

        // Configurar Toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        bindViews()
        configurarNFC()
        carregarEmpresasEUnidades()
        configurarListeners()
        verificarAulaEmAndamento()
    }

    override fun onResume() {
        super.onResume()
        habilitarLeituraNFC()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                fazerLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        desabilitarLeituraNFC()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent?.action) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let { processarTagNFC(it) }
        }
    }

    // ==========================================
    // INICIALIZAÇÃO
    // ==========================================
    private fun bindViews() {
        layoutCriacaoAula = findViewById(R.id.layoutCriacaoAula)
        spinnerEmpresa = findViewById(R.id.spinnerEmpresa)
        spinnerUnidade = findViewById(R.id.spinnerUnidade)
        editTextDescricaoAula = findViewById(R.id.editTextDescricaoAula)
        buttonIniciarAula = findViewById(R.id.buttonIniciarAula)
        textViewBemVindo = findViewById(R.id.textViewBemVindo)

        layoutLeituraNFC = findViewById(R.id.layoutLeituraNFC)
        textViewAulaEmAndamento = findViewById(R.id.textViewAulaEmAndamento)
        textViewDescricaoAtual = findViewById(R.id.textViewDescricaoAtual)
        textViewUnidadeAtual = findViewById(R.id.textViewUnidadeAtual)
        textViewContador = findViewById(R.id.textViewContador)
        textViewListaNFCs = findViewById(R.id.textViewListaNFCs)
        buttonConfirmarAula = findViewById(R.id.buttonConfirmarAula)
        buttonAbortarAula = findViewById(R.id.buttonAbortarAula)

        val nomeUsuario = sessionManager.getNomeUsuario()
        textViewBemVindo.text = "Olá, $nomeUsuario!"
    }

    private fun fazerLogout() {
        AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Deseja realmente sair?")
            .setPositiveButton("Sim") { _, _ ->
                sessionManager.clearSession()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun configurarNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "❌ Dispositivo não suporta NFC", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (nfcAdapter?.isEnabled == false) {
            Toast.makeText(this, "⚠️ NFC desabilitado. Ative nas configurações.", Toast.LENGTH_LONG).show()
        }
    }

    // ==========================================
    // ✅ CARREGAR E FILTRAR EMPRESAS/UNIDADES
    // ==========================================
    private fun carregarEmpresasEUnidades() {
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "🔄 Carregando empresas e unidades...")

                // 1. Pegar unidades que o instrutor atende (do login)
                val usuario = sessionManager.getUsuario()
                val unidadesAtendidasDoLogin = usuario?.unidadesAtendidas ?: emptyList()

                if (unidadesAtendidasDoLogin.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "❌ Você não tem unidades atribuídas",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                Log.d("MainActivity", "📋 Unidades do login: ${unidadesAtendidasDoLogin.size}")
                unidadesAtendidasDoLogin.forEach {
                    Log.d("MainActivity", "  - ${it.nome} (${it.empresa})")
                }

                // 2. Buscar TODAS as unidades do banco (com empresa_id)
                val response = RetrofitInstance.api.getEmpresas()

                if (response.isSuccessful) {
                    val todasUnidadesDoBanco = response.body() ?: emptyList()
                    Log.d("MainActivity", "🗄️ Unidades do banco: ${todasUnidadesDoBanco.size}")

                    // 3. ✅ FILTRAR: só unidades que o instrutor atende
                    val unidadeIdsDoInstrutor = unidadesAtendidasDoLogin.map { it.id }.toSet()

                    todasUnidadesDoInstrutor = todasUnidadesDoBanco.filter { unidade ->
                        unidade.unidadeId in unidadeIdsDoInstrutor
                    }

                    Log.d("MainActivity", "✅ Unidades filtradas: ${todasUnidadesDoInstrutor.size}")

                    if (todasUnidadesDoInstrutor.isEmpty()) {
                        Toast.makeText(
                            this@MainActivity,
                            "❌ Erro ao carregar dados das unidades",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    // 4. Agrupar por empresa
                    processarEmpresasEUnidades()
                    configurarSpinnerEmpresas()

                } else {
                    Log.e("MainActivity", "❌ Erro API: ${response.code()}")
                    Toast.makeText(
                        this@MainActivity,
                        "❌ Erro ao carregar unidades: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Exceção: ${e.message}", e)
                Toast.makeText(
                    this@MainActivity,
                    "❌ Erro: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun processarEmpresasEUnidades() {
        // Agrupar unidades por empresa_nome (empresa pode ter múltiplas unidades)
        empresasMap = todasUnidadesDoInstrutor
            .groupBy { it.nomeEmpresa }
            .mapValues { (empresaNome, unidades) ->
                EmpresaComUnidades(
                    empresaNome = empresaNome,
                    empresaId = unidades.first().empresaId,  // Todas têm o mesmo empresa_id
                    unidades = unidades.map {
                        UnidadeInfo(
                            unidadeId = it.unidadeId,
                            unidadeNome = it.nomeUnidade
                        )
                    }.sortedBy { it.unidadeNome }
                )
            }

        Log.d("MainActivity", "📊 Processadas ${empresasMap.size} empresas:")
        empresasMap.forEach { (nome, dados) ->
            Log.d("MainActivity", "  🏢 $nome (ID: ${dados.empresaId}): ${dados.unidades.size} unidades")
            dados.unidades.forEach { unidade ->
                Log.d("MainActivity", "    🏭 ${unidade.unidadeNome} (ID: ${unidade.unidadeId})")
            }
        }
    }

    private fun configurarSpinnerEmpresas() {
        val nomesEmpresas = empresasMap.keys.sorted()

        if (nomesEmpresas.isEmpty()) {
            Toast.makeText(this, "❌ Nenhuma empresa disponível", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("MainActivity", "🏢 Empresas no spinner: $nomesEmpresas")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            nomesEmpresas
        )
        spinnerEmpresa.setAdapter(adapter)

        spinnerEmpresa.setOnItemClickListener { parent, view, position, id ->
            val empresaNome = nomesEmpresas[position]
            empresaSelecionadaNome = empresaNome

            val empresaData = empresasMap[empresaNome]
            Log.d("MainActivity", "🏢 Empresa selecionada: $empresaNome (ID: ${empresaData?.empresaId})")

            atualizarSpinnerUnidades(empresaNome)
        }
    }

    private fun atualizarSpinnerUnidades(empresaNome: String) {
        val empresaData = empresasMap[empresaNome]

        if (empresaData == null) {
            Log.e("MainActivity", "❌ Empresa não encontrada: $empresaNome")
            spinnerUnidade.isEnabled = false
            spinnerUnidade.setAdapter(null)
            return
        }

        val unidades = empresaData.unidades

        Log.d("MainActivity", "🏭 Unidades da empresa '$empresaNome': ${unidades.size}")

        if (unidades.isEmpty()) {
            Toast.makeText(this, "⚠️ Nenhuma unidade disponível para esta empresa", Toast.LENGTH_SHORT).show()
            spinnerUnidade.isEnabled = false
            spinnerUnidade.setAdapter(null)
            return
        }

        // Criar lista de strings para o spinner
        val nomesUnidades = unidades.map { it.unidadeNome }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            nomesUnidades
        )
        spinnerUnidade.setAdapter(adapter)
        spinnerUnidade.isEnabled = true

        spinnerUnidade.setOnItemClickListener { parent, view, position, id ->
            val unidade = unidades[position]
            unidadeIdSelecionada = unidade.unidadeId

            Log.d("MainActivity", "🏭 Unidade selecionada: ${unidade.unidadeNome} (ID: ${unidade.unidadeId})")
        }
    }

    private fun configurarListeners() {
        buttonIniciarAula.setOnClickListener { iniciarNovaAula() }
        buttonConfirmarAula.setOnClickListener { mostrarDialogConfirmacao() }
        buttonAbortarAula.setOnClickListener { mostrarDialogAbortarAula() }
    }

    // ==========================================
    // CRASH RECOVERY
    // ==========================================
    private fun verificarAulaEmAndamento() {
        val aulaId = sessionManager.getAulaEmProgresso()

        if (aulaId != null) {
            AlertDialog.Builder(this)
                .setTitle("⚠️ Aula em Andamento")
                .setMessage("Você tem uma aula que não foi finalizada. Deseja continuar ou abortar?")
                .setPositiveButton("Continuar") { _, _ ->
                    aulaEmProgressoId = aulaId
                    mostrarEstadoLeituraNFC("Aula em andamento", "Recuperada")
                }
                .setNegativeButton("Abortar") { _, _ ->
                    abortarAulaSemParticipantes(aulaId)
                }
                .setCancelable(false)
                .show()
        }
    }

    // ==========================================
    // FLUXO 1: CRIAR AULA
    // ==========================================
    private fun iniciarNovaAula() {
        val descricao = editTextDescricaoAula.text.toString().trim()

        if (descricao.isEmpty()) {
            Toast.makeText(this, "⚠️ Digite uma descrição para a aula", Toast.LENGTH_SHORT).show()
            return
        }

        if (empresaSelecionadaNome == null) {
            Toast.makeText(this, "⚠️ Selecione uma empresa", Toast.LENGTH_SHORT).show()
            return
        }

        if (unidadeIdSelecionada == null) {
            Toast.makeText(this, "⚠️ Selecione uma unidade", Toast.LENGTH_SHORT).show()
            return
        }

        val empresaData = empresasMap[empresaSelecionadaNome]
        Log.d("MainActivity", "🎯 Criar aula: Empresa='$empresaSelecionadaNome' (ID: ${empresaData?.empresaId}), Unidade ID=$unidadeIdSelecionada")

        criarAula(descricao, unidadeIdSelecionada!!)
    }

    private fun criarAula(descricao: String, unidadeId: Int) {
        val token = sessionManager.getTokenWithBearer() ?: run {
            irParaLogin()
            return
        }

        buttonIniciarAula.isEnabled = false
        buttonIniciarAula.text = "Criando aula..."

        lifecycleScope.launch {
            try {
                val dataAtual = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

                val request = CriarAulaRequest(
                    descricao = descricao,
                    unidadeId = unidadeId,
                    data = dataAtual
                )

                Log.d("MainActivity", "📤 Request: $request")

                val response = RetrofitInstance.api.criarAula(token, request)

                if (response.isSuccessful) {
                    val aulaResponse = response.body()

                    if (aulaResponse != null) {
                        aulaEmProgressoId = aulaResponse.aula.aulaId
                        descricaoAulaAtual = descricao

                        sessionManager.salvarAulaEmProgresso(aulaResponse.aula.aulaId)

                        Toast.makeText(
                            this@MainActivity,
                            "✅ ${aulaResponse.message}",
                            Toast.LENGTH_SHORT
                        ).show()

                        val unidadeNome = todasUnidadesDoInstrutor
                            .find { it.unidadeId == unidadeId }
                            ?.nomeUnidade ?: "Unidade"

                        mostrarEstadoLeituraNFC(descricao, unidadeNome)
                    } else {
                        mostrarErro("Resposta vazia do servidor")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("MainActivity", "❌ Erro response: $errorBody")
                    mostrarErro(errorBody ?: "Erro ao criar aula")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Exception criar aula: ${e.message}", e)
                mostrarErro("Erro: ${e.message}")
            } finally {
                buttonIniciarAula.isEnabled = true
                buttonIniciarAula.text = "🎯 Iniciar Aula"
            }
        }
    }

    // ==========================================
    // RESTO DO CÓDIGO (NFC, CONFIRMAR, ABORTAR)
    // ==========================================

    private fun mostrarEstadoLeituraNFC(descricao: String, unidade: String) {
        layoutCriacaoAula.visibility = View.GONE
        layoutLeituraNFC.visibility = View.VISIBLE

        textViewDescricaoAtual.text = descricao
        textViewUnidadeAtual.text = "Unidade: $unidade"
        textViewContador.text = "0"
        textViewListaNFCs.text = "Nenhum crachá lido ainda."

        listaParticipantes.clear()
    }

    private fun mostrarErro(mensagem: String) {
        Toast.makeText(this, "❌ $mensagem", Toast.LENGTH_LONG).show()
        Log.e("MainActivity", mensagem)
    }

    private fun irParaLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun habilitarLeituraNFC() {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    private fun desabilitarLeituraNFC() {
        nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun processarTagNFC(tag: Tag) {
        val agora = System.currentTimeMillis()
        if (agora - ultimaLeituraNFC < DEBOUNCE_DELAY_MS) {
            Log.d("NFC", "⏭️ Leitura ignorada (debounce)")
            return
        }
        ultimaLeituraNFC = agora

        val nfcId = tag.id.joinToString("") { "%02X".format(it) }
        Log.d("NFC", "📱 NFC lido: $nfcId")

        if (listaParticipantes.any { it.nfc == nfcId }) {
            Log.w("NFC", "⚠️ NFC duplicado: $nfcId")
            Toast.makeText(this, "⚠️ Crachá já registrado!", Toast.LENGTH_SHORT).show()
            return
        }

        buscarFuncionarioPorNFC(nfcId)
    }

    private fun buscarFuncionarioPorNFC(nfc: String) {
        if (unidadeIdSelecionada == null) {
            Log.e("NFC", "❌ unidadeIdSelecionada é null")
            return
        }

        lifecycleScope.launch {
            try {
                val request = GetFuncionarioByNFCRequest(
                    nfc = nfc,
                    unidade_id = unidadeIdSelecionada!!
                )

                val response = RetrofitInstance.api.getFuncionarioByNFC(request)

                if (response.isSuccessful) {
                    val funcionario = response.body()

                    if (funcionario != null) {
                        val existe = funcionario.funcionario_id != null
                        val nome = funcionario.nome ?: "Desconhecido"

                        val participante = ParticipanteComStatus(
                            nfc = nfc,
                            funcionarioId = funcionario.funcionario_id,
                            nome = nome,
                            existe = existe
                        )

                        listaParticipantes.add(participante)
                        atualizarUI()

                        val mensagem = if (existe) {
                            "✅ $nome"
                        } else {
                            "⚠️ Crachá não cadastrado"
                        }

                        Toast.makeText(this@MainActivity, mensagem, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("NFC", "❌ Erro response: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("NFC", "❌ Exceção: ${e.message}", e)
            }
        }
    }

    private fun atualizarUI() {
        textViewContador.text = listaParticipantes.size.toString()

        val listaTexto = if (listaParticipantes.isEmpty()) {
            "Nenhum crachá lido ainda."
        } else {
            listaParticipantes.joinToString("\n") { p ->
                val status = if (p.existe) "✅" else "⚠️"
                "$status ${p.nome ?: "Desconhecido"} (${p.nfc})"
            }
        }

        textViewListaNFCs.text = listaTexto
    }

    private fun mostrarDialogConfirmacao() {
        if (listaParticipantes.isEmpty()) {
            Toast.makeText(this, "⚠️ Nenhum participante registrado!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("✅ Confirmar Aula")
            .setMessage("Confirmar aula com ${listaParticipantes.size} participante(s)?")
            .setPositiveButton("Sim") { _, _ -> confirmarAula() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarAula() {
        val aulaId = aulaEmProgressoId ?: return
        val token = sessionManager.getTokenWithBearer() ?: run {
            irParaLogin()
            return
        }

        buttonConfirmarAula.isEnabled = false
        buttonConfirmarAula.text = "Confirmando..."

        lifecycleScope.launch {
            try {
                val participantes = listaParticipantes.map { p ->
                    ParticipanteAula(
                        nfc = p.nfc,
                        unidadeId = unidadeIdSelecionada!!,
                        funcionarioId = p.funcionarioId,
                        nome = p.nome
                    )
                }

                val request = ConfirmarAulaRequest(participantes = participantes)
                val response = RetrofitInstance.api.confirmarAula(token, aulaId, request)

                if (response.isSuccessful) {
                    sessionManager.limparAulaEmProgresso()

                    Toast.makeText(
                        this@MainActivity,
                        "✅ Aula confirmada com sucesso!",
                        Toast.LENGTH_LONG
                    ).show()

                    voltarParaEstadoInicial()
                } else {
                    val errorBody = response.errorBody()?.string()
                    mostrarErro(errorBody ?: "Erro ao confirmar aula")
                }
            } catch (e: Exception) {
                mostrarErro("Erro: ${e.message}")
            } finally {
                buttonConfirmarAula.isEnabled = true
                buttonConfirmarAula.text = "✅ Confirmar Aula"
            }
        }
    }

    private fun mostrarDialogAbortarAula() {
        AlertDialog.Builder(this)
            .setTitle("❌ Abortar Aula")
            .setMessage("Tem certeza que deseja abortar esta aula?")
            .setPositiveButton("Sim") { _, _ -> abortarAula() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abortarAula() {
        val aulaId = aulaEmProgressoId ?: return
        val token = sessionManager.getTokenWithBearer() ?: run {
            irParaLogin()
            return
        }

        buttonAbortarAula.isEnabled = false
        buttonAbortarAula.text = "Abortando..."

        lifecycleScope.launch {
            try {
                val request = AbortarAulaRequest(participantes = null)
                val response = RetrofitInstance.api.abortarAula(token, aulaId, request)

                if (response.isSuccessful) {
                    sessionManager.limparAulaEmProgresso()

                    Toast.makeText(
                        this@MainActivity,
                        "✅ Aula abortada",
                        Toast.LENGTH_SHORT
                    ).show()

                    voltarParaEstadoInicial()
                } else {
                    val errorBody = response.errorBody()?.string()
                    mostrarErro(errorBody ?: "Erro ao abortar aula")
                }
            } catch (e: Exception) {
                mostrarErro("Erro: ${e.message}")
            } finally {
                buttonAbortarAula.isEnabled = true
                buttonAbortarAula.text = "❌ Abortar Aula"
            }
        }
    }

    private fun abortarAulaSemParticipantes(aulaId: Int) {
        val token = sessionManager.getTokenWithBearer() ?: run {
            irParaLogin()
            return
        }

        lifecycleScope.launch {
            try {
                val request = AbortarAulaRequest(participantes = null)
                val response = RetrofitInstance.api.abortarAula(token, aulaId, request)

                if (response.isSuccessful) {
                    sessionManager.limparAulaEmProgresso()
                    Toast.makeText(
                        this@MainActivity,
                        "✅ Aula abortada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao abortar aula: ${e.message}")
            }
        }
    }

    private fun voltarParaEstadoInicial() {
        layoutLeituraNFC.visibility = View.GONE
        layoutCriacaoAula.visibility = View.VISIBLE

        aulaEmProgressoId = null
        unidadeIdSelecionada = null
        empresaSelecionadaNome = null
        descricaoAulaAtual = ""
        listaParticipantes.clear()

        editTextDescricaoAula.text?.clear()

        spinnerEmpresa.setText("", false)
        spinnerUnidade.isEnabled = false
        spinnerUnidade.setText("", false)
    }
}