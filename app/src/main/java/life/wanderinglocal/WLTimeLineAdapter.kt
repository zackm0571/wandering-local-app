package life.wanderinglocal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import life.wanderinglocal.WLTimeLineAdapter.YelpBusiness
import java.util.*

class WLTimeLineAdapter(private val context: Context) : RecyclerView.Adapter<YelpBusiness>() {
    val itemClickedSubject: Subject<WLTimelineEntry> = PublishSubject.create()
    var displayHeight = -1
        get() {
            if (field == -1) {
                val displayMetrics = DisplayMetrics()
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                field = displayMetrics.heightPixels
            }
            return field
        }
    private var data: List<WLTimelineEntry> = ArrayList()

    /**
     * Replace with Google Paging Library
     */
    fun addData(yd: WLTimelineEntry) {
    }

    fun setData(data: List<WLTimelineEntry>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YelpBusiness {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.yelp_business_row, parent, false)
        return YelpBusiness(v)
    }

    override fun onBindViewHolder(holder: YelpBusiness, position: Int) {
        val timelineEntry = data[position]
        holder.businessNameTextView.text = timelineEntry.businessName
        holder.businessAddressTextView.text = timelineEntry.locationString
        holder.businessAddressTextView.setOnClickListener { view: View? ->
            val locationUri = Uri.parse(String.format("http://maps.google.com/maps?q=" + timelineEntry.locationString))
            val intent = Intent(Intent.ACTION_VIEW, locationUri)
            context.startActivity(intent)
        }
        holder.imgView.setImageBitmap(timelineEntry.bmp)
        val params = holder.imgView.layoutParams
        params.height = displayHeight / 3
        params.width = displayHeight / 3
        holder.imgView.layoutParams = params
        Glide.with(context).load(timelineEntry.imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(holder.imgView)
        holder.ratingBar.numStars = 5
        holder.ratingBar.rating = timelineEntry.rating.toFloat()
        holder.itemView.setOnClickListener { view: View? ->
            itemClickedSubject.onNext(timelineEntry)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class YelpBusiness(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ratingBar: RatingBar
        var imgView: ImageView
        var businessNameTextView: TextView
        var businessAddressTextView: TextView

        init {
            imgView = itemView.findViewById(R.id.businessImg)
            businessNameTextView = itemView.findViewById(R.id.businessName)
            ratingBar = itemView.findViewById(R.id.businessRatingBar)
            businessAddressTextView = itemView.findViewById(R.id.businessAddress)
        }
    }

}