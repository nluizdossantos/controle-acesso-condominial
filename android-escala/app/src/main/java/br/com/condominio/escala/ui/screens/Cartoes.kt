package br.com.condominio.escala.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import br.com.condominio.escala.data.Equipe
import br.com.condominio.escala.domain.Turno
import br.com.condominio.escala.ui.Bolinha
import br.com.condominio.escala.ui.horario

@Composable
fun CartaoTurno(turno: Turno, equipe: Equipe, destaque: Boolean = false, aoClicar: (Long) -> Unit = {}) {
    val f = equipe.funcionario(turno.funcionarioId) ?: return
    val cores = if (destaque) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    else CardDefaults.cardColors()
    Card(colors = cores, modifier = Modifier.fillMaxWidth().clickable { aoClicar(f.id) }) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Bolinha(f.cor, 16.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    f.nome,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (turno.confirmado) null else TextDecoration.LineThrough,
                )
                Text("${f.cargo} · ${turno.posto}", style = MaterialTheme.typography.bodySmall)
                turno.ausencia?.let {
                    Text(it.rotulo, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(turno.horario(), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun Secao(titulo: String) {
    Text(
        titulo,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
fun Vazio(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
