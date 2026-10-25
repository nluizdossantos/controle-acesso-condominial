package br.com.condominio.escala.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condominio.escala.data.FuncionarioEntity
import br.com.condominio.escala.domain.CalculadoraEscala
import br.com.condominio.escala.ui.Bolinha
import br.com.condominio.escala.ui.EscalaViewModel
import br.com.condominio.escala.ui.curta
import br.com.condominio.escala.ui.theme.CoresFuncionario
import java.time.LocalDate

@Composable
fun EquipeScreen(vm: EscalaViewModel, abrirFuncionario: (Long) -> Unit, modifier: Modifier = Modifier) {
    val equipe by vm.equipe.collectAsStateWithLifecycle()
    val hoje = LocalDate.now()

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (equipe.funcionarios.isEmpty()) {
            item { Vazio("Nenhum funcionário cadastrado. Toque em + para adicionar.") }
        }
        items(equipe.funcionarios, key = { it.id }) { f ->
            val escalas = equipe.escalas.filter { it.funcionarioId == f.id }
            val proximo = escalas
                .flatMap { CalculadoraEscala.proximosTurnos(it, equipe.ausencias[f.id].orEmpty(), hoje, 1) }
                .minByOrNull { it.inicio }
            Card(Modifier.fillMaxWidth().alpha(if (f.ativo) 1f else 0.5f).clickable { abrirFuncionario(f.id) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Bolinha(f.cor, 20.dp)
                    Column(Modifier.weight(1f)) {
                        Text(f.nome, fontWeight = FontWeight.SemiBold)
                        Text(f.cargo, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        when {
                            !f.ativo -> "Inativo"
                            escalas.isEmpty() -> "Sem escala"
                            proximo == null -> "—"
                            proximo.data == hoje -> "Hoje"
                            else -> proximo.data.curta()
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun DialogoFuncionario(
    inicial: FuncionarioEntity?,
    quantidadeExistente: Int,
    aoSalvar: (FuncionarioEntity) -> Unit,
    aoFechar: () -> Unit,
) {
    var nome by remember { mutableStateOf(inicial?.nome ?: "") }
    var cargo by remember { mutableStateOf(inicial?.cargo ?: "Porteiro") }
    var telefone by remember { mutableStateOf(inicial?.telefone ?: "") }
    var ativo by remember { mutableStateOf(inicial?.ativo ?: true) }
    var cor by remember { mutableLongStateOf(inicial?.cor ?: CoresFuncionario[quantidadeExistente % CoresFuncionario.size]) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (inicial == null) "Novo funcionário" else "Editar funcionário") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(cargo, { cargo = it }, label = { Text("Cargo / função") }, singleLine = true)
                OutlinedTextField(
                    telefone, { telefone = it }, label = { Text("Telefone (opcional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                Text("Cor no calendário", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CoresFuncionario.take(5).forEach { c -> SeletorCor(c, c == cor) { cor = c } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CoresFuncionario.drop(5).forEach { c -> SeletorCor(c, c == cor) { cor = c } }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ativo", modifier = Modifier.weight(1f))
                    Switch(checked = ativo, onCheckedChange = { ativo = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = nome.isNotBlank(),
                onClick = {
                    aoSalvar(
                        FuncionarioEntity(
                            id = inicial?.id ?: 0,
                            nome = nome.trim(),
                            cargo = cargo.trim(),
                            telefone = telefone.trim(),
                            cor = cor,
                            ativo = ativo,
                        ),
                    )
                    aoFechar()
                },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

@Composable
private fun SeletorCor(cor: Long, selecionada: Boolean, aoClicar: () -> Unit) {
    Box(Modifier.clickable(onClick = aoClicar).padding(2.dp)) { Bolinha(cor, if (selecionada) 32.dp else 24.dp) }
}
