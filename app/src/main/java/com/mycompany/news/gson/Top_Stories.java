package com.mycompany.news.gson;

import com.google.gson.annotations.SerializedName;


public class Top_Stories {

    @SerializedName("title")
    public String newTitle_top;

    @SerializedName("image")
    public String newImage_top;

    @SerializedName("id")
    public String newId_top;
}
