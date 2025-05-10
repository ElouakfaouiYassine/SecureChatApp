package com.example.myapplication
data class ChatBubble(
    val sender: String,
    val content: String,
    val isEncrypted: Boolean,
    val isDecrypted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)