package br.com.condominio.escala.ui

import br.com.condominio.escala.domain.Turno
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val LocalePtBr: Locale = Locale.forLanguageTag("pt-BR")

private val fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy", LocalePtBr)
private val fmtDiaCurto = DateTimeFormatter.ofPattern("EEE, dd/MM", LocalePtBr)
private val fmtDiaLongo = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", LocalePtBr)
private val fmtHora = DateTimeFormatter.ofPattern("HH:mm", LocalePtBr)

fun LocalDate.formatada(): String = format(fmtData)
fun LocalDate.curta(): String = format(fmtDiaCurto).replaceFirstChar { it.uppercase() }
fun LocalDate.longa(): String = format(fmtDiaLongo).replaceFirstChar { it.uppercase() }
fun LocalTime.formatada(): String = format(fmtHora)
fun YearMonth.nome(): String =
    "${month.getDisplayName(TextStyle.FULL, LocalePtBr).replaceFirstChar { it.uppercase() }} de $year"

fun Turno.horario(): String {
    val fimTxt = fim.toLocalTime().format(fmtHora)
    val vira = if (fim.toLocalDate().isAfter(inicio.toLocalDate())) " (+1)" else ""
    return "${inicio.toLocalTime().format(fmtHora)}–$fimTxt$vira"
}

fun Int.paraHora(): LocalTime = LocalTime.of(this / 60, this % 60)
fun LocalTime.emMinutos(): Int = hour * 60 + minute

fun Double.horasTxt(): String =
    if (this % 1.0 == 0.0) "${toInt()}h" else String.format(LocalePtBr, "%.1fh", this)
