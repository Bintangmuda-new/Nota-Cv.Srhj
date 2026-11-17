package com.srhj.nota

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Uji alur lengkap aplikasi (end-to-end) di Robolectric:
 * buat nota -> isi barang -> tanda tangan -> simpan -> detail -> laporan -> pengaturan.
 * Screenshot setiap layar disimpan di app/build/hasil-uji/layar_*.png
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AplikasiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    /** Tutup database agar tidak mengganggu uji berikutnya (Robolectric berbagi thread Room antar-uji). */
    @org.junit.After fun tutupDb() {
        androidx.test.core.app.ApplicationProvider.getApplicationContext<NotaApp>().database.close()
    }

    private fun foto(nama: String) {
        rule.waitForIdle()
        captureScreenRoboImage(File(FOLDER_HASIL, "layar_$nama.png").path)
    }

    private fun tunggu(teks: String) = try {
        rule.waitUntil(20_000) { rule.onAllNodesWithText(teks, substring = true).fetchSemanticsNodes().isNotEmpty() }
    } catch (e: Throwable) {
        foto("GAGAL_menunggu_" + teks.filter { it.isLetterOrDigit() })
        throw e
    }

    private fun klik(teks: String) = rule.onNodeWithText(teks, useUnmergedTree = true).performClick()

    private fun isi(label: String, teks: String) =
        rule.onNode(hasSetTextAction() and hasText(label, substring = true)).performTextInput(teks)

    @Test fun alurLengkap() {
        tunggu("Belum ada nota")
        foto("1_daftar_kosong")

        klik("Nota Baru")
        tunggu("SRHJ-")
        isi("Nama Pelanggan", "Bengkel Maju Jaya")
        isi("No. WhatsApp", "081234567890")

        // Barang 1
        klik("Tambah Barang")
        isi("Nama barang", "Klakson Denso 12V")
        rule.onNode(hasSetTextAction() and hasText("Jumlah")).performTextReplacement("10")
        isi("Harga satuan", "85000")
        tunggu("Subtotal: Rp 850.000")
        foto("2_dialog_barang")
        klik("Simpan")

        // Barang 2
        klik("Tambah Barang")
        isi("Nama barang", "Relay Klakson")
        isi("Harga satuan", "15000")
        klik("Simpan")
        tunggu("Rp 865.000")

        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Nama Penerima"))
        isi("Nama Penerima", "Budi Santoso")
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Buat Tanda Tangan"))
        klik("Buat Tanda Tangan")
        tunggu("Tanda tangan di sini")
        val pad = rule.onNodeWithContentDescription("Area tanda tangan")
        pad.performTouchInput { swipe(Offset(width * 0.1f, height * 0.7f), Offset(width * 0.5f, height * 0.2f), 300) }
        pad.performTouchInput { swipe(Offset(width * 0.4f, height * 0.6f), Offset(width * 0.9f, height * 0.4f), 300) }
        foto("3_dialog_ttd")
        klik("Simpan")
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription("Tanda tangan").fetchSemanticsNodes().isNotEmpty() }
        foto("4_form_terisi")

        klik("Simpan Nota")
        rule.mainClock.advanceTimeBy(2000) // beri waktu Room menyimpan di thread latar
        tunggu("Kirim WA")
        tunggu("Budi Santoso")
        foto("5_detail")

        // Periksa data benar-benar tersimpan di database
        val app = rule.activity.application as NotaApp
        val n = kotlinx.coroutines.runBlocking { app.dao.ambil(1) }
        assertNotNull(n)
        assertEquals(865_000L, n!!.nota.total)
        assertEquals(2, n.items.size)
        assertNotNull(n.nota.ttdPath)

        // Ubah nota -> kembali -> detail tetap
        rule.onNodeWithContentDescription("Ubah nota").performClick()
        tunggu("Ubah Nota")
        rule.onNodeWithContentDescription("Kembali").performClick()
        tunggu("Kirim WA")

        rule.onNodeWithContentDescription("Kembali").performClick()
        tunggu("Bengkel Maju Jaya")
        foto("6_daftar_isi")

        klik("Laporan")
        tunggu("Barang Terlaris")
        foto("7_laporan")

        klik("Pengaturan")
        tunggu("Printer Thermal Bluetooth")
        foto("8_pengaturan")
    }
}
