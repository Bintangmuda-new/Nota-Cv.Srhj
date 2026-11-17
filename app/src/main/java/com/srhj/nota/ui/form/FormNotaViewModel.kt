package com.srhj.nota.ui.form

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.srhj.nota.NotaApp
import com.srhj.nota.data.ItemNota
import com.srhj.nota.data.Nota
import com.srhj.nota.util.Goresan
import com.srhj.nota.util.NomorNota
import com.srhj.nota.util.TandaTangan
import com.srhj.nota.util.keLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ItemForm(val kunci: Long, val nama: String, val jumlah: Int, val harga: Long) {
    val subtotal: Long get() = jumlah.toLong() * harga
}

data class StateForm(
    val id: Long = 0,
    val nomor: String = "",
    val tanggal: Long = System.currentTimeMillis(),
    val pelanggan: String = "",
    val telepon: String = "",
    val penerima: String = "",
    val catatan: String = "",
    val ttdPath: String? = null,
    val items: List<ItemForm> = emptyList(),
    val dimuat: Boolean = false,
) {
    val total: Long get() = items.sumOf { it.subtotal }
    val baru: Boolean get() = id == 0L
}

class FormNotaViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NotaApp
    var state by mutableStateOf(StateForm())
        private set
    var galat by mutableStateOf<String?>(null)
        private set
    var menyimpan by mutableStateOf(false)
        private set
    private var kunci = 1L
    private var dimulai = false

    fun mulai(id: Long) {
        if (dimulai) return
        dimulai = true
        viewModelScope.launch {
            if (id == 0L) {
                val now = System.currentTimeMillis()
                state = StateForm(tanggal = now, nomor = nomorBaru(now), dimuat = true)
            } else {
                val n = app.dao.ambil(id)
                if (n == null) { galat = "Nota tidak ditemukan"; return@launch }
                state = StateForm(
                    id = n.nota.id, nomor = n.nota.nomor, tanggal = n.nota.tanggal,
                    pelanggan = n.nota.pelanggan, telepon = n.nota.teleponPelanggan,
                    penerima = n.nota.namaPenerima, catatan = n.nota.catatan, ttdPath = n.nota.ttdPath,
                    items = n.items.map { ItemForm(kunci++, it.nama, it.jumlah, it.harga) }, dimuat = true,
                )
            }
        }
    }

    private suspend fun nomorBaru(tanggal: Long): String {
        val awalan = app.pengaturan.info.value.awalanNomor
        val tgl = keLocalDate(tanggal)
        val ada = app.dao.nomorDenganAwalan(NomorNota.awalanHari(awalan, tgl))
        return NomorNota.berikutnya(awalan, tgl, ada)
    }

    fun ubah(f: (StateForm) -> StateForm) { state = f(state); galat = null }

    fun ubahTanggal(millis: Long) {
        val lama = state
        state = lama.copy(tanggal = millis)
        if (lama.baru && keLocalDate(lama.tanggal) != keLocalDate(millis)) {
            viewModelScope.launch { state = state.copy(nomor = nomorBaru(millis)) }
        }
    }

    fun simpanItem(kunciItem: Long?, nama: String, jumlah: Int, harga: Long) {
        val items = state.items.toMutableList()
        val idx = items.indexOfFirst { it.kunci == kunciItem }
        if (idx >= 0) items[idx] = items[idx].copy(nama = nama, jumlah = jumlah, harga = harga)
        else items += ItemForm(kunci++, nama, jumlah, harga)
        state = state.copy(items = items)
        galat = null
    }

    fun hapusItem(kunciItem: Long) { state = state.copy(items = state.items.filterNot { it.kunci == kunciItem }) }

    fun simpanTtd(goresan: List<Goresan>) {
        viewModelScope.launch {
            val f = withContext(Dispatchers.IO) { TandaTangan.simpan(app, goresan) }
            if (f != null) state = state.copy(ttdPath = f.absolutePath)
        }
    }

    fun hapusTtd() { state = state.copy(ttdPath = null) }

    fun simpan(selesai: (Long) -> Unit) {
        val s = state
        galat = when {
            s.pelanggan.isBlank() -> "Nama pelanggan wajib diisi"
            s.items.isEmpty() -> "Tambahkan minimal 1 barang"
            else -> null
        }
        if (galat != null || menyimpan) return
        menyimpan = true
        viewModelScope.launch {
            try {
                val lama = if (s.baru) null else app.dao.ambil(s.id)?.nota
                val nomor = if (s.baru) nomorBaru(s.tanggal) else s.nomor
                val id = app.dao.simpan(
                    Nota(
                        id = s.id, nomor = nomor, tanggal = s.tanggal,
                        pelanggan = s.pelanggan.trim(), teleponPelanggan = s.telepon.trim(),
                        namaPenerima = s.penerima.trim(), ttdPath = s.ttdPath, catatan = s.catatan.trim(),
                    ),
                    s.items.map { ItemNota(nama = it.nama.trim(), jumlah = it.jumlah, harga = it.harga) },
                )
                // Hapus file tanda tangan lama yang sudah diganti.
                lama?.ttdPath?.takeIf { it != s.ttdPath }?.let { withContext(Dispatchers.IO) { File(it).delete() } }
                selesai(id)
            } catch (e: Exception) {
                galat = "Gagal menyimpan: ${e.message}"
            } finally {
                menyimpan = false
            }
        }
    }
}
