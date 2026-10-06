package com.kotlin.basic.trpl3c.ui.dashboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.kotlin.basic.trpl3c.R
import com.kotlin.basic.trpl3c.databinding.ActivityMainBinding
import com.kotlin.basic.trpl3c.ui.laporan.LaporanFragment
import com.kotlin.basic.trpl3c.ui.pengaturan.PengaturanFragment
import com.kotlin.basic.trpl3c.ui.tabungan.TabunganFragment
import com.kotlin.basic.trpl3c.ui.transaksi.TransaksiFragment

/**
 * MainActivity sebagai Host Bottom Navigation 5 Menu Utama Driver Finance:
 * 1. Dashboard  - Ringkasan keuangan, grafik mingguan, transaksi terbaru
 * 2. Transaksi  - Pemasukan, pengeluaran, riwayat transaksi lengkap
 * 3. Tabungan   - Target Rp5jt, 3 pos tabungan, ambil dana, mutasi
 * 4. Laporan    - Grafik pemasukan, grafik pengeluaran, ringkasan finansial
 * 5. Pengaturan - Persentase pembagian uang, target tabungan, status offline
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        // Muat halaman Dashboard sebagai halaman default saat pertama kali dibuka
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_transaksi -> {
                    loadFragment(TransaksiFragment())
                    true
                }
                R.id.nav_tabungan -> {
                    loadFragment(TabunganFragment())
                    true
                }
                R.id.nav_laporan -> {
                    loadFragment(LaporanFragment())
                    true
                }
                R.id.nav_pengaturan -> {
                    loadFragment(PengaturanFragment())
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Mengganti fragment yang aktif di container nav_host_fragment.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}
