package com.kotlin.basic.trpl3c.ui.pemasukan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kotlin.basic.trpl3c.data.PembagianKalkulator
import com.kotlin.basic.trpl3c.data.entity.Pemasukan
import com.kotlin.basic.trpl3c.data.entity.Tabungan
import com.kotlin.basic.trpl3c.data.repository.DriverFinanceRepository
import kotlinx.coroutines.launch

/**
 * ViewModel untuk menangani proses bisnis penyimpanan pemasukan ke Room Database.
 *
 * Setelah pemasukan disimpan, secara otomatis:
 * - Menghitung pembagian menggunakan PembagianKalkulator.
 * - Menyimpan bagian TABUNGAN ke tabel tabungan (sebagai setoran "Masuk").
 * - Bagian Motor, Bensin, Jajan, Dana Bebas TIDAK dicatat sebagai pengeluaran
 *   karena itu hanya alokasi anggaran, bukan uang yang sudah dibelanjakan.
 */
class PemasukanViewModel(
    private val repository: DriverFinanceRepository,
) : ViewModel() {

    fun simpanPemasukanDanAlokasi(pemasukan: Pemasukan, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Simpan pemasukan dan alokasikan ke 5 kategori via repository transaction
            repository.simpanPemasukanDenganAlokasi(pemasukan)
            // Callback sukses (Toast & finish di Activity)
            onSuccess()
        }
    }
}

class PemasukanViewModelFactory(
    private val repository: DriverFinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PemasukanViewModel::class.java)) {
            return PemasukanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
