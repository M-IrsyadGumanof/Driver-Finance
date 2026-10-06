package com.kotlin.basic.trpl3c.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.kotlin.basic.trpl3c.R
import com.kotlin.basic.trpl3c.data.AppDatabase
import com.kotlin.basic.trpl3c.data.SistemKeuanganManager
import com.kotlin.basic.trpl3c.data.repository.DriverFinanceRepository
import com.kotlin.basic.trpl3c.databinding.FragmentDashboardBinding
import com.kotlin.basic.trpl3c.ui.pemasukan.CatatPemasukanActivity
import com.kotlin.basic.trpl3c.ui.pengeluaran.CatatPengeluaranActivity
import com.kotlin.basic.trpl3c.ui.tabungan.TabunganActivity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment yang menampilkan Dashboard utama Driver Finance.
 * Mengamati data secara realtime dari Room Database melalui DashboardViewModel.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var transaksiAdapter: TransaksiAdapter

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupBarChart()
        setupListeners()
        observeDashboardData()
    }

    private fun setupRecyclerView() {
        transaksiAdapter = TransaksiAdapter()
        binding.rvTransaksiTerbaru.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transaksiAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupBarChart() {
        binding.barChartMingguan.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(true)
                axisLineColor = ContextCompat.getColor(requireContext(), R.color.card_border)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 10f
                granularity = 1f
                setCenterAxisLabels(false)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.card_border)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
                textSize = 9f
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value >= 1000) "${(value / 1000).toInt()}k" else value.toInt().toString()
                    }
                }
            }

            axisRight.isEnabled = false
        }
    }

    private fun updateBarChart(chartData: WeeklyChartUiModel) {
        val entries = chartData.dataHarian.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.nominal.toFloat())
        }

        val colors = chartData.dataHarian.map { item ->
            if ((item.nominal == chartData.pemasukanTertinggi) && (item.nominal > 0)) {
                ContextCompat.getColor(requireContext(), R.color.hero_gradient_start)
            } else {
                ContextCompat.getColor(requireContext(), R.color.primary)
            }
        }

        val dataSet = BarDataSet(entries, getString(R.string.label_pemasukan)).apply {
            this.colors = colors
            setDrawValues(false)
            highLightAlpha = 0
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.5f
        }

        binding.barChartMingguan.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(chartData.dataHarian.map { it.labelHari })
            data = barData
            invalidate()
            animateY(700)
        }
    }

    private fun setupListeners() {
        binding.btnCatatPemasukan.setOnClickListener {
            val intent = Intent(requireContext(), CatatPemasukanActivity::class.java)
            startActivity(intent)
        }

        binding.btnCatatPengeluaran.setOnClickListener {
            val intent = Intent(requireContext(), CatatPengeluaranActivity::class.java)
            startActivity(intent)
        }

        binding.cardTotalTabungan.setOnClickListener {
            val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
            if (bottomNav != null) {
                bottomNav.selectedItemId = R.id.nav_tabungan
            } else {
                val intent = Intent(requireContext(), TabunganActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun observeDashboardData() {
        val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. Amati Saldo Tersedia
                launch {
                    viewModel.saldoTersedia.collect { saldo ->
                        binding.tvValueSaldoTersedia.text = getString(R.string.currency_format, formatter.format(saldo))
                    }
                }

                // 2. Amati Pemasukan Hari Ini
                launch {
                    viewModel.pemasukanHariIni.collect { pemasukan ->
                        binding.tvValuePemasukanHariIni.text = getString(R.string.currency_format, formatter.format(pemasukan))
                    }
                }

                // 2a. Amati Bensin Hari Ini & Pemasukan Bersih Hari Ini
                launch {
                    viewModel.bensinHariIni.collect { bensin ->
                        binding.tvValueBensinHariIni.text = getString(R.string.currency_format, formatter.format(bensin))
                    }
                }

                launch {
                    viewModel.pemasukanBersihHariIni.collect { bersih ->
                        binding.tvValuePemasukanBersihHariIni.text = getString(R.string.currency_format, formatter.format(bersih))
                    }
                }

                // 2b. Amati Alokasi Harian Hari Ini
                launch {
                    viewModel.alokasiHariIni.collect { alokasi ->
                        binding.tvHarianTabungan.text = "Tabungan (20%)\nRp${formatter.format(alokasi.tabungan)}"
                        binding.tvHarianMotor.text = "Motor (15%)\nRp${formatter.format(alokasi.motor)}"
                        binding.tvHarianKeluarga.text = "Keluarga (10%)\nRp${formatter.format(alokasi.keluarga)}"
                        binding.tvHarianKehidupan.text = "Kehidupan (45%)\nRp${formatter.format(alokasi.kehidupan)}"
                        binding.tvHarianBebas.text = "Bebas (10%)\nRp${formatter.format(alokasi.danaBebas)}"
                    }
                }

                // 2c. Amati Saldo Aktual Tiap Kategori (Real-time dari Room Database)
                launch {
                    viewModel.saldoKategoriMap.collect { map ->
                        val tabungan = map[SistemKeuanganManager.KATEGORI_TABUNGAN] ?: 0L
                        val motor = map[SistemKeuanganManager.KATEGORI_MOTOR] ?: 0L
                        val keluarga = map[SistemKeuanganManager.KATEGORI_KELUARGA] ?: 0L
                        val kehidupan = map[SistemKeuanganManager.KATEGORI_KEHIDUPAN] ?: 0L
                        val bebas = map[SistemKeuanganManager.KATEGORI_DANA_BEBAS] ?: 0L

                        binding.tvAllocTabunganValue.text  = getString(R.string.currency_format, formatter.format(tabungan))
                        binding.tvAllocMotorValue.text     = getString(R.string.currency_format, formatter.format(motor))
                        binding.tvAllocKeluargaValue.text  = getString(R.string.currency_format, formatter.format(keluarga))
                        binding.tvAllocKehidupanValue.text = getString(R.string.currency_format, formatter.format(kehidupan))
                        binding.tvAllocBebasValue.text     = getString(R.string.currency_format, formatter.format(bebas))
                    }
                }

                // 3. Amati Jumlah Order Hari Ini
                launch {
                    viewModel.orderHariIni.collect { order ->
                        binding.tvOrderBadge.text = getString(R.string.order_count_format, order)
                    }
                }

                // 4. Amati Pengeluaran Hari Ini
                launch {
                    viewModel.pengeluaranHariIni.collect { pengeluaran ->
                        binding.tvValuePengeluaranHariIni.text = getString(R.string.currency_format, formatter.format(pengeluaran))
                    }
                }

                // 5. Amati Total Tabungan
                launch {
                    viewModel.totalTabungan.collect { tabungan ->
                        binding.tvValueTotalTabungan.text = getString(R.string.currency_format, formatter.format(tabungan))
                    }
                }

                // 6. Amati Grafik & Statistik Pemasukan Minggu Ini (7 hari)
                launch {
                    viewModel.weeklyChartData.collect { chartData ->
                        if (chartData.hasData) {
                            binding.tvEmptyPemasukanMingguIni.visibility = View.GONE
                            binding.layoutPemasukanMingguIniData.visibility = View.VISIBLE

                            binding.tvTotalPemasukanMingguIni.text = getString(R.string.currency_format, formatter.format(chartData.totalMingguIni))
                            binding.tvRataRataPemasukanMingguIni.text = getString(R.string.currency_format, formatter.format(chartData.rataRataPerHari.toLong()))
                            binding.tvTertinggiPemasukanMingguIni.text = getString(R.string.currency_format, formatter.format(chartData.pemasukanTertinggi))

                            updateBarChart(chartData)
                        } else {
                            binding.tvEmptyPemasukanMingguIni.visibility = View.VISIBLE
                            binding.layoutPemasukanMingguIniData.visibility = View.GONE
                        }
                    }
                }

                // 7. Amati Daftar Transaksi Terbaru
                launch {
                    viewModel.transaksiTerbaru.collect { list ->
                        if (list.isEmpty()) {
                            binding.tvEmptyTransaksi.visibility = View.VISIBLE
                            binding.rvTransaksiTerbaru.visibility = View.GONE
                            binding.tvTransaksiCount.text = getString(R.string.transaction_count_format, 0)
                        } else {
                            binding.tvEmptyTransaksi.visibility = View.GONE
                            binding.rvTransaksiTerbaru.visibility = View.VISIBLE
                            binding.tvTransaksiCount.text = getString(R.string.transaction_count_format, list.size)
                            transaksiAdapter.submitList(list)
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
