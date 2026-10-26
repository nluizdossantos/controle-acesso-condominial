package br.com.condominio.escala.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.condominio.escala.domain.Ausencia
import br.com.condominio.escala.domain.EscalaFuncionario
import br.com.condominio.escala.domain.MotivoAusencia
import br.com.condominio.escala.domain.RegraEscala
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "funcionarios")
data class FuncionarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val cargo: String,
    val telefone: String = "",
    /** Cor ARGB usada para identificar o funcionário no calendário. */
    val cor: Long,
    val ativo: Boolean = true,
)

/** Datas são gravadas como epochDay e horários como minutos desde a meia-noite. */
@Entity(
    tableName = "escalas",
    foreignKeys = [ForeignKey(
        entity = FuncionarioEntity::class,
        parentColumns = ["id"],
        childColumns = ["funcionarioId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("funcionarioId")],
)
data class EscalaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val funcionarioId: Long,
    val tipo: String,
    val diasTrabalho: Int,
    val diasFolga: Int,
    val inicioEpochDay: Long,
    val fimEpochDay: Long? = null,
    val horaInicioMin: Int,
    val horaFimMin: Int,
    val posto: String,
    /** Dia do mês (1–28) em que trabalho e folga se invertem; null desativa. */
    val diaInversao: Int? = null,
    /** Quantos dias o ciclo anda a cada inversão; null = diasTrabalho (inversão simples). */
    val deslocamentoInversao: Int? = null,
) {
    fun paraDominio() = EscalaFuncionario(
        escalaId = id,
        funcionarioId = funcionarioId,
        regra = RegraEscala(
            diasTrabalho = diasTrabalho,
            diasFolga = diasFolga,
            inicio = LocalDate.ofEpochDay(inicioEpochDay),
            horaInicio = LocalTime.of(horaInicioMin / 60, horaInicioMin % 60),
            horaFim = LocalTime.of(horaFimMin / 60, horaFimMin % 60),
            diaInversao = diaInversao,
            deslocamentoInversao = deslocamentoInversao,
        ),
        posto = posto,
        fimVigencia = fimEpochDay?.let(LocalDate::ofEpochDay),
    )
}

@Entity(
    tableName = "ausencias",
    foreignKeys = [ForeignKey(
        entity = FuncionarioEntity::class,
        parentColumns = ["id"],
        childColumns = ["funcionarioId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("funcionarioId")],
)
data class AusenciaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val funcionarioId: Long,
    val inicioEpochDay: Long,
    val fimEpochDay: Long,
    val motivo: String,
    val observacao: String = "",
) {
    fun paraDominio() = Ausencia(
        inicio = LocalDate.ofEpochDay(inicioEpochDay),
        fim = LocalDate.ofEpochDay(fimEpochDay),
        motivo = MotivoAusencia.porNome(motivo),
    )
}
