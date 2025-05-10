package com.example.myapplication.data.model.network


import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class ApiHelper(context: Context) {
    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)

    fun registerUser(
        username: String,
        email: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val url = "http://192.168.0.51:8081/auth/register"
        // Replace with your API URL

        val jsonBody = JSONObject().apply {
            put("username", username)
            put("email", email)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            Response.Listener { response ->
                callback(true, response.toString())
            },
            Response.ErrorListener { error ->
                callback(false, error.message)
            }
        )

        requestQueue.add(request)
    }
}