package br.com.condominio.escala.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condominio.escala.data.Equipe
import br.com.condominio.escala.domain.CalculadoraEscala
import br.com.condominio.escala.domain.Turno
import br.com.condominio.escala.ui.Bolinha
import br.com.condominio.escala.ui.EscalaViewModel
import br.com.condominio.escala.ui.horasTxt
import br.com.condominio.escala.ui.longa
import br.com.condominio.escala.ui.nome
import java.time.LocalDate
import java.time.YearMonth

private val diasSemana = listOf("D", "S", "T", "Q", "Q", "S", "S")

@Composable
fun CalendarioScreen(vm: EscalaViewModel, abrirFuncionario: (Long) -> Unit, modifier: Modifier = Modifier) {
    val equipe by vm.equipe.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var mesEpoch by rememberSaveable { mutableStateOf(YearMonth.now().atDay(1).toEpochDay()) }
    var selEpoch by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    val mes = YearMonth.from(LocalDate.ofEpochDay(mesEpoch))
    val selecionado = LocalDate.ofEpochDay(selEpoch)

    val turnosMes: Map<LocalDate, List<Turno>> = remember(equipe, mes) {
        (1..mes.lengthOfMonth()).associate { d ->
            val data = mes.atDay(d)
            data to CalculadoraEscala.turnosDoDia(data, equipe.escalasAtivas, equipe.ausencias)
        }
    }
    val horasMes = remember(equipe, mes) {
        CalculadoraEscala.horasNoPeriodo(mes.atDay(1), mes.atEndOfMonth(), equipe.escalasAtivas, equipe.ausencias)
    }
    val turnosSel = turnosMes[selecionado]
        ?: CalculadoraEscala.turnosDoDia(selecionado, equipe.escalasAtivas, equipe.ausencias)

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { mesEpoch = mes.minusMonths(1).atDay(1).toEpochDay() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mês anterior")
                }
                Text(mes.nome(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { mesEpoch = mes.plusMonths(1).atDay(1).toEpochDay() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Próximo mês")
                }
                IconButton(onClick = {
                    val envio = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Escala — ${mes.nome()}")
                        putExtra(Intent.EXTRA_TEXT, vm.textoDoMes(equipe, mes))
                    }
                    contexto.startActivity(Intent.createChooser(envio, "Compartilhar escala"))
                }) { Icon(Icons.Default.Share, "Compartilhar escala do mês") }
            }
        }
        item {
            GradeMes(mes, selecionado, turnosMes, equipe) { selEpoch = it.toEpochDay() }
        }
        item {
            TextButton(onClick = {
                mesEpoch = YearMonth.now().atDay(1).toEpochDay()
                selEpoch = LocalDate.now().toEpochDay()
            }) { Text("Ir para hoje") }
        }
        item { Secao(selecionado.longa()) }
        if (turnosSel.isEmpty()) item { Vazio("Nenhum turno neste dia.") }
        items(turnosSel, key = { "${it.escalaId}-${it.inicio}" }) { CartaoTurno(it, equipe, aoClicar = abrirFuncionario) }

        if (horasMes.isNotEmpty()) {
            item { Secao("Horas previstas no mês") }
            items(horasMes.entries.sortedByDescending { it.value }.toList(), key = { "h-${it.key}" }) { (id, h) ->
                val f = equipe.funcionario(id) ?: return@items
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bolinha(f.cor)
                    Text(f.nome, modifier = Modifier.weight(1f))
                    Text(h.horasTxt(), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GradeMes(
    mes: YearMonth,
    selecionado: LocalDate,
    turnos: Map<LocalDate, List<Turno>>,
    equipe: Equipe,
    aoSelecionar: (LocalDate) -> Unit,
) {
    val hoje = LocalDate.now()
    // DayOfWeek: segunda = 1 ... domingo = 7. A grade começa no domingo.
    val deslocamento = mes.atDay(1).dayOfWeek.value % 7
    val celulas = deslocamento + mes.lengthOfMonth()
    val linhas = (celulas + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            diasSemana.forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
            }
        }
        repeat(linhas) { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { col ->
                    val dia = linha * 7 + col - deslocamento + 1
                    Box(Modifier.weight(1f).aspectRatio(0.8f)) {
                        if (dia in 1..mes.lengthOfMonth()) {
                            val data = mes.atDay(dia)
                            CelulaDia(
                                data = data,
                                turnos = turnos[data].orEmpty(),
                                equipe = equipe,
                                selecionado = data == selecionado,
                                hoje = data == hoje,
                                aoClicar = { aoSelecionar(data) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CelulaDia(
    data: LocalDate,
    turnos: List<Turno>,
    equipe: Equipe,
    selecionado: Boolean,
    hoje: Boolean,
    aoClicar: () -> Unit,
) {
    val forma = RoundedCornerShape(8.dp)
    val cs = MaterialTheme.colorScheme
    var mod = Modifier.fillMaxWidth().aspectRatio(0.8f)
        .background(if (selecionado) cs.primaryContainer else cs.surfaceVariant.copy(alpha = 0.4f), forma)
    if (hoje) mod = mod.border(2.dp, cs.primary, forma)
    Column(
        mod.clickable(onClick = aoClicar).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            data.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (hoje) FontWeight.Bold else FontWeight.Normal,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            turnos.filter { it.confirmado }.take(6).forEach { t ->
                equipe.funcionario(t.funcionarioId)?.let { Bolinha(it.cor, 7.dp) }
            }
        }
    }
}
