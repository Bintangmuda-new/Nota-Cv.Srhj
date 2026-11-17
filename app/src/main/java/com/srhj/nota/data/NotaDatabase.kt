package com.srhj.nota.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Nota::class, ItemNota::class], version = 1, exportSchema = true)
abstract class NotaDatabase : RoomDatabase() {
    abstract fun notaDao(): NotaDao

    companion object {
        fun buat(context: Context): NotaDatabase =
            Room.databaseBuilder(context, NotaDatabase::class.java, "nota_srhj.db").build()
    }
}
