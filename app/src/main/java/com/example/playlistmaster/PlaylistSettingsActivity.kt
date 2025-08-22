package com.example.playlistmaster

import CustomAdapter
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Layout
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.ArrayList
import kotlin.io.copyTo

class PlaylistSettingsActivity : AppCompatActivity() {

    lateinit var playlist: File
    lateinit var unfinishedDir: File
    val audio = mutableListOf<File>()
    lateinit var customAdapter: CustomAdapter

    fun makeToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun getPathFromUri(uri: Uri): String? {
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

    fun setupRecycler() {
        customAdapter = CustomAdapter(this@PlaylistSettingsActivity, audio)

        val recyclerView: RecyclerView = findViewById(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.adapter = customAdapter


        val itemTouchHelperCallback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val dragFlags =
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN // Allow vertical dragging
                return makeMovementFlags(dragFlags, 0) // No swipe actions
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // Update your data source (myDataSet) to reflect the new order
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                // Perform the swap in your data list, e.g., Collections.swap(myDataSet, fromPosition, toPosition)
                customAdapter.notifyItemMoved(
                    fromPosition,
                    toPosition
                ) // Notify adapter of the move
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used for drag-and-drop reordering
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true // Enable dragging by long press
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

    }

    fun setup() {
        val playlistSerializable = intent.getSerializableExtra("playlist")
        val audioSerializable = intent.getSerializableExtra("audio")

        playlist = playlistSerializable as File
        audio.clear()

        if (audioSerializable is ArrayList<*>) {
            audioSerializable.forEach {
                audio.add(it as File)
            }
        }

        unfinishedDir = File(playlist, "unfinished")
        if (!unfinishedDir.exists()) unfinishedDir.mkdir()

    }

    fun writeUnfinished(contentUri: Uri, fileName: String) {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            // Open an InputStream from the content URI
            inputStream = contentResolver.openInputStream(contentUri)

            // Create a FileOutputStream for the destination file
            outputStream = FileOutputStream(File(unfinishedDir, fileName))

            // Copy the data from the InputStream to the FileOutputStream
            inputStream?.let { input ->
                val buffer = ByteArray(4096) // Adjust buffer size as needed
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle the exception (e.g., file not found, I/O error)
        } finally {
            // Close the streams
            try {
                inputStream?.close()
                outputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun deleteUnfinished(fileName: String) {
        val file = File(unfinishedDir, fileName)
        if (file.exists()) file.delete()
    }

    fun savePlaylist() {
        fun deleteRemovedFiles() {
            File(cacheDir, playlist.name).listFiles()?.forEach {
                if (!audio.contains(it)) {
                    it.delete()
                    makeToast("deleted ${it.name}")
                }
            }
        }
        fun createAddedFiles() {
            unfinishedDir.listFiles()?.forEach {
                val file = File(playlist, it.name)
                if (file.exists()) file.delete()

                it.copyTo(File(playlist, it.name), true)
            }
        }

        deleteRemovedFiles()
        createAddedFiles()

        startActivity(Intent(this, MainActivity::class.java))

    }

    fun launchFileSelection() {
        var chooseFile = Intent(Intent.ACTION_OPEN_DOCUMENT)
        chooseFile.setType("audio/*")
        chooseFile = Intent.createChooser(chooseFile, "Choose an audio file")
        startActivityForResult(chooseFile, 100)
    }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log the crash or show a toast
            // Note: Toasts from background threads need to be posted to main thread
            runOnUiThread {
                Toast.makeText(this, "App crashed: ${throwable.cause}", Toast.LENGTH_LONG).show()
                val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("Copied Text", throwable.stackTraceToString())
                clipboardManager.setPrimaryClip(clipData)
            }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_playlist_settings)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        setup()
        setupRecycler()


        findViewById<TextView>(R.id.managePlaylistText).text = playlist.name

        findViewById<AppCompatButton>(R.id.playlistAddSongM).setOnClickListener {
            launchFileSelection()
        }

        findViewById<AppCompatButton>(R.id.saveButtonM).setOnClickListener {
            savePlaylist()
        }
    }
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            val uri: Uri = data!!.data!!
            val name = getPathFromUri(uri)!!

            if (File(unfinishedDir, name).exists()) File(unfinishedDir, name).delete()

            customAdapter.addAudio(File(unfinishedDir, name))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unfinishedDir.listFiles()?.forEach { it.delete() }
    }
}