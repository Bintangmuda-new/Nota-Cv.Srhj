package com.srhj.nota.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Kepala nota. Semua nilai uang disimpan dalam Rupiah (Long, tanpa desimal). */
@Entity(tableName = "nota", indices = [Index(value = ["nomor"], unique = true), Index("tanggal")])
data class Nota(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nomor: String,
    /** Waktu nota dalam epoch millis. */
    val tanggal: Long,
    val pelanggan: String,
    /** Nomor WhatsApp pelanggan (opsional), dipakai untuk kirim langsung. */
    val teleponPelanggan: String = "",
    val namaPenerima: String = "",
    /** Path file PNG tanda tangan penerima di penyimpanan internal aplikasi. */
    val ttdPath: String? = null,
    val catatan: String = "",
    val total: Long = 0,
)

@Entity(
    tableName = "item_nota",
    foreignKeys = [
        ForeignKey(
            entity = Nota::class,
            parentColumns = ["id"],
            childColumns = ["notaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("notaId")],
)
data class ItemNota(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notaId: Long = 0,
    val nama: String,
    val jumlah: Int,
    val harga: Long,
)

fun ItemNota.subtotal(): Long = jumlah.toLong() * harga

data class NotaLengkap(
    @Embedded val nota: Nota,
    @Relation(parentColumn = "id", entityColumn = "notaId")
    val items: List<ItemNota>,
)
