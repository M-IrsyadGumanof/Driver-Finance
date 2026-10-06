package com.kotlin.basic.trpl3c.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kotlin.basic.trpl3c.R
import com.kotlin.basic.trpl3c.databinding.ItemTransaksiTerbaruBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter RecyclerView untuk menampilkan daftar transaksi terbaru (Pemasukan & Pengeluaran).
 */
class TransaksiAdapter : ListAdapter<TransaksiUiModel, TransaksiAdapter.TransaksiViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransaksiViewHolder {
        val binding = ItemTransaksiTerbaruBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransaksiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransaksiViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransaksiViewHolder(
        private val binding: ItemTransaksiTerbaruBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransaksiUiModel) {
            val context = binding.root.context
            val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))

            binding.tvTransaksiJudul.text = item.judul
            binding.tvTransaksiSubjudul.text = item.subjudul
            binding.tvTransaksiBadge.text = item.badge

            if (item.isPemasukan) {
                binding.tvTransaksiNominal.text = "+Rp${formatter.format(item.nominal)}"
                binding.tvTransaksiNominal.setTextColor(
                    ContextCompat.getColor(context, R.color.income_green)
                )
                binding.ivTransaksiIcon.setImageResource(R.drawable.ic_income)
                binding.ivTransaksiIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.on_primary)
                )
                binding.iconContainer.backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.income_green)
                binding.tvTransaksiBadge.backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.income_container)
                binding.tvTransaksiBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.income_dark)
                )
            } else {
                binding.tvTransaksiNominal.text = "-Rp${formatter.format(item.nominal)}"
                binding.tvTransaksiNominal.setTextColor(
                    ContextCompat.getColor(context, R.color.expense_red)
                )
                binding.ivTransaksiIcon.setImageResource(R.drawable.ic_expense)
                binding.ivTransaksiIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.on_primary)
                )
                binding.iconContainer.backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.expense_red)
                binding.tvTransaksiBadge.backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.expense_container)
                binding.tvTransaksiBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.expense_dark)
                )
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TransaksiUiModel>() {
        override fun areItemsTheSame(oldItem: TransaksiUiModel, newItem: TransaksiUiModel): Boolean {
            return oldItem.id == newItem.id && oldItem.isPemasukan == newItem.isPemasukan
        }

        override fun areContentsTheSame(oldItem: TransaksiUiModel, newItem: TransaksiUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
