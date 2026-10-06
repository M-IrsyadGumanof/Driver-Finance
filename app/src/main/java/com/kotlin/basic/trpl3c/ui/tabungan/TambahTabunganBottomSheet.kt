package com.kotlin.basic.trpl3c.ui.tabungan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kotlin.basic.trpl3c.R
import com.kotlin.basic.trpl3c.databinding.BottomSheetTambahTabunganBinding

/**
 * BottomSheetDialogFragment untuk menambahkan saldo tabungan secara manual ke pos tertentu.
 */
class TambahTabunganBottomSheet(
    private val viewModel: TabunganViewModel
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTambahTabunganBinding? = null
    private val binding get() = _binding!!

    private val daftarJenis = listOf("Tabungan Utama", "Dana Motor", "Dana Darurat")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTambahTabunganBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdown()
        setupListeners()
    }

    private fun setupDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, daftarJenis)
        binding.actvJenisTabunganTambah.setAdapter(adapter)
        binding.actvJenisTabunganTambah.setText(daftarJenis[0], false)

        binding.actvJenisTabunganTambah.setOnItemClickListener { _, _, _, _ ->
            binding.tilJenisTabunganTambah.error = null
        }
    }

    private fun setupListeners() {
        binding.btnKonfirmasiTambah.setOnClickListener {
            val jenis = binding.actvJenisTabunganTambah.text.toString().trim()
            val nominalStr = binding.etNominalTambah.text.toString().trim()
            val catatan = binding.etCatatanTambah.text.toString().trim()

            var isValid = true

            if (jenis.isBlank()) {
                binding.tilJenisTabunganTambah.error = getString(R.string.err_jenis_tabungan_kosong)
                isValid = false
            } else {
                binding.tilJenisTabunganTambah.error = null
            }

            val nominal = nominalStr.toDoubleOrNull()
            if (nominalStr.isEmpty()) {
                binding.tilNominalTambah.error = "Nominal tabungan tidak boleh kosong"
                isValid = false
            } else if (nominal == null || nominal <= 0) {
                binding.tilNominalTambah.error = "Nominal harus lebih dari 0"
                isValid = false
            } else {
                binding.tilNominalTambah.error = null
            }

            if (!isValid) return@setOnClickListener

            viewModel.tambahTabungan(
                jenis = jenis,
                nominal = nominal!!,
                catatan = catatan,
                onSuccess = {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.msg_tambah_tabungan_sukses),
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                },
                onError = { pesanError ->
                    binding.tilNominalTambah.error = pesanError
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TambahTabunganBottomSheet"
        fun newInstance(viewModel: TabunganViewModel) = TambahTabunganBottomSheet(viewModel)
    }
}
