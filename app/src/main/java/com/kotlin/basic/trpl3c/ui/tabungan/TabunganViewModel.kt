package com.kotlin.basic.trpl3c.ui.tabungan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kotlin.basic.trpl3c.data.entity.Tabungan
import com.kotlin.basic.trpl3c.data.repository.DriverFinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel untuk mengelola data dan operasi Halaman Tabungan Driver Finance.
 * Menyediakan saldo per kategori, progres target Rp5.000.000, serta logika Ambil Dana dan Tambah Tabungan.
 */
class TabunganViewModel(
    private val repository: DriverFinanceRepository
) : ViewModel() {

    // Target default tabungan sesuai spesifikasi
    val targetTabungan: Double = 5_000_000.0

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))

    // 1. Total Saldo Semua Tabungan (StateFlow)
    val totalSaldo: StateFlow<Double> = repository.totalSaldoSemuaTabungan
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // 2. Saldo Pos: Tabungan Utama
    val saldoUtama: StateFlow<Double> = repository.getSaldoByJenis("Tabungan Utama")
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // 3. Saldo Pos: Dana Motor
    val saldoMotor: StateFlow<Double> = repository.getSaldoByJenis("Dana Motor")
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // 4. Saldo Pos: Dana Darurat
    val saldoDarurat: StateFlow<Double> = repository.getSaldoByJenis("Dana Darurat")
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // 5. Riwayat Seluruh Mutasi Tabungan (Masuk & Keluar)
    val riwayatTabungan: Flow<List<Tabungan>> = repository.allTabungan

    /**
     * Menambahkan saldo tabungan secara manual ke pos tertentu (Tipe: "Masuk").
     */
    fun tambahTabungan(
        jenis: String,
        nominal: Double,
        catatan: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (nominal <= 0) {
            onError("Nominal harus lebih dari 0")
            return
        }

        viewModelScope.launch {
            val tanggalHariIni = dateFormat.format(Date())
            val keterangan = if (catatan.isNotBlank()) catatan else "Setoran manual $jenis"
            val tabunganBaru = Tabungan(
                tanggal = tanggalHariIni,
                jenis = jenis,
                nominal = nominal,
                tipeTransaksi = "Masuk",
                catatan = keterangan
            )

            repository.tambahDanaTabungan(tabunganBaru)
            onSuccess()
        }
    }

    /**
     * Menarik dana dari pos tabungan (Tipe: "Keluar").
     * Validasi:
     * - Nominal harus > 0.
     * - Nominal tidak boleh lebih besar dari saldo tabungan yang dipilih.
     */
    fun ambilDana(
        jenis: String,
        nominal: Double,
        alasan: String,
        catatanTambahan: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (nominal <= 0) {
            onError("Nominal harus lebih dari 0")
            return
        }

        viewModelScope.launch {
            // Ambil saldo terbaru dari pos yang dipilih untuk validasi langsung dari Room
            val saldoSaatIni = repository.getSaldoByJenis(jenis).first() ?: 0.0

            if (nominal > saldoSaatIni) {
                onError("Nominal melebihi saldo tersedia (Rp${formatter.format(saldoSaatIni)})")
                return@launch
            }

            val tanggalHariIni = dateFormat.format(Date())
            val alasanLengkap = if (catatanTambahan.isNotBlank()) {
                "$alasan: $catatanTambahan"
            } else {
                alasan
            }

            val transaksiKeluar = Tabungan(
                tanggal = tanggalHariIni,
                jenis = jenis,
                nominal = nominal,
                tipeTransaksi = "Keluar",
                catatan = alasanLengkap
            )

            repository.ambilDanaTabungan(transaksiKeluar)
            onSuccess()
        }
    }
}

class TabunganViewModelFactory(
    private val repository: DriverFinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TabunganViewModel::class.java)) {
            return TabunganViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
