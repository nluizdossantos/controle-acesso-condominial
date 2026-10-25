package br.com.condominio.escala

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.condominio.escala.ui.EscalaViewModel
import br.com.condominio.escala.ui.screens.CalendarioScreen
import br.com.condominio.escala.ui.screens.DialogoFuncionario
import br.com.condominio.escala.ui.screens.EquipeScreen
import br.com.condominio.escala.ui.screens.FuncionarioScreen
import br.com.condominio.escala.ui.screens.HojeScreen
import br.com.condominio.escala.ui.theme.EscalaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EscalaTheme { App() } }
    }
}

private enum class Aba(val rota: String, val titulo: String, val icone: ImageVector) {
    HOJE("hoje", "Hoje", Icons.Default.Today),
    CALENDARIO("calendario", "Calendário", Icons.Default.CalendarMonth),
    EQUIPE("equipe", "Equipe", Icons.Default.Groups),
}

private const val ROTA_FUNCIONARIO = "funcionario/{id}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(vm: EscalaViewModel = viewModel()) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val rota = entrada?.destination?.route
    val abaAtual = Aba.entries.firstOrNull { it.rota == rota }
    val equipe by vm.equipe.collectAsStateWithLifecycle()
    var novoFuncionario by rememberSaveable { mutableStateOf(false) }
    val abrirFuncionario: (Long) -> Unit = { nav.navigate("funcionario/$it") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(abaAtual?.let { if (it == Aba.HOJE) "Escala de Trabalho" else it.titulo } ?: "Funcionário") },
                navigationIcon = {
                    if (abaAtual == null) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (abaAtual != null) {
                NavigationBar {
                    Aba.entries.forEach { aba ->
                        NavigationBarItem(
                            selected = aba == abaAtual,
                            onClick = {
                                nav.navigate(aba.rota) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(aba.icone, null) },
                            label = { Text(aba.titulo) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (abaAtual == Aba.EQUIPE) {
                FloatingActionButton(onClick = { novoFuncionario = true }) {
                    Icon(Icons.Default.PersonAdd, "Novo funcionário")
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Aba.HOJE.rota, modifier = Modifier.padding(padding)) {
            composable(Aba.HOJE.rota) { HojeScreen(vm, abrirFuncionario) }
            composable(Aba.CALENDARIO.rota) { CalendarioScreen(vm, abrirFuncionario) }
            composable(Aba.EQUIPE.rota) { EquipeScreen(vm, abrirFuncionario) }
            composable(ROTA_FUNCIONARIO, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                FuncionarioScreen(vm, it.arguments?.getLong("id") ?: 0L, aoExcluir = { nav.popBackStack() })
            }
        }
    }

    if (novoFuncionario) {
        DialogoFuncionario(
            inicial = null,
            quantidadeExistente = equipe.funcionarios.size,
            aoSalvar = { vm.salvarFuncionario(it) },
            aoFechar = { novoFuncionario = false },
        )
    }
}
