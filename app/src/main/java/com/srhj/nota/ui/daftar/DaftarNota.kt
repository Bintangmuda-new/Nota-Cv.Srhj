package com.srhj.nota.ui.daftar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srhj.nota.NotaApp
import com.srhj.nota.data.NotaLengkap
import com.srhj.nota.ui.komponen.KartuAngka
import com.srhj.nota.util.formatRupiah
import com.srhj.nota.util.formatTanggalJam
import com.srhj.nota.util.keLocalDate
import java.time.LocalDate

fun saringNota(daftar: List<NotaLengkap>, kata: String): List<NotaLengkap> {
    val q = kata.trim().lowercase()
    if (q.isEmpty()) return daftar
    return daftar.filter { n ->
        n.nota.nomor.lowercase().contains(q) ||
            n.nota.pelanggan.lowercase().contains(q) ||
            n.nota.namaPenerima.lowercase().contains(q) ||
            n.items.any { it.nama.lowercase().contains(q) }
    }
}

@Composable
fun LayarDaftarNota(onBuka: (Long) -> Unit, onBaru: () -> Unit, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as NotaApp
    val daftar by app.dao.semua().collectAsStateWithLifecycle(initialValue = null)
    val toko by app.pengaturan.info.collectAsStateWithLifecycle()
    var cari by rememberSaveable { mutableStateOf("") }
    IsiDaftarNota(daftar, toko.namaToko, cari, { cari = it }, onBuka, onBaru, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsiDaftarNota(
    daftar: List<NotaLengkap>?,
    namaToko: String,
    cari: String,
    onCari: (String) -> Unit,
    onBuka: (Long) -> Unit,
    onBaru: () -> Unit,
    modifier: Modifier = Modifier,
    hariIni: LocalDate = LocalDate.now(),
) {
    val tersaring = saringNota(daftar.orEmpty(), cari)
    val notaHariIni = daftar.orEmpty().filter { keLocalDate(it.nota.tanggal) == hariIni }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nota Penjualan", fontWeight = FontWeight.Bold)
                        Text(namaToko, style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onBaru,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nota Baru") },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KartuAngka("Nota hari ini", notaHariIni.size.toString(), Modifier.weight(1f))
                    KartuAngka("Omzet hari ini", formatRupiah(notaHariIni.sumOf { it.nota.total }), Modifier.weight(1.6f))
                }
            }
            item {
                OutlinedTextField(
                    value = cari,
                    onValueChange = onCari,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari no. nota, pelanggan, barang…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (cari.isNotEmpty()) IconButton(onClick = { onCari("") }) { Icon(Icons.Default.Clear, "Hapus pencarian") }
                    },
                    singleLine = true,
                )
            }
            if (daftar != null && tersaring.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (cari.isBlank()) "Belum ada nota.\nTekan \"Nota Baru\" untuk membuat nota pertama."
                            else "Tidak ada nota yang cocok dengan \"$cari\".",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(tersaring, key = { it.nota.id }) { n -> KartuNota(n) { onBuka(n.nota.id) } }
        }
    }
}

@Composable
private fun KartuNota(n: NotaLengkap, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(n.nota.nomor, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Text(formatTanggalJam(n.nota.tanggal), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(n.nota.pelanggan, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${n.items.size} jenis barang · ${n.items.sumOf { it.jumlah }} pcs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(formatRupiah(n.nota.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
