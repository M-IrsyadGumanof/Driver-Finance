package com.kotlin.basic.trpl3c

import com.kotlin.basic.trpl3c.data.SistemKeuanganManager
import com.kotlin.basic.trpl3c.data.SistemKeuanganManager.KATEGORI_BENSIN
import com.kotlin.basic.trpl3c.data.SistemKeuanganManager.KATEGORI_DANA_BEBAS
import com.kotlin.basic.trpl3c.data.SistemKeuanganManager.KATEGORI_KEBUTUHAN_HIDUP
import com.kotlin.basic.trpl3c.data.SistemKeuanganManager.KATEGORI_MOTOR
import com.kotlin.basic.trpl3c.data.SistemKeuanganManager.KATEGORI_TABUNGAN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk memverifikasi logika finansial Driver Finance sesuai spesifikasi:
 * - TEST 1: Pemasukan Rp150.000 (15% Tabungan, 10% Motor, 25% Bensin, 40% Kebutuhan, 10% Bebas)
 * - TEST 2: Pengeluaran Bensin Rp20.000 dari saldo Bensin cukup
 * - TEST 3: Pengeluaran Bensin Rp50.000 (Fallback Bensin -> Dana Bebas)
 * - TEST 4: Pengeluaran Bensin Rp40.000 (Fallback Bensin -> Dana Bebas -> Kebutuhan Hidup)
 * - TEST 5: Total dana tidak mencukupi (Saldo tidak negatif, peringatan "Saldo tidak mencukupi", Tabungan aman)
 * - Pengeluaran Motor (Motor -> Dana Bebas -> Kebutuhan Hidup)
 * - Pengeluaran Kebutuhan Hidup (Langsung dari Kebutuhan Hidup, tidak mengambil Dana Bebas)
 */
class SistemKeuanganTest {

    @Test
    fun test1_pemasukan150000() {
        val pemasukan = 150_000L
        val alokasi = SistemKeuanganManager.hitungAlokasiPemasukan(pemasukan)

        assertEquals("Tabungan harus 15% (Rp22.500)", 22_500L, alokasi.tabungan)
        assertEquals("Motor harus 10% (Rp15.000)", 15_000L, alokasi.motor)
        assertEquals("Bensin harus 25% (Rp37.500)", 37_500L, alokasi.bensin)
        assertEquals("Kebutuhan Hidup harus 40% (Rp60.000)", 60_000L, alokasi.kebutuhanHidup)
        assertEquals("Dana Bebas harus 10% (Rp15.000)", 15_000L, alokasi.danaBebas)

        val totalAlokasi = alokasi.tabungan + alokasi.motor + alokasi.bensin + alokasi.kebutuhanHidup + alokasi.danaBebas
        assertEquals("Total alokasi harus persis 100% (Rp150.000)", pemasukan, totalAlokasi)
    }

    @Test
    fun test2_pengeluaranBensin20000_saldoMencukupi() {
        // Saldo awal setelah pemasukan Rp150.000
        val saldoMap = mutableMapOf(
            KATEGORI_TABUNGAN to 22_500L,
            KATEGORI_MOTOR to 15_000L,
            KATEGORI_BENSIN to 37_500L,
            KATEGORI_KEBUTUHAN_HIDUP to 60_000L,
            KATEGORI_DANA_BEBAS to 15_000L
        )

        val hasil = SistemKeuanganManager.prosesPengeluaran(
            kategoriDipilih = "Bensin",
            nominalPengeluaran = 20_000L,
            saldoMap = saldoMap
        )

        assertTrue("Pengeluaran harus berhasil", hasil.berhasil)
        assertEquals("Hanya satu sumber yang digunakan", 1, hasil.potonganList.size)
        assertEquals(KATEGORI_BENSIN, hasil.potonganList[0].kategori)
        assertEquals(20_000L, hasil.potonganList[0].nominal)

        // Terapkan potongan
        hasil.potonganList.forEach {
            saldoMap[it.kategori] = saldoMap[it.kategori]!! - it.nominal
        }

        assertEquals("Bensin akhir = Rp17.500", 17_500L, saldoMap[KATEGORI_BENSIN])
        assertEquals("Dana Bebas akhir = Rp15.000 (tidak berkurang)", 15_000L, saldoMap[KATEGORI_DANA_BEBAS])
        assertEquals("Kebutuhan Hidup akhir = Rp60.000 (tidak berkurang)", 60_000L, saldoMap[KATEGORI_KEBUTUHAN_HIDUP])
        assertEquals("Tabungan tetap utuh Rp22.500", 22_500L, saldoMap[KATEGORI_TABUNGAN])
    }

    @Test
    fun test3_pengeluaranBensin50000_fallbackDanaBebas() {
        // Saldo awal: Bensin 37.500, Dana Bebas 15.000, Kebutuhan Hidup 60.000
        val saldoMap = mutableMapOf(
            KATEGORI_TABUNGAN to 22_500L,
            KATEGORI_MOTOR to 15_000L,
            KATEGORI_BENSIN to 37_500L,
            KATEGORI_KEBUTUHAN_HIDUP to 60_000L,
            KATEGORI_DANA_BEBAS to 15_000L
        )

        val hasil = SistemKeuanganManager.prosesPengeluaran(
            kategoriDipilih = "Bensin",
            nominalPengeluaran = 50_000L,
            saldoMap = saldoMap
        )

        assertTrue("Pengeluaran harus berhasil", hasil.berhasil)
        assertEquals("Harus menggunakan 2 sumber (Bensin dan Dana Bebas)", 2, hasil.potonganList.size)
        assertEquals(KATEGORI_BENSIN, hasil.potonganList[0].kategori)
        assertEquals(37_500L, hasil.potonganList[0].nominal)
        assertEquals(KATEGORI_DANA_BEBAS, hasil.potonganList[1].kategori)
        assertEquals(12_500L, hasil.potonganList[1].nominal)

        // Terapkan potongan
        hasil.potonganList.forEach {
            saldoMap[it.kategori] = saldoMap[it.kategori]!! - it.nominal
        }

        assertEquals("Bensin akhir = Rp0", 0L, saldoMap[KATEGORI_BENSIN])
        assertEquals("Dana Bebas akhir = Rp2.500", 2_500L, saldoMap[KATEGORI_DANA_BEBAS])
        assertEquals("Kebutuhan Hidup akhir = Rp60.000", 60_000L, saldoMap[KATEGORI_KEBUTUHAN_HIDUP])
        assertEquals("Tabungan tetap utuh Rp22.500", 22_500L, saldoMap[KATEGORI_TABUNGAN])
    }

    @Test
    fun test4_pengeluaranBensin40000_fallbackDanaBebasDanKebutuhanHidup() {
        // Saldo awal sesuai spesifikasi TEST 4:
        // Bensin = Rp20.000, Dana Bebas = Rp5.000, Kebutuhan Hidup = Rp50.000
        val saldoMap = mutableMapOf(
            KATEGORI_TABUNGAN to 22_500L,
            KATEGORI_MOTOR to 15_000L,
            KATEGORI_BENSIN to 20_000L,
            KATEGORI_KEBUTUHAN_HIDUP to 50_000L,
            KATEGORI_DANA_BEBAS to 5_000L
        )

        val hasil = SistemKeuanganManager.prosesPengeluaran(
            kategoriDipilih = "Bensin",
            nominalPengeluaran = 40_000L,
            saldoMap = saldoMap
        )

        assertTrue("Pengeluaran harus berhasil via fallback 3 tingkat", hasil.berhasil)
        assertEquals(3, hasil.potonganList.size)
        assertEquals(KATEGORI_BENSIN, hasil.potonganList[0].kategori)
        assertEquals(20_000L, hasil.potonganList[0].nominal)
        assertEquals(KATEGORI_DANA_BEBAS, hasil.potonganList[1].kategori)
        assertEquals(5_000L, hasil.potonganList[1].nominal)
        assertEquals(KATEGORI_KEBUTUHAN_HIDUP, hasil.potonganList[2].kategori)
        assertEquals(15_000L, hasil.potonganList[2].nominal)

        // Terapkan potongan
        hasil.potonganList.forEach {
            saldoMap[it.kategori] = saldoMap[it.kategori]!! - it.nominal
        }

        assertEquals("Bensin akhir = Rp0", 0L, saldoMap[KATEGORI_BENSIN])
        assertEquals("Dana Bebas akhir = Rp0", 0L, saldoMap[KATEGORI_DANA_BEBAS])
        assertEquals("Kebutuhan Hidup akhir = Rp35.000", 35_000L, saldoMap[KATEGORI_KEBUTUHAN_HIDUP])
        assertEquals("Tabungan tetap utuh Rp22.500", 22_500L, saldoMap[KATEGORI_TABUNGAN])
    }

    @Test
    fun test5_totalDanaTidakMencukupi_tidakBolehNegatif() {
        // Saldo: Bensin = Rp10.000, Dana Bebas = Rp5.000, Kebutuhan Hidup = Rp20.000 (Total = Rp35.000)
        // Tabungan = Rp50.000 (TIDAK boleh disentuh!)
        val saldoMap = mutableMapOf(
            KATEGORI_TABUNGAN to 50_000L,
            KATEGORI_MOTOR to 10_000L,
            KATEGORI_BENSIN to 10_000L,
            KATEGORI_KEBUTUHAN_HIDUP to 20_000L,
            KATEGORI_DANA_BEBAS to 5_000L
        )

        val pengeluaranBensin = 40_000L // Melebihi total dana operasional Rp35.000

        val hasil = SistemKeuanganManager.prosesPengeluaran(
            kategoriDipilih = "Bensin",
            nominalPengeluaran = pengeluaranBensin,
            saldoMap = saldoMap
        )

        assertFalse("Pengeluaran harus ditolak", hasil.berhasil)
        assertEquals("Pesan harus 'Saldo tidak mencukupi.'", "Saldo tidak mencukupi.", hasil.pesanError)
        assertEquals("Kekurangan dana = Rp5.000", 5_000L, hasil.kekuranganDana)
        assertEquals("Total dana tersedia = Rp35.000", 35_000L, hasil.totalDanaTersedia)
        assertEquals("Saldo Bensin dilaporkan = Rp10.000", 10_000L, hasil.saldoKategoriDipilih)
        assertEquals("Saldo Dana Bebas dilaporkan = Rp5.000", 5_000L, hasil.saldoDanaBebas)
        assertEquals("Saldo Kebutuhan Hidup dilaporkan = Rp20.000", 20_000L, hasil.saldoKebutuhanHidup)

        // Pastikan saldo di saldoMap tidak berubah sedikitpun
        assertEquals(10_000L, saldoMap[KATEGORI_BENSIN])
        assertEquals(5_000L, saldoMap[KATEGORI_DANA_BEBAS])
        assertEquals(20_000L, saldoMap[KATEGORI_KEBUTUHAN_HIDUP])
        assertEquals(50_000L, saldoMap[KATEGORI_TABUNGAN])
    }

    @Test
    fun test6_pengeluaranMotor_prioritasFallback() {
        // Saldo awal: Motor 10.000, Dana Bebas 5.000, Kebutuhan Hidup 50.000
        val saldoMap = mutableMapOf(
            KATEGORI_TABUNGAN to 30_000L,
            KATEGORI_MOTOR to 10_000L,
            KATEGORI_BENSIN to 20_000L,
            KATEGORI_KEBUTUHAN_HIDUP to 50_000L,
            KATEGORI_DANA_BEBAS to 5_000L
        )

        // Pengeluaran Motor = Rp20.000
        val hasil = SistemKeuanganManager.prosesPengeluaran(
            kategoriDipilih = "Motor",
            nominalPengeluaran = 20_000L,
            saldoMap = saldoMap
        )

        assertTrue(hasil.berhasil)
        // Motor dipakai 10k, Bebas 5k, Kebutuhan 5k
        hasil.potonganList.forEach {
            saldoMap[it.kategori] = saldoMap[it.kategori]!! - it.nominal
        }

        assertEquals("Motor = Rp0", 0L, saldoMap[KATEGORI_MOTOR])
        assertEquals("Dana Bebas = Rp0", 0L, saldoMap[KATEGORI_DANA_BEBAS])
        assertEquals("Kebutuhan Hidup = Rp45.000", 45_000L, saldoMap[KATEGORI_KEBUTUHAN_HIDUP])
        assertEquals("Bensin tidak tersentuh", 20_000L, saldoMap[KATEGORI_BENSIN])
        assertEquals("Tabungan tidak tersentuh", 30_000L, saldoMap[KATEGORI_TABUNGAN])
    }

    @Test
    fun test7_pengeluaranKebutuhanHidup_tidakMengambilDanaBebas() {
        val saldoMap = mutableMapOf(
            KATEGORI_TABUNGAN to 30_000L,
            KATEGORI_MOTOR to 10_000L,
            KATEGORI_BENSIN to 20_000L,
            KATEGORI_KEBUTUHAN_HIDUP to 50_000L,
            KATEGORI_DANA_BEBAS to 15_000L
        )

        // Pengeluaran Kebutuhan Hidup Sehari-hari = Rp60.000 (saldo hanya 50.000)
        // Aturan spesifikasi 8:
        // "Jangan mengambil Dana Bebas terlebih dahulu jika pengguna memang sedang menggunakan kategori Kebutuhan Hidup.
        // Jika saldo Kebutuhan Hidup tidak mencukupi, tampilkan peringatan saldo tidak mencukupi."
        val hasil = SistemKeuanganManager.prosesPengeluaran(
            kategoriDipilih = "Kebutuhan Hidup Sehari-hari",
            nominalPengeluaran = 60_000L,
            saldoMap = saldoMap
        )

        assertFalse("Harus gagal karena tidak boleh mengambil Dana Bebas", hasil.berhasil)
        assertEquals("Saldo tidak mencukupi.", hasil.pesanError)
        assertEquals(10_000L, hasil.kekuranganDana)
        assertEquals(50_000L, hasil.totalDanaTersedia)
    }

    @Test
    fun test8_pengeluaranDanaBebas_tidakMengambilTabungan() {
        val saldoMap = mutableMapOf(
            KATEGORI_TABUNGAN to 50_000L,
            KATEGORI_MOTOR to 10_000L,
            KATEGORI_BENSIN to 20_000L,
            KATEGORI_KEBUTUHAN_HIDUP to 50_000L,
            KATEGORI_DANA_BEBAS to 10_000L
        )

        // Pengeluaran Dana Bebas = Rp15.000 (saldo Dana Bebas hanya Rp10.000)
        // Aturan spesifikasi 9:
        // "Jangan mengambil uang dari Tabungan. Jika tidak cukup, tampilkan peringatan saldo tidak mencukupi."
        val hasil = SistemKeuanganManager.prosesPengeluaran(
            kategoriDipilih = "Dana Bebas",
            nominalPengeluaran = 15_000L,
            saldoMap = saldoMap
        )

        assertFalse("Harus gagal dan tidak memotong Tabungan", hasil.berhasil)
        assertEquals("Saldo tidak mencukupi.", hasil.pesanError)
        assertEquals(5_000L, hasil.kekuranganDana)
        assertEquals(50_000L, saldoMap[KATEGORI_TABUNGAN])
    }
}
