package com.srhj.nota

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.srhj.nota.data.ItemNota
import com.srhj.nota.data.Nota
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Tampilan di HP ukuran umum (411dp) dengan data contoh. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AplikasiLebarTest {
    init {
        val app = ApplicationProvider.getApplicationContext<NotaApp>()
        app.pengaturan.simpan(app.pengaturan.info.value.copy(alamat = "Jl. Gajah Mada No. 10, Jember", telepon = "0812 3456 7890", printerNama = "RPP02N", printerAlamat = "00:11:22:33:44:55"))
        val now = System.currentTimeMillis()
        runBlocking {
            listOf(
                Triple("Bengkel Maju Jaya", "Budi Santoso", listOf(ItemNota(nama = "Klakson Denso 12V", jumlah = 10, harga = 85_000), ItemNota(nama = "Relay Klakson 4 Pin", jumlah = 10, harga = 15_000))),
                Triple("Toko Sumber Rejeki", "Siti", listOf(ItemNota(nama = "Klakson Keong Bosch Super Tone Dual", jumlah = 4, harga = 112_500))),
                Triple("UD Sinar Motor", "Andi", listOf(ItemNota(nama = "Kabel Set Klakson", jumlah = 6, harga = 25_000), ItemNota(nama = "Klakson Denso 12V", jumlah = 2, harga = 85_000))),
            ).forEachIndexed { i, (pel, pen, items) ->
                app.dao.simpan(Nota(nomor = "SRHJ-261117-00${i + 1}", tanggal = now - i * 3_600_000L, pelanggan = pel, teleponPelanggan = "0812345678${i}0", namaPenerima = pen), items)
            }
        }
    }

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    /** Tutup database agar tidak mengganggu uji berikutnya (Robolectric berbagi thread Room antar-uji). */
    @org.junit.After fun tutupDb() {
        androidx.test.core.app.ApplicationProvider.getApplicationContext<NotaApp>().database.close()
    }

    private fun foto(nama: String) { rule.waitForIdle(); captureScreenRoboImage(File(FOLDER_HASIL, "lebar_$nama.png").path) }
    private fun tunggu(t: String) = rule.waitUntil(10_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Test fun tampilan() {
        tunggu("Bengkel Maju Jaya")
        foto("1_daftar")
        rule.onNodeWithText("Bengkel Maju Jaya").performClick()
        tunggu("Kirim WA")
        foto("2_detail")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        tunggu("Nota hari ini")
        rule.onNodeWithText("Laporan", useUnmergedTree = true).performClick()
        tunggu("Barang Terlaris")
        foto("3_laporan")
        rule.onNodeWithText("Pengaturan", useUnmergedTree = true).performClick()
        tunggu("RPP02N")
        foto("4_pengaturan")
    }
}
