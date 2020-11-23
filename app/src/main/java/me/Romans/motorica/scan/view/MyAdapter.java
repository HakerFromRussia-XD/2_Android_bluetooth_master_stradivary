package me.Romans.motorica.scan.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import me.Romans.motorica.R;

public class MyAdapter extends ArrayAdapter<String> {

    Context mContext;
    String[] mTitle;
    int[] mImages;

    MyAdapter (Context c, String[] title, int[] images) {
        super(c, R.layout.cell_scan_list, title);
        this.mContext = c;
        this.mTitle = title;
        this.mImages = images;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = layoutInflater.inflate(R.layout.cell_scan_list, parent, false);
        ImageView images = row.findViewById(R.id.imageView);
        TextView myTitle = row.findViewById(R.id.textViewTitle);

        images.setImageResource(mImages[position]);
        myTitle.setText(mTitle[position]);

        return row;
    }
}
