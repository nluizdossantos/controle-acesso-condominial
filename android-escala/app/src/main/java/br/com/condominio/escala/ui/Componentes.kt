package br.com.condominio.escala.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun Bolinha(cor: Long, tamanho: Dp = 12.dp) {
    Box(Modifier.size(tamanho).background(Color(cor), CircleShape))
}

/** Campo somente leitura que abre um seletor de data. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoData(rotulo: String, valor: LocalDate?, aoMudar: (LocalDate?) -> Unit, opcional: Boolean = false) {
    var aberto by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = valor?.formatada() ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(rotulo) },
        placeholder = { if (opcional) Text("Sem data") },
        trailingIcon = {
            IconButton(onClick = { aberto = true }) { Icon(Icons.Default.CalendarMonth, "Escolher data") }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    if (aberto) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = (valor ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { aoMudar(LocalDate.ofEpochDay(it / 86_400_000L)) }
                    aberto = false
                }) { Text("OK") }
            },
            dismissButton = {
                if (opcional) TextButton(onClick = { aoMudar(null); aberto = false }) { Text("Limpar") }
                else TextButton(onClick = { aberto = false }) { Text("Cancelar") }
            },
        ) { DatePicker(estado) }
    }
}

/** Campo somente leitura que abre um seletor de horário (24h). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoHora(rotulo: String, valor: LocalTime, aoMudar: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    var aberto by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = valor.formatada(),
        onValueChange = {},
        readOnly = true,
        label = { Text(rotulo) },
        trailingIcon = { IconButton(onClick = { aberto = true }) { Icon(Icons.Default.Schedule, "Escolher horário") } },
        modifier = modifier,
    )
    if (aberto) {
        val estado = rememberTimePickerState(valor.hour, valor.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                TextButton(onClick = { aoMudar(LocalTime.of(estado.hour, estado.minute)); aberto = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { aberto = false }) { Text("Cancelar") } },
            text = { TimePicker(estado) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Seletor(rotulo: String, opcoes: List<T>, selecionado: T, texto: (T) -> String, aoMudar: (T) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = texto(selecionado),
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandido) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opcoes.forEach {
                DropdownMenuItem(text = { Text(texto(it)) }, onClick = { aoMudar(it); expandido = false })
            }
        }
    }
}

@Composable
fun ConfirmarExclusao(mensagem: String, aoConfirmar: () -> Unit, aoCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Excluir?") },
        text = { Text(mensagem) },
        confirmButton = { TextButton(onClick = { aoConfirmar(); aoCancelar() }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } },
    )
}

