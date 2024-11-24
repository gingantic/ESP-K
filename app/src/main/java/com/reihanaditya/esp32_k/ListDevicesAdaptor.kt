package com.reihanaditya.esp32_k

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class ListDevicesAdaptor(private val listDevice: List<Model.DeviceList>) : RecyclerView.Adapter<ListDevicesAdaptor.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_list_device, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listDevice.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.txtDevice.text = listDevice[position].device_id
        holder.txtStatus.text = listDevice[position].status
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtDevice: TextView = itemView.findViewById(R.id.txt_nama)
        val txtStatus: TextView = itemView.findViewById(R.id.txt_status)

        init {
            itemView.setOnClickListener { v: View  ->
                val position: Int = bindingAdapterPosition
                val data = listDevice[position].device_id

                Intent(itemView.context, DeviceActivity::class.java).also {
                    it.putExtra("device_id", data)
                    itemView.context.startActivity(it)
                }
            }
        }
    }

}