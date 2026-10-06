package com.kotlin.basic.trpl3c.data

/**
 * Kalkulator pembagian uang otomatis untuk driver ShopeeFood.
 *
 * Berisi persentase alokasi default dan fungsi perhitungan.
 * Jika ingin mengubah persentase, cukup ubah nilai di bagian KONFIGURASI ini.
 *
 * PENTING: Total semua persentase harus selalu = 100 (1.0)
 */
object PembagianKalkulator {

    // ============================================================
    // KONFIGURASI PERSENTASE ALOKASI (100% Total)
    // ============================================================
    const val PERSEN_TABUNGAN   = 0.20  // 20% → masuk ke saldo tabungan
    const val PERSEN_MOTOR      = 0.15  // 15% → anggaran perawatan motor
    const val PERSEN_KELUARGA   = 0.10  // 10% → uang untuk keluarga
    const val PERSEN_KEHIDUPAN  = 0.45  // 45% → kehidupan sehari-hari
    const val PERSEN_DANA_BEBAS = 0.10  // 10% → dana bebas / keperluan lainnya
    // Total: 20 + 15 + 10 + 45 + 10 = 100%

    // ============================================================
    // DATA CLASS: Hasil Pembagian
    // ============================================================
    /**
     * Menyimpan hasil perhitungan pembagian nominal pemasukan bersih ke setiap pos.
     */
    data class HasilPembagian(
        val tabungan: Double,
        val motor: Double,
        val keluarga: Double,
        val kehidupan: Double,
        val danaBebas: Double
    )

    data class HasilPembagianLong(
        val tabungan: Long,
        val motor: Long,
        val keluarga: Long,
        val kehidupan: Long,
        val danaBebas: Long
    )

    // ============================================================
    // FUNGSI PERHITUNGAN UTAMA
    // ============================================================
    /**
     * Menghitung pembagian nominal pemasukan bersih ke semua pos alokasi (Double).
     *
     * @param nominalPemasukanBersih Total nominal pemasukan bersih (setelah dikurangi bensin).
     * @return [HasilPembagian] berisi nominal untuk setiap pos keuangan.
     */
    fun hitung(nominalPemasukanBersih: Double): HasilPembagian {
        return HasilPembagian(
            tabungan  = nominalPemasukanBersih * PERSEN_TABUNGAN,
            motor     = nominalPemasukanBersih * PERSEN_MOTOR,
            keluarga  = nominalPemasukanBersih * PERSEN_KELUARGA,
            kehidupan = nominalPemasukanBersih * PERSEN_KEHIDUPAN,
            danaBebas = nominalPemasukanBersih * PERSEN_DANA_BEBAS
        )
    }

    /**
     * Menghitung pembagian dengan presisi Long (Rupiah bulat aman).
     * Menjamin jumlah total pos pas 100% persis sama dengan nominalPemasukanBersih.
     */
    fun hitungLong(nominalPemasukanBersih: Long): HasilPembagianLong {
        val tabungan = (nominalPemasukanBersih * 20) / 100
        val motor = (nominalPemasukanBersih * 15) / 100
        val keluarga = (nominalPemasukanBersih * 10) / 100
        val kehidupan = (nominalPemasukanBersih * 45) / 100
        val danaBebas = nominalPemasukanBersih - (tabungan + motor + keluarga + kehidupan)
        return HasilPembagianLong(
            tabungan = tabungan,
            motor = motor,
            keluarga = keluarga,
            kehidupan = kehidupan,
            danaBebas = danaBebas
        )
    }

    /**
     * Mengembalikan persentase masing-masing pos dalam format yang mudah dibaca.
     * Berguna untuk menampilkan label persentase pada Dashboard.
     */
    fun persenTabunganLabel()  = "${(PERSEN_TABUNGAN * 100).toInt()}%"
    fun persenMotorLabel()     = "${(PERSEN_MOTOR * 100).toInt()}%"
    fun persenKeluargaLabel()  = "${(PERSEN_KELUARGA * 100).toInt()}%"
    fun persenKehidupanLabel() = "${(PERSEN_KEHIDUPAN * 100).toInt()}%"
    fun persenDanaBebasLabel() = "${(PERSEN_DANA_BEBAS * 100).toInt()}%"

}
