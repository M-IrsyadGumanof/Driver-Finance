package com.kotlin.basic.trpl3c.ui.transaksi

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kotlin.basic.trpl3c.R
import com.kotlin.basic.trpl3c.data.AppDatabase
import com.kotlin.basic.trpl3c.data.repository.DriverFinanceRepository
import com.kotlin.basic.trpl3c.databinding.FragmentTransaksiBinding
import com.kotlin.basic.trpl3c.ui.dashboard.DashboardViewModel
import com.kotlin.basic.trpl3c.ui.dashboard.DashboardViewModelFactory
import com.kotlin.basic.trpl3c.ui.dashboard.TransaksiAdapter
import com.kotlin.basic.trpl3c.ui.dashboard.TransaksiUiModel
import com.kotlin.basic.trpl3c.ui.pemasukan.CatatPemasukanActivity
import com.kotlin.basic.trpl3c.ui.pengeluaran.CatatPengeluaranActivity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment untuk halaman utama Transaksi (Pemasukan, Pengeluaran, Riwayat Transaksi).
 */
class TransaksiFragment : Fragment() {

    private var _binding: FragmentTransaksiBinding? = null
    private val binding get() = _binding!!

    private lateinit var transaksiAdapter: TransaksiAdapter
    private var allTransactions: List<TransaksiUiModel> = emptyList()

    private val viewModel: DashboardViewModel by activityViewModels {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = DriverFinanceRepository(
            database.pemasukanDao(),
            database.pengeluaranDao(),
            database.tabunganDao(),
            database.saldoKategoriDao(),
            database
        )
        DashboardViewModelFactory(repository)
    }

    private val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransaksiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        transaksiAdapter = TransaksiAdapter()
        binding.rvDaftarTransaksi.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transaksiAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.btnTambahPemasukan.setOnClickListener {
            val intent = Intent(requireContext(), CatatPemasukanActivity::class.java)
            startActivity(intent)
        }

        binding.btnTambahPengeluaran.setOnClickListener {
            val intent = Intent(requireContext(), CatatPengeluaranActivity::class.java)
            startActivity(intent)
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, _ ->
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = when (binding.chipGroupFilter.checkedChipId) {
            R.id.chipPemasukan -> allTransactions.filter { it.isPemasukan }
            R.id.chipPengeluaran -> allTransactions.filter { !it.isPemasukan }
            else -> allTransactions
        }

        if (filtered.isEmpty()) {
            binding.tvEmptyTransaksi.visibility = View.VISIBLE
            binding.rvDaftarTransaksi.visibility = View.GONE
        } else {
            binding.tvEmptyTransaksi.visibility = View.GONE
            binding.rvDaftarTransaksi.visibility = View.VISIBLE
            transaksiAdapter.submitList(filtered)
        }
    }

    private fun observeData() {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = DriverFinanceRepository(
            database.pemasukanDao(),
            database.pengeluaranDao(),
            database.tabunganDao(),
            database.saldoKategoriDao(),
            database
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. Amati Total Pemasukan
                launch {
                    repository.totalPemasukan.collect { totalIn ->
                        binding.tvRingkasanTotalPemasukan.text = "Rp${formatter.format(totalIn ?: 0.0)}"
                    }
                }

                // 2. Amati Total Pengeluaran
                launch {
                    repository.totalPengeluaran.collect { totalOut ->
                        binding.tvRingkasanTotalPengeluaran.text = "Rp${formatter.format(totalOut ?: 0.0)}"
                    }
                }

                // 3. Amati Seluruh Riwayat Transaksi (Pemasukan & Pengeluaran)
                launch {
                    combine(repository.allPemasukan, repository.allPengeluaran) { listPemasukan, listPengeluaran ->
                        val items = mutableListOf<TransaksiUiModel>()

                        listPemasukan.forEach {
                            val bensinInfo = if (it.bensin > 0) " (Bensin: Rp${formatter.format(it.bensin.toLong())} | Bersih: Rp${formatter.format(it.pemasukanBersih.toLong())})" else ""
                            val jamInfo = if (it.jamMulai.isNotBlank() && it.jamSelesai.isNotBlank()) " • ${it.jamMulai}-${it.jamSelesai}" else ""
                            items.add(
                                TransaksiUiModel(
                                    id = it.id,
                                    judul = "${it.sumber} (${it.jumlahOrder} Order)",
                                    subjudul = "${it.tanggal}$jamInfo$bensinInfo",
                                    nominal = it.nominal,
                                    isPemasukan = true,
                                    badge = "Pemasukan",
                                    tanggal = it.tanggal,
                                    bensin = it.bensin,
                                    pemasukanBersih = it.pemasukanBersih,
                                    catatan = it.catatan
                                )
                            )
                        }

                        listPengeluaran.forEach {
                            val subjudulText = buildString {
                                append(it.tanggal)
                                if (it.catatan.isNotBlank()) {
                                    append(" • ${it.catatan}")
                                }
                                if (it.sumberDana.isNotBlank()) {
                                    append(" • Sumber: ${it.sumberDana}")
                                }
                            }
                            items.add(
                                TransaksiUiModel(
                                    id = it.id,
                                    judul = it.kategori,
                                    subjudul = subjudulText,
                                    nominal = it.nominal,
                                    isPemasukan = false,
                                    badge = it.kategori,
                                    tanggal = it.tanggal
                                )
                            )
                        }

                        items.sortedWith(compareByDescending<TransaksiUiModel> { it.tanggal }.thenByDescending { it.id })
                    }.collect { list ->
                        allTransactions = list
                        applyFilter()
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
