package com.parassidhu.pdfpin

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.parassidhu.pdfpin.databinding.RowLayoutBinding

class DataAdapter(
    private val context: Context,
    private val listItems: List<ListItem>
) : RecyclerView.Adapter<DataAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = listItems.size

    inner class ViewHolder(private val binding: RowLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            val item = listItems[position]
            binding.filename.text = item.name
            binding.edit.setOnClickListener { showRenameDialog(item) }
        }

        private fun showRenameDialog(item: ListItem) {
            val input = EditText(context).apply {
                setText(item.name)
                inputType = InputType.TYPE_CLASS_TEXT
                setSelection(text.length)
            }

            AlertDialog.Builder(context)
                .setTitle("Rename")
                .setView(input)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val updatedName = input.text?.toString()?.trim().orEmpty()
                    if (updatedName.isNotEmpty()) {
                        item.name = updatedName
                        binding.filename.text = updatedName
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
}
