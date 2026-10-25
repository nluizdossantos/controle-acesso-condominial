package br.com.condominio.escala.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condominio.escala.data.AusenciaEntity
import br.com.condominio.escala.data.EscalaEntity
import br.com.condominio.escala.data.FuncionarioEntity
import br.com.condominio.escala.domain.CalculadoraEscala
import br.com.condominio.escala.domain.MotivoAusencia
import br.com.condominio.escala.domain.TipoEscala
import br.com.condominio.escala.ui.Bolinha
import br.com.condominio.escala.ui.CampoData
import br.com.condominio.escala.ui.CampoHora
import br.com.condominio.escala.ui.ConfirmarExclusao
import br.com.condominio.escala.ui.EscalaViewModel
import br.com.condominio.escala.ui.Seletor
import br.com.condominio.escala.ui.curta
import br.com.condominio.escala.ui.emMinutos
import br.com.condominio.escala.ui.formatada
import br.com.condominio.escala.ui.horario
import br.com.condominio.escala.ui.horasTxt
import br.com.condominio.escala.ui.paraHora
import java.time.LocalDate
import java.time.LocalTime

/** Itens que podem ser abertos em diálogo nesta tela. */
private sealed interface Dialogo {
    data object EditarFuncionario : Dialogo
    data object ExcluirFuncionario : Dialogo
    data class Escala(val escala: EscalaEntity?) : Dialogo
    data class ExcluirEscala(val escala: EscalaEntity) : Dialogo
    data class Ausencia(val ausencia: AusenciaEntity?) : Dialogo
    data class ExcluirAusencia(val ausencia: AusenciaEntity) : Dialogo
}

@Composable
fun FuncionarioScreen(vm: EscalaViewModel, id: Long, aoExcluir: () -> Unit, modifier: Modifier = Modifier) {
    val funcionario by remember(id) { vm.funcionario(id) }.collectAsStateWithLifecycle(null)
    val escalas by remember(id) { vm.escalasDe(id) }.collectAsStateWithLifecycle(emptyList())
    val ausencias by remember(id) { vm.ausenciasDe(id) }.collectAsStateWithLifecycle(emptyList())
    var dialogo by remember { mutableStateOf<Dialogo?>(null) }
    val contexto = LocalContext.current
    val f = funcionario ?: return
    val hoje = LocalDate.now()

    val ausDominio = ausencias.map { it.paraDominio() }
    val proximos = escalas
        .flatMap { CalculadoraEscala.proximosTurnos(it.paraDominio(), ausDominio, hoje, 7) }
        .sortedBy { it.inicio }
        .take(7)

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Bolinha(f.cor, 28.dp)
                Column(Modifier.weight(1f)) {
                    Text(f.nome, style = MaterialTheme.typography.headlineSmall)
                    Text(if (f.ativo) f.cargo else "${f.cargo} · inativo", style = MaterialTheme.typography.bodyMedium)
                }
                if (f.telefone.isNotBlank()) {
                    IconButton(onClick = {
                        contexto.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${f.telefone}")))
                    }) { Icon(Icons.Default.Phone, "Ligar") }
                }
                IconButton(onClick = { dialogo = Dialogo.EditarFuncionario }) { Icon(Icons.Default.Edit, "Editar") }
                IconButton(onClick = { dialogo = Dialogo.ExcluirFuncionario }) { Icon(Icons.Default.Delete, "Excluir") }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Secao("Escalas")
                Spacer(Modifier.weight(1f))
                AssistChip(onClick = { dialogo = Dialogo.Escala(null) }, label = { Text("Nova escala") },
                    leadingIcon = { Icon(Icons.Default.Add, null) })
            }
        }
        if (escalas.isEmpty()) item { Vazio("Defina a escala para que os turnos apareçam no calendário.") }
        items(escalas, key = { "e-${it.id}" }) { e ->
            val regra = e.paraDominio().regra
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        val tipo = TipoEscala.porNome(e.tipo)
                        val nomeTipo = if (tipo == TipoEscala.PERSONALIZADA) "${e.diasTrabalho}x${e.diasFolga} dias" else tipo.rotulo
                        Text("$nomeTipo · ${e.posto}", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${regra.horaInicio.formatada()}–${regra.horaFim.formatada()} " +
                                "(${(regra.duracao.toMinutes() / 60.0).horasTxt()})",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        val vigencia = "Desde ${regra.inicio.formatada()}" +
                            (e.fimEpochDay?.let { " até ${LocalDate.ofEpochDay(it).formatada()}" } ?: "")
                        Text(vigencia, style = MaterialTheme.typography.bodySmall)
                        e.diaInversao?.let {
                            Text("Inverte trabalho/folga todo dia $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    IconButton(onClick = { dialogo = Dialogo.Escala(e) }) { Icon(Icons.Default.Edit, "Editar escala") }
                    IconButton(onClick = { dialogo = Dialogo.ExcluirEscala(e) }) { Icon(Icons.Default.Delete, "Excluir escala") }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Secao("Férias, folgas e afastamentos")
                Spacer(Modifier.weight(1f))
                AssistChip(onClick = { dialogo = Dialogo.Ausencia(null) }, label = { Text("Adicionar") },
                    leadingIcon = { Icon(Icons.Default.Add, null) })
            }
        }
        if (ausencias.isEmpty()) item { Vazio("Nenhum registro.") }
        items(ausencias, key = { "a-${it.id}" }) { a ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        val d = a.paraDominio()
                        Text(d.motivo.rotulo, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (d.inicio == d.fim) d.inicio.formatada() else "${d.inicio.formatada()} a ${d.fim.formatada()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (a.observacao.isNotBlank()) Text(a.observacao, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { dialogo = Dialogo.Ausencia(a) }) { Icon(Icons.Default.Edit, "Editar") }
                    IconButton(onClick = { dialogo = Dialogo.ExcluirAusencia(a) }) { Icon(Icons.Default.Delete, "Excluir") }
                }
            }
        }

        item { Secao("Próximos turnos") }
        if (proximos.isEmpty()) item { Vazio("Nenhum turno previsto.") }
        items(proximos, key = { "p-${it.escalaId}-${it.inicio}" }) { t ->
            Row {
                Text(t.data.curta(), modifier = Modifier.weight(1f))
                Text("${t.horario()} · ${t.posto}")
            }
        }
    }

    when (val d = dialogo) {
        null -> {}
        Dialogo.EditarFuncionario -> DialogoFuncionario(f, 0, { vm.salvarFuncionario(it) }, { dialogo = null })
        Dialogo.ExcluirFuncionario -> ConfirmarExclusao(
            "Excluir ${f.nome} e todas as suas escalas?",
            aoConfirmar = { vm.excluirFuncionario(f); aoExcluir() },
            aoCancelar = { dialogo = null },
        )
        is Dialogo.Escala -> DialogoEscala(f, d.escala, { vm.salvarEscala(it) }, { dialogo = null })
        is Dialogo.ExcluirEscala -> ConfirmarExclusao(
            "Excluir esta escala?", { vm.excluirEscala(d.escala) }, { dialogo = null },
        )
        is Dialogo.Ausencia -> DialogoAusencia(f, d.ausencia, { vm.salvarAusencia(it) }, { dialogo = null })
        is Dialogo.ExcluirAusencia -> ConfirmarExclusao(
            "Excluir este registro?", { vm.excluirAusencia(d.ausencia) }, { dialogo = null },
        )
    }
}

/** Horários padrão sugeridos para cada tipo de escala. */
private fun horarioPadrao(tipo: TipoEscala): Pair<LocalTime, LocalTime> = when (tipo) {
    TipoEscala.VINTE_QUATRO_X_QUARENTA_E_OITO -> LocalTime.of(7, 0) to LocalTime.of(7, 0)
    TipoEscala.DOZE_X_TRINTA_E_SEIS -> LocalTime.of(7, 0) to LocalTime.of(19, 0)
    else -> LocalTime.of(8, 0) to LocalTime.of(17, 0)
}

@Composable
private fun DialogoEscala(
    funcionario: FuncionarioEntity,
    inicial: EscalaEntity?,
    aoSalvar: (EscalaEntity) -> Unit,
    aoFechar: () -> Unit,
) {
    var tipo by remember { mutableStateOf(inicial?.let { TipoEscala.porNome(it.tipo) } ?: TipoEscala.DOZE_X_TRINTA_E_SEIS) }
    var trabalho by remember { mutableStateOf((inicial?.diasTrabalho ?: tipo.diasTrabalho).toString()) }
    var folga by remember { mutableStateOf((inicial?.diasFolga ?: tipo.diasFolga).toString()) }
    var inicio by remember { mutableStateOf(inicial?.let { LocalDate.ofEpochDay(it.inicioEpochDay) } ?: LocalDate.now()) }
    var fim by remember { mutableStateOf(inicial?.fimEpochDay?.let(LocalDate::ofEpochDay)) }
    var horaInicio by remember { mutableStateOf(inicial?.horaInicioMin?.paraHora() ?: horarioPadrao(tipo).first) }
    var horaFim by remember { mutableStateOf(inicial?.horaFimMin?.paraHora() ?: horarioPadrao(tipo).second) }
    var posto by remember { mutableStateOf(inicial?.posto ?: "Portaria") }
    var inverter by remember { mutableStateOf(inicial?.diaInversao != null) }
    var diaInversao by remember { mutableStateOf((inicial?.diaInversao ?: 10).toString()) }

    val diasTrab = trabalho.toIntOrNull()
    val diasFolga = folga.toIntOrNull()
    val fimVigencia = fim
    // A inversão só mantém o formato da escala quando trabalho e folga têm a mesma duração (12x36, 2x2...).
    val permiteInversao = diasTrab != null && diasTrab == diasFolga
    val diaInv = diaInversao.toIntOrNull()?.takeIf { it in 1..28 }
    val valido = diasTrab != null && diasTrab >= 1 && diasFolga != null && diasFolga >= 0 &&
        posto.isNotBlank() && (fimVigencia == null || !fimVigencia.isBefore(inicio)) &&
        (!inverter || !permiteInversao || diaInv != null)

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (inicial == null) "Nova escala — ${funcionario.nome}" else "Editar escala") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Seletor("Tipo de escala", TipoEscala.entries, tipo, { it.rotulo }) { novo ->
                    tipo = novo
                    if (novo != TipoEscala.PERSONALIZADA) {
                        trabalho = novo.diasTrabalho.toString()
                        folga = novo.diasFolga.toString()
                    }
                    horarioPadrao(novo).let { (a, b) -> horaInicio = a; horaFim = b }
                }
                if (tipo == TipoEscala.PERSONALIZADA) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            trabalho, { trabalho = it.filter(Char::isDigit) }, label = { Text("Dias trabalho") },
                            singleLine = true, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        OutlinedTextField(
                            folga, { folga = it.filter(Char::isDigit) }, label = { Text("Dias folga") },
                            singleLine = true, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoHora("Entrada", horaInicio, { horaInicio = it }, Modifier.weight(1f))
                    CampoHora("Saída", horaFim, { horaFim = it }, Modifier.weight(1f))
                }
                if (!horaFim.isAfter(horaInicio)) {
                    Text("Turno termina no dia seguinte.", style = MaterialTheme.typography.bodySmall)
                }
                CampoData("Primeiro dia de trabalho", inicio, { if (it != null) inicio = it })
                CampoData("Fim da vigência", fim, { fim = it }, opcional = true)
                OutlinedTextField(posto, { posto = it }, label = { Text("Posto / local") }, singleLine = true)
                if (permiteInversao) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Inverter trabalho/folga todo mês", modifier = Modifier.weight(1f))
                        Switch(checked = inverter, onCheckedChange = { inverter = it })
                    }
                    if (inverter) {
                        OutlinedTextField(
                            diaInversao, { diaInversao = it.filter(Char::isDigit).take(2) },
                            label = { Text("Dia do mês (1 a 28)") }, singleLine = true,
                            isError = diaInv == null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Text(
                            "A partir desse dia, quem folgaria passa a trabalhar e vice-versa.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valido, onClick = {
                aoSalvar(
                    EscalaEntity(
                        id = inicial?.id ?: 0,
                        funcionarioId = funcionario.id,
                        tipo = tipo.name,
                        diasTrabalho = diasTrab!!,
                        diasFolga = diasFolga!!,
                        inicioEpochDay = inicio.toEpochDay(),
                        fimEpochDay = fim?.toEpochDay(),
                        horaInicioMin = horaInicio.emMinutos(),
                        horaFimMin = horaFim.emMinutos(),
                        posto = posto.trim(),
                        diaInversao = if (inverter && permiteInversao) diaInv else null,
                    ),
                )
                aoFechar()
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

@Composable
private fun DialogoAusencia(
    funcionario: FuncionarioEntity,
    inicial: AusenciaEntity?,
    aoSalvar: (AusenciaEntity) -> Unit,
    aoFechar: () -> Unit,
) {
    var motivo by remember { mutableStateOf(inicial?.let { MotivoAusencia.porNome(it.motivo) } ?: MotivoAusencia.FERIAS) }
    var inicio by remember { mutableStateOf(inicial?.let { LocalDate.ofEpochDay(it.inicioEpochDay) } ?: LocalDate.now()) }
    var fim by remember { mutableStateOf(inicial?.let { LocalDate.ofEpochDay(it.fimEpochDay) } ?: LocalDate.now()) }
    var obs by remember { mutableStateOf(inicial?.observacao ?: "") }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (inicial == null) "Ausência — ${funcionario.nome}" else "Editar ausência") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Seletor("Motivo", MotivoAusencia.entries, motivo, { it.rotulo }) { motivo = it }
                CampoData("De", inicio, { if (it != null) { inicio = it; if (fim.isBefore(it)) fim = it } })
                CampoData("Até", fim, { if (it != null) fim = it })
                OutlinedTextField(obs, { obs = it }, label = { Text("Observação (opcional)") })
                if (fim.isBefore(inicio)) {
                    Text("A data final deve ser igual ou posterior à inicial.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !fim.isBefore(inicio), onClick = {
                aoSalvar(
                    AusenciaEntity(
                        id = inicial?.id ?: 0,
                        funcionarioId = funcionario.id,
                        inicioEpochDay = inicio.toEpochDay(),
                        fimEpochDay = fim.toEpochDay(),
                        motivo = motivo.name,
                        observacao = obs.trim(),
                    ),
                )
                aoFechar()
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}
