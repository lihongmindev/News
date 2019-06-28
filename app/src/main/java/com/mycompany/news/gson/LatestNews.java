package com.mycompany.news.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LatestNews {

    public String date;

    @SerializedName("stories")
    public List<LatestStories> LatestStoriesList;

    @SerializedName("top_stories")
    public List<Top_Stories> LatestTopStoriesList;

}
