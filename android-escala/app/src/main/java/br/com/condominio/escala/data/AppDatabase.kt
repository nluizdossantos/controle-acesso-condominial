package br.com.condominio.escala.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FuncionarioEntity::class, EscalaEntity::class, AusenciaEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun funcionarioDao(): FuncionarioDao
    abstract fun escalaDao(): EscalaDao
    abstract fun ausenciaDao(): AusenciaDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "escala.db")
                .build()
                .also { instancia = it }
        }
    }
}
