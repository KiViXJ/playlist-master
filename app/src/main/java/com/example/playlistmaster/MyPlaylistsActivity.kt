package com.example.playlistmaster

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.setPadding
import androidx.transition.Visibility
import java.io.File
import java.lang.reflect.Array

class MyPlaylistsActivity : AppCompatActivity() {

    private val playlists = mutableMapOf<File, List<File>>()
    val match = FrameLayout.LayoutParams.MATCH_PARENT; val wrap = FrameLayout.LayoutParams.WRAP_CONTENT

    @RequiresApi(Build.VERSION_CODES.O)
    fun createPlaylistView(playlist: File, songsCount: Int) {

        val container = FrameLayout(this).apply {
            setPadding(50,0,0,0)
            background = resources.getDrawable(R.drawable.button)
        }
        container.layoutParams = FrameLayout.LayoutParams(800,500).apply {
            topMargin = 50
        }

        val imageContainer = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = resources.getDrawable(R.drawable.button)
        }
        imageContainer.layoutParams = FrameLayout.LayoutParams(match,wrap).apply {
            gravity = Gravity.CENTER or Gravity.BOTTOM
        }


        val playlistText = TextView(this).apply {
            setPadding(0,30,0,0)
            width = match
            height = wrap
            setTextColor(resources.getColor(R.color.black))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 90f)
            typeface = resources.getFont(R.font.montserrat_bold)
            text = playlist.name
            setSingleLine()
        }
        playlistText.layoutParams = FrameLayout.LayoutParams(match, wrap)

        val songsCountText = TextView(this).apply {
            setTextColor(resources.getColor(R.color.black))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 80f)
            typeface = resources.getFont(R.font.montserrat_bold)
            text = "$songsCount songs"
        }
        songsCountText.layoutParams = FrameLayout.LayoutParams(wrap, wrap).apply {
            topMargin = 150
        }

        val playImage = ImageView(this).apply {
            setImageDrawable(resources.getDrawable(R.drawable.play))
        }
        playImage.layoutParams = FrameLayout.LayoutParams(150,150).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER
            bottomMargin = 50
        }

        playImage.setOnClickListener {
            startActivity(
                Intent(this, PlaylistActivity::class.java).apply {
                    putExtra("playlist", playlist)
                    putExtra("audio", playlists[playlist]!!.toCollection(ArrayList()))
        }
            )}

        val settingsImage = ImageView(this).apply {
            setImageDrawable(resources.getDrawable(R.drawable.settings))
        }
        settingsImage.layoutParams = FrameLayout.LayoutParams(150,150).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER
            bottomMargin = 50
        }

        settingsImage.setOnClickListener {
            try {
                startActivity(
                    Intent(this, PlaylistSettingsActivity::class.java).apply {
                        putExtra("playlist", playlist)
                        putExtra("audio", playlists[playlist]!!.toCollection(ArrayList()))
                    }
                )
            }
            catch (e: Exception) { makeToast(e.toString()); makeToast(e.message.toString()) }
        }

        imageContainer.addView(playImage)
        imageContainer.addView(settingsImage)

        container.addView(playlistText)
        container.addView(songsCountText)

        container.addView(imageContainer)

        findViewById<LinearLayout>(R.id.rootLayout).addView(container)


    }
    fun makeToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun loadPlaylists() {
        val playlistDirectories: List<File> =
            cacheDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        if (playlistDirectories.isNotEmpty()) {
            for (playlistDirectory in playlistDirectories) {
                val playlistsAudio =
                    playlistDirectory.listFiles()?.filter { it.isFile && it.canRead() } ?: emptyList()

                if (playlistsAudio.isNotEmpty()) {
                    playlists[playlistDirectory] =
                        playlistsAudio.sortedBy { it.name } // Optional: sort files alphabetically.
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            setContentView(R.layout.activity_my_playlists)

            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            loadPlaylists()

            if (playlists.isNotEmpty()) {
                for ((playlistFile, audioFiles) in playlists) {
                    createPlaylistView(playlistFile, audioFiles.size)
                }
            }
        }
        catch (e: Exception) { makeToast(e.message.toString()) }
    }
}