package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class RingtoneAdapter extends ArrayAdapter<RingtoneItem> {
    private String selectedUri;

    public RingtoneAdapter(Context context, List<RingtoneItem> items, String selectedUri) {
        super(context, 0, items);
        this.selectedUri = selectedUri;
    }

    public void setSelected(String uri) {
        this.selectedUri = uri;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RingtoneItem item = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_ringtone, parent, false);
        }
        TextView tvTitle = convertView.findViewById(R.id.tv_ringtone_title);
        View vSelected = convertView.findViewById(R.id.v_selected);
        tvTitle.setText(item.getTitle());
        boolean isSelected = item.getUriString().equals(selectedUri);
        vSelected.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
        return convertView;
    }
}
