package com.example.myapplication.data.model.websocket


import android.util.Log
import okhttp3.*
import okio.ByteString

class ChatWebSocketClient(private val serverUrl: String, private val listener: ChatListener) {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connect() {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("ChatWebSocketClient", "Connected to WebSocket")
                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("ChatWebSocketClient", "Received message: $text")
                listener.onMessageReceived(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {}

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                listener.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("ChatWebSocketClient", "WebSocket error: ${t.message}")
                listener.onError(t.message ?: "Unknown error")
            }
        })
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun close() {
        webSocket?.close(1000, "Closing WebSocket")
    }

    interface ChatListener {
        fun onConnected()
        fun onMessageReceived(message: String)
        fun onDisconnected()
        fun onError(error: String)
    }
}