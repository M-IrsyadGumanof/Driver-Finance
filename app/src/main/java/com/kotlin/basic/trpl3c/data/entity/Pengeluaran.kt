package com.kotlin.basic.trpl3c.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas untuk mencatat pengeluaran operasional maupun pribadi driver.
 *
 * Kategori yang tersedia:
 * - Bensin
 * - Motor (servis, oli, tambal ban)
 * - Jajan
 * - Kuliah
 * - Kebutuhan Rumah
 * - Lainnya
 */
@Entity(tableName = "pengeluaran")
data class Pengeluaran(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tanggal: String,       // Format: YYYY-MM-DD
    val kategori: String,      // "Bensin", "Motor", "Jajan", "Kuliah", "Kebutuhan Rumah", "Lainnya"
    val nominal: Double,       // Nominal biaya pengeluaran
    val catatan: String = "",  // Keterangan detail pengeluaran
    val sumberDana: String = "" // Rincian sumber dana (misal: "Bensin: Rp20.000 • Dana Bebas: Rp5.000")
)
