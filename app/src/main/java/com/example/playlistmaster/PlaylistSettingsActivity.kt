package com.example.playlistmaster

import CustomAdapter
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Collections

class PlaylistSettingsActivity : AppCompatActivity() {

    lateinit var playlist: File
    lateinit var unfinishedDir: File
    lateinit var songOrderFile: File
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
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
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

    fun setup() {
        val playlistSerializable = intent.getSerializableExtra("playlist")
        playlist = playlistSerializable as File
        audio.clear()

        songOrderFile = File(playlist, "songOrder.json")
        JSON.getSongsOrdered(playlist).forEach { audio.add(File(playlist, it)) }

        unfinishedDir = File(playlist, "unfinished")
        if (!unfinishedDir.exists()) unfinishedDir.mkdir()
    }


    fun showConfirmationDialogue() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Deletion confirmation")
        builder.setMessage("Delete playlist ${playlist.name}? This action cannot be undone!")

        builder.setPositiveButton("Delete") { dialog: DialogInterface, _: Int ->
            playlist.deleteRecursively()
            makeToast("Playlist ${playlist.name} deleted successfully!")
            startActivity(Intent(this, MyPlaylistsActivity::class.java))
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
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
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN
                return makeMovementFlags(dragFlags, 0)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                Collections.swap(audio, fromPosition, toPosition)
                customAdapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean { return true }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

    }

    fun writeUnfinished(contentUri: Uri, fileName: String) {

        val inputStream = contentResolver.openInputStream(contentUri)
        val outputStream = FileOutputStream(File(unfinishedDir, fileName))

        inputStream?.let { input ->
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            inputStream.close()
            outputStream.close()
        }
    }
    fun deleteUnfinished(fileName: String) {
        val file = File(unfinishedDir, fileName)
        if (file.exists()) file.delete()
    }

    fun savePlaylist() {

        fun deleteRemovedFiles() {
            File(cacheDir, playlist.name).listFiles()?.forEach {
                if (!audio.contains(it) && it.extension != "json") it.delete()
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
        songOrderFile.writeText(Gson().toJson(audio.map { it.name }))

        startActivity(Intent(this, MainActivity::class.java))

    }

    fun launchFileSelection() {
        var chooseFile = Intent(Intent.ACTION_OPEN_DOCUMENT)
        chooseFile.setType("audio/*")
        chooseFile = Intent.createChooser(chooseFile, "Choose audio to add to the playlist")
        startActivityForResult(chooseFile, 100)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
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
        findViewById<AppCompatButton>(R.id.deleteButtonM).setOnClickListener {
            showConfirmationDialogue()
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

            customAdapter.addAudio(File(unfinishedDir, name), uri)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unfinishedDir.listFiles()?.forEach { it.delete() }
    }
}