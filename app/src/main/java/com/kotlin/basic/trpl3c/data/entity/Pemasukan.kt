package com.kotlin.basic.trpl3c.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas untuk mencatat pemasukan harian driver dari ShopeeFood.
 */
@Entity(tableName = "pemasukan")
data class Pemasukan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tanggal: String,               // Format: YYYY-MM-DD, misal: "2026-09-20"
    val sumber: String = "ShopeeFood", // Default sumber: "ShopeeFood"
    val nominal: Double,               // Pemasukan kotor harian (ShopeeFood)
    val bensin: Double = 0.0,          // Biaya bensin hari ini (tidak menjadi saldo kategori)
    val pemasukanBersih: Double = 0.0, // Pemasukan bersih = nominal - bensin
    val jumlahOrder: Int,              // Jumlah trip / pesanan yang diselesaikan
    val jamMulai: String,              // Jam mulai on-bid, misal: "08:00"
    val jamSelesai: String,            // Jam selesai on-bid, misal: "14:00"
    val catatan: String = ""           // Catatan opsional
) {
    val pemasukan: Double get() = nominal
}

