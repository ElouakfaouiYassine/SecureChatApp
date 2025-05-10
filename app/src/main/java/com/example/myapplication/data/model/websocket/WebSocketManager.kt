package com.example.myapplication.websocket

import android.util.Log
import okhttp3.*
import okio.ByteString

class WebSocketManager(private val serverUrl: String) {

    private var webSocket: WebSocket? = null
    private var listener: ((String) -> Unit)? = null
    private val client = OkHttpClient() // Keep OkHttpClient instance

    fun connect() {
        if (webSocket != null) {
            Log.d("WebSocket", "Already connected or connecting.")
            return // Avoid multiple connections
        }
        val request = Request.Builder().url(serverUrl).build()
        Log.d("WebSocket", "Attempting to connect to: $serverUrl")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connection OPENED")
                // Maybe notify listener about connection status if needed
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Received TEXT: $text")
                // Ensure listener is invoked on the main thread if it updates UI directly
                // androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot { listener?.invoke(text) } might be needed if direct UI updates happen in listener
                listener?.invoke(text) // Call the listener
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                // Handle binary messages if your backend sends them
                Log.d("WebSocket", "Received BYTES: ${bytes.hex()}")
            }


            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closing: Code=$code, Reason=$reason")
                webSocket.close(1000, null) // Acknowledge close
                this@WebSocketManager.webSocket = null // Clear reference
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Connection FAILURE: ${t.message}", t)
                this@WebSocketManager.webSocket = null // Clear reference on failure
                // Maybe notify listener about failure if needed
            }
        })
    }

    fun sendMessage(message: String) {
        Log.d("WebSocket", "Attempting to send: $message")
        val sent = webSocket?.send(message)
        if (sent == true) {
            Log.d("WebSocket", "Message sent successfully.")
        } else {
            Log.e("WebSocket", "Failed to send message. WebSocket null or queue full.")
            // Consider adding retry logic or notifying the user/caller
        }
    }

    fun setMessageListener(listener: (String) -> Unit) {
        this.listener = listener
    }

    fun closeConnection() {
        Log.d("WebSocket", "closeConnection() called.")
        webSocket?.close(1000, "Client requested disconnect")
        webSocket = null
    }

    fun isConnected(): Boolean {
        return webSocket != null
    }
}