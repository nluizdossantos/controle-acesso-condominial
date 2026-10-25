package br.com.condominio.escala.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condominio.escala.domain.CalculadoraEscala
import br.com.condominio.escala.ui.EscalaViewModel
import br.com.condominio.escala.ui.longa

@Composable
fun HojeScreen(vm: EscalaViewModel, abrirFuncionario: (Long) -> Unit, modifier: Modifier = Modifier) {
    val equipe by vm.equipe.collectAsStateWithLifecycle()
    val agora by vm.agora.collectAsStateWithLifecycle()
    val hoje = agora.toLocalDate()
    val escalas = equipe.escalasAtivas

    val emServico = CalculadoraEscala.emServico(agora, escalas, equipe.ausencias)
    val emServicoIds = emServico.map { it.escalaId to it.inicio }.toSet()
    val restoHoje = CalculadoraEscala.turnosDoDia(hoje, escalas, equipe.ausencias)
        .filter { (it.escalaId to it.inicio) !in emServicoIds }
    val amanha = CalculadoraEscala.turnosDoDia(hoje.plusDays(1), escalas, equipe.ausencias)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text(hoje.longa(), style = MaterialTheme.typography.headlineSmall) }

        if (equipe.funcionarios.isEmpty()) {
            item { Secao("Bem-vindo!") }
            item { Vazio("Cadastre a equipe na aba \"Equipe\" e defina a escala de cada funcionário.") }
            return@LazyColumn
        }

        item { Secao("Em serviço agora") }
        if (emServico.isEmpty()) item { Vazio("Ninguém escalado neste momento.") }
        items(emServico, key = { "agora-${it.escalaId}-${it.inicio}" }) {
            CartaoTurno(it, equipe, destaque = true, aoClicar = abrirFuncionario)
        }

        item { Secao("Demais turnos de hoje") }
        if (restoHoje.isEmpty()) item { Vazio("Nenhum outro turno hoje.") }
        items(restoHoje, key = { "hoje-${it.escalaId}-${it.inicio}" }) { CartaoTurno(it, equipe, aoClicar = abrirFuncionario) }

        item { Secao("Amanhã") }
        if (amanha.isEmpty()) item { Vazio("Nenhum turno amanhã.") }
        items(amanha, key = { "amanha-${it.escalaId}-${it.inicio}" }) { CartaoTurno(it, equipe, aoClicar = abrirFuncionario) }
    }
}
