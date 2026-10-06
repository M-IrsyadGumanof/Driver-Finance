package com.kotlin.basic.trpl3c.ui.dashboard

/**
 * Model presentasi untuk baris transaksi terbaru di Dashboard.
 * Menggabungkan entitas Pemasukan maupun Pengeluaran secara seragam.
 */
data class TransaksiUiModel(
    val id: Int,
    val judul: String,
    val subjudul: String,
    val nominal: Double,
    val isPemasukan: Boolean,
    val badge: String,
    val tanggal: String,
    val bensin: Double = 0.0,
    val pemasukanBersih: Double = 0.0,
    val catatan: String = ""
)

