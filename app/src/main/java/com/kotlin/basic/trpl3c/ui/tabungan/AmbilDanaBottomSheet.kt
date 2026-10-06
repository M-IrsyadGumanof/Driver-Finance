package com.kotlin.basic.trpl3c.ui.tabungan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kotlin.basic.trpl3c.R
import com.kotlin.basic.trpl3c.databinding.BottomSheetAmbilDanaBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * BottomSheetDialogFragment untuk fitur "Ambil Dana" dari pos Tabungan.
 * Mengimplementasikan validasi saldo, alasan penarikan, dan pencatatan transaksi keluar ke Room.
 */
class AmbilDanaBottomSheet(
    private val viewModel: TabunganViewModel
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAmbilDanaBinding? = null
    private val binding get() = _binding!!

    private val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))

    private val daftarJenis = listOf("Tabungan Utama", "Dana Motor", "Dana Darurat")
    private val daftarAlasan = listOf(
        "Perbaikan motor",
        "Kebutuhan mendesak",
        "Kuliah",
        "Keluarga",
        "Lainnya"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAmbilDanaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdowns()
        setupListeners()
    }

    private fun setupDropdowns() {
        // Dropdown Jenis Tabungan
        val jenisAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, daftarJenis)
        binding.actvJenisTabungan.setAdapter(jenisAdapter)

        // Default pilih pos pertama
        binding.actvJenisTabungan.setText(daftarJenis[0], false)
        updateInfoSaldo(daftarJenis[0])

        binding.actvJenisTabungan.setOnItemClickListener { _, _, position, _ ->
            val jenisDipilih = daftarJenis[position]
            updateInfoSaldo(jenisDipilih)
            binding.tilJenisTabungan.error = null
        }

        // Dropdown Alasan Penarikan
        val alasanAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, daftarAlasan)
        binding.actvAlasan.setAdapter(alasanAdapter)
        binding.actvAlasan.setText(daftarAlasan[0], false)

        binding.actvAlasan.setOnItemClickListener { _, _, _, _ ->
            binding.tilAlasan.error = null
        }
    }

    private fun updateInfoSaldo(jenis: String) {
        val saldo = when (jenis) {
            "Dana Motor" -> viewModel.saldoMotor.value
            "Dana Darurat" -> viewModel.saldoDarurat.value
            else -> viewModel.saldoUtama.value
        }
        binding.tvSaldoTersediaInfo.text = getString(R.string.label_saldo_tersedia_format, "Rp${formatter.format(saldo)}")
    }

    private fun setupListeners() {
        binding.btnKonfirmasiAmbil.setOnClickListener {
            val jenis = binding.actvJenisTabungan.text.toString().trim()
            val nominalStr = binding.etNominalAmbil.text.toString().trim()
            val alasan = binding.actvAlasan.text.toString().trim()
            val catatanTambahan = binding.etCatatanAmbil.text.toString().trim()

            var isValid = true

            // Validasi Jenis Tabungan
            if (jenis.isBlank()) {
                binding.tilJenisTabungan.error = getString(R.string.err_jenis_tabungan_kosong)
                isValid = false
            } else {
                binding.tilJenisTabungan.error = null
            }

            // Validasi Alasan
            if (alasan.isBlank()) {
                binding.tilAlasan.error = getString(R.string.err_alasan_kosong)
                isValid = false
            } else {
                binding.tilAlasan.error = null
            }

            // Validasi Nominal
            val nominal = nominalStr.toDoubleOrNull()
            if (nominalStr.isEmpty()) {
                binding.tilNominalAmbil.error = "Nominal penarikan tidak boleh kosong"
                isValid = false
            } else if (nominal == null || nominal <= 0) {
                binding.tilNominalAmbil.error = "Nominal harus lebih dari 0"
                isValid = false
            } else {
                // Validasi saldo tabungan yang dipilih
                val saldoSaatIni = when (jenis) {
                    "Dana Motor" -> viewModel.saldoMotor.value
                    "Dana Darurat" -> viewModel.saldoDarurat.value
                    else -> viewModel.saldoUtama.value
                }
                if (nominal > saldoSaatIni) {
                    binding.tilNominalAmbil.error = getString(
                        R.string.err_saldo_kurang,
                        "Rp${formatter.format(saldoSaatIni)}"
                    )
                    isValid = false
                } else {
                    binding.tilNominalAmbil.error = null
                }
            }

            if (!isValid) return@setOnClickListener

            // Eksekusi penarikan dana melalui ViewModel
            viewModel.ambilDana(
                jenis = jenis,
                nominal = nominal!!,
                alasan = alasan,
                catatanTambahan = catatanTambahan,
                onSuccess = {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.msg_ambil_dana_sukses),
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                },
                onError = { pesanError ->
                    binding.tilNominalAmbil.error = pesanError
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AmbilDanaBottomSheet"
        fun newInstance(viewModel: TabunganViewModel) = AmbilDanaBottomSheet(viewModel)
    }
}
