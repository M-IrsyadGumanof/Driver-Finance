package com.kotlin.basic.trpl3c.ui.pengeluaran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kotlin.basic.trpl3c.data.SistemKeuanganManager
import com.kotlin.basic.trpl3c.data.entity.Pengeluaran
import com.kotlin.basic.trpl3c.data.repository.DriverFinanceRepository
import kotlinx.coroutines.launch

/**
 * ViewModel untuk menangani proses bisnis penyimpanan pengeluaran ke Room Database.
 *
 * CATATAN PENTING:
 * Pengeluaran adalah uang yang BENAR-BENAR sudah dibelanjakan.
 * Ini BERBEDA dari alokasi anggaran (Pembagian Uang di Dashboard).
 *
 * Contoh:
 * - Alokasi Bensin = Rp30.000 (anggaran, belum tentu dipakai)
 * - Pengeluaran Bensin = Rp28.000 (nyata, ketika benar-benar isi bensin)
 */
class PengeluaranViewModel(
    private val repository: DriverFinanceRepository,
) : ViewModel() {

    fun simpanPengeluaran(
        pengeluaran: Pengeluaran,
        onSuccess: (SistemKeuanganManager.HasilPengeluaran) -> Unit,
        onErrorKurangSaldo: (SistemKeuanganManager.HasilPengeluaran) -> Unit
    ) {
        viewModelScope.launch {
            val hasil = repository.simpanPengeluaranDenganFallback(pengeluaran)
            if (hasil.berhasil) {
                onSuccess(hasil)
            } else {
                onErrorKurangSaldo(hasil)
            }
        }
    }

    fun simpanPengeluaran(pengeluaran: Pengeluaran, onSuccess: () -> Unit) {
        simpanPengeluaran(pengeluaran, onSuccess = { onSuccess() }, onErrorKurangSaldo = {})
    }
}

class PengeluaranViewModelFactory(
    private val repository: DriverFinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PengeluaranViewModel::class.java)) {
            return PengeluaranViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
