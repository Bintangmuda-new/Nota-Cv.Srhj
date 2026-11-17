package com.srhj.nota.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NotaDao {
    @Transaction
    @Query("SELECT * FROM nota ORDER BY tanggal DESC, id DESC")
    abstract fun semua(): Flow<List<NotaLengkap>>

    @Transaction
    @Query("SELECT * FROM nota WHERE id = :id")
    abstract fun pantau(id: Long): Flow<NotaLengkap?>

    @Transaction
    @Query("SELECT * FROM nota WHERE id = :id")
    abstract suspend fun ambil(id: Long): NotaLengkap?

    @Transaction
    @Query("SELECT * FROM nota WHERE tanggal BETWEEN :dari AND :sampai ORDER BY tanggal ASC, id ASC")
    abstract fun rentang(dari: Long, sampai: Long): Flow<List<NotaLengkap>>

    @Query("SELECT nomor FROM nota WHERE nomor LIKE :prefix || '%'")
    abstract suspend fun nomorDenganAwalan(prefix: String): List<String>

    @Insert
    protected abstract suspend fun tambahNota(nota: Nota): Long

    @Update
    protected abstract suspend fun ubahNota(nota: Nota)

    @Insert
    protected abstract suspend fun tambahItem(items: List<ItemNota>)

    @Query("DELETE FROM item_nota WHERE notaId = :notaId")
    protected abstract suspend fun hapusItemNota(notaId: Long)

    @Delete
    abstract suspend fun hapus(nota: Nota)

    /** Simpan nota baru (id = 0) atau perbarui nota lama beserta seluruh itemnya. */
    @Transaction
    open suspend fun simpan(nota: Nota, items: List<ItemNota>): Long {
        val total = items.sumOf { it.subtotal() }
        val id = if (nota.id == 0L) {
            tambahNota(nota.copy(total = total))
        } else {
            ubahNota(nota.copy(total = total))
            hapusItemNota(nota.id)
            nota.id
        }
        tambahItem(items.map { it.copy(id = 0, notaId = id) })
        return id
    }
}
