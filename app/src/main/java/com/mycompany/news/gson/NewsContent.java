package com.mycompany.news.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NewsContent {

    @SerializedName("body")
    public String NewBody;

    @SerializedName("image_source")
    public String ImageSource;

    @SerializedName("title")
    public String NewTitle;

    @SerializedName("image")
    public String NewImage;

    @SerializedName("share_url")
    public String ShareUrl;

    @SerializedName("images")
    public List<String> NewsImageList;

    @SerializedName("id")
    public String NewId;

    @SerializedName("css")
    public List<String> NewCssList;

    @SerializedName("js")
    public List<String> NewJs;



}
