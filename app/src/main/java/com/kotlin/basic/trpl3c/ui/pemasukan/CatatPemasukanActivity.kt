package com.kotlin.basic.trpl3c.ui.pemasukan

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.kotlin.basic.trpl3c.R
import com.kotlin.basic.trpl3c.data.AppDatabase
import com.kotlin.basic.trpl3c.data.entity.Pemasukan
import com.kotlin.basic.trpl3c.data.repository.DriverFinanceRepository
import com.kotlin.basic.trpl3c.databinding.ActivityCatatPemasukanBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity untuk formulir pencatatan pemasukan harian driver ShopeeFood.
 */
class CatatPemasukanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCatatPemasukanBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val viewModel: PemasukanViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repository = DriverFinanceRepository(
            database.pemasukanDao(),
            database.pengeluaranDao(),
            database.tabunganDao(),
            database.saldoKategoriDao(),
            database
        )
        PemasukanViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCatatPemasukanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDefaultValues()
        setupDateTimePickers()
        setupSimpanButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupDefaultValues() {
        // Tanggal default hari ini
        binding.etTanggal.setText(dateFormat.format(Date()))

        // Sumber default ShopeeFood
        if (binding.etSumber.text.isNullOrBlank()) {
            binding.etSumber.setText("ShopeeFood")
        }

        // Jam mulai & selesai default
        if (binding.etJamMulai.text.isNullOrBlank()) {
            binding.etJamMulai.setText("08:00")
        }
        if (binding.etJamSelesai.text.isNullOrBlank()) {
            binding.etJamSelesai.setText("14:00")
        }
    }

    private fun setupDateTimePickers() {
        // Pemilih Tanggal
        binding.etTanggal.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                binding.etTanggal.setText(dateFormat.format(calendar.time))
            }, year, month, day).show()
        }

        // Pemilih Jam Mulai
        binding.etJamMulai.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                val timeString = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                binding.etJamMulai.setText(timeString)
            }, 8, 0, true).show()
        }

        // Pemilih Jam Selesai
        binding.etJamSelesai.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                val timeString = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                binding.etJamSelesai.setText(timeString)
            }, 14, 0, true).show()
        }
    }

    private fun setupSimpanButton() {
        binding.btnSimpanPemasukan.setOnClickListener {
            val nominalText = binding.etNominal.text.toString().trim()
            val bensinText = binding.etBensin.text.toString().trim()

            // 1. Validasi pemasukan tidak boleh kosong
            if (nominalText.isEmpty()) {
                binding.tilNominal.error = getString(R.string.err_nominal_kosong)
                binding.etNominal.requestFocus()
                return@setOnClickListener
            }

            val nominal = nominalText.toDoubleOrNull()
            if (nominal == null || nominal < 0.0) {
                binding.tilNominal.error = getString(R.string.err_nominal_invalid)
                binding.etNominal.requestFocus()
                return@setOnClickListener
            }
            binding.tilNominal.error = null

            // 2. Validasi nominal bensin (boleh Rp0 jika tidak isi bensin)
            val bensin = if (bensinText.isEmpty()) 0.0 else bensinText.toDoubleOrNull()
            if (bensin == null || bensin < 0.0) {
                binding.tilBensin.error = getString(R.string.err_bensin_invalid)
                binding.etBensin.requestFocus()
                return@setOnClickListener
            }

            // 4. Pastikan bensin tidak lebih besar dari pemasukan
            if (bensin > nominal) {
                binding.tilBensin.error = getString(R.string.err_bensin_lebih_besar)
                binding.etBensin.requestFocus()
                Toast.makeText(this, getString(R.string.err_bensin_lebih_besar), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.tilBensin.error = null

            // 5. Hitung Pemasukan Bersih
            val pemasukanBersih = nominal - bensin

            val tanggal = binding.etTanggal.text.toString().trim().ifBlank {
                dateFormat.format(Date())
            }
            val sumber = binding.etSumber.text.toString().trim().ifBlank {
                "ShopeeFood"
            }
            val jumlahOrder = binding.etJumlahOrder.text.toString().trim().toIntOrNull() ?: 0
            val jamMulai = binding.etJamMulai.text.toString().trim().ifBlank { "08:00" }
            val jamSelesai = binding.etJamSelesai.text.toString().trim().ifBlank { "14:00" }
            val catatan = binding.etCatatan.text.toString().trim()

            // Buat objek entitas Pemasukan lengkap dengan bensin dan pemasukan bersih
            val pemasukan = Pemasukan(
                tanggal = tanggal,
                sumber = sumber,
                nominal = nominal,
                bensin = bensin,
                pemasukanBersih = pemasukanBersih,
                jumlahOrder = jumlahOrder,
                jamMulai = jamMulai,
                jamSelesai = jamSelesai,
                catatan = catatan
            )

            // Simpan ke Room Database dan hitung alokasi otomatis dari pemasukan bersih
            viewModel.simpanPemasukanDanAlokasi(pemasukan) {
                Toast.makeText(this, getString(R.string.msg_pemasukan_sukses), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

}
