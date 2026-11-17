package com.srhj.nota.ui.detail

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srhj.nota.NotaApp
import com.srhj.nota.data.InfoToko
import com.srhj.nota.data.NotaLengkap
import com.srhj.nota.data.subtotal
import com.srhj.nota.ui.komponen.BarisInfo
import com.srhj.nota.ui.komponen.DialogKonfirmasi
import com.srhj.nota.ui.komponen.TombolAksi
import com.srhj.nota.ui.rememberAksiDokumen
import com.srhj.nota.util.formatAngka
import com.srhj.nota.util.formatRupiah
import com.srhj.nota.util.formatTanggalJam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LayarDetailNota(id: Long, onKembali: () -> Unit, onUbah: (Long) -> Unit) {
    val app = LocalContext.current.applicationContext as NotaApp
    val nota by remember(id) { app.dao.pantau(id) }.collectAsStateWithLifecycle(initialValue = null)
    val toko by app.pengaturan.info.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pesan: (String) -> Unit = { m -> scope.launch { snackbar.showSnackbar(m) } }
    val aksi = rememberAksiDokumen(pesan)
    var konfirmasiHapus by remember { mutableStateOf(false) }
    var sudahDimuat by remember { mutableStateOf(false) }
    if (nota != null) sudahDimuat = true

    IsiDetailNota(
        nota = nota, toko = toko, snackbar = snackbar, onKembali = onKembali,
        onUbah = { onUbah(id) },
        onHapus = { konfirmasiHapus = true },
        onCetak = { nota?.let(aksi::cetakNota) },
        onPdf = { nota?.let(aksi::simpanNota) },
        onKirim = { nota?.let(aksi::kirimNota) },
    )
    if (konfirmasiHapus) {
        DialogKonfirmasi(
            "Hapus Nota?", "Nota ${nota?.nota?.nomor} akan dihapus permanen.", "Hapus",
            onBatal = { konfirmasiHapus = false },
        ) {
            konfirmasiHapus = false
            val n = nota ?: return@DialogKonfirmasi
            scope.launch {
                withContext(Dispatchers.IO) {
                    app.dao.hapus(n.nota)
                    n.nota.ttdPath?.let { File(it).delete() }
                }
                onKembali()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsiDetailNota(
    nota: NotaLengkap?,
    toko: InfoToko,
    snackbar: SnackbarHostState,
    onKembali: () -> Unit,
    onUbah: () -> Unit,
    onHapus: () -> Unit,
    onCetak: () -> Unit,
    onPdf: () -> Unit,
    onKirim: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nota?.nota?.nomor ?: "Detail Nota", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onKembali) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } },
                actions = {
                    IconButton(onClick = onUbah, enabled = nota != null) { Icon(Icons.Default.Edit, "Ubah nota") }
                    IconButton(onClick = onHapus, enabled = nota != null) { Icon(Icons.Default.Delete, "Hapus nota") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TombolAksi("Cetak", Icons.Default.Print, nota != null, Modifier.weight(1f), onClick = onCetak)
                    TombolAksi("PDF", Icons.Default.PictureAsPdf, nota != null, Modifier.weight(1f), onClick = onPdf)
                    TombolAksi("Kirim WA", Icons.AutoMirrored.Filled.Send, nota != null, Modifier.weight(1f), utama = true, onClick = onKirim)
                }
            }
        },
    ) { pad ->
        if (nota == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val n = nota.nota
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(toko.namaToko.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    if (toko.alamat.isNotBlank()) Text(toko.alamat, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    BarisInfo("No. Nota", n.nomor)
                    BarisInfo("Tanggal", formatTanggalJam(n.tanggal))
                    BarisInfo("Pelanggan", n.pelanggan)
                    if (n.teleponPelanggan.isNotBlank()) BarisInfo("WhatsApp", n.teleponPelanggan)
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    nota.items.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(item.nama, style = MaterialTheme.typography.bodyLarge)
                                Text("${item.jumlah} x ${formatAngka(item.harga)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(formatAngka(item.subtotal()), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    BarisInfo("Jumlah barang", "${nota.items.sumOf { it.jumlah }} pcs")
                    BarisInfo("TOTAL", formatRupiah(n.total), tebal = true)
                    if (n.catatan.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Catatan: ${n.catatan}", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Penerima,", style = MaterialTheme.typography.bodyMedium)
                        val bmp = remember(n.ttdPath) {
                            n.ttdPath?.takeIf { File(it).exists() }?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                        }
                        if (bmp != null) Image(bmp, "Tanda tangan penerima", Modifier.height(80.dp).padding(4.dp))
                        else Spacer(Modifier.height(48.dp))
                        Text("( ${n.namaPenerima.ifBlank { "-" }} )", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
