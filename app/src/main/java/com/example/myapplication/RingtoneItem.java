package com.example.myapplication;

import android.net.Uri;

public class RingtoneItem {
    private String uriString;
    private String title;
    private Uri uri;

    public RingtoneItem(String uriString, String title, Uri uri) {
        this.uriString = uriString;
        this.title = title;
        this.uri = uri;
    }

    public String getUriString() {
        return uriString;
    }

    public String getTitle() {
        return title;
    }

    public Uri getUri() {
        return uri;
    }
}
