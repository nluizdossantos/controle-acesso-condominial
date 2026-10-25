package br.com.condominio.escala.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.condominio.escala.data.AppDatabase
import br.com.condominio.escala.data.AusenciaEntity
import br.com.condominio.escala.data.Equipe
import br.com.condominio.escala.data.EscalaEntity
import br.com.condominio.escala.data.EscalaRepository
import br.com.condominio.escala.data.FuncionarioEntity
import br.com.condominio.escala.domain.CalculadoraEscala
import br.com.condominio.escala.domain.Turno
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.YearMonth

class EscalaViewModel(app: Application) : AndroidViewModel(app) {
    val repo = EscalaRepository(AppDatabase.get(app))

    val equipe: StateFlow<Equipe> =
        repo.equipe.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Equipe())

    /** Relógio que avança a cada 30s para manter a tela "Hoje" atualizada. */
    val agora: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(30_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDateTime.now())

    fun salvarFuncionario(f: FuncionarioEntity) = viewModelScope.launch { repo.salvar(f) }
    fun excluirFuncionario(f: FuncionarioEntity) = viewModelScope.launch { repo.excluir(f) }
    fun salvarEscala(e: EscalaEntity) = viewModelScope.launch { repo.salvar(e) }
    fun excluirEscala(e: EscalaEntity) = viewModelScope.launch { repo.excluir(e) }
    fun salvarAusencia(a: AusenciaEntity) = viewModelScope.launch { repo.salvar(a) }
    fun excluirAusencia(a: AusenciaEntity) = viewModelScope.launch { repo.excluir(a) }

    fun funcionario(id: Long): Flow<FuncionarioEntity?> = repo.funcionario(id)
    fun escalasDe(id: Long): Flow<List<EscalaEntity>> = repo.escalasDe(id)
    fun ausenciasDe(id: Long): Flow<List<AusenciaEntity>> = repo.ausenciasDe(id)

    /** Texto da escala do mês para compartilhar (WhatsApp, e-mail etc.). */
    fun textoDoMes(equipe: Equipe, mes: YearMonth): String = buildString {
        appendLine("📅 Escala de trabalho — ${mes.nome()}")
        appendLine()
        var d = mes.atDay(1)
        while (!d.isAfter(mes.atEndOfMonth())) {
            val turnos = CalculadoraEscala.turnosDoDia(d, equipe.escalasAtivas, equipe.ausencias)
            if (turnos.isNotEmpty()) {
                appendLine("*${d.curta()}*")
                turnos.forEach { appendLine("  ${linhaTurno(equipe, it)}") }
            }
            d = d.plusDays(1)
        }
        val horas = CalculadoraEscala.horasNoPeriodo(
            mes.atDay(1), mes.atEndOfMonth(), equipe.escalasAtivas, equipe.ausencias,
        )
        if (horas.isNotEmpty()) {
            appendLine()
            appendLine("⏱ Horas no mês")
            horas.entries.sortedBy { equipe.funcionario(it.key)?.nome }.forEach { (id, h) ->
                appendLine("  ${equipe.funcionario(id)?.nome ?: "?"}: ${h.horasTxt()}")
            }
        }
    }

    private fun linhaTurno(equipe: Equipe, t: Turno): String {
        val nome = equipe.funcionario(t.funcionarioId)?.nome ?: "?"
        val falta = t.ausencia?.let { " — ${it.rotulo.uppercase()}" } ?: ""
        return "${t.horario()} ${nome} (${t.posto})$falta"
    }
}
