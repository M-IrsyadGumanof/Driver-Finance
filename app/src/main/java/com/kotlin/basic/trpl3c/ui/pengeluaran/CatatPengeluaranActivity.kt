package com.kotlin.basic.trpl3c.ui.pengeluaran

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.kotlin.basic.trpl3c.R
import com.kotlin.basic.trpl3c.data.AppDatabase
import com.kotlin.basic.trpl3c.data.entity.Pengeluaran
import com.kotlin.basic.trpl3c.data.repository.DriverFinanceRepository
import com.kotlin.basic.trpl3c.databinding.ActivityCatatPengeluaranBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity untuk formulir pencatatan pengeluaran nyata driver ShopeeFood.
 *
 * PENTING: Pengeluaran di sini adalah uang yang BENAR-BENAR sudah dibelanjakan.
 * Ini BERBEDA dari alokasi anggaran (Pembagian Uang) di Dashboard.
 *
 * Contoh penggunaan:
 * - Kategori: Bensin, Nominal: Rp28.000, Catatan: "Isi bensin sebelum narik"
 * - Kategori: Jajan, Nominal: Rp15.000, Catatan: "Makan siang warteg"
 */
class CatatPengeluaranActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCatatPengeluaranBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Daftar kategori pengeluaran yang tersedia (Tabungan & Bensin tidak termasuk)
    private val daftarKategori = listOf(
        "Motor",
        "Keluarga",
        "Kehidupan Sehari-hari",
        "Dana Bebas"
    )


    private val viewModel: PengeluaranViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repository = DriverFinanceRepository(
            database.pemasukanDao(),
            database.pengeluaranDao(),
            database.tabunganDao(),
            database.saldoKategoriDao(),
            database
        )
        PengeluaranViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCatatPengeluaranBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDefaultValues()
        setupKategoriDropdown()
        setupDatePicker()
        setupSimpanButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupDefaultValues() {
        // Tanggal default: hari ini
        binding.etTanggal.setText(dateFormat.format(Date()))
    }

    /**
     * Mengisi dropdown kategori menggunakan ArrayAdapter.
     * ExposedDropdownMenu membutuhkan AutoCompleteTextView sebagai input-nya.
     */
    private fun setupKategoriDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            daftarKategori
        )
        binding.actvKategori.setAdapter(adapter)

        // Pastikan dropdown tidak bisa diketik manual, hanya bisa dipilih
        binding.actvKategori.setOnClickListener {
            binding.actvKategori.showDropDown()
        }
    }

    private fun setupDatePicker() {
        binding.etTanggal.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                binding.etTanggal.setText(dateFormat.format(calendar.time))
            }, year, month, day).show()
        }
    }

    private fun setupSimpanButton() {
        binding.btnSimpanPengeluaran.setOnClickListener {
            if (validasiInput()) {
                simpanKeDatabase()
            }
        }
    }

    /**
     * Validasi semua input wajib sebelum menyimpan.
     * @return true jika semua input valid, false jika ada yang kosong/tidak valid.
     */
    private fun validasiInput(): Boolean {
        var isValid = true

        // Validasi Nominal
        val nominalText = binding.etNominal.text.toString().trim()
        if (nominalText.isEmpty()) {
            binding.tilNominal.error = getString(R.string.err_nominal_pengeluaran_kosong)
            binding.etNominal.requestFocus()
            isValid = false
        } else {
            val nominal = nominalText.toDoubleOrNull()
            if (nominal == null || nominal <= 0.0) {
                binding.tilNominal.error = getString(R.string.err_nominal_pengeluaran_invalid)
                binding.etNominal.requestFocus()
                isValid = false
            } else {
                binding.tilNominal.error = null
            }
        }

        // Validasi Kategori (harus dipilih dari dropdown atau yang valid)
        val kategori = binding.actvKategori.text.toString().trim()
        if (kategori.isEmpty()) {
            binding.tilKategori.error = getString(R.string.err_kategori_kosong)
            if (isValid) binding.actvKategori.requestFocus()
            isValid = false
        } else {
            binding.tilKategori.error = null
        }

        return isValid
    }

    /**
     * Membuat objek Pengeluaran dari input form dan memproses penyimpanan dengan sistem fallback.
     */
    private fun simpanKeDatabase() {
        val nominal = binding.etNominal.text.toString().trim().toDouble()
        val tanggal = binding.etTanggal.text.toString().trim().ifBlank {
            dateFormat.format(Date())
        }
        val kategori = binding.actvKategori.text.toString().trim()
        val catatan = binding.etCatatan.text.toString().trim()

        val pengeluaran = Pengeluaran(
            tanggal = tanggal,
            kategori = kategori,
            nominal = nominal,
            catatan = catatan
        )

        viewModel.simpanPengeluaran(
            pengeluaran = pengeluaran,
            onSuccess = { hasil ->
                val pesan = if (hasil.potonganList.size > 1) {
                    "Pengeluaran disimpan!\nSumber dana: ${hasil.sumberDanaString}"
                } else {
                    getString(R.string.msg_pengeluaran_sukses)
                }
                Toast.makeText(this, pesan, Toast.LENGTH_LONG).show()
                finish() // Kembali ke Dashboard
            },
            onErrorKurangSaldo = { hasil ->
                tampilkanDialogSaldoKurang(kategori, hasil)
            }
        )
    }

    /**
     * Menampilkan dialog peringatan lengkap jika dana tidak mencukupi sesuai aturan prioritas fallback.
     */
    private fun tampilkanDialogSaldoKurang(
        kategori: String,
        hasil: com.kotlin.basic.trpl3c.data.SistemKeuanganManager.HasilPengeluaran
    ) {
        val format = { nominal: Long ->
            com.kotlin.basic.trpl3c.data.SistemKeuanganManager.formatRupiah(nominal)
        }

        val pesanUtama = hasil.pesanError ?: "Saldo $kategori tidak mencukupi."
        Toast.makeText(this, pesanUtama, Toast.LENGTH_SHORT).show()

        val pesan = buildString {
            append("$pesanUtama\n\n")
            append("• Saldo $kategori saat ini: Rp${format(hasil.saldoKategoriDipilih)}\n")
            append("• Kekurangan: Rp${format(hasil.kekuranganDana)}\n\n")
            append("Sistem tidak mengambil uang dari kategori lain karena setiap kategori memiliki saldo terpisah.")
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Saldo Tidak Mencukupi")
            .setMessage(pesan)
            .setPositiveButton("Mengerti") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}
