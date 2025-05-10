package com.example.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import okhttp3.*
import okio.ByteString

class ChatViewModel : ViewModel() {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    fun connectWebSocket() {
        val request = Request.Builder().url("ws://192.168.43.164:8081/ws/chat").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected to Server")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Received Message: $text")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                Log.d("WebSocket", "Closing Connection: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error: ${t.message}")
            }
        })
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun closeConnection() {
        webSocket?.close(1000, "App closed")
    }
}
