
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.playlistmaster.PlaylistSettingsActivity
import com.example.playlistmaster.R
import java.io.File
import java.net.URI


class CustomAdapter(
    private val context: PlaylistSettingsActivity,
    private val audioFiles: MutableList<File>
) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    // ViewHolder class
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val audioTextView: TextView = view.findViewById(R.id.audioNameM)
        val deleteImageView: ImageView = view.findViewById(R.id.deleteAudioM)

        val containerLayout: LinearLayout = view.findViewById(R.id.containerM)

        var currentFile: File? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.text_row_item, parent, false)
        return ViewHolder(view)

    }

    override fun getItemCount(): Int = audioFiles.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = audioFiles[position]
        holder.currentFile = file
        holder.audioTextView.text = file.name
        // Handle delete/restore icon click
        holder.deleteImageView.setOnClickListener {
            if (audioFiles.size > 1) {
                val index = audioFiles.indexOf(file)
                audioFiles.remove(file)
                context.deleteUnfinished(file.name)
                notifyItemRemoved(index)
            }
//            if (audioFiles.contains(file)) {
//                // Restore (deselect)
//                audioFiles.remove(file)
//                holder.containerLayout.setBackgroundResource(R.drawable.button_red)
//                holder.deleteImageView.setImageResource(R.drawable.restore)
//                notifyItemRemoved(audioFiles.indexOf(file))
//            } else {
//                // Delete (select for removal)
//                audioFiles.add(file)
//                holder.containerLayout.setBackgroundResource(R.drawable.button)
//                holder.deleteImageView.setImageResource(R.drawable.trash_can)
//                notifyItemInserted(audioFiles.indexOf(file))
//            }
        }
    }

    fun addAudio(file: File, uri: Uri) {
        context.writeUnfinished(uri, file.name)
        audioFiles.add(file)
        notifyItemInserted(audioFiles.size - 1)
    }
}