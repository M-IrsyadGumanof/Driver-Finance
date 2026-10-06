package com.kotlin.basic.trpl3c.data

/**
 * Konfigurasi dan fondasi Room Database untuk aplikasi Driver Finance.
 *
 * Catatan untuk mahasiswa:
 * Class RoomDatabase akan diimplementasikan setelah Entity dan DAO
 * dibuat pada tahap berikutnya agar tidak terjadi error kompilasi karena entitas kosong:
 *
 * @Database(entities = [Pemasukan::class, ...], version = 1, exportSchema = false)
 * abstract class AppDatabase : RoomDatabase() { ... }
 */
object DatabaseConfig {
    const val DATABASE_NAME = "driver_finance_database"
}
