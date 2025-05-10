package com.example.myapplication.ui.theme.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.myapplication.websocket.WebSocketManager
import org.json.JSONObject
import com.android.volley.Request
import com.example.myapplication.data.model.SessionManager
import com.example.myapplication.data.model.websocket.PGPUtils
import com.example.myapplication.data.model.websocket.SecureStorage
import com.example.myapplication.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateChatScreen(navController: NavController, recipient: String) {
    // Define a proper data class for messages
    data class ChatMessage(
        val sender: String,
        val content: String,
        val isEncrypted: Boolean,
        val isDecrypted: Boolean = false
    )

    var messageInput by remember { mutableStateOf("") }
    val chatMessages = remember { mutableStateListOf<ChatMessage>() } // Changed to use ChatMessage
    val context = LocalContext.current
    val currentUser = remember { SessionManager.getLoggedInUsername(context) ?: "unknown_user" } // Renamed from currentUsername for consistency
    val authViewModel: AuthViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    val webSocketManager = remember {
        WebSocketManager("ws://192.168.0.51:8081/ws/private-chat?username=$currentUser")
    }

    var isSending by remember { mutableStateOf(false) }
    var isDecrypting by remember { mutableStateOf(false) }
    var showDecryptError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        webSocketManager.connect()
        webSocketManager.setMessageListener { incomingMessage ->
            val parts = incomingMessage.split(":", limit = 2)
            if (parts.size == 2) {
                val sender = parts[0].trim()
                val encryptedContent = parts[1].trim()

                if (sender != currentUser) {
                    chatMessages.add(ChatMessage(
                        sender = sender,
                        content = encryptedContent,
                        isEncrypted = true
                    ))
                }
            } else {
                chatMessages.add(ChatMessage(
                    sender = "[System]",
                    content = "Received malformed message",
                    isEncrypted = false,
                    isDecrypted = true
                ))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webSocketManager.closeConnection()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Chat with $recipient",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Messages List
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(chatMessages) { message ->
                val isMe = message.sender == currentUser
                val alignment = if (isMe) Alignment.End else Alignment.Start

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = alignment
                ) {
                    // Sender label
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    // Message bubble
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isMe) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (message.isEncrypted && !message.isDecrypted) {
                                Text(
                                    text = "🔒 Encrypted Message from ${message.sender}",
                                    style = MaterialTheme.typography.labelMedium
                                )

                                // Show the actual encrypted content
                                Text(
                                    text = message.content,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                )

                                if (!isMe) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            isDecrypting = true
                                            coroutineScope.launch {
                                                try {
                                                    val privateKey = SecureStorage.getPrivateKey(context, currentUser)
                                                    val passphrase = SecureStorage.getPGPPassphrase(context, currentUser)

                                                    if (privateKey != null && passphrase != null) {
                                                        val decrypted = PGPUtils.decryptMessage(
                                                            message.content,
                                                            privateKey,
                                                            passphrase
                                                        )
                                                        val index = chatMessages.indexOf(message)
                                                        chatMessages[index] = message.copy(
                                                            content = decrypted,
                                                            isDecrypted = true
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    showDecryptError = true
                                                    Log.e("Decryption", "Failed to decrypt", e)
                                                } finally {
                                                    isDecrypting = false
                                                }
                                            }
                                        },
                                        enabled = !isDecrypting
                                    ) {
                                        if (isDecrypting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Text("Decrypt")
                                        }
                                    }
                                }
                            } else {
                                // Show decrypted message
                                Text(message.content)
                            }
                        }
                    }
                }
            }
        }

        // Message Input
        Column {
            if (showDecryptError) {
                Text(
                    text = "Decryption failed. Check your keys.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Type a message") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (messageInput.isNotBlank() && !isSending) {
                            isSending = true
                            coroutineScope.launch {
                                try {
                                    authViewModel.getUserPublicKey(
                                        username = recipient,
                                        onSuccess = { publicKey ->
                                            try {
                                                val encrypted = PGPUtils.encryptMessage(
                                                    messageInput,
                                                    publicKey
                                                )
                                                webSocketManager.sendMessage("$recipient:$encrypted")
                                                chatMessages.add(ChatMessage(
                                                    sender = currentUser,
                                                    content = messageInput,
                                                    isEncrypted = false,
                                                    isDecrypted = true
                                                ))
                                                messageInput = ""
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Encryption failed: ${e.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                Log.e("Encryption", "Failed to encrypt", e)
                                            } finally {
                                                isSending = false
                                            }
                                        },
                                        onError = { error ->
                                            Toast.makeText(
                                                context,
                                                "Couldn't get recipient's key: $error",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            isSending = false
                                        }
                                    )
                                } catch (e: Exception) {
                                    isSending = false
                                    Log.e("SendMessage", "Error", e)
                                }
                            }
                        }
                    },
                    enabled = messageInput.isNotBlank() && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}