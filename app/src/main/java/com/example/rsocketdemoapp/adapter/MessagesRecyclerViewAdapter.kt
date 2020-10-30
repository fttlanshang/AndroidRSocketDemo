package com.example.rsocketdemoapp.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rsocketdemoapp.R
import com.example.rsocketdemoapp.data.Message


class MessagesRecyclerViewAdapter: RecyclerView.Adapter<MessagesRecyclerViewAdapter.MessageItemViewHolder>() {
    private var messages: MutableList<Message> = mutableListOf()

    fun setDataSource(items: MutableList<Message>) {
        messages = items
        Handler(Looper.getMainLooper()).post(Runnable {
            notifyDataSetChanged()
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageItemViewHolder {
        val messageView = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return MessageItemViewHolder(messageView)
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: MessageItemViewHolder, position: Int) {
        val message: Message = messages[position]
        holder.setupView(message)
    }

    class MessageItemViewHolder(view: View): RecyclerView.ViewHolder(view) {
        private var messageTextView: TextView = view.findViewById(R.id.textView_message)

        fun setupView(message: Message) {
            messageTextView.text = "${message.body}"
        }
    }
}