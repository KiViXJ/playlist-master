package com.example.playlistmaster
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class JSON {
    companion object {
        fun getSongsOrdered(playlist: File): List<String> {
            val file = File(playlist, "songOrder.json")
            val gson = Gson()
            val jsonString = file.bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<String>>() {}.type
            val data: List<String> = gson.fromJson(jsonString, type)
            return data
        }
    }
}