package br.com.condominio.escala.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CalculadoraEscalaTest {

    private val inicio = LocalDate.of(2026, 10, 1)

    private fun escala(
        id: Long,
        tipo: TipoEscala,
        de: String = "07:00",
        ate: String = "19:00",
        comeco: LocalDate = inicio,
    ) = EscalaFuncionario(
        escalaId = id,
        funcionarioId = id,
        regra = RegraEscala(tipo.diasTrabalho, tipo.diasFolga, comeco, LocalTime.parse(de), LocalTime.parse(ate)),
        posto = "Portaria",
    )

    @Test
    fun `12x36 alterna dias de trabalho e folga`() {
        val r = escala(1, TipoEscala.DOZE_X_TRINTA_E_SEIS).regra
        assertTrue(r.trabalhaEm(inicio))
        assertFalse(r.trabalhaEm(inicio.plusDays(1)))
        assertTrue(r.trabalhaEm(inicio.plusDays(2)))
        assertFalse(r.trabalhaEm(inicio.minusDays(2)))
        assertEquals(12.0, r.duracao.toMinutes() / 60.0, 0.0)
    }

    @Test
    fun `6x1 folga no setimo dia`() {
        val r = escala(1, TipoEscala.SEIS_X_UM).regra
        (0L..5L).forEach { assertTrue(r.trabalhaEm(inicio.plusDays(it))) }
        assertFalse(r.trabalhaEm(inicio.plusDays(6)))
        assertTrue(r.trabalhaEm(inicio.plusDays(7)))
    }

    @Test
    fun `turno 24h vira o dia e dura 24 horas`() {
        val r = escala(1, TipoEscala.VINTE_QUATRO_X_QUARENTA_E_OITO, "07:00", "07:00").regra
        assertTrue(r.viraODia)
        assertEquals(24L, r.duracao.toHours())
        assertTrue(r.trabalhaEm(inicio.plusDays(3)))
        assertFalse(r.trabalhaEm(inicio.plusDays(4)))
    }

    @Test
    fun `turno noturno aparece em servico na madrugada seguinte`() {
        val noturno = escala(2, TipoEscala.DOZE_X_TRINTA_E_SEIS, "19:00", "07:00")
        val madrugada = LocalDateTime.of(2026, 10, 2, 3, 0)
        val emServico = CalculadoraEscala.emServico(madrugada, listOf(noturno), emptyMap())
        assertEquals(listOf(2L), emServico.map { it.funcionarioId })
        val manha = LocalDateTime.of(2026, 10, 2, 7, 0)
        assertTrue(CalculadoraEscala.emServico(manha, listOf(noturno), emptyMap()).isEmpty())
    }

    @Test
    fun `ausencia marca o turno e remove de em servico`() {
        val e = escala(1, TipoEscala.DOZE_X_TRINTA_E_SEIS)
        val aus = mapOf(1L to listOf(Ausencia(inicio, inicio, MotivoAusencia.ATESTADO)))
        val turnos = CalculadoraEscala.turnosDoDia(inicio, listOf(e), aus)
        assertEquals(MotivoAusencia.ATESTADO, turnos.single().ausencia)
        assertTrue(CalculadoraEscala.emServico(inicio.atTime(10, 0), listOf(e), aus).isEmpty())
    }

    @Test
    fun `horas no mes descontam ausencias`() {
        val e = escala(1, TipoEscala.DOZE_X_TRINTA_E_SEIS)
        val fim = LocalDate.of(2026, 10, 31)
        // Outubro/2026 começando no dia 1: 16 plantões de 12h.
        assertEquals(192.0, CalculadoraEscala.horasNoPeriodo(inicio, fim, listOf(e), emptyMap())[1L]!!, 0.0)
        val aus = mapOf(1L to listOf(Ausencia(inicio, inicio.plusDays(3), MotivoAusencia.FERIAS)))
        assertEquals(168.0, CalculadoraEscala.horasNoPeriodo(inicio, fim, listOf(e), aus)[1L]!!, 0.0)
    }

    @Test
    fun `fim de vigencia encerra a escala`() {
        val e = escala(1, TipoEscala.DOZE_X_TRINTA_E_SEIS).copy(fimVigencia = inicio.plusDays(2))
        assertEquals(1, CalculadoraEscala.turnosDoDia(inicio.plusDays(2), listOf(e), emptyMap()).size)
        assertTrue(CalculadoraEscala.turnosDoDia(inicio.plusDays(4), listOf(e), emptyMap()).isEmpty())
    }

    @Test
    fun `proximos turnos pulam ausencias`() {
        val e = escala(1, TipoEscala.DOZE_X_TRINTA_E_SEIS)
        val aus = listOf(Ausencia(inicio, inicio, MotivoAusencia.FOLGA))
        val proximos = CalculadoraEscala.proximosTurnos(e, aus, inicio, 2)
        assertEquals(listOf(inicio.plusDays(2), inicio.plusDays(4)), proximos.map { it.data })
    }

    @Test
    fun `12x36 inverte os dias de trabalho todo dia 10`() {
        val r = escala(1, TipoEscala.DOZE_X_TRINTA_E_SEIS).regra.copy(diaInversao = 10)
        // Até o dia 9 segue o ciclo normal: trabalha nos dias ímpares de outubro.
        assertTrue(r.trabalhaEm(LocalDate.of(2026, 10, 9)))
        // Dia 10 inverte: passa a trabalhar nos dias pares.
        assertTrue(r.trabalhaEm(LocalDate.of(2026, 10, 10)))
        assertFalse(r.trabalhaEm(LocalDate.of(2026, 10, 11)))
        assertTrue(r.trabalhaEm(LocalDate.of(2026, 10, 12)))
        // Em 10/11 inverte de novo (segunda inversão volta ao ciclo original).
        val semInversao = r.copy(diaInversao = null)
        val nov10 = LocalDate.of(2026, 11, 10)
        assertEquals(2L, r.inversoesAte(nov10))
        assertEquals(semInversao.trabalhaEm(nov10), r.trabalhaEm(nov10))
        assertEquals(!semInversao.trabalhaEm(nov10.minusDays(1)), r.trabalhaEm(nov10.minusDays(1)))
    }

    @Test
    fun `inversao nao conta o proprio dia de inicio`() {
        val r = escala(1, TipoEscala.DOZE_X_TRINTA_E_SEIS, comeco = LocalDate.of(2026, 10, 10)).regra.copy(diaInversao = 10)
        assertEquals(0L, r.inversoesAte(LocalDate.of(2026, 10, 10)))
        assertTrue(r.trabalhaEm(LocalDate.of(2026, 10, 10)))
        assertEquals(1L, r.inversoesAte(LocalDate.of(2026, 11, 10)))
    }
}
