package com.kotlin.basic.trpl3c.ui.tabungan

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kotlin.basic.trpl3c.data.AppDatabase
import com.kotlin.basic.trpl3c.data.repository.DriverFinanceRepository
import com.kotlin.basic.trpl3c.databinding.ActivityTabunganBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Activity untuk menampilkan Halaman Tabungan Driver.
 * Menampilkan total tabungan, target Rp5.000.000, progress bar, saldo 3 pos tabungan,
 * tombol "+ Tambah Tabungan", "Ambil Dana", dan riwayat transaksi dari Room Database.
 */
class TabunganActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTabunganBinding
    private lateinit var riwayatAdapter: RiwayatTabunganAdapter

    private val viewModel: TabunganViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repository = DriverFinanceRepository(
            database.pemasukanDao(),
            database.pengeluaranDao(),
            database.tabunganDao(),
            database.saldoKategoriDao(),
            database
        )
        TabunganViewModelFactory(repository)
    }

    private val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTabunganBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        riwayatAdapter = RiwayatTabunganAdapter()
        binding.rvRiwayatTabungan.apply {
            layoutManager = LinearLayoutManager(this@TabunganActivity)
            adapter = riwayatAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        // Tombol + Tambah Tabungan
        binding.btnTambahTabungan.setOnClickListener {
            val sheet = TambahTabunganBottomSheet.newInstance(viewModel)
            sheet.show(supportFragmentManager, TambahTabunganBottomSheet.TAG)
        }

        // Tombol Ambil Dana
        binding.btnAmbilDana.setOnClickListener {
            val sheet = AmbilDanaBottomSheet.newInstance(viewModel)
            sheet.show(supportFragmentManager, AmbilDanaBottomSheet.TAG)
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. Amati Total Saldo & Hitung Progress Target
                launch {
                    viewModel.totalSaldo.collect { total ->
                        binding.tvValueTotalTabungan.text = "Rp${formatter.format(total)}"

                        val target = viewModel.targetTabungan
                        val persen = ((total / target) * 100).toInt().coerceIn(0, 100)
                        binding.progressBarTabungan.progress = persen
                        binding.tvProgressPersen.text = "$persen% tercapai"

                        val sisa = target - total
                        if (sisa <= 0) {
                            binding.tvSisaTarget.text = "Target Tercapai! 🎉"
                        } else {
                            binding.tvSisaTarget.text = "Kurang Rp${formatter.format(sisa)}"
                        }
                    }
                }

                // 2. Amati Saldo Pos: Tabungan Utama
                launch {
                    viewModel.saldoUtama.collect { saldo ->
                        binding.tvSaldoUtama.text = "Rp${formatter.format(saldo)}"
                    }
                }

                // 3. Amati Saldo Pos: Dana Motor
                launch {
                    viewModel.saldoMotor.collect { saldo ->
                        binding.tvSaldoMotor.text = "Rp${formatter.format(saldo)}"
                    }
                }

                // 4. Amati Saldo Pos: Dana Darurat
                launch {
                    viewModel.saldoDarurat.collect { saldo ->
                        binding.tvSaldoDarurat.text = "Rp${formatter.format(saldo)}"
                    }
                }

                // 5. Amati Riwayat Mutasi Tabungan
                launch {
                    viewModel.riwayatTabungan.collect { list ->
                        if (list.isEmpty()) {
                            binding.tvEmptyRiwayat.visibility = View.VISIBLE
                            binding.rvRiwayatTabungan.visibility = View.GONE
                            binding.tvJumlahRiwayat.text = "0 transaksi"
                        } else {
                            binding.tvEmptyRiwayat.visibility = View.GONE
                            binding.rvRiwayatTabungan.visibility = View.VISIBLE
                            binding.tvJumlahRiwayat.text = "${list.size} transaksi"
                            riwayatAdapter.submitList(list)
                        }
                    }
                }
            }
        }
    }
}
