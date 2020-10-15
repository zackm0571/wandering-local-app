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
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.squareup.picasso.Picasso;

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

    private List<YelpData> data = new ArrayList<>();
    private Context context;

    public WLTimeLineAdapter(Context context) {
        this.context = context;
    }

    public void addData(YelpData yd) {
        data.add(yd);
        notifyDataSetChanged();
    }

    public void setData(List<YelpData> data) {
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
        final YelpData yelpData = data.get(position);
        holder.businessNameTextView.setText(yelpData.getBusinessName());
        holder.businessAddressTextView.setText(yelpData.getLocationString());
        holder.businessAddressTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri locationUri = Uri.parse(String.format("http://maps.google.com/maps?q=" + yelpData.getLocationString()));
                Intent intent = new Intent(Intent.ACTION_VIEW, locationUri);
                context.startActivity(intent);
            }
        });
        holder.imgView.setImageBitmap(yelpData.getBmp());

        ViewGroup.LayoutParams params = holder.imgView.getLayoutParams();
        params.height = getDisplayHeight() / 3;
        params.width = getDisplayHeight() / 3;
        holder.imgView.setLayoutParams(params);
        Glide.with(context).load(yelpData.getImageUrl())
                .transition(withCrossFade())
                .centerCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(holder.imgView);

        holder.ratingBar.setNumStars(5);
        holder.ratingBar.setRating((float) yelpData.getRating());
        holder.itemView.setOnClickListener(view -> {
            if (yelpData.getYelpUrl() == null) return;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(yelpData.getYelpUrl()));
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
