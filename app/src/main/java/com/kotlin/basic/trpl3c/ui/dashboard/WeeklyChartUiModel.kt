package com.kotlin.basic.trpl3c.ui.dashboard

/**
 * Model data harian untuk satu batang (bar) pada grafik mingguan.
 *
 * @param labelHari Nama hari dalam Bahasa Indonesia (contoh: "Senin", "Selasa", dll.)
 * @param tanggal Tanggal transaksi format YYYY-MM-DD
 * @param nominal Total akumulasi nominal pemasukan pada hari tersebut
 */
data class DailyIncomeItem(
    val labelHari: String,
    val tanggal: String,
    val nominal: Double
)

/**
 * Model UI untuk menyajikan data grafik mingguan beserta agregat ringkasannya.
 */
data class WeeklyChartUiModel(
    val dataHarian: List<DailyIncomeItem>,
    val totalMingguIni: Double,
    val rataRataPerHari: Double,
    val pemasukanTertinggi: Double,
    val hasData: Boolean
)
