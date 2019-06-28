package com.mycompany.news.util;
import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;

//获得数据需要与服务器进行交互
//发起一条HTTP请求只需调用sendOkHttpRequest方法，传入请求地址，并注册一个回调来处理服务器响应就可以了

public class HttpUtil {
    private static OkHttpClient client = new OkHttpClient();    //单实例线程，避免每次请求网络都会新建线程
    public static void sendOkHttpRequest(String address,okhttp3.Callback callback){
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }
}
