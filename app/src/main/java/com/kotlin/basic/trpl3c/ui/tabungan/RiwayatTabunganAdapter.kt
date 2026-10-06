package com.kotlin.basic.trpl3c.ui.tabungan

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kotlin.basic.trpl3c.R
import com.kotlin.basic.trpl3c.data.entity.Tabungan
import com.kotlin.basic.trpl3c.databinding.ItemRiwayatTabunganBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter untuk menampilkan mutasi riwayat tabungan (Masuk & Keluar) pada RecyclerView.
 */
class RiwayatTabunganAdapter : ListAdapter<Tabungan, RiwayatTabunganAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatTabunganBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemRiwayatTabunganBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))

        fun bind(item: Tabungan) {
            val context = binding.root.context
            val isMasuk = item.tipeTransaksi.equals("Masuk", ignoreCase = true)

            binding.tvTanggal.text = item.tanggal
            binding.tvBadgeJenisTabungan.text = item.jenis

            // Set styling badge kategori pos tabungan
            when (item.jenis) {
                "Dana Motor" -> {
                    binding.tvBadgeJenisTabungan.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.dana_motor_light))
                    binding.tvBadgeJenisTabungan.setTextColor(
                        ContextCompat.getColor(context, R.color.dana_motor)
                    )
                }
                "Dana Darurat" -> {
                    binding.tvBadgeJenisTabungan.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.dana_darurat_light))
                    binding.tvBadgeJenisTabungan.setTextColor(
                        ContextCompat.getColor(context, R.color.dana_darurat)
                    )
                }
                else -> { // "Tabungan Utama"
                    binding.tvBadgeJenisTabungan.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.tabungan_utama_light))
                    binding.tvBadgeJenisTabungan.setTextColor(
                        ContextCompat.getColor(context, R.color.tabungan_utama)
                    )
                }
            }

            // Atur keterangan alasan / catatan
            val deskripsi = if (item.catatan.isNotBlank()) {
                item.catatan
            } else {
                if (isMasuk) "Setoran tabungan" else "Penarikan dana"
            }
            binding.tvAlasanCatatan.text = deskripsi

            // Atur styling Masuk vs Keluar
            if (isMasuk) {
                binding.layoutIconContainer.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.income_container))
                binding.ivTipeTransaksi.setImageResource(R.drawable.ic_income)
                binding.tvNominal.text = "+Rp${formatter.format(item.nominal)}"
                binding.tvNominal.setTextColor(
                    ContextCompat.getColor(context, R.color.income_green)
                )
            } else {
                binding.layoutIconContainer.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.expense_container))
                binding.ivTipeTransaksi.setImageResource(R.drawable.ic_withdraw)
                binding.tvNominal.text = "-Rp${formatter.format(item.nominal)}"
                binding.tvNominal.setTextColor(
                    ContextCompat.getColor(context, R.color.expense_red)
                )
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Tabungan>() {
            override fun areItemsTheSame(oldItem: Tabungan, newItem: Tabungan): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Tabungan, newItem: Tabungan): Boolean {
                return oldItem == newItem
            }
        }
    }
}
