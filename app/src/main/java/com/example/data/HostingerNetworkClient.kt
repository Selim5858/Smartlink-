package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object HostingerNetworkClient {
    private const val TAG = "HostingerNetwork"

    // Save & Retrieve Hostinger Configuration using SharedPreferences
    fun saveApiConfig(context: Context, apiUrl: String, apiKey: String, useCloud: Boolean) {
        val prefs = context.getSharedPreferences("hostinger_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("api_url", apiUrl.trim())
            .putString("api_key", apiKey.trim())
            .putBoolean("use_cloud", useCloud)
            .apply()
    }

    fun getApiUrl(context: Context): String {
        val prefs = context.getSharedPreferences("hostinger_prefs", Context.MODE_PRIVATE)
        return prefs.getString("api_url", "") ?: ""
    }

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences("hostinger_prefs", Context.MODE_PRIVATE)
        return prefs.getString("api_key", "default_secret_key") ?: "default_secret_key"
    }

    fun isCloudEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("hostinger_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("use_cloud", false)
    }

    // Ping / Test Connection to PHP Server
    suspend fun testConnection(context: Context): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val endpoint = getApiUrl(context)
        val apiKey = getApiKey(context)
        if (endpoint.isEmpty()) {
            return@withContext Pair(false, "API URL boş olamaz!")
        }

        try {
            val url = URL("$endpoint?action=test&api_key=${URLEncoder.encode(apiKey, "UTF-8")}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = InputStreamReader(connection.inputStream)
                val responseText = reader.readText()
                val json = JSONObject(responseText)
                if (json.optBoolean("success", false)) {
                    return@withContext Pair(true, json.optString("message", "Bağlantı başarılı!"))
                } else {
                    return@withContext Pair(false, "Sunucu Hatası: " + json.optString("message", "Bilinmeyen hata"))
                }
            } else {
                return@withContext Pair(false, "Bağlantı başarısız. HTTP Kod: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            return@withContext Pair(false, "Hata: ${e.localizedMessage}")
        }
    }

    // Fetch active buttons from PHP API
    suspend fun fetchButtons(context: Context): List<LinkButton> = withContext(Dispatchers.IO) {
        val endpoint = getApiUrl(context)
        val apiKey = getApiKey(context)
        if (endpoint.isEmpty() || !isCloudEnabled(context)) {
            return@withContext emptyList()
        }

        try {
            val url = URL("$endpoint?action=get_buttons&api_key=${URLEncoder.encode(apiKey, "UTF-8")}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = InputStreamReader(connection.inputStream).readText()
                val json = JSONObject(responseText)
                if (json.optBoolean("success", false)) {
                    val dataArray = json.getJSONArray("data")
                    val list = mutableListOf<LinkButton>()
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        list.add(
                            LinkButton(
                                id = item.getInt("id"),
                                label = item.getString("label"),
                                targetUrl = item.getString("target_url"),
                                popupTitle = item.getString("popup_title"),
                                popupMessage = item.getString("popup_message"),
                                popupImageUrl = item.optString("popup_image_url", ""),
                                countdownSeconds = item.optInt("countdown_seconds", 5),
                                clickCount = item.optInt("click_count", 0)
                            )
                        )
                    }
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching buttons from remote", e)
        }
        return@withContext emptyList()
    }

    // Add new button to live DB
    suspend fun addBtn(context: Context, btn: LinkButton): Boolean = withContext(Dispatchers.IO) {
        val endpoint = getApiUrl(context)
        val apiKey = getApiKey(context)
        try {
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.connectTimeout = 8000

            val postData = "action=add_button" +
                    "&api_key=${URLEncoder.encode(apiKey, "UTF-8")}" +
                    "&label=${URLEncoder.encode(btn.label, "UTF-8")}" +
                    "&target_url=${URLEncoder.encode(btn.targetUrl, "UTF-8")}" +
                    "&popup_title=${URLEncoder.encode(btn.popupTitle, "UTF-8")}" +
                    "&popup_message=${URLEncoder.encode(btn.popupMessage, "UTF-8")}" +
                    "&countdown_seconds=${btn.countdownSeconds}"

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(postData)
            writer.flush()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val data = InputStreamReader(connection.inputStream).readText()
                val json = JSONObject(data)
                return@withContext json.optBoolean("success", false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding button on remote", e)
        }
        return@withContext false
    }

    // Delete button from remote
    suspend fun deleteBtn(context: Context, id: Int): Boolean = withContext(Dispatchers.IO) {
        val endpoint = getApiUrl(context)
        val apiKey = getApiKey(context)
        try {
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val postData = "action=delete_button" +
                    "&api_key=${URLEncoder.encode(apiKey, "UTF-8")}" +
                    "&id=$id"

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(postData)
            writer.flush()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val data = InputStreamReader(connection.inputStream).readText()
                val json = JSONObject(data)
                return@withContext json.optBoolean("success", false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting remote button", e)
        }
        return@withContext false
    }

    // Track click on remote button
    suspend fun registerClick(context: Context, id: Int) = withContext(Dispatchers.IO) {
        val endpoint = getApiUrl(context)
        val apiKey = getApiKey(context)
        try {
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val postData = "action=click_button" +
                    "&api_key=${URLEncoder.encode(apiKey, "UTF-8")}" +
                    "&id=$id"

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(postData)
            writer.flush()
            connection.responseCode // trigger connection
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking remote click", e)
        }
    }
}
