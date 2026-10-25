package br.com.condominio.escala.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** Modelos de escala mais usados em portarias e condomínios. */
enum class TipoEscala(val rotulo: String, val diasTrabalho: Int, val diasFolga: Int) {
    DOZE_X_TRINTA_E_SEIS("12x36", 1, 1),
    VINTE_QUATRO_X_QUARENTA_E_OITO("24x48", 1, 2),
    SEIS_X_UM("6x1", 6, 1),
    CINCO_X_DOIS("5x2", 5, 2),
    QUATRO_X_DOIS("4x2", 4, 2),
    PERSONALIZADA("Personalizada", 1, 1);

    companion object {
        fun porNome(nome: String): TipoEscala = entries.firstOrNull { it.name == nome } ?: PERSONALIZADA
    }
}

enum class MotivoAusencia(val rotulo: String) {
    FERIAS("Férias"),
    ATESTADO("Atestado"),
    FOLGA("Folga extra"),
    TROCA("Troca de plantão"),
    OUTRO("Outro");

    companion object {
        fun porNome(nome: String): MotivoAusencia = entries.firstOrNull { it.name == nome } ?: OUTRO
    }
}

/**
 * Regra cíclica de escala: a partir de [inicio], o funcionário trabalha [diasTrabalho] dias
 * seguidos e folga [diasFolga] dias, repetindo indefinidamente.
 *
 * Quando [horaFim] não é posterior a [horaInicio] o turno é noturno e termina no dia seguinte
 * (ex.: 19:00–07:00, ou 07:00–07:00 para um plantão de 24h).
 *
 * Se [diaInversao] estiver definido, todo mês nesse dia os dias de trabalho e de folga se
 * invertem (quem folgaria passa a trabalhar e vice-versa). Útil para alternar quem pega os
 * dias pares/ímpares numa 12x36 a cada mês.
 */
data class RegraEscala(
    val diasTrabalho: Int,
    val diasFolga: Int,
    val inicio: LocalDate,
    val horaInicio: LocalTime,
    val horaFim: LocalTime,
    val diaInversao: Int? = null,
) {
    init {
        require(diasTrabalho >= 1) { "diasTrabalho deve ser >= 1" }
        require(diasFolga >= 0) { "diasFolga deve ser >= 0" }
        require(diaInversao == null || diaInversao in 1..28) { "diaInversao deve estar entre 1 e 28" }
    }

    val ciclo: Int get() = diasTrabalho + diasFolga

    val viraODia: Boolean get() = !horaFim.isAfter(horaInicio)

    val duracao: Duration
        get() {
            val d = Duration.between(horaInicio, horaFim)
            return if (viraODia) d.plusDays(1) else d
        }

    /** Indica se um turno desta escala começa em [data]. */
    fun trabalhaEm(data: LocalDate): Boolean {
        val dias = ChronoUnit.DAYS.between(inicio, data)
        if (dias < 0) return false
        val noCiclo = Math.floorMod(dias, ciclo.toLong()) < diasTrabalho
        return if (inversoesAte(data) % 2 == 1L) !noCiclo else noCiclo
    }

    /** Quantas vezes o dia de inversão ocorreu depois de [inicio] e até [data] (inclusive). */
    fun inversoesAte(data: LocalDate): Long {
        val dia = diaInversao ?: return 0
        fun marcos(d: LocalDate) = d.year * 12L + d.monthValue + if (d.dayOfMonth >= dia) 1 else 0
        return maxOf(0, marcos(data) - marcos(inicio))
    }

    fun inicioDoTurno(data: LocalDate): LocalDateTime = data.atTime(horaInicio)

    fun fimDoTurno(data: LocalDate): LocalDateTime =
        if (viraODia) data.plusDays(1).atTime(horaFim) else data.atTime(horaFim)
}

data class Ausencia(
    val inicio: LocalDate,
    val fim: LocalDate,
    val motivo: MotivoAusencia,
) {
    fun cobre(data: LocalDate): Boolean = !data.isBefore(inicio) && !data.isAfter(fim)
}

/** Um turno concreto de um funcionário. */
data class Turno(
    val funcionarioId: Long,
    val escalaId: Long,
    val inicio: LocalDateTime,
    val fim: LocalDateTime,
    val posto: String,
    val ausencia: MotivoAusencia? = null,
) {
    val data: LocalDate get() = inicio.toLocalDate()
    val confirmado: Boolean get() = ausencia == null
    fun contem(momento: LocalDateTime): Boolean = !momento.isBefore(inicio) && momento.isBefore(fim)
    val horas: Double get() = Duration.between(inicio, fim).toMinutes() / 60.0
}

/** Escala atribuída a um funcionário (visão de domínio, sem dependências do Android). */
data class EscalaFuncionario(
    val escalaId: Long,
    val funcionarioId: Long,
    val regra: RegraEscala,
    val posto: String,
    val fimVigencia: LocalDate? = null,
)

object CalculadoraEscala {

    /**
     * Turnos que começam em [data]. Turnos que caem em uma ausência do funcionário são
     * retornados com [Turno.ausencia] preenchido, para que a interface possa exibir a falta.
     */
    fun turnosDoDia(
        data: LocalDate,
        escalas: List<EscalaFuncionario>,
        ausencias: Map<Long, List<Ausencia>>,
    ): List<Turno> = escalas
        .filter { it.regra.trabalhaEm(data) && (it.fimVigencia == null || !data.isAfter(it.fimVigencia)) }
        .map { e ->
            val motivo = ausencias[e.funcionarioId].orEmpty().firstOrNull { it.cobre(data) }?.motivo
            Turno(
                funcionarioId = e.funcionarioId,
                escalaId = e.escalaId,
                inicio = e.regra.inicioDoTurno(data),
                fim = e.regra.fimDoTurno(data),
                posto = e.posto,
                ausencia = motivo,
            )
        }
        .sortedBy { it.inicio }

    /** Turnos confirmados em andamento no [momento], incluindo noturnos iniciados na véspera. */
    fun emServico(
        momento: LocalDateTime,
        escalas: List<EscalaFuncionario>,
        ausencias: Map<Long, List<Ausencia>>,
    ): List<Turno> {
        val hoje = momento.toLocalDate()
        return (turnosDoDia(hoje.minusDays(1), escalas, ausencias) + turnosDoDia(hoje, escalas, ausencias))
            .filter { it.confirmado && it.contem(momento) }
    }

    /** Próximos [quantidade] turnos confirmados de uma escala a partir de [aPartirDe] (inclusive). */
    fun proximosTurnos(
        escala: EscalaFuncionario,
        ausencias: List<Ausencia>,
        aPartirDe: LocalDate,
        quantidade: Int,
        limiteDias: Int = 400,
    ): List<Turno> {
        val mapa = mapOf(escala.funcionarioId to ausencias)
        return generateSequence(aPartirDe) { it.plusDays(1) }
            .take(limiteDias)
            .flatMap { turnosDoDia(it, listOf(escala), mapa) }
            .filter { it.confirmado }
            .take(quantidade)
            .toList()
    }

    /** Total de horas confirmadas por funcionário entre [de] e [ate] (inclusive). */
    fun horasNoPeriodo(
        de: LocalDate,
        ate: LocalDate,
        escalas: List<EscalaFuncionario>,
        ausencias: Map<Long, List<Ausencia>>,
    ): Map<Long, Double> {
        val total = mutableMapOf<Long, Double>()
        var d = de
        while (!d.isAfter(ate)) {
            turnosDoDia(d, escalas, ausencias).filter { it.confirmado }.forEach {
                total[it.funcionarioId] = (total[it.funcionarioId] ?: 0.0) + it.horas
            }
            d = d.plusDays(1)
        }
        return total
    }
}
