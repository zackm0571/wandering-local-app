package life.wanderinglocal;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class WLTimeLineAdapter extends RecyclerView.Adapter<WLTimeLineAdapter.YelpBusiness> {
    private int displayHeight = -1;

    public int getDisplayHeight() {
        if (displayHeight == -1) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            WindowManager windowManager =
                    (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            displayHeight = displayMetrics.heightPixels;
        }
        return displayHeight;
    }

    private List<WLTimelineEntry> data = new ArrayList<>();
    private Context context;

    public WLTimeLineAdapter(Context context) {
        this.context = context;
    }

    public void addData(WLTimelineEntry yd) {
        data.add(yd);
        notifyDataSetChanged();
    }

    public void setData(List<WLTimelineEntry> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public YelpBusiness onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.yelp_business_row, parent, false);
        YelpBusiness yb = new YelpBusiness(v);
        return yb;
    }

    @Override
    public void onBindViewHolder(@NonNull YelpBusiness holder, int position) {
        final WLTimelineEntry timelineEntry = data.get(position);
        holder.businessNameTextView.setText(timelineEntry.getBusinessName());
        holder.businessAddressTextView.setText(timelineEntry.getLocationString());
        holder.businessAddressTextView.setOnClickListener(view -> {
            Uri locationUri = Uri.parse(String.format("http://maps.google.com/maps?q=" + timelineEntry.getLocationString()));
            Intent intent = new Intent(Intent.ACTION_VIEW, locationUri);
            context.startActivity(intent);
        });
        holder.imgView.setImageBitmap(timelineEntry.getBmp());

        ViewGroup.LayoutParams params = holder.imgView.getLayoutParams();
        params.height = getDisplayHeight() / 3;
        params.width = getDisplayHeight() / 3;
        holder.imgView.setLayoutParams(params);
        Glide.with(context).load(timelineEntry.imageUrl)
                .transition(withCrossFade())
                .centerCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(holder.imgView);

        holder.ratingBar.setNumStars(5);
        holder.ratingBar.setRating((float) timelineEntry.getRating());
        holder.itemView.setOnClickListener(view -> {
            if (timelineEntry.yelpUrl == null) return;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(timelineEntry.yelpUrl));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class YelpBusiness extends RecyclerView.ViewHolder {
        RatingBar ratingBar;
        ImageView imgView;
        TextView businessNameTextView;
        TextView businessAddressTextView;

        public YelpBusiness(@NonNull View itemView) {
            super(itemView);
            this.imgView = itemView.findViewById(R.id.businessImg);
            this.businessNameTextView = itemView.findViewById(R.id.businessName);
            this.ratingBar = itemView.findViewById(R.id.businessRatingBar);
            this.businessAddressTextView = itemView.findViewById(R.id.businessAddress);
        }
    }
}
