package com.kotlin.basic.trpl3c.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kotlin.basic.trpl3c.data.entity.SaldoKategori
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) untuk mengelola saldo per kategori keuangan.
 */
@Dao
interface SaldoKategoriDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(saldoKategori: SaldoKategori)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(list: List<SaldoKategori>)

    @Query("SELECT * FROM saldo_kategori")
    fun getAllSaldoKategori(): Flow<List<SaldoKategori>>

    @Query("SELECT * FROM saldo_kategori")
    suspend fun getAllSaldoKategoriSync(): List<SaldoKategori>

    @Query("SELECT saldo FROM saldo_kategori WHERE kategori = :kategori LIMIT 1")
    fun getSaldoByKategori(kategori: String): Flow<Long?>

    @Query("SELECT saldo FROM saldo_kategori WHERE kategori = :kategori LIMIT 1")
    suspend fun getSaldoByKategoriSync(kategori: String): Long?

    @Query("UPDATE saldo_kategori SET saldo = :saldoBaru WHERE kategori = :kategori")
    suspend fun setSaldo(kategori: String, saldoBaru: Long)

    @Query("UPDATE saldo_kategori SET saldo = saldo + :tambahSaldo WHERE kategori = :kategori")
    suspend fun tambahSaldo(kategori: String, tambahSaldo: Long)

    @Query("UPDATE saldo_kategori SET saldo = saldo - :kurangSaldo WHERE kategori = :kategori")
    suspend fun kurangiSaldo(kategori: String, kurangSaldo: Long)
}
