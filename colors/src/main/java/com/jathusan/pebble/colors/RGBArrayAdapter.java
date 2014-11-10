package com.jathusan.pebble.colors;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

// An array adapter for the RGBObject Class

public class RGBArrayAdapter extends ArrayAdapter<RGBObject> {

    int resource;

    public RGBArrayAdapter(Context context, int resource, List<RGBObject> items) {
        super(context, resource, items);
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout rgbView = new LinearLayout(getContext());

        try {
            RGBObject rgbObject = getItem(position);

            if (convertView == null) {
                rgbView = new LinearLayout(getContext());
                String inflater = Context.LAYOUT_INFLATER_SERVICE;
                LayoutInflater vi;
                vi = (LayoutInflater) getContext().getSystemService(inflater);
                vi.inflate(resource, rgbView, true);
            } else {
                rgbView = (LinearLayout) convertView;
            }

            TextView logMessage = (TextView) rgbView.findViewById(R.id.logMessage);

            if (rgbObject.isSelected()) {
                // indication that an item is selected (blue colour)
                logMessage.setTextColor(Color.parseColor("#608AEB"));
            } else {
                // indicator that an item is not selected (gray colour)
                logMessage.setTextColor(Color.parseColor("#616161"));
            }

            // Building the log message
            String logText = "Command: ";

            if (rgbObject.isAbsolute()) {
                logText += "Absolute ";
            } else {
                logText += "Relative ";
            }

            logText += "[ R: " + rgbObject.getRValue() + ", G: " + rgbObject.getGValue() + ", B: " + rgbObject.getBValue() + " ]";
            logMessage.setText(logText);

        } catch (Exception e) {
            Log.e("RGB", "Exception while getting view for RGBArrayAdapter");
        }

        return rgbView;
    }

}