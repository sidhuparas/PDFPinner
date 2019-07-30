package com.parassidhu.pdfpin

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.yarolegovich.lovelydialog.LovelyTextInputDialog
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.row_layout.*
import java.util.*

class DataAdapter(private val context: Context, private val listItems: ArrayList<ListItem>)
    : RecyclerView.Adapter<DataAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): DataAdapter.ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.row_layout, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(i)
    }

    override fun getItemCount(): Int {
        return listItems.size
    }

    inner class ViewHolder(override val containerView: View?)
        : RecyclerView.ViewHolder(containerView!!), LayoutContainer {

        fun bind(i: Int) {
            filename.text = listItems[i].name
            edit.setOnClickListener {
                LovelyTextInputDialog(context)
                        .setTopColorRes(R.color.blue)
                        .setTitle("Rename")
                        .setInitialInput(filename.text.toString())
                        .setConfirmButton(android.R.string.ok) { text ->
                            if (!text.isEmpty()) {
                                listItems[adapterPosition].name = text
                                filename.text = listItems[adapterPosition].name
                            }
                        }
                        .show()
            }
        }
    }
}