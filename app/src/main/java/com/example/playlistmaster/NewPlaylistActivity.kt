package com.example.playlistmaster

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.icu.text.ListFormatter
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColor
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.setPadding
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class NewPlaylistActivity : AppCompatActivity() {
    lateinit var addedAudioView: LinearLayout
    val addedAudioNameXUri = mutableMapOf<String, Uri>()

    val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
    val match = LinearLayout.LayoutParams.MATCH_PARENT
    fun makeToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("ResourceAsColor")
    fun addNewAudioText(name: String, uri: Uri) {

        if (addedAudioNameXUri.contains(name)) {
            return makeToast("This audio is already added")
        }

        makeToast("Added $name")

        findViewById<TextView>(R.id.addedAudioText).text = "Added audio"

        val actualPixels = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX,
            70f,
            resources.displayMetrics
        )

        val newAudioText = TextView(this)
        val trashCanImage = ImageView(this)
        val container = LinearLayout(this)

        container.apply {
            val containerParams = LinearLayout.LayoutParams(800,200).apply {
                topMargin = 100
            }
            tag = name
            gravity = Gravity.START

            orientation = LinearLayout.HORIZONTAL
            layoutParams = containerParams
            background = AppCompatResources.getDrawable(this@NewPlaylistActivity, R.drawable.button)
        }

        newAudioText.apply {
            layoutParams = LinearLayout.LayoutParams(630, 200)
            text = if (name.length < 15) name else name.substring(0, 12) + "..."
            gravity = Gravity.CENTER
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(R.color.black)
            typeface = ResourcesCompat.getFont(this@NewPlaylistActivity, R.font.montserrat_bold)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, actualPixels)
        }

        trashCanImage.apply {
            setOnClickListener {
                if (addedAudioNameXUri.contains(name)) {
                    container.background = AppCompatResources.getDrawable(this@NewPlaylistActivity,
                        R.drawable.button_red
                    )
                    addedAudioNameXUri.remove(name)
                    setImageResource(R.drawable.restore)
                }
                else {
                    container.background = AppCompatResources.getDrawable(this@NewPlaylistActivity, R.drawable.button)
                    addedAudioNameXUri.put(name, uri)
                    setImageResource(R.drawable.trash_can)
                }
            }

            layoutParams = LinearLayout.LayoutParams(120,120).apply {
                gravity = Gravity.CENTER or Gravity.END
                rightMargin = 10
            }

            setImageResource(R.drawable.trash_can)
        }

        container.addView(newAudioText)
        container.addView(trashCanImage)
        addedAudioView.addView(container)
        addedAudioNameXUri.put(name, uri)
    }
    fun getNameFromUri(uri: Uri): String? {
        var filePath: String? = null
        val scheme = uri.scheme
        if (scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME) // Or MediaStore.Audio.Media.DATA
                    if (columnIndex != -1) {
                        filePath = it.getString(columnIndex)
                    }
                }
            }
        } else if (scheme == "file") {
            filePath = uri.path
        }
        return filePath
    }
    fun trySavePlaylist() {

        val title = findViewById<EditText>(R.id.newPlaylistTitle).text.toString()
        val playlistDirectory = File(cacheDir, title)

        if (addedAudioNameXUri.isEmpty()) {
            return makeToast("No audio added. Add at least 1 audio file")
        }
        else if (findViewById<EditText>(R.id.newPlaylistTitle).text.toString().isEmpty()) {
            return makeToast("Title field is empty. Make a title for this playlist")
        }
        else if (playlistDirectory.exists() && playlistDirectory.isDirectory()) {
            return makeToast("Playlist '$title' already exists")
        }

        makeToast("Successfully created playlist '$title'")
        playlistDirectory.mkdirs()

        for ((name, uri) in addedAudioNameXUri) {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                if (inputStream != null) {

                    val copiedFile = File(playlistDirectory, name)

                    FileOutputStream(copiedFile).use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        startActivity(Intent(this, MainActivity::class.java))

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_playlist)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        addedAudioView = findViewById(R.id.addedAudio)

        var chooseFile = Intent(Intent.ACTION_OPEN_DOCUMENT)
        chooseFile.setType("audio/*")
        chooseFile = Intent.createChooser(chooseFile, "Choose an audio file")

        findViewById<AppCompatButton>(R.id.newPlaylistAddSong).setOnClickListener {
            startActivityForResult(chooseFile, 100)
        }
        findViewById<AppCompatButton>(R.id.newPlaylistReady).setOnClickListener {
            trySavePlaylist()
        }

    }
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            val uri: Uri = data!!.data!!
            val name = getNameFromUri(uri)!!
            addNewAudioText(name, uri)
        }
    }
}