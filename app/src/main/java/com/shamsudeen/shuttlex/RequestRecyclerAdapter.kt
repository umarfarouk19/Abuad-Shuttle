package com.shamsudeen.shuttlex

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RequestRecyclerAdapter(var context: Context, var requests: List<Request>) : RecyclerView.Adapter<RequestRecyclerAdapter.ViewHolder>() {

    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_request_list, parent, false)
        return ViewHolder(
            itemView
        )
    }

    override fun getItemCount(): Int {
        return requests.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]

        holder.requestString.text = "${String.format("%.2f", request.distText)}km"

        holder.requestImage.setImageResource(R.drawable.ic_baseline_location_on_24)

        holder.currentPosition = position

        holder.itemView.setOnClickListener { _ ->
            val intent = Intent(context, DriverMapsActivity::class.java)

            intent.putExtra("request-id", request.requestId)

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            context.startActivity(intent)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val requestString: TextView = itemView.findViewById(R.id.request_location_text)
        val requestImage: ImageView = itemView.findViewById(R.id.imageView)

        var currentPosition:Int? = null

    }
}