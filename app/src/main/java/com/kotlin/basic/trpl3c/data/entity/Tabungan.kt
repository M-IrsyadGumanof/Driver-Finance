package com.kotlin.basic.trpl3c.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas untuk mengelola tabungan dan dana darurat driver.
 *
 * Jenis Tabungan:
 * - Tabungan Utama
 * - Dana Motor
 * - Dana Darurat
 *
 * Tipe Transaksi:
 * - Masuk  (Setor / Simpan)
 * - Keluar (Tarik / Pakai saat darurat)
 */
@Entity(tableName = "tabungan")
data class Tabungan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tanggal: String,          // Format: YYYY-MM-DD
    val jenis: String,            // "Tabungan Utama", "Dana Motor", "Dana Darurat"
    val nominal: Double,          // Nominal uang yang disimpan atau ditarik
    val tipeTransaksi: String,    // "Masuk" atau "Keluar"
    val catatan: String = ""      // Keterangan transaksi
)
