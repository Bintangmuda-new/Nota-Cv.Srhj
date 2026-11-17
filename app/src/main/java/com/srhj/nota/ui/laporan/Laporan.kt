package com.srhj.nota.ui.laporan

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srhj.nota.NotaApp
import com.srhj.nota.data.NotaLengkap
import com.srhj.nota.ui.DataLaporan
import com.srhj.nota.ui.komponen.KartuAngka
import com.srhj.nota.ui.komponen.TombolAksi
import com.srhj.nota.ui.rememberAksiDokumen
import com.srhj.nota.util.akhirHari
import com.srhj.nota.util.awalHari
import com.srhj.nota.util.formatAngka
import com.srhj.nota.util.formatRupiah
import com.srhj.nota.util.formatTanggal
import com.srhj.nota.util.formatTanggalPanjang
import com.srhj.nota.util.hitungRingkasan
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

enum class JenisPeriode(val label: String) { HARI_INI("Hari ini"), MINGGU_INI("Minggu ini"), BULAN_INI("Bulan ini"), BULAN_LALU("Bulan lalu"), PILIH("Pilih tanggal") }

/** Rentang tanggal (inklusif) untuk tiap jenis periode. */
fun rentangPeriode(jenis: JenisPeriode, hariIni: LocalDate, dari: LocalDate, sampai: LocalDate): Pair<LocalDate, LocalDate> = when (jenis) {
    JenisPeriode.HARI_INI -> hariIni to hariIni
    JenisPeriode.MINGGU_INI -> hariIni.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to hariIni
    JenisPeriode.BULAN_INI -> hariIni.withDayOfMonth(1) to hariIni
    JenisPeriode.BULAN_LALU -> hariIni.minusMonths(1).withDayOfMonth(1).let { it to it.with(TemporalAdjusters.lastDayOfMonth()) }
    JenisPeriode.PILIH -> minOf(dari, sampai) to maxOf(dari, sampai)
}

fun judulPeriode(dari: LocalDate, sampai: LocalDate): String =
    if (dari == sampai) formatTanggalPanjang(dari) else "${formatTanggalPanjang(dari)} - ${formatTanggalPanjang(sampai)}"

fun namaFileLaporan(dari: LocalDate, sampai: LocalDate): String {
    val f = DateTimeFormatter.BASIC_ISO_DATE
    return if (dari == sampai) "Laporan_${dari.format(f)}" else "Laporan_${dari.format(f)}_${sampai.format(f)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayarLaporan(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as NotaApp
    var jenis by rememberSaveable { mutableStateOf(JenisPeriode.HARI_INI) }
    var pilihDari by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var pilihSampai by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var dialog by remember { mutableStateOf(false) }
    val (dari, sampai) = rentangPeriode(jenis, LocalDate.now(), LocalDate.ofEpochDay(pilihDari), LocalDate.ofEpochDay(pilihSampai))
    val daftar by remember(dari, sampai) { app.dao.rentang(awalHari(dari), akhirHari(sampai)) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val aksi = rememberAksiDokumen { m -> scope.launch { snackbar.showSnackbar(m) } }
    val data = DataLaporan(judulPeriode(dari, sampai), namaFileLaporan(dari, sampai), daftar, hitungRingkasan(daftar))

    IsiLaporan(
        jenis = jenis, data = data, snackbar = snackbar, modifier = modifier,
        onJenis = { if (it == JenisPeriode.PILIH) dialog = true else jenis = it },
        onCetak = { aksi.cetakLaporan(data) },
        onPdf = { aksi.simpanLaporanPdf(data) },
        onExcel = { aksi.simpanLaporanExcel(data) },
        onKirim = { aksi.kirimLaporan(data) },
    )

    if (dialog) {
        fun keUtc(d: Long) = LocalDate.ofEpochDay(d).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        fun dariUtc(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
        val st = rememberDateRangePickerState(initialSelectedStartDateMillis = keUtc(pilihDari), initialSelectedEndDateMillis = keUtc(pilihSampai))
        DatePickerDialog(
            onDismissRequest = { dialog = false },
            confirmButton = {
                TextButton(
                    enabled = st.selectedStartDateMillis != null,
                    onClick = {
                        val a = dariUtc(st.selectedStartDateMillis!!)
                        pilihDari = a
                        pilihSampai = st.selectedEndDateMillis?.let(::dariUtc) ?: a
                        jenis = JenisPeriode.PILIH
                        dialog = false
                    },
                ) { Text("Terapkan") }
            },
            dismissButton = { TextButton(onClick = { dialog = false }) { Text("Batal") } },
        ) {
            DateRangePicker(st, Modifier.weight(1f), title = { Text("Pilih rentang tanggal", Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsiLaporan(
    jenis: JenisPeriode,
    data: DataLaporan,
    snackbar: SnackbarHostState,
    onJenis: (JenisPeriode) -> Unit,
    onCetak: () -> Unit,
    onPdf: () -> Unit,
    onExcel: () -> Unit,
    onKirim: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val r = data.ringkasan
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Laporan Penjualan", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JenisPeriode.entries.forEach { j -> FilterChip(selected = j == jenis, onClick = { onJenis(j) }, label = { Text(j.label) }) }
                }
            }
            item { Text(data.judulPeriode, style = MaterialTheme.typography.titleMedium) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KartuAngka("Nota", r.jumlahNota.toString(), Modifier.weight(0.8f))
                    KartuAngka("Barang", formatAngka(r.totalBarang.toLong()), Modifier.weight(0.9f))
                    KartuAngka("Omzet", formatRupiah(r.totalOmzet), Modifier.weight(1.6f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TombolAksi("Cetak", Icons.Default.Print, true, Modifier.weight(1f), onClick = onCetak)
                    TombolAksi("PDF", Icons.Default.PictureAsPdf, true, Modifier.weight(1f), onClick = onPdf)
                    TombolAksi("Excel", Icons.Default.TableChart, true, Modifier.weight(1f), onClick = onExcel)
                    TombolAksi("Kirim WA", Icons.AutoMirrored.Filled.Send, true, Modifier.weight(1.2f), utama = true, onClick = onKirim)
                }
            }
            if (r.barang.isNotEmpty()) {
                item { Text("Barang Terlaris", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            r.barang.take(10).forEachIndexed { i, b ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${i + 1}. ${b.nama}", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${b.jumlah} pcs", Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatAngka(b.omzet), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
            item { Text("Rincian Nota", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            if (data.daftar.isEmpty()) item { Text("Tidak ada nota pada periode ini.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(data.daftar, key = { it.nota.id }) { n -> BarisNota(n) }
        }
    }
}

@Composable
private fun BarisNota(n: NotaLengkap) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(n.nota.pelanggan, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${n.nota.nomor} · ${formatTanggal(n.nota.tanggal)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatRupiah(n.nota.total), fontWeight = FontWeight.Bold)
        }
    }
}
