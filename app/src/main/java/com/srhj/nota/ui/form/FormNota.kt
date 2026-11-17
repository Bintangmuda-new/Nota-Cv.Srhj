package com.srhj.nota.ui.form

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.srhj.nota.ui.komponen.DialogTandaTangan
import com.srhj.nota.util.Goresan
import com.srhj.nota.util.formatAngka
import com.srhj.nota.util.formatRupiah
import com.srhj.nota.util.formatTanggalJam
import com.srhj.nota.util.parseAngka
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun LayarFormNota(id: Long, onKembali: () -> Unit, onTersimpan: (Long) -> Unit) {
    val vm: FormNotaViewModel = viewModel()
    LaunchedEffect(id) { vm.mulai(id) }
    IsiFormNota(
        state = vm.state,
        galat = vm.galat,
        menyimpan = vm.menyimpan,
        onKembali = onKembali,
        onUbah = vm::ubah,
        onUbahTanggal = vm::ubahTanggal,
        onSimpanItem = vm::simpanItem,
        onHapusItem = vm::hapusItem,
        onSimpanTtd = vm::simpanTtd,
        onHapusTtd = vm::hapusTtd,
        onSimpan = { vm.simpan(onTersimpan) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsiFormNota(
    state: StateForm,
    galat: String?,
    menyimpan: Boolean,
    onKembali: () -> Unit,
    onUbah: ((StateForm) -> StateForm) -> Unit,
    onUbahTanggal: (Long) -> Unit,
    onSimpanItem: (Long?, String, Int, Long) -> Unit,
    onHapusItem: (Long) -> Unit,
    onSimpanTtd: (List<Goresan>) -> Unit,
    onHapusTtd: () -> Unit,
    onSimpan: () -> Unit,
) {
    var dialogItem by remember { mutableStateOf<ItemForm?>(null) }
    var tambahItem by remember { mutableStateOf(false) }
    var dialogTtd by remember { mutableStateOf(false) }
    var dialogTanggal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.baru) "Nota Baru" else "Ubah Nota") },
                navigationIcon = { IconButton(onClick = onKembali) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp).imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Total", style = MaterialTheme.typography.labelMedium)
                        Text(formatRupiah(state.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Button(onClick = onSimpan, enabled = state.dimuat && !menyimpan) {
                        if (menyimpan) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        else Text("Simpan Nota")
                    }
                }
            }
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (galat != null) item {
                Text(
                    galat,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp)).padding(12.dp),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.nomor, onValueChange = {}, readOnly = true,
                        label = { Text("No. Nota (otomatis)") }, modifier = Modifier.weight(1f), singleLine = true,
                    )
                    OutlinedTextField(
                        value = formatTanggalJam(state.tanggal), onValueChange = {}, readOnly = true,
                        label = { Text("Tanggal") }, modifier = Modifier.weight(1f), singleLine = true,
                        trailingIcon = { IconButton(onClick = { dialogTanggal = true }) { Icon(Icons.Default.CalendarMonth, "Pilih tanggal") } },
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = state.pelanggan, onValueChange = { v -> onUbah { it.copy(pelanggan = v) } },
                    label = { Text("Nama Pelanggan *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
            item {
                OutlinedTextField(
                    value = state.telepon, onValueChange = { v -> onUbah { it.copy(telepon = v) } },
                    label = { Text("No. WhatsApp Pelanggan (opsional)") }, placeholder = { Text("08xxxxxxxxxx") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daftar Barang", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { tambahItem = true }) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Tambah Barang")
                    }
                }
            }
            if (state.items.isEmpty()) item {
                Text("Belum ada barang.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            }
            items(state.items, key = { it.kunci }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.nama, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("${item.jumlah} x ${formatAngka(item.harga)}", style = MaterialTheme.typography.bodySmall)
                            Text(formatRupiah(item.subtotal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { dialogItem = item }) { Icon(Icons.Default.Edit, "Ubah ${item.nama}") }
                        IconButton(onClick = { onHapusItem(item.kunci) }) { Icon(Icons.Default.Delete, "Hapus ${item.nama}", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
            item {
                OutlinedTextField(
                    value = state.penerima, onValueChange = { v -> onUbah { it.copy(penerima = v) } },
                    label = { Text("Nama Penerima") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Tanda Tangan Penerima", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        val bmp = remember(state.ttdPath) {
                            state.ttdPath?.takeIf { File(it).exists() }?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                        }
                        if (bmp != null) {
                            Box(Modifier.fillMaxWidth().height(110.dp).background(Color.White, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Image(bmp, "Tanda tangan", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(6.dp))
                            }
                            Row {
                                TextButton(onClick = { dialogTtd = true }) { Text("Ulangi") }
                                TextButton(onClick = onHapusTtd) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
                            }
                        } else {
                            OutlinedButton(onClick = { dialogTtd = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Draw, null); Spacer(Modifier.width(6.dp)); Text("Buat Tanda Tangan")
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.catatan, onValueChange = { v -> onUbah { it.copy(catatan = v) } },
                    label = { Text("Catatan (opsional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2,
                )
            }
        }
    }

    if (tambahItem || dialogItem != null) {
        DialogItem(
            awal = dialogItem,
            onBatal = { tambahItem = false; dialogItem = null },
            onSimpan = { nama, jml, harga ->
                onSimpanItem(dialogItem?.kunci, nama, jml, harga)
                tambahItem = false; dialogItem = null
            },
        )
    }
    if (dialogTtd) {
        DialogTandaTangan(state.penerima, onBatal = { dialogTtd = false }) { g -> onSimpanTtd(g); dialogTtd = false }
    }
    if (dialogTanggal) {
        // DatePicker bekerja dalam UTC; jam tetap dipertahankan dari nota.
        val zona = ZoneId.systemDefault()
        val waktuLama = Instant.ofEpochMilli(state.tanggal).atZone(zona)
        val dp = rememberDatePickerState(initialSelectedDateMillis = waktuLama.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { dialogTanggal = false },
            confirmButton = {
                TextButton(onClick = {
                    dp.selectedDateMillis?.let { ms ->
                        val tgl = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        val jam: LocalTime = waktuLama.toLocalTime()
                        onUbahTanggal(tgl.atTime(jam).atZone(zona).toInstant().toEpochMilli())
                    }
                    dialogTanggal = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { dialogTanggal = false }) { Text("Batal") } },
        ) { DatePicker(dp) }
    }
}

@Composable
fun DialogItem(awal: ItemForm?, onBatal: () -> Unit, onSimpan: (String, Int, Long) -> Unit) {
    var nama by remember { mutableStateOf(awal?.nama ?: "") }
    var jumlah by remember { mutableStateOf(awal?.jumlah?.toString() ?: "1") }
    var harga by remember {
        val t = awal?.harga?.let { formatAngka(it) } ?: ""
        mutableStateOf(TextFieldValue(t, TextRange(t.length)))
    }
    val jml = jumlah.toIntOrNull()
    val hrg = parseAngka(harga.text)
    val valid = nama.isNotBlank() && jml != null && jml > 0 && hrg != null
    AlertDialog(
        onDismissRequest = onBatal,
        title = { Text(if (awal == null) "Tambah Barang" else "Ubah Barang") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    nama, { nama = it }, label = { Text("Nama barang") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                OutlinedTextField(
                    jumlah, { v -> jumlah = v.filter { it.isDigit() }.take(6) }, label = { Text("Jumlah") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    harga, { v ->
                        val t = parseAngka(v.text)?.let { formatAngka(it) } ?: ""
                        harga = TextFieldValue(t, TextRange(t.length))
                    },
                    label = { Text("Harga satuan (Rp)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (jml != null && hrg != null) Text("Subtotal: ${formatRupiah(jml.toLong() * hrg)}", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSimpan(nama.trim(), jml!!, hrg!!) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onBatal) { Text("Batal") } },
    )
}
