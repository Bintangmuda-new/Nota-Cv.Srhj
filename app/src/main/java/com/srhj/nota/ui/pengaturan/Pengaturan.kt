package com.srhj.nota.ui.pengaturan

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srhj.nota.NotaApp
import com.srhj.nota.data.InfoToko
import com.srhj.nota.print.PerangkatBluetooth
import com.srhj.nota.ui.rememberAksiDokumen
import kotlinx.coroutines.launch

@Composable
fun LayarPengaturan(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as NotaApp
    val tersimpan by app.pengaturan.info.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pesan: (String) -> Unit = { m -> scope.launch { snackbar.showSnackbar(m) } }
    val aksi = rememberAksiDokumen(pesan)
    var perangkat by remember { mutableStateOf<List<PerangkatBluetooth>?>(null) }

    fun muatPerangkat() {
        when {
            !app.printer.didukung() -> pesan("HP ini tidak mendukung Bluetooth")
            !app.printer.aktif() -> pesan("Nyalakan Bluetooth terlebih dahulu")
            else -> perangkat = try { app.printer.perangkatTerpasang() } catch (e: SecurityException) { pesan("Izin Bluetooth ditolak"); null }
        }
    }
    val izin = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) muatPerangkat() else pesan("Izin Bluetooth ditolak")
    }

    IsiPengaturan(
        tersimpan = tersimpan, snackbar = snackbar, modifier = modifier,
        onSimpan = { app.pengaturan.simpan(it); pesan("Pengaturan disimpan") },
        onPilihPrinter = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !app.printer.punyaIzin()) izin.launch(Manifest.permission.BLUETOOTH_CONNECT)
            else muatPerangkat()
        },
        onTesCetak = { aksi.tesCetak() },
    )

    perangkat?.let { daftar ->
        AlertDialog(
            onDismissRequest = { perangkat = null },
            title = { Text("Pilih Printer") },
            text = {
                if (daftar.isEmpty()) {
                    Text("Belum ada perangkat yang di-pairing.\n\nBuka Pengaturan HP > Bluetooth, nyalakan printer, lalu pairing (PIN biasanya 0000 atau 1234).")
                } else {
                    LazyColumn {
                        items(daftar) { d ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    app.pengaturan.simpan(app.pengaturan.info.value.copy(printerAlamat = d.alamat, printerNama = d.nama))
                                    perangkat = null
                                    pesan("Printer dipilih: ${d.nama}")
                                }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Print, null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(d.nama, fontWeight = FontWeight.Medium)
                                    Text(d.alamat, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { perangkat = null }) { Text("Tutup") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsiPengaturan(
    tersimpan: InfoToko,
    snackbar: SnackbarHostState,
    onSimpan: (InfoToko) -> Unit,
    onPilihPrinter: () -> Unit,
    onTesCetak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var f by remember(tersimpan) { mutableStateOf(tersimpan) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Data Toko", style = MaterialTheme.typography.titleMedium)
            val kapital = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            OutlinedTextField(f.namaToko, { f = f.copy(namaToko = it) }, Modifier.fillMaxWidth(), label = { Text("Nama toko") }, singleLine = true, keyboardOptions = kapital)
            OutlinedTextField(f.alamat, { f = f.copy(alamat = it) }, Modifier.fillMaxWidth(), label = { Text("Alamat") }, keyboardOptions = kapital)
            OutlinedTextField(
                f.telepon, { f = f.copy(telepon = it) }, Modifier.fillMaxWidth(), label = { Text("Telepon / WA toko") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            OutlinedTextField(f.catatanKaki, { f = f.copy(catatanKaki = it) }, Modifier.fillMaxWidth(), label = { Text("Catatan kaki nota") })
            OutlinedTextField(
                f.awalanNomor, { v -> f = f.copy(awalanNomor = v.uppercase().filter { it.isLetterOrDigit() }.take(8)) },
                Modifier.fillMaxWidth(), label = { Text("Awalan nomor nota") }, singleLine = true,
                supportingText = { Text("Contoh nomor: ${f.awalanNomor.ifEmpty { "NOTA" }}-261117-001") },
            )
            Button(onClick = { onSimpan(f) }, enabled = f != tersimpan && f.namaToko.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Simpan Data Toko")
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Printer Thermal Bluetooth", style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bluetooth, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tersimpan.printerNama.ifBlank { "Belum dipilih" }, fontWeight = FontWeight.Medium)
                        if (tersimpan.printerAlamat.isNotBlank()) Text(tersimpan.printerAlamat, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onPilihPrinter) { Text("Pilih") }
                }
            }
            Text("Lebar kertas", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(58, 80).forEachIndexed { i, mm ->
                    SegmentedButton(
                        selected = tersimpan.lebarKertasMm == mm,
                        onClick = { onSimpan(tersimpan.copy(lebarKertasMm = mm)) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text("$mm mm") }
                }
            }
            OutlinedButton(onClick = onTesCetak, enabled = tersimpan.printerAlamat.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Print, null); Spacer(Modifier.width(6.dp)); Text("Tes Cetak")
            }
            Text(
                "Tips: pairing printer dulu di menu Bluetooth HP. File PDF & Excel yang disimpan ada di folder Download/NotaSRHJ.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
