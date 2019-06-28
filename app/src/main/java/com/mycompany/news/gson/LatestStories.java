package com.mycompany.news.gson;



import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LatestStories {
    @SerializedName("title")
    public String newTitle;

    @SerializedName("images")
    //public String newImages;
    public List<String> NewsImageList;

    @SerializedName("id")
    public String newId;




}
