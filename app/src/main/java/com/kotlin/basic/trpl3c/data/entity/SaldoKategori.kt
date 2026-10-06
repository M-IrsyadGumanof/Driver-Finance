package com.kotlin.basic.trpl3c.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas untuk menyimpan saldo aktual tiap kategori keuangan.
 *
 * Kategori yang dikelola:
 * - "Tabungan" (15%)
 * - "Motor" (10%)
 * - "Bensin" (25%)
 * - "Kebutuhan Hidup" (40%)
 * - "Dana Bebas" (10%)
 *
 * Nominal disimpan sebagai Long (Rupiah bulat) untuk menjamin keamanan presisi uang.
 */
@Entity(tableName = "saldo_kategori")
data class SaldoKategori(
    @PrimaryKey
    val kategori: String,
    val saldo: Long = 0L
)
