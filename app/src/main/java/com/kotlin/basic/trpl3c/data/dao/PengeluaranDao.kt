package com.kotlin.basic.trpl3c.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kotlin.basic.trpl3c.data.entity.Pengeluaran
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) untuk tabel pengeluaran.
 */
@Dao
interface PengeluaranDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pengeluaran: Pengeluaran): Long

    @Update
    suspend fun update(pengeluaran: Pengeluaran)

    @Delete
    suspend fun delete(pengeluaran: Pengeluaran)

    @Query("SELECT * FROM pengeluaran ORDER BY tanggal DESC, id DESC")
    fun getAllPengeluaran(): Flow<List<Pengeluaran>>

    @Query("SELECT * FROM pengeluaran WHERE tanggal = :tanggal ORDER BY id DESC")
    fun getPengeluaranByTanggal(tanggal: String): Flow<List<Pengeluaran>>

    @Query("SELECT * FROM pengeluaran WHERE kategori = :kategori ORDER BY tanggal DESC")
    fun getPengeluaranByKategori(kategori: String): Flow<List<Pengeluaran>>

    @Query("SELECT SUM(nominal) FROM pengeluaran")
    fun getTotalPengeluaran(): Flow<Double?>

    @Query("SELECT SUM(nominal) FROM pengeluaran WHERE tanggal BETWEEN :startDate AND :endDate")
    fun getTotalPengeluaranRange(startDate: String, endDate: String): Flow<Double?>
}
