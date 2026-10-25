package br.com.condominio.escala.data

import br.com.condominio.escala.domain.Ausencia
import br.com.condominio.escala.domain.EscalaFuncionario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Retrato completo da equipe, já convertido para o domínio para os cálculos de escala. */
data class Equipe(
    val funcionarios: List<FuncionarioEntity> = emptyList(),
    val escalas: List<EscalaFuncionario> = emptyList(),
    val ausencias: Map<Long, List<Ausencia>> = emptyMap(),
) {
    private val porId = funcionarios.associateBy { it.id }
    fun funcionario(id: Long): FuncionarioEntity? = porId[id]

    /** Apenas escalas de funcionários ativos entram no calendário. */
    val escalasAtivas: List<EscalaFuncionario>
        get() = escalas.filter { porId[it.funcionarioId]?.ativo == true }
}

class EscalaRepository(private val db: AppDatabase) {
    private val funcionarios = db.funcionarioDao()
    private val escalas = db.escalaDao()
    private val ausencias = db.ausenciaDao()

    val equipe: Flow<Equipe> = combine(funcionarios.todos(), escalas.todas(), ausencias.todas()) { f, e, a ->
        Equipe(
            funcionarios = f,
            escalas = e.map { it.paraDominio() },
            ausencias = a.groupBy({ it.funcionarioId }, { it.paraDominio() }),
        )
    }

    val listaFuncionarios: Flow<List<FuncionarioEntity>> = funcionarios.todos()

    fun funcionario(id: Long): Flow<FuncionarioEntity?> = funcionarios.porId(id)
    fun escalasDe(id: Long): Flow<List<EscalaEntity>> = escalas.doFuncionario(id)
    fun ausenciasDe(id: Long): Flow<List<AusenciaEntity>> = ausencias.doFuncionario(id)
    fun ausenciasDominioDe(id: Long): Flow<List<Ausencia>> = ausenciasDe(id).map { l -> l.map { it.paraDominio() } }

    suspend fun salvar(f: FuncionarioEntity): Long = funcionarios.salvar(f)
    suspend fun excluir(f: FuncionarioEntity) = funcionarios.excluir(f)
    suspend fun salvar(e: EscalaEntity): Long = escalas.salvar(e)
    suspend fun excluir(e: EscalaEntity) = escalas.excluir(e)
    suspend fun salvar(a: AusenciaEntity): Long = ausencias.salvar(a)
    suspend fun excluir(a: AusenciaEntity) = ausencias.excluir(a)
}
