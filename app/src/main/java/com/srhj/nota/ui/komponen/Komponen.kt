package com.srhj.nota.ui.komponen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.srhj.nota.util.Goresan

@Composable
fun KartuAngka(label: String, nilai: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                nilai,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun BarisInfo(label: String, nilai: String, tebal: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            nilai,
            style = if (tebal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (tebal) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** Area tanda tangan: coret dengan jari. */
@Composable
fun PapanTandaTangan(goresan: MutableList<MutableList<Offset>>, modifier: Modifier = Modifier) {
    Box(
        modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp)),
    ) {
        Text(
            "Tanda tangan di sini",
            Modifier.align(Alignment.BottomCenter).padding(8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
        )
        Canvas(
            Modifier
                .matchParentSize()
                .semantics { contentDescription = "Area tanda tangan" }
                .pointerInput(Unit) {
                    detectTapGestures { p -> goresan.add(mutableStateListOf(p, p + Offset(0.5f, 0.5f))) }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { p -> goresan.add(mutableStateListOf(p)) },
                        onDrag = { change, _ ->
                            change.consume()
                            goresan.lastOrNull()?.add(change.position)
                        },
                    )
                },
        ) {
            for (g in goresan) {
                if (g.isEmpty()) continue
                val path = Path().apply {
                    moveTo(g[0].x, g[0].y)
                    for (i in 1 until g.size) lineTo(g[i].x, g[i].y)
                }
                drawPath(path, Color.Black, style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
fun DialogTandaTangan(namaPenerima: String, onBatal: () -> Unit, onSimpan: (List<Goresan>) -> Unit) {
    val goresan = remember { mutableStateListOf<MutableList<Offset>>() }
    AlertDialog(
        onDismissRequest = onBatal,
        title = { Text("Tanda Tangan Penerima") },
        text = {
            Column {
                if (namaPenerima.isNotBlank()) Text("Penerima: $namaPenerima", style = MaterialTheme.typography.bodySmall)
                PapanTandaTangan(goresan, Modifier.fillMaxWidth().height(220.dp).padding(top = 8.dp))
                TextButton(onClick = { goresan.clear() }) { Text("Ulangi") }
            }
        },
        confirmButton = {
            TextButton(
                enabled = goresan.isNotEmpty(),
                onClick = { onSimpan(goresan.map { g -> g.map { it.x to it.y } }) },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onBatal) { Text("Batal") } },
    )
}

@Composable
fun DialogKonfirmasi(judul: String, pesan: String, tombol: String, onBatal: () -> Unit, onYa: () -> Unit) {
    AlertDialog(
        onDismissRequest = onBatal,
        title = { Text(judul) },
        text = { Text(pesan) },
        confirmButton = { TextButton(onClick = onYa) { Text(tombol, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onBatal) { Text("Batal") } },
    )
}

/** Tombol aksi ringkas: ikon di atas, label satu baris — tetap rapi di layar HP kecil. */
@Composable
fun TombolAksi(
    label: String,
    ikon: ImageVector,
    aktif: Boolean,
    modifier: Modifier = Modifier,
    utama: Boolean = false,
    onClick: () -> Unit,
) {
    val isi: @Composable RowScope.() -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(ikon, null, Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    val pad = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    if (utama) {
        Button(
            onClick = onClick, enabled = aktif, modifier = modifier, contentPadding = pad,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HijauWa),
            content = isi,
        )
    } else {
        OutlinedButton(onClick = onClick, enabled = aktif, modifier = modifier, contentPadding = pad, shape = RoundedCornerShape(12.dp), content = isi)
    }
}

val HijauWa = Color(0xFF1B8C4E)
