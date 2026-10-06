package com.kotlin.basic.trpl3c.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kotlin.basic.trpl3c.data.entity.Pemasukan
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) untuk tabel pemasukan.
 */
@Dao
interface PemasukanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pemasukan: Pemasukan): Long

    @Update
    suspend fun update(pemasukan: Pemasukan)

    @Delete
    suspend fun delete(pemasukan: Pemasukan)

    @Query("SELECT * FROM pemasukan ORDER BY tanggal DESC, id DESC")
    fun getAllPemasukan(): Flow<List<Pemasukan>>

    @Query("SELECT * FROM pemasukan WHERE tanggal = :tanggal ORDER BY id DESC")
    fun getPemasukanByTanggal(tanggal: String): Flow<List<Pemasukan>>

    @Query("SELECT SUM(nominal) FROM pemasukan")
    fun getTotalPemasukan(): Flow<Double?>

    @Query("SELECT SUM(nominal) FROM pemasukan WHERE tanggal BETWEEN :startDate AND :endDate")
    fun getTotalPemasukanRange(startDate: String, endDate: String): Flow<Double?>

    @Query("SELECT SUM(bensin) FROM pemasukan")
    fun getTotalBensin(): Flow<Double?>

    @Query("SELECT SUM(bensin) FROM pemasukan WHERE tanggal BETWEEN :startDate AND :endDate")
    fun getTotalBensinRange(startDate: String, endDate: String): Flow<Double?>

    @Query("SELECT SUM(pemasukanBersih) FROM pemasukan")
    fun getTotalPemasukanBersih(): Flow<Double?>

    @Query("SELECT SUM(pemasukanBersih) FROM pemasukan WHERE tanggal BETWEEN :startDate AND :endDate")
    fun getTotalPemasukanBersihRange(startDate: String, endDate: String): Flow<Double?>

    @Query("SELECT SUM(jumlahOrder) FROM pemasukan")
    fun getTotalOrder(): Flow<Int?>

}
