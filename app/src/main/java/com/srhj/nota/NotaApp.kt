package com.srhj.nota

import android.app.Application
import com.srhj.nota.data.NotaDatabase
import com.srhj.nota.data.Pengaturan
import com.srhj.nota.print.PrinterBluetooth

class NotaApp : Application() {
    val database by lazy { NotaDatabase.buat(this) }
    val dao get() = database.notaDao()
    val pengaturan by lazy { Pengaturan(this) }
    val printer by lazy { PrinterBluetooth(this) }
}
