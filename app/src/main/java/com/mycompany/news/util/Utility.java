package com.mycompany.news.util;

import com.google.gson.Gson;
import com.mycompany.news.gson.LatestNews;
import com.mycompany.news.gson.NewsContent;

import org.json.JSONObject;

public class Utility {

    /**
     * 将返回的JSON数据解析成LastestNews实体类
     */
    public static LatestNews handleLatestNewsResponse(String response){
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject LatestNews = jsonObject.getJSONObject("STORIES");
            String latestNews = LatestNews.toString();
            return new Gson().fromJson(latestNews,LatestNews.class);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public static NewsContent handleNewsContentResponse(String response){
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject NewsContent = jsonObject.getJSONObject("CONTENTS");
            String newsContent = NewsContent.toString();
            return new Gson().fromJson(newsContent,NewsContent.class);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
