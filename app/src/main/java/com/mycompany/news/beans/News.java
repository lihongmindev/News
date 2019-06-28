package com.mycompany.news.beans;

import java.net.MalformedURLException;
import java.net.URL;

public class News {
    private String newTitle;
    private String newId;
    private URL imageUrl;
    private String date;

    public News(String date, String newId, String title, String imageUrl){
        this.date = date;
        this.newId = newId;
        this.newTitle = title;
        try {
            this.imageUrl = new URL(imageUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public String getDate(){return date;}

    public String getTitle(){
        return newTitle;
    }

    public String getNewId(){
        return newId;
    }

    public URL getImageUrl(){
        return imageUrl;}



}
