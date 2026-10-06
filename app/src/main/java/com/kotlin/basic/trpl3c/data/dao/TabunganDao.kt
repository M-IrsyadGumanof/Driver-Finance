package com.kotlin.basic.trpl3c.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kotlin.basic.trpl3c.data.entity.Tabungan
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) untuk tabel tabungan dan dana darurat.
 */
@Dao
interface TabunganDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tabungan: Tabungan): Long

    @Update
    suspend fun update(tabungan: Tabungan)

    @Delete
    suspend fun delete(tabungan: Tabungan)

    @Query("SELECT * FROM tabungan ORDER BY tanggal DESC, id DESC")
    fun getAllTabungan(): Flow<List<Tabungan>>

    @Query("SELECT * FROM tabungan WHERE jenis = :jenis ORDER BY tanggal DESC, id DESC")
    fun getTabunganByJenis(jenis: String): Flow<List<Tabungan>>

    @Query("SELECT SUM(nominal) FROM tabungan WHERE jenis = :jenis AND tipeTransaksi = 'Masuk'")
    fun getTotalMasukByJenis(jenis: String): Flow<Double?>

    @Query("SELECT SUM(nominal) FROM tabungan WHERE jenis = :jenis AND tipeTransaksi = 'Keluar'")
    fun getTotalKeluarByJenis(jenis: String): Flow<Double?>

    /**
     * Menghitung saldo bersih (Masuk - Keluar) untuk pos tabungan tertentu.
     */
    @Query("""
        SELECT (
            COALESCE(SUM(CASE WHEN tipeTransaksi = 'Masuk' THEN nominal ELSE 0 END), 0) -
            COALESCE(SUM(CASE WHEN tipeTransaksi = 'Keluar' THEN nominal ELSE 0 END), 0)
        )
        FROM tabungan
        WHERE jenis = :jenis
    """)
    fun getSaldoByJenis(jenis: String): Flow<Double?>

    /**
     * Menghitung total saldo seluruh pos tabungan dan dana darurat.
     */
    @Query("""
        SELECT (
            COALESCE(SUM(CASE WHEN tipeTransaksi = 'Masuk' THEN nominal ELSE 0 END), 0) -
            COALESCE(SUM(CASE WHEN tipeTransaksi = 'Keluar' THEN nominal ELSE 0 END), 0)
        )
        FROM tabungan
    """)
    fun getTotalSaldoSemua(): Flow<Double?>
}
