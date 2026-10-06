package com.kotlin.basic.trpl3c.ui.tabungan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kotlin.basic.trpl3c.data.AppDatabase
import com.kotlin.basic.trpl3c.data.repository.DriverFinanceRepository
import com.kotlin.basic.trpl3c.databinding.FragmentTabunganBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment untuk halaman Tabungan Driver pada Bottom Navigation.
 */
class TabunganFragment : Fragment() {

    private var _binding: FragmentTabunganBinding? = null
    private val binding get() = _binding!!

    private lateinit var riwayatAdapter: RiwayatTabunganAdapter

    private val viewModel: TabunganViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTabunganBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        riwayatAdapter = RiwayatTabunganAdapter()
        binding.rvRiwayatTabungan.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = riwayatAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.btnTambahTabungan.setOnClickListener {
            val sheet = TambahTabunganBottomSheet.newInstance(viewModel)
            sheet.show(childFragmentManager, TambahTabunganBottomSheet.TAG)
        }

        binding.btnAmbilDana.setOnClickListener {
            val sheet = AmbilDanaBottomSheet.newInstance(viewModel)
            sheet.show(childFragmentManager, AmbilDanaBottomSheet.TAG)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. Amati Total Saldo & Progres Target
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
