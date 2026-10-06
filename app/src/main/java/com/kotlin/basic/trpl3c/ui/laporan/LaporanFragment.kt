package com.kotlin.basic.trpl3c.ui.laporan

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
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.kotlin.basic.trpl3c.R
import com.kotlin.basic.trpl3c.data.AppDatabase
import com.kotlin.basic.trpl3c.data.repository.DriverFinanceRepository
import com.kotlin.basic.trpl3c.databinding.FragmentLaporanBinding
import com.kotlin.basic.trpl3c.ui.dashboard.DashboardViewModel
import com.kotlin.basic.trpl3c.ui.dashboard.DashboardViewModelFactory
import com.kotlin.basic.trpl3c.ui.dashboard.WeeklyChartUiModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment untuk halaman Laporan Keuangan (Grafik Pemasukan, Breakdown Pengeluaran, Ringkasan Finansial).
 */
class LaporanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!

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
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChart()
        observeData()
    }

    private fun setupChart() {
        binding.chartLaporanPemasukan.apply {
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
                textSize = 9f
                granularity = 1f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.card_border)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
                textSize = 8f
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value >= 1000) "${(value / 1000).toInt()}k" else "${value.toInt()}"
                    }
                }
            }

            axisRight.isEnabled = false
        }
    }

    private fun updateChart(chartData: WeeklyChartUiModel) {
        val entries = chartData.dataHarian.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.nominal.toFloat())
        }

        val colors = chartData.dataHarian.map { item ->
            if (item.nominal == chartData.pemasukanTertinggi && item.nominal > 0) {
                ContextCompat.getColor(requireContext(), R.color.hero_gradient_start)
            } else {
                ContextCompat.getColor(requireContext(), R.color.primary)
            }
        }

        val dataSet = BarDataSet(entries, "Pemasukan").apply {
            this.colors = colors
            setDrawValues(false)
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.5f
        }

        binding.chartLaporanPemasukan.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(chartData.dataHarian.map { it.labelHari })
            data = barData
            invalidate()
            animateY(600)
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

                // 1. Ringkasan Finansial Akumulatif
                launch {
                    combine(repository.totalPemasukan, repository.totalPengeluaran) { totalIn, totalOut ->
                        val masuk = totalIn ?: 0.0
                        val keluar = totalOut ?: 0.0
                        val net = masuk - keluar
                        Triple(masuk, keluar, net)
                    }.collect { (masuk, keluar, net) ->
                        binding.tvLaporanTotalMasuk.text = "Rp${formatter.format(masuk)}"
                        binding.tvLaporanTotalKeluar.text = "Rp${formatter.format(keluar)}"
                        binding.tvLaporanKasBersih.text = "Rp${formatter.format(net)}"
                    }
                }

                // 2. Grafik Pemasukan
                launch {
                    viewModel.weeklyChartData.collect { chartData ->
                        updateChart(chartData)
                    }
                }

                // 3. Distribusi Pengeluaran per Kategori
                launch {
                    repository.allPengeluaran.collect { list ->
                        var bensin = 0.0
                        var motor = 0.0
                        var jajan = 0.0
                        var kuliah = 0.0
                        var rumah = 0.0
                        var lainnya = 0.0

                        list.forEach {
                            when (it.kategori) {
                                "Bensin" -> bensin += it.nominal
                                "Motor" -> motor += it.nominal
                                "Jajan" -> jajan += it.nominal
                                "Kuliah" -> kuliah += it.nominal
                                "Kebutuhan Rumah" -> rumah += it.nominal
                                else -> lainnya += it.nominal
                            }
                        }

                        binding.tvPengeluaranBensin.text = "Rp${formatter.format(bensin)}"
                        binding.tvPengeluaranMotor.text = "Rp${formatter.format(motor)}"
                        binding.tvPengeluaranJajan.text = "Rp${formatter.format(jajan)}"
                        binding.tvPengeluaranKuliah.text = "Rp${formatter.format(kuliah)}"
                        binding.tvPengeluaranRumah.text = "Rp${formatter.format(rumah)}"
                        binding.tvPengeluaranLainnya.text = "Rp${formatter.format(lainnya)}"
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
