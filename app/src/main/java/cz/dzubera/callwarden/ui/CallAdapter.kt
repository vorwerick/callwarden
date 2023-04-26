package cz.dzubera.callwarden.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cz.dzubera.callwarden.R
import cz.dzubera.callwarden.model.Call
import java.text.SimpleDateFormat

class CallAdapter(
    private val onItemClick: (Long) -> Unit,
    private val calls: (List<Call>) -> Unit
) :
    ListAdapter<Call, CallAdapter.ViewHolder>(CallDiffCallback) {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, private val onItemClick: (Long) -> Unit) :
        RecyclerView.ViewHolder(view) {
        private val imageViewCallType: AppCompatImageView = view.findViewById(R.id.call_type)
        private val textViewPhoneNumber: TextView = view.findViewById(R.id.call_number)
        private val textViewCallProject: TextView = view.findViewById(R.id.call_project)
        private val textViewCallDate: TextView = view.findViewById(R.id.call_date)
        private val textViewCallTime: TextView = view.findViewById(R.id.call_time)
        private val textViewCallInfo: TextView = view.findViewById(R.id.call_dir)
        private val imageButtonEditCall: AppCompatImageButton = view.findViewById(R.id.edit_call)


        @SuppressLint("SimpleDateFormat")
        fun bind(call: Call) {
            if (call.phoneNumber.isNotEmpty()) {
                textViewPhoneNumber.text = call.phoneNumber
            } else {
                textViewPhoneNumber.text = "neznámé číslo"
            }


            imageButtonEditCall.setOnClickListener {
                onItemClick.invoke(call.id)
            }
            textViewCallProject.text = call.projectName
            textViewCallDate.text = SimpleDateFormat("dd.MM.yyyy").format(call.callStarted)
            textViewCallTime.text = SimpleDateFormat("HH:mm:ss").format(call.callStarted)

            var imageIcon = R.drawable.ic_call_missed
            if (call.direction == Call.Direction.INCOMING) {
                if (call.duration <= 0) {
                    imageIcon = R.drawable.ic_call_missed
                    textViewCallInfo.text = "příchozí - nespojený"
                } else {
                    imageIcon = R.drawable.ic_incoming_connected
                    textViewCallInfo.text = "příchozí - spojený " + getDurationString(call.duration)

                }
            } else {
                if (call.duration <= 0) {
                    imageIcon = R.drawable.ic_outgoing_missed
                    textViewCallInfo.text = "odchozí - nespojený"
                } else {
                    imageIcon = R.drawable.ic_call_back
                    textViewCallInfo.text = "odchozí - spojený " + getDurationString(call.duration)

                }
            }
            imageViewCallType.setImageResource(imageIcon)
        }

        private fun getDurationString(duration: Int): String {
            val minutes = duration / 60
            val seconds = duration % 60
            if (minutes == 0) {
                return "${seconds}s"
            }
            return "${minutes}m ${seconds}s"
        }

    }

    //call duration to minutes and seconds

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.list_item_call, viewGroup, false)

        return ViewHolder(view, onItemClick)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val call = getItem(position)
        if (call != null) {
            viewHolder.bind(call)

        }
    }

    override fun onCurrentListChanged(
        previousList: MutableList<Call>,
        currentList: MutableList<Call>
    ) {
        super.onCurrentListChanged(previousList, currentList)
        calls.invoke(currentList)
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