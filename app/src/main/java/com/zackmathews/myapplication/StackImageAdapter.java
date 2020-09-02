package com.zackmathews.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class StackImageAdapter extends BaseAdapter {
    List<StackItem> data = new ArrayList<>();

    public StackImageAdapter(List<String> data) {
        if (data != null) {
            for (String s : data) {
                this.data.add(new StackItem(s));
            }
            notifyDataSetChanged();
        }
    }

    static class StackItem {
        String imageUrl;

        public StackItem(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View v = view;
        if (v == null) {
            v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.stack_image_layout, null);
        }
        StackItem item = data.get(i);
        Picasso.get().load(item.imageUrl).into((ImageView) v.findViewById(R.id.stackImageView));
        return v;
    }
}
