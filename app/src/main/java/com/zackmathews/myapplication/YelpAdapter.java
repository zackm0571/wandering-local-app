package com.zackmathews.myapplication;

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
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class YelpAdapter extends RecyclerView.Adapter<YelpAdapter.YelpBusiness> {
    private int displayHeight = -1;
    public int getDisplayHeight() {
        if(displayHeight == -1){
            DisplayMetrics displayMetrics = new DisplayMetrics();
            WindowManager windowManager  =
                    (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            displayHeight = displayMetrics.heightPixels;
        }
        return displayHeight;
    }

    private List<YelpData> data = new ArrayList<>();
    private Context context;

    public YelpAdapter(Context context){
        this.context = context;
    }
    public void addData(YelpData yd){
        data.add(yd);
        notifyDataSetChanged();
    }
    public void setData(List<YelpData> data){
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
            holder.textView.setText(yelpData.getBusinessName());
            holder.imgView.setImageBitmap(yelpData.getBmp());

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.imgView.getLayoutParams();
            params.height = getDisplayHeight() / 4;
            params.width = getDisplayHeight() / 4;
            holder.imgView.setLayoutParams(params);
            Picasso.get().load(Uri.decode(yelpData.getImageUrl())).into(holder.imgView);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(yelpData.getYelpUrl() == null) return;
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(yelpData.getYelpUrl()));
                    context.startActivity(intent);
                }
            });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class YelpBusiness extends RecyclerView.ViewHolder{
        ImageView imgView;
        TextView textView;
        public YelpBusiness(@NonNull View itemView) {
            super(itemView);
            this.imgView = itemView.findViewById(R.id.businessImg);
            this.textView = itemView.findViewById(R.id.businessName);
        }
    }
}
