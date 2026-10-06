package com.kotlin.basic.trpl3c.data.repository

import androidx.room.withTransaction
import com.kotlin.basic.trpl3c.data.AppDatabase
import com.kotlin.basic.trpl3c.data.SistemKeuanganManager
import com.kotlin.basic.trpl3c.data.dao.PemasukanDao
import com.kotlin.basic.trpl3c.data.dao.PengeluaranDao
import com.kotlin.basic.trpl3c.data.dao.SaldoKategoriDao
import com.kotlin.basic.trpl3c.data.dao.TabunganDao
import com.kotlin.basic.trpl3c.data.entity.Pemasukan
import com.kotlin.basic.trpl3c.data.entity.Pengeluaran
import com.kotlin.basic.trpl3c.data.entity.SaldoKategori
import com.kotlin.basic.trpl3c.data.entity.Tabungan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Repository yang mengelola akses data lokal Room Database untuk aplikasi Driver Finance.
 * Berperan sebagai jembatan antara DAO (Data Layer) dan ViewModel / UI Layer.
 */
class DriverFinanceRepository(
    private val pemasukanDao: PemasukanDao,
    private val pengeluaranDao: PengeluaranDao,
    private val tabunganDao: TabunganDao,
    private val saldoKategoriDao: SaldoKategoriDao? = null,
    private val appDatabase: AppDatabase? = null
) {

    // ==========================================
    // OPERASI SALDO KATEGORI
    // ==========================================

    val allSaldoKategori: Flow<List<SaldoKategori>> =
        saldoKategoriDao?.getAllSaldoKategori() ?: flowOf(emptyList())

    val saldoKategoriMap: Flow<Map<String, Long>> = allSaldoKategori.map { list ->
        val map = list.associate { it.kategori to it.saldo }.toMutableMap()
        SistemKeuanganManager.SEMUA_KATEGORI.forEach { kategori ->
            if (!map.containsKey(kategori)) {
                map[kategori] = 0L
            }
        }
        map
    }

    /**
     * Mengambil map saldo kategori saat ini secara sinkron (untuk operasi transaksi).
     * Jika tabel belum diinisialisasi, inisialisasi default 0.
     */
    suspend fun getSaldoMapSync(): Map<String, Long> {
        val dao = saldoKategoriDao ?: return emptyMap()
        val list = dao.getAllSaldoKategoriSync()
        val map = list.associate { it.kategori to it.saldo }.toMutableMap()

        var adaYangDiinisialisasi = false
        SistemKeuanganManager.SEMUA_KATEGORI.forEach { kategori ->
            if (!map.containsKey(kategori)) {
                dao.insertOrUpdate(SaldoKategori(kategori, 0L))
                map[kategori] = 0L
                adaYangDiinisialisasi = true
            }
        }
        return map
    }

    // ==========================================
    // OPERASI PEMASUKAN & ALOKASI
    // ==========================================

    val allPemasukan: Flow<List<Pemasukan>> = pemasukanDao.getAllPemasukan()
    val totalPemasukan: Flow<Double?> = pemasukanDao.getTotalPemasukan()
    val totalBensin: Flow<Double?> = pemasukanDao.getTotalBensin()
    val totalPemasukanBersih: Flow<Double?> = pemasukanDao.getTotalPemasukanBersih()
    val totalOrder: Flow<Int?> = pemasukanDao.getTotalOrder()

    suspend fun insertPemasukan(pemasukan: Pemasukan): Long {
        return pemasukanDao.insert(pemasukan)
    }

    /**
     * Mencatat pemasukan driver ShopeeFood:
     * 1. Hitung Pemasukan Bersih = Pemasukan (Kotor) - Bensin.
     * 2. Bagi Pemasukan Bersih ke 5 kategori:
     *    - Tabungan = 20%
     *    - Motor = 15%
     *    - Keluarga = 10%
     *    - Kehidupan Sehari-hari = 45%
     *    - Dana Bebas = 10%
     * 3. Tambahkan saldo ke masing-masing kategori secara konsisten via Database Transaction.
     * 4. Bensin TIDAK menjadi saldo kategori, hanya dicatat sebagai pengurang pemasukan.
     */
    suspend fun simpanPemasukanDenganAlokasi(pemasukan: Pemasukan): Long {
        val kotorLong = pemasukan.nominal.toLong()
        val bensinLong = pemasukan.bensin.toLong()
        val bersihLong = (kotorLong - bensinLong).coerceAtLeast(0L)
        val alokasi = SistemKeuanganManager.hitungAlokasiPemasukanBersih(bersihLong)

        val pemasukanFinal = pemasukan.copy(
            nominal = kotorLong.toDouble(),
            bensin = bensinLong.toDouble(),
            pemasukanBersih = bersihLong.toDouble()
        )

        val runInTransaction: suspend () -> Long = {
            // 1. Simpan pemasukan
            val id = pemasukanDao.insert(pemasukanFinal)

            // 2. Pastikan tabel saldo_kategori terisi
            getSaldoMapSync()

            // 3. Tambahkan saldo ke masing-masing kategori
            saldoKategoriDao?.tambahSaldo(SistemKeuanganManager.KATEGORI_TABUNGAN, alokasi.tabungan)
            saldoKategoriDao?.tambahSaldo(SistemKeuanganManager.KATEGORI_MOTOR, alokasi.motor)
            saldoKategoriDao?.tambahSaldo(SistemKeuanganManager.KATEGORI_KELUARGA, alokasi.keluarga)
            saldoKategoriDao?.tambahSaldo(SistemKeuanganManager.KATEGORI_KEHIDUPAN, alokasi.kehidupan)
            saldoKategoriDao?.tambahSaldo(SistemKeuanganManager.KATEGORI_DANA_BEBAS, alokasi.danaBebas)

            // 4. Catat bagian Tabungan ke tabel tabungan untuk sinkronisasi fitur Tabungan (20% bersih)
            val tabunganEntry = Tabungan(
                tanggal = pemasukan.tanggal,
                jenis = "Tabungan Utama",
                nominal = alokasi.tabungan.toDouble(),
                tipeTransaksi = "Masuk",
                catatan = "Alokasi otomatis 20% dari ${pemasukan.sumber}: Rp${SistemKeuanganManager.formatRupiah(bersihLong)}"
            )
            tabunganDao.insert(tabunganEntry)

            id
        }

        return if (appDatabase != null) {
            appDatabase.withTransaction { runInTransaction() }
        } else {
            runInTransaction()
        }
    }

    suspend fun updatePemasukan(pemasukan: Pemasukan) {
        pemasukanDao.update(pemasukan)
    }

    suspend fun deletePemasukan(pemasukan: Pemasukan) {
        pemasukanDao.delete(pemasukan)
    }

    fun getPemasukanByTanggal(tanggal: String): Flow<List<Pemasukan>> {
        return pemasukanDao.getPemasukanByTanggal(tanggal)
    }

    fun getTotalPemasukanRange(startDate: String, endDate: String): Flow<Double?> {
        return pemasukanDao.getTotalPemasukanRange(startDate, endDate)
    }

    fun getTotalBensinRange(startDate: String, endDate: String): Flow<Double?> {
        return pemasukanDao.getTotalBensinRange(startDate, endDate)
    }

    fun getTotalPemasukanBersihRange(startDate: String, endDate: String): Flow<Double?> {
        return pemasukanDao.getTotalPemasukanBersihRange(startDate, endDate)
    }


    // ==========================================
    // OPERASI PENGELUARAN & FALLBACK
    // ==========================================

    val allPengeluaran: Flow<List<Pengeluaran>> = pengeluaranDao.getAllPengeluaran()
    val totalPengeluaran: Flow<Double?> = pengeluaranDao.getTotalPengeluaran()

    suspend fun insertPengeluaran(pengeluaran: Pengeluaran): Long {
        return pengeluaranDao.insert(pengeluaran)
    }

    /**
     * Memproses pengeluaran dengan sistem prioritas fallback:
     * - Bensin: Bensin -> Dana Bebas -> Kebutuhan Hidup
     * - Motor: Motor -> Dana Bebas -> Kebutuhan Hidup
     * - Kebutuhan Hidup: Kebutuhan Hidup
     * - Dana Bebas: Dana Bebas
     *
     * JANGAN memotong Tabungan!
     * Jika saldo tidak mencukupi, transaksi tidak disimpan dan mengembalikan HasilPengeluaran (gagal).
     */
    suspend fun simpanPengeluaranDenganFallback(pengeluaran: Pengeluaran): SistemKeuanganManager.HasilPengeluaran {
        val nominalLong = pengeluaran.nominal.toLong()
        val saldoMap = getSaldoMapSync()

        val hasil = SistemKeuanganManager.prosesPengeluaran(
            kategoriDipilih = pengeluaran.kategori,
            nominalPengeluaran = nominalLong,
            saldoMap = saldoMap
        )

        if (!hasil.berhasil) {
            return hasil
        }

        val runInTransaction: suspend () -> SistemKeuanganManager.HasilPengeluaran = {
            // 1. Kurangi saldo kategori yang dipakai
            for (potongan in hasil.potonganList) {
                saldoKategoriDao?.kurangiSaldo(potongan.kategori, potongan.nominal)
            }

            // 2. Simpan entitas pengeluaran beserta string rincian sumber dana
            val pengeluaranWithSumber = pengeluaran.copy(
                sumberDana = hasil.sumberDanaString
            )
            pengeluaranDao.insert(pengeluaranWithSumber)

            hasil
        }

        return if (appDatabase != null) {
            appDatabase.withTransaction { runInTransaction() }
        } else {
            runInTransaction()
        }
    }

    suspend fun updatePengeluaran(pengeluaran: Pengeluaran) {
        pengeluaranDao.update(pengeluaran)
    }

    suspend fun deletePengeluaran(pengeluaran: Pengeluaran) {
        pengeluaranDao.delete(pengeluaran)
    }

    fun getPengeluaranByTanggal(tanggal: String): Flow<List<Pengeluaran>> {
        return pengeluaranDao.getPengeluaranByTanggal(tanggal)
    }

    fun getPengeluaranByKategori(kategori: String): Flow<List<Pengeluaran>> {
        return pengeluaranDao.getPengeluaranByKategori(kategori)
    }

    fun getTotalPengeluaranRange(startDate: String, endDate: String): Flow<Double?> {
        return pengeluaranDao.getTotalPengeluaranRange(startDate, endDate)
    }

    // ==========================================
    // OPERASI TABUNGAN & DANA DARURAT
    // ==========================================

    val allTabungan: Flow<List<Tabungan>> = tabunganDao.getAllTabungan()
    val totalSaldoSemuaTabungan: Flow<Double?> = tabunganDao.getTotalSaldoSemua()

    suspend fun insertTabungan(tabungan: Tabungan): Long {
        return tabunganDao.insert(tabungan)
    }

    /**
     * Menangani penarikan dana tabungan manual, sekaligus menyinkronkan saldo Tabungan di saldo_kategori.
     */
    suspend fun ambilDanaTabungan(tabungan: Tabungan): Long {
        val runInTransaction: suspend () -> Long = {
            val id = tabunganDao.insert(tabungan)
            val nominalLong = tabungan.nominal.toLong()
            if (tabungan.jenis == "Tabungan Utama") {
                saldoKategoriDao?.kurangiSaldo(SistemKeuanganManager.KATEGORI_TABUNGAN, nominalLong)
            }
            id
        }

        return if (appDatabase != null) {
            appDatabase.withTransaction { runInTransaction() }
        } else {
            runInTransaction()
        }
    }

    suspend fun tambahDanaTabungan(tabungan: Tabungan): Long {
        val runInTransaction: suspend () -> Long = {
            val id = tabunganDao.insert(tabungan)
            val nominalLong = tabungan.nominal.toLong()
            if (tabungan.jenis == "Tabungan Utama") {
                saldoKategoriDao?.tambahSaldo(SistemKeuanganManager.KATEGORI_TABUNGAN, nominalLong)
            }
            id
        }

        return if (appDatabase != null) {
            appDatabase.withTransaction { runInTransaction() }
        } else {
            runInTransaction()
        }
    }

    suspend fun updateTabungan(tabungan: Tabungan) {
        tabunganDao.update(tabungan)
    }

    suspend fun deleteTabungan(tabungan: Tabungan) {
        tabunganDao.delete(tabungan)
    }

    fun getTabunganByJenis(jenis: String): Flow<List<Tabungan>> {
        return tabunganDao.getTabunganByJenis(jenis)
    }

    fun getSaldoByJenis(jenis: String): Flow<Double?> {
        return tabunganDao.getSaldoByJenis(jenis)
    }
}

