package com.bailout.stickk.scan.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;

import com.bailout.stickk.R;

public class MyAdapter extends ArrayAdapter<String> {

    Context mContext;
    String[] mTitle;
    int[] mImages;

    MyAdapter(Context c, String[] title, int[] images) {
        super(c, R.layout.item_scanlist, title);
        this.mContext = c;
        this.mTitle = title;
        this.mImages = images;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = layoutInflater.inflate(R.layout.item_scanlist, parent, false);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) ImageView images = row.findViewById(R.id.imageView);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView myTitle = row.findViewById(R.id.textViewTitle);

        images.setImageResource(mImages[position]);
        myTitle.setText(mTitle[position]);

        return row;
    }
}
