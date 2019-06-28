package com.mycompany.news.beans;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class RecyclerList {
    private int Type;
    private String date;
    private String newTitle;
    private String newId;
    private URL imageUrl;
    private List<News> list;

    public RecyclerList(int type , String date, String newId, String title, String imageUrl,List<News> list){
        this.Type = type;
        this.date = date;
        this.newId = newId;
        this.newTitle = title;
        this.list = list;
        try {
            this.imageUrl = new URL(imageUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public List<News> getList(){return list;}

    public int getType(){return Type;}

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
