package com.example.myapplication.data.model.websocket

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
object SecureStorage {
    private const val PREF_NAME = "secure_prefs"

    fun savePrivateKey(context: Context, username: String, privateKey: String) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val prefs = EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.edit().putString("privateKey_$username", privateKey).apply()
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error saving private key", e)
            // Fallback to regular SharedPreferences if encryption fails
            context.getSharedPreferences("fallback_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("privateKey_$username", privateKey)
                .apply()
        }
    }

    fun getPrivateKey(context: Context, username: String): String? {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val prefs = EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString("privateKey_$username", null)
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error getting private key", e)
            // Try fallback if encrypted prefs fail
            context.getSharedPreferences("fallback_prefs", Context.MODE_PRIVATE)
                .getString("privateKey_$username", null)
        }
    }

    // Similar modifications for savePGPPassphrase and getPGPPassphrase
    fun savePGPPassphrase(context: Context, username: String, passphrase: String) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val prefs = EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.edit().putString("passphrase_$username", passphrase).apply()
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error saving passphrase", e)
            context.getSharedPreferences("fallback_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("passphrase_$username", passphrase)
                .apply()
        }
    }

    fun getPGPPassphrase(context: Context, username: String): String? {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val prefs = EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString("passphrase_$username", null)
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error getting passphrase", e)
            context.getSharedPreferences("fallback_prefs", Context.MODE_PRIVATE)
                .getString("passphrase_$username", null)
        }
    }
}