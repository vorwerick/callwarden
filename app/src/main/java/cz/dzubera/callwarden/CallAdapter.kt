package cz.dzubera.callwarden

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cz.dzubera.callwarden.Call.Type.*
import java.text.SimpleDateFormat

class CallAdapter(private val onClick: (Call) -> Unit) :
    ListAdapter<Call, CallAdapter.ViewHolder>(CallDiffCallback) {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, val onClick: (Call) -> Unit) : RecyclerView.ViewHolder(view) {
        private val imageViewCallType: AppCompatImageView = view.findViewById(R.id.call_type)
        private val textViewPhoneNumber: TextView = view.findViewById(R.id.call_number)
        private val textViewCallDate: TextView = view.findViewById(R.id.call_date)
        private val textViewCallTime: TextView = view.findViewById(R.id.call_time)
        private val textViewCallInfo: TextView = view.findViewById(R.id.call_dir)
        private var currentItem: Call? = null

        init {
            view.setOnClickListener {
                currentItem?.let { onClick(it) }
            }
        }

        @SuppressLint("SimpleDateFormat")
        fun bind(call: Call) {
            textViewPhoneNumber.text = call.phoneNumber
            textViewCallDate.text = SimpleDateFormat("dd.MM.yyyy").format(call.callStarted)
            textViewCallTime.text = SimpleDateFormat("HH:mm:ss").format(call.callStarted)
            val imageIcon = when (call.type) {
                MISSED -> R.drawable.ic_call_missed
                CALLBACK -> R.drawable.ic_call_back
                DIALED -> R.drawable.ic_call_back
                ACCEPTED -> R.drawable.ic_call_answered
            }
            textViewCallInfo.text = when(call.type){
                MISSED -> "zmeškaný"
                ACCEPTED -> "přijatý"
                CALLBACK -> "volaný"
                DIALED -> "volaný"
            }
            imageViewCallType.setImageResource(imageIcon)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.list_item_call, viewGroup, false)

        return ViewHolder(view, onClick)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val call = getItem(position)
        viewHolder.bind(call)
    }
}

object CallDiffCallback : DiffUtil.ItemCallback<Call>() {
    override fun areItemsTheSame(oldItem: Call, newItem: Call): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Call, newItem: Call): Boolean {
        return oldItem.id == newItem.id
    }
}