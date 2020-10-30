package com.example.rsocketdemoapp.data

import kotlinx.serialization.Serializable

@Serializable
data class Message(
        val id: String,
        val author: String,
        val body: String,
        val date: String
)
