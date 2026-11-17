package com.srhj.nota.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InfoToko(
    val namaToko: String = "CV. SRI REJEKI HORN",
    val alamat: String = "Jember",
    val telepon: String = "",
    val catatanKaki: String = "Terima kasih atas kepercayaan Anda",
    val awalanNomor: String = "SRHJ",
    val printerAlamat: String = "",
    val printerNama: String = "",
    /** 58 = kertas 58mm (32 karakter), 80 = kertas 80mm (48 karakter). */
    val lebarKertasMm: Int = 58,
) {
    val karakterPerBaris: Int get() = if (lebarKertasMm == 80) 48 else 32
    val lebarPixelPrinter: Int get() = if (lebarKertasMm == 80) 576 else 384
}

/** Pengaturan toko & printer, disimpan di SharedPreferences. */
class Pengaturan(context: Context) {
    private val prefs = context.getSharedPreferences("pengaturan", Context.MODE_PRIVATE)
    private val _info = MutableStateFlow(baca())
    val info: StateFlow<InfoToko> = _info.asStateFlow()

    private fun baca(): InfoToko {
        val d = InfoToko()
        return InfoToko(
            namaToko = prefs.getString("namaToko", d.namaToko)!!,
            alamat = prefs.getString("alamat", d.alamat)!!,
            telepon = prefs.getString("telepon", d.telepon)!!,
            catatanKaki = prefs.getString("catatanKaki", d.catatanKaki)!!,
            awalanNomor = prefs.getString("awalanNomor", d.awalanNomor)!!,
            printerAlamat = prefs.getString("printerAlamat", d.printerAlamat)!!,
            printerNama = prefs.getString("printerNama", d.printerNama)!!,
            lebarKertasMm = prefs.getInt("lebarKertasMm", d.lebarKertasMm),
        )
    }

    fun simpan(info: InfoToko) {
        prefs.edit()
            .putString("namaToko", info.namaToko)
            .putString("alamat", info.alamat)
            .putString("telepon", info.telepon)
            .putString("catatanKaki", info.catatanKaki)
            .putString("awalanNomor", info.awalanNomor)
            .putString("printerAlamat", info.printerAlamat)
            .putString("printerNama", info.printerNama)
            .putInt("lebarKertasMm", info.lebarKertasMm)
            .apply()
        _info.value = info
    }
}
