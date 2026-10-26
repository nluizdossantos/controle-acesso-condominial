package br.com.condominio.escala.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FuncionarioEntity::class, EscalaEntity::class, AusenciaEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun funcionarioDao(): FuncionarioDao
    abstract fun escalaDao(): EscalaDao
    abstract fun ausenciaDao(): AusenciaDao

    companion object {
        /** v2: inversão mensal dos dias de trabalho. */
        private val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE escalas ADD COLUMN diaInversao INTEGER")
            }
        }

        /** v3: quantos dias as folgas andam a cada inversão (ex.: 5x2). */
        private val MIGRACAO_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE escalas ADD COLUMN deslocamentoInversao INTEGER")
            }
        }

        /** v4: escala semanal com dias de folga fixos que alternam na virada. */
        private val MIGRACAO_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE escalas ADD COLUMN folgasSemana INTEGER")
                db.execSQL("ALTER TABLE escalas ADD COLUMN folgasVirada INTEGER")
            }
        }

        @Volatile
        private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "escala.db")
                .addMigrations(MIGRACAO_1_2, MIGRACAO_2_3, MIGRACAO_3_4)
                .build()
                .also { instancia = it }
        }
    }
}
