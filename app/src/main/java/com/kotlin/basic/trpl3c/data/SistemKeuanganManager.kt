package com.kotlin.basic.trpl3c.data

import java.text.NumberFormat
import java.util.Locale

/**
 * Manajer logika bisnis sistem keuangan Driver Finance:
 * 1. Alokasi pemasukan ke 5 pos persentase dengan tipe Long (Rupiah bulat aman).
 * 2. Sistem fallback prioritas pengeluaran.
 * 3. Validasi kekurangan dana agar saldo tidak pernah negatif.
 * 4. Isolasi saldo Tabungan dari pengeluaran biasa.
 */
object SistemKeuanganManager {

    // Nama Kategori Baku
    const val KATEGORI_TABUNGAN = "Tabungan"
    const val KATEGORI_MOTOR = "Motor"
    const val KATEGORI_KELUARGA = "Keluarga"
    const val KATEGORI_KEHIDUPAN = "Kehidupan Sehari-hari"
    const val KATEGORI_DANA_BEBAS = "Dana Bebas"

    val SEMUA_KATEGORI = listOf(
        KATEGORI_TABUNGAN,
        KATEGORI_MOTOR,
        KATEGORI_KELUARGA,
        KATEGORI_KEHIDUPAN,
        KATEGORI_DANA_BEBAS
    )

    // Kategori yang diizinkan untuk formulir pengeluaran biasa
    val KATEGORI_PENGELUARAN = listOf(
        KATEGORI_MOTOR,
        KATEGORI_KELUARGA,
        KATEGORI_KEHIDUPAN,
        KATEGORI_DANA_BEBAS
    )

    // Persentase Alokasi Pemasukan Bersih:
    // Tabungan = 20%, Motor = 15%, Keluarga = 10%, Kehidupan Sehari-hari = 45%, Dana Bebas = 10% (Total = 100%)
    const val PERSEN_TABUNGAN = 20
    const val PERSEN_MOTOR = 15
    const val PERSEN_KELUARGA = 10
    const val PERSEN_KEHIDUPAN = 45
    const val PERSEN_DANA_BEBAS = 10

    data class AlokasiPemasukan(
        val tabungan: Long,
        val motor: Long,
        val keluarga: Long,
        val kehidupan: Long,
        val danaBebas: Long
    )

    data class PotonganSumber(
        val kategori: String,
        val nominal: Long
    )

    data class HasilPengeluaran(
        val berhasil: Boolean,
        val potonganList: List<PotonganSumber> = emptyList(),
        val sumberDanaString: String = "",
        val pesanError: String? = null,
        val saldoKategoriDipilih: Long = 0L,
        val kekuranganDana: Long = 0L
    )

    private val numberFormatter = NumberFormat.getNumberInstance(Locale("id", "ID"))

    fun formatRupiah(nominal: Long): String {
        return numberFormatter.format(nominal)
    }

    /**
     * Menghitung alokasi pemasukan bersih ke 5 pos secara matematis aman tanpa pembulatan desimal merugikan.
     * Total semua alokasi selalu persis 100% sama dengan nominalPemasukanBersih.
     */
    fun hitungAlokasiPemasukanBersih(nominalPemasukanBersih: Long): AlokasiPemasukan {
        val tabungan = (nominalPemasukanBersih * PERSEN_TABUNGAN) / 100
        val motor = (nominalPemasukanBersih * PERSEN_MOTOR) / 100
        val keluarga = (nominalPemasukanBersih * PERSEN_KELUARGA) / 100
        val kehidupan = (nominalPemasukanBersih * PERSEN_KEHIDUPAN) / 100
        val danaBebas = (nominalPemasukanBersih * PERSEN_DANA_BEBAS) / 100

        // Menjamin total alokasi pas 100% dari nominal pemasukan bersih
        val selisih = nominalPemasukanBersih - (tabungan + motor + keluarga + kehidupan + danaBebas)
        val danaBebasFinal = danaBebas + selisih

        return AlokasiPemasukan(
            tabungan = tabungan,
            motor = motor,
            keluarga = keluarga,
            kehidupan = kehidupan,
            danaBebas = danaBebasFinal
        )
    }

    /**
     * Kompatibilitas untuk pemanggilan fungsi lama, mengasumsikan nominal yang masuk adalah pemasukan bersih.
     */
    fun hitungAlokasiPemasukan(nominal: Long): AlokasiPemasukan {
        return hitungAlokasiPemasukanBersih(nominal)
    }

    /**
     * Menormalisasi input teks kategori menjadi salah satu kategori baku.
     */
    fun normalisasiKategori(kategori: String): String {
        val normal = kategori.trim()
        return when {
            normal.equals(KATEGORI_MOTOR, ignoreCase = true) -> KATEGORI_MOTOR
            normal.equals(KATEGORI_KELUARGA, ignoreCase = true) -> KATEGORI_KELUARGA
            normal.contains("Kehidupan", ignoreCase = true) ||
                    normal.contains("Kebutuhan", ignoreCase = true) ||
                    normal.equals("Jajan", ignoreCase = true) -> KATEGORI_KEHIDUPAN
            normal.equals(KATEGORI_DANA_BEBAS, ignoreCase = true) -> KATEGORI_DANA_BEBAS
            normal.equals(KATEGORI_TABUNGAN, ignoreCase = true) -> KATEGORI_TABUNGAN
            else -> normal
        }
    }

    /**
     * Memproses pemotongan pengeluaran berdasarkan isolasi saldo mandiri per kategori:
     * - Setiap kategori berdiri sendiri.
     * - TIDAK mengambil dari kategori lain jika saldo tidak cukup.
     * - Tabungan TIDAK dijadikan sumber pengeluaran biasa.
     * - Jika saldo kurang, transaksi ditolak dengan pesan: "Saldo [Kategori] tidak mencukupi."
     */
    fun prosesPengeluaran(
        kategoriDipilih: String,
        nominalPengeluaran: Long,
        saldoMap: Map<String, Long>
    ): HasilPengeluaran {
        if (nominalPengeluaran <= 0L) {
            return HasilPengeluaran(
                berhasil = false,
                pesanError = "Nominal pengeluaran harus lebih dari 0"
            )
        }

        val kategoriBaku = normalisasiKategori(kategoriDipilih)

        // Tabungan tidak boleh digunakan untuk pengeluaran biasa
        if (kategoriBaku == KATEGORI_TABUNGAN) {
            return HasilPengeluaran(
                berhasil = false,
                pesanError = "Tabungan tidak dapat digunakan untuk pengeluaran biasa."
            )
        }

        val saldoKategori = (saldoMap[kategoriBaku] ?: 0L).coerceAtLeast(0L)

        if (saldoKategori < nominalPengeluaran) {
            val kekurangan = nominalPengeluaran - saldoKategori
            return HasilPengeluaran(
                berhasil = false,
                pesanError = "Saldo $kategoriBaku tidak mencukupi.",
                saldoKategoriDipilih = saldoKategori,
                kekuranganDana = kekurangan
            )
        }

        // Saldo mencukupi -> potong HANYA kategori yang dipilih
        val potonganList = listOf(
            PotonganSumber(kategori = kategoriBaku, nominal = nominalPengeluaran)
        )
        val sumberDanaString = "${kategoriBaku}: Rp${formatRupiah(nominalPengeluaran)}"

        return HasilPengeluaran(
            berhasil = true,
            potonganList = potonganList,
            sumberDanaString = sumberDanaString,
            saldoKategoriDipilih = saldoKategori - nominalPengeluaran,
            kekuranganDana = 0L
        )
    }
}
