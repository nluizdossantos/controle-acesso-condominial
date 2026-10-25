package br.com.condominio.escala.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FuncionarioDao {
    @Query("SELECT * FROM funcionarios ORDER BY ativo DESC, nome COLLATE NOCASE")
    fun todos(): Flow<List<FuncionarioEntity>>

    @Query("SELECT * FROM funcionarios WHERE id = :id")
    fun porId(id: Long): Flow<FuncionarioEntity?>

    @Upsert
    suspend fun salvar(funcionario: FuncionarioEntity): Long

    @Delete
    suspend fun excluir(funcionario: FuncionarioEntity)
}

@Dao
interface EscalaDao {
    @Query("SELECT * FROM escalas")
    fun todas(): Flow<List<EscalaEntity>>

    @Query("SELECT * FROM escalas WHERE funcionarioId = :funcionarioId ORDER BY inicioEpochDay DESC")
    fun doFuncionario(funcionarioId: Long): Flow<List<EscalaEntity>>

    @Upsert
    suspend fun salvar(escala: EscalaEntity): Long

    @Delete
    suspend fun excluir(escala: EscalaEntity)
}

@Dao
interface AusenciaDao {
    @Query("SELECT * FROM ausencias")
    fun todas(): Flow<List<AusenciaEntity>>

    @Query("SELECT * FROM ausencias WHERE funcionarioId = :funcionarioId ORDER BY inicioEpochDay DESC")
    fun doFuncionario(funcionarioId: Long): Flow<List<AusenciaEntity>>

    @Upsert
    suspend fun salvar(ausencia: AusenciaEntity): Long

    @Delete
    suspend fun excluir(ausencia: AusenciaEntity)
}
