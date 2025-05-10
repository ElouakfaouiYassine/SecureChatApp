package com.example.myapplication.data.model

data class ChatMessage(
    val sender: String,
    val content: String,
    val isDecrypted: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)