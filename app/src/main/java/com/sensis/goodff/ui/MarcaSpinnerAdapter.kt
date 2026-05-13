package com.sensis.goodff.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.sensis.goodff.R

class MarcaSpinnerAdapter(ctx: Context, items: List<String>) :
    ArrayAdapter<String>(ctx, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_spinner_marca, parent, false)
        val tv = view.findViewById<TextView>(R.id.tvMarca)
        tv.text = getItem(position)
        return view
    }
}
