package com.srhj.nota

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.srhj.nota.ui.daftar.LayarDaftarNota
import com.srhj.nota.ui.detail.LayarDetailNota
import com.srhj.nota.ui.form.LayarFormNota
import com.srhj.nota.ui.laporan.LayarLaporan
import com.srhj.nota.ui.pengaturan.LayarPengaturan
import com.srhj.nota.ui.theme.TemaNota

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TemaNota { AplikasiNota() } }
    }
}

private data class Tab(val rute: String, val label: String, val ikon: ImageVector)

private val TABS = listOf(
    Tab("daftar", "Nota", Icons.Default.ReceiptLong),
    Tab("laporan", "Laporan", Icons.Default.Assessment),
    Tab("pengaturan", "Pengaturan", Icons.Default.Settings),
)

@Composable
fun AplikasiNota() {
    val nav = rememberNavController()
    val entri by nav.currentBackStackEntryAsState()
    val ruteSekarang = entri?.destination?.route
    val tampilkanTab = TABS.any { it.rute == ruteSekarang }

    Scaffold(
        bottomBar = {
            if (tampilkanTab) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = ruteSekarang == tab.rute,
                            onClick = {
                                nav.navigate(tab.rute) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.ikon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        // Layar dengan tab punya Scaffold sendiri; hanya sisakan ruang untuk tab bawah.
        Box(Modifier.fillMaxSize().padding(bottom = if (tampilkanTab) pad.calculateBottomPadding() else androidx.compose.ui.unit.Dp(0f))) {
            NavHost(nav, startDestination = "daftar") {
                composable("daftar") {
                    LayarDaftarNota(onBuka = { nav.navigate("detail/$it") }, onBaru = { nav.navigate("form/0") })
                }
                composable("laporan") { LayarLaporan() }
                composable("pengaturan") { LayarPengaturan() }
                composable("form/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    val id = e.arguments?.getLong("id") ?: 0L
                    LayarFormNota(
                        id = id,
                        onKembali = { nav.popBackStack() },
                        onTersimpan = { idBaru ->
                            if (id == 0L) {
                                nav.navigate("detail/$idBaru") { popUpTo("form/{id}") { inclusive = true } }
                            } else {
                                nav.popBackStack()
                            }
                        },
                    )
                }
                composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    LayarDetailNota(
                        id = e.arguments?.getLong("id") ?: 0L,
                        onKembali = { nav.popBackStack() },
                        onUbah = { nav.navigate("form/$it") },
                    )
                }
            }
        }
    }
}
