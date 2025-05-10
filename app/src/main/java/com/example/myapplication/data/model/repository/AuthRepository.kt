package com.example.myapplication.data.model.repository

import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.myapplication.data.model.SessionManager
import com.example.myapplication.data.model.User
import org.json.JSONObject

class AuthRepository(private val context: Context) {
    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)
    private val BASE_URL = "http://192.168.0.51:8081"

    // ✅ Register User Function
    fun registerUser(
        username: String,
        password: String,
        publicKey: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$BASE_URL/auth/register"
        Log.d("AuthRepository", "Sending POST request to: $url")

        val jsonRequest = JSONObject().apply {
            put("username", username)
            put("password", password)
            put("publicKey", publicKey)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, jsonRequest,
            Response.Listener { response ->
                onSuccess(response.toString())
            },
            Response.ErrorListener { error ->
                handleError(error, onError)
            }
        )
        requestQueue.add(request)
    }


    fun loginUser(
        username: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$BASE_URL/auth/login"
        Log.d("AuthRepository", "Sending POST request to: $url")

        val jsonRequest = JSONObject().apply {
            put("username", username)
            put("password", password)
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, jsonRequest,
            Response.Listener { response ->
                Log.d("AuthRepository", "Login successful, received token: $response")
                onSuccess(response.getString("token"))
                SessionManager.saveLoggedInUsername(context, username)
            },
            Response.ErrorListener { error ->
                handleError(error, onError)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf("Content-Type" to "application/json")
            }
        }

        requestQueue.add(request)
    }

    private fun handleError(error: VolleyError, onError: (String) -> Unit) {
        if (error.networkResponse != null) {
            val statusCode = error.networkResponse.statusCode
            val errorData = String(error.networkResponse.data, Charsets.UTF_8)
            Log.e("AuthRepository", "HTTP Error $statusCode: $errorData")
            onError("Error $statusCode: $errorData")
        } else {
            Log.e("AuthRepository", "Unknown error: ${error.message}")
            onError(error.message ?: "Unknown error")
        }
    }

    fun getUserPublicKey(
        username: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$BASE_URL/api/users/$username/public-key"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                onSuccess(response.getString("publicKey"))
            },
            Response.ErrorListener { error ->
                handleError(error, onError)
            }
        )

        requestQueue.add(request)
    }
    fun searchUsers(
        query: String,
        onSuccess: (List<User>) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$BASE_URL/api/users/search?query=$query"

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                val users = mutableListOf<User>()
                for (i in 0 until response.length()) {
                    val userJson = response.getJSONObject(i)
                    users.add(
                        User(
                            id = userJson.getString("id"),
                            username = userJson.getString("username")
                        )
                    )
                }
                onSuccess(users)
            },
            Response.ErrorListener { error ->
                onError("Error searching users: ${error.message}")
            }
        )

        requestQueue.add(request)
    }
}