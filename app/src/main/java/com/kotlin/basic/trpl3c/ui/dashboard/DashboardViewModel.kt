package com.kotlin.basic.trpl3c.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kotlin.basic.trpl3c.data.PembagianKalkulator
import com.kotlin.basic.trpl3c.data.SistemKeuanganManager
import com.kotlin.basic.trpl3c.data.repository.DriverFinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ViewModel untuk Dashboard Driver Finance.
 * Mengolah data finansial dari Room Database secara reaktif.
 */
class DashboardViewModel(
    repository: DriverFinanceRepository,
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val todayDateString: String
        get() = dateFormat.format(Date())

    // 1. Pemasukan Hari Ini (Kotor)
    val pemasukanHariIni: Flow<Double> = repository.getPemasukanByTanggal(todayDateString).map { list ->
        list.sumOf { it.nominal }
    }

    // 1b. Bensin Hari Ini
    val bensinHariIni: Flow<Double> = repository.getPemasukanByTanggal(todayDateString).map { list ->
        list.sumOf { it.bensin }
    }

    // 1c. Pemasukan Bersih Hari Ini (Pemasukan Kotor - Bensin)
    val pemasukanBersihHariIni: Flow<Double> = repository.getPemasukanByTanggal(todayDateString).map { list ->
        list.sumOf { it.pemasukanBersih.takeIf { pb -> pb > 0 } ?: (it.nominal - it.bensin).coerceAtLeast(0.0) }
    }

    // Jumlah order hari ini
    val orderHariIni: Flow<Int> = repository.getPemasukanByTanggal(todayDateString).map { list ->
        list.sumOf { it.jumlahOrder }
    }

    // 2. Pengeluaran Hari Ini
    val pengeluaranHariIni: Flow<Double> = repository.getPengeluaranByTanggal(todayDateString).map { list ->
        list.sumOf { it.nominal }
    }

    // 3. Total Tabungan
    val totalTabungan: Flow<Double> = repository.totalSaldoSemuaTabungan.map { saldo ->
        saldo ?: 0.0
    }

    // 4. Saldo Tersedia (Total Saldo Likuid di kategori operasional: Motor + Keluarga + Kehidupan + Dana Bebas)
    val saldoTersedia: Flow<Double> = repository.saldoKategoriMap.map { map ->
        val motor = map[SistemKeuanganManager.KATEGORI_MOTOR] ?: 0L
        val keluarga = map[SistemKeuanganManager.KATEGORI_KELUARGA] ?: 0L
        val kehidupan = map[SistemKeuanganManager.KATEGORI_KEHIDUPAN] ?: 0L
        val bebas = map[SistemKeuanganManager.KATEGORI_DANA_BEBAS] ?: 0L
        (motor + keluarga + kehidupan + bebas).toDouble()
    }

    // 5. Grafik & Statistik Pemasukan Mingguan (Menggunakan PEMASUKAN KOTOR)
    val weeklyChartData: Flow<WeeklyChartUiModel> = repository.allPemasukan.map { allPemasukanList ->
        val cal = Calendar.getInstance(Locale("id", "ID"))
        cal.firstDayOfWeek = Calendar.MONDAY
        cal[Calendar.DAY_OF_WEEK] = Calendar.MONDAY
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0

        val dayNames = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
        val dailyItems = mutableListOf<DailyIncomeItem>()

        for (i in 0..6) {
            val tgl = dateFormat.format(cal.time)
            val nominalHari = allPemasukanList
                .asSequence()
                .filter { it.tanggal == tgl }
                .sumOf { it.nominal }
            dailyItems.add(DailyIncomeItem(labelHari = dayNames[i], tanggal = tgl, nominal = nominalHari))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val total = dailyItems.sumOf { it.nominal }
        val rataRata = if (total > 0) total / 7.0 else 0.0
        val tertinggi = dailyItems.maxOfOrNull { it.nominal } ?: 0.0

        WeeklyChartUiModel(
            dataHarian = dailyItems,
            totalMingguIni = total,
            rataRataPerHari = rataRata,
            pemasukanTertinggi = tertinggi,
            hasData = total > 0.0,
        )
    }

    // 6. Saldo Aktual per Kategori dari Room Database (Realtime)
    val saldoKategoriMap: Flow<Map<String, Long>> = repository.saldoKategoriMap

    val saldoTabungan: Flow<Long> = saldoKategoriMap.map { it[SistemKeuanganManager.KATEGORI_TABUNGAN] ?: 0L }
    val saldoMotor: Flow<Long> = saldoKategoriMap.map { it[SistemKeuanganManager.KATEGORI_MOTOR] ?: 0L }
    val saldoKeluarga: Flow<Long> = saldoKategoriMap.map { it[SistemKeuanganManager.KATEGORI_KELUARGA] ?: 0L }
    val saldoKehidupan: Flow<Long> = saldoKategoriMap.map { it[SistemKeuanganManager.KATEGORI_KEHIDUPAN] ?: 0L }
    val saldoDanaBebas: Flow<Long> = saldoKategoriMap.map { it[SistemKeuanganManager.KATEGORI_DANA_BEBAS] ?: 0L }

    // Alokasi Hari Ini (dihitung dari Pemasukan Bersih Hari Ini menggunakan PembagianKalkulator)
    val alokasiHariIni: Flow<PembagianKalkulator.HasilPembagian> =
        pemasukanBersihHariIni.map { bersih ->
            PembagianKalkulator.hitung(bersih)
        }

    // 7. Transaksi Terbaru (Gabungan Pemasukan & Pengeluaran)
    val transaksiTerbaru: Flow<List<TransaksiUiModel>> = combine(
        repository.allPemasukan,
        repository.allPengeluaran
    ) { listPemasukan, listPengeluaran ->
        val items = mutableListOf<TransaksiUiModel>()
        val numFormat = java.text.NumberFormat.getNumberInstance(Locale("id", "ID"))

        listPemasukan.forEach {
            val bersih = if (it.pemasukanBersih > 0.0) it.pemasukanBersih else (it.nominal - it.bensin).coerceAtLeast(0.0)
            val subjudulText = "Pemasukan: Rp${numFormat.format(it.nominal.toLong())} • Bensin: Rp${numFormat.format(it.bensin.toLong())} • Bersih: Rp${numFormat.format(bersih.toLong())}"

            items.add(
                TransaksiUiModel(
                    id = it.id,
                    judul = "${it.sumber} (${it.jumlahOrder} Order)",
                    subjudul = subjudulText,
                    nominal = it.nominal,
                    isPemasukan = true,
                    badge = "Pemasukan",
                    tanggal = it.tanggal,
                    bensin = it.bensin,
                    pemasukanBersih = bersih,
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
            }

            items.add(
                TransaksiUiModel(
                    id = it.id,
                    judul = it.kategori,
                    subjudul = subjudulText,
                    nominal = it.nominal,
                    isPemasukan = false,
                    badge = it.kategori,
                    tanggal = it.tanggal,
                    catatan = it.catatan
                )
            )
        }

        // Urutkan transaksi terbaru berdasarkan tanggal dan ID secara menurun (terbaru di atas)
        items.asSequence()
            .sortedWith(compareByDescending<TransaksiUiModel> { it.tanggal }.thenByDescending { it.id })
            .take(10)
            .toList()
    }

}

/**
 * Factory untuk membuat instance DashboardViewModel dengan dependency repository.
 */
class DashboardViewModelFactory(
    private val repository: DriverFinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
