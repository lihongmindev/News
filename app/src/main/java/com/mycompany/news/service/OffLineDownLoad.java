package com.mycompany.news.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.mycompany.news.db.BeforeNews;
import com.mycompany.news.db.TodayNews;
import com.mycompany.news.db.TopNews;
import com.mycompany.news.gson.LatestNews;
import com.mycompany.news.gson.LatestStories;
import com.mycompany.news.gson.Top_Stories;
import com.mycompany.news.threadpool.ThreadPoolProxyFactory;
import com.mycompany.news.util.HttpUtil;
import com.mycompany.news.util.Utility;

import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static java.lang.String.format;

public class OffLineDownLoad extends Service {

    private int numTopNews = 0;
    private int numTodayNews = 0;
    private int numBeforeNews = 0;
    private int sumTopNews = 0;
    private int sumTodayNews = 0;
    private int sumBeforeNews = 0;
    private boolean fTopNewsGet = false;   //顶部新闻已经全部插入数据库
    private boolean fTodayNewsGet = false;
    private boolean fBeforeNewsGet = false;
    private int beforeNewsCounts = 0;     //beforeNews的总数量计数
    private int topNewsCounts = 0;        //topNews的总条数
    private int todayNewsCounts = 0;
    private int downloadprogress = 0;


    private DownloadBinder mBinder = new DownloadBinder();

    public class DownloadBinder extends Binder {
        public int getProgress() {
            if (topNewsCounts > 0 && todayNewsCounts > 0 && beforeNewsCounts > 0) {
                downloadprogress = (numTopNews + numTodayNews + numBeforeNews) * 100 /
                        (topNewsCounts + todayNewsCounts + beforeNewsCounts);        //下载进度百分比
                Log.d("downloadprogress", String.valueOf(downloadprogress));
            }
            Log.d("bindService", "getProgress executed");
            return downloadprogress;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Connector.getDatabase();                //创建数据库
                Log.d("tag", "create a Database");

                requestLatestNews();                    //请求最近一天的新闻并存在数据库

                for (int i = 0; i < 2; i++) {
                    Date date = new Date();               //新建一个日期
                    Calendar c = Calendar.getInstance();  //默认得到的是当前的日期
                    c.setTime(date);
                    c.set(Calendar.DATE, c.get(Calendar.DATE) - i);
                    int year = c.get(Calendar.YEAR);
                    int month = c.get(Calendar.MONTH) + 1;
                    int day = c.get(Calendar.DAY_OF_MONTH);
                    String lastDate = format(Locale.CHINA, "%s%02d%02d", year, month, day);
                    requestBeforeNews(lastDate);         //请求之前三天的新闻并存在数据库
                }
                stopSelf();                               //停止服务
                Log.d("tag", "stop service");

            }
        };
        ThreadPoolProxyFactory.getDownLoadThreadPoolProxy().execute(runnable);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public void requestLatestNews() {

        Log.d("tag", "start requestLatestNews");
        String latestNewsUrl = "https://zhihu-daily.leanapp.cn/api/v1/last-stories";
        HttpUtil.sendOkHttpRequest(latestNewsUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();                           //获得原始数据
                final LatestNews latestNews = Utility.handleLatestNewsResponse(responseText);   //解析数据
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Thread reqLatestNews", "run: " + Thread.currentThread().getName());
                        if (latestNews != null) {
                            Log.d("tag", "get requestLatestNews");
                            String LatestDate = latestNews.date;     //最新日期
                            topNewsCounts = latestNews.LatestTopStoriesList.size();  //topNews的总条数
                            todayNewsCounts = latestNews.LatestStoriesList.size();
                            for (Top_Stories topStories : latestNews.LatestTopStoriesList) {
                                String newid = topStories.newId_top;              //Top新闻ID
                                requestTopNewsContent(newid, topNewsCounts);       //请求Top新闻
                            }
                            for (LatestStories latestStories : latestNews.LatestStoriesList) {
                                String newid = latestStories.newId;               //Today新闻ID
                                requestTodayNewsContent(newid, todayNewsCounts, LatestDate);   //请求Today新闻
                            }

                        } else {
                            downloadprogress = 101;
                        }
                    }
                };
                ThreadPoolProxyFactory.getDownLoadThreadPoolProxy().execute(runnable);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.d("tag", "fail");
                downloadprogress = 101;
            }
        });
    }

    public void requestTopNewsContent(String news_id, final int newsCounts) {
        Log.d("tag", "start request top NewsContent " + news_id);
        String newsContentUrl = "https://zhihu-daily.leanapp.cn/api/v1/contents/" + news_id;
        HttpUtil.sendOkHttpRequest(newsContentUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("tag", "get top NewsContent onResponse");
                final String responseText = response.body().string();                  //获得原始数据
                final com.mycompany.news.gson.NewsContent newsContent = Utility.handleNewsContentResponse(responseText);   //解析数据
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Thread reTopnewsContent", "run: " + Thread.currentThread().getName());
                        if (newsContent != null) {
                            String newid = newsContent.NewId;
                            String newTitle = newsContent.NewTitle;
                            String newImage = newsContent.NewImage;      //需要下载图片
                            String newBody = newsContent.NewBody;
                            String newCss = "file:///android_asset/zhihu_master.css";
                            String newJs = null;
                            for (String newJsa : newsContent.NewJs) {
                                newJs = newJsa;
                            }
                            List<TopNews> topnews = DataSupport.where("newid = ?", newid).find(TopNews.class);
                            if (topnews.size() == 0) {              //如果表中存在一个这个id的数据，证明已经下载了
                                TopNews topNews = new TopNews();
                                topNews.setNewId(newid);
                                topNews.setNewTitle(newTitle);
                                topNews.setNewImage(newImage);
                                topNews.setNewBody(newBody);
                                topNews.setNewCss(newCss);
                                topNews.setNewJS(newJs);
                                topNews.save();
                                Thread thread = new saveImage(newImage, newid);  //需要将图片下载好存在目录中取用
                                ThreadPoolProxyFactory.getDownLoadThreadPoolProxy().execute(thread);
                                //thread.start();
                                Log.d("tag", "insert top news " + newTitle);
                            }
                            numTopNews++;        //每插入一条数据加一
                            sumTopNews += Math.pow(2, numTopNews - 1);        //计算插入数据顺序的累加 1+2+4.。。
                            Log.d("tag", "numTopNews " + numTopNews);
                            Log.d("tag", "sumTopNews " + sumTopNews);
                            if (numTopNews == newsCounts && sumTopNews == Math.pow(2, newsCounts) - 1) {
                                //若插入数据的数量和新闻的真实数量相等，且累加等于2的真实数量次方减1
                                fTopNewsGet = true;       //TopNews全部插入
                                Log.d("tag", "fTopNewsGet " + true);
                            }
                            if (fTopNewsGet && fTodayNewsGet && fBeforeNewsGet) {
                                Intent intent = new Intent("com.mycompany.news.offlinedownload");
                                sendBroadcast(intent);      //发送广播
                                Log.d("tag", "TopNews发送广播");
                            }
                        } else {
                            downloadprogress = 101;
                            Log.d("tag", "顶部新闻下载失败");
                        }
                    }
                };
                ThreadPoolProxyFactory.getDownLoadThreadPoolProxy().execute(runnable);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                downloadprogress = 101;
            }
        });
    }

    public void requestTodayNewsContent(String news_id, final int newsCounts, final String today) {
        Log.d("tag", "start request today NewsContent " + news_id);
        String newsContentUrl = "https://zhihu-daily.leanapp.cn/api/v1/contents/" + news_id;
        HttpUtil.sendOkHttpRequest(newsContentUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("tag", "get today NewsContent onResponse");
                final String responseText = response.body().string();                  //获得原始数据
                final com.mycompany.news.gson.NewsContent newsContent = Utility.handleNewsContentResponse(responseText);   //解析数据
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Thread reTodanewContent", "run: " + Thread.currentThread().getName());
                        if (newsContent != null) {
                            Log.d("tag", "get today NewsContent");
                            String newid = newsContent.NewId;
                            String newTitle = newsContent.NewTitle;
                            String newImage = newsContent.NewImage;
                            String newBody = newsContent.NewBody;
                            String newCss = "file:///android_asset/zhihu_master.css";
                            String newJs = null;
                            for (String newJsa : newsContent.NewJs) {
                                newJs = newJsa;
                            }
                            List<TodayNews> todaynews = DataSupport.where("newid = ?", newid).find(TodayNews.class);
                            if (todaynews.size() == 0) {          //如果表中存在一个这个id的数据，证明已经下载了这张照片
                                TodayNews todayNews = new TodayNews();
                                todayNews.setNewId(newid);
                                todayNews.setNewTitle(newTitle);
                                todayNews.setNewImage(newImage);
                                todayNews.setNewBody(newBody);
                                todayNews.setNewCss(newCss);
                                todayNews.setNewJS(newJs);
                                todayNews.setDate(today);
                                todayNews.save();
                                Thread thread = new saveImage(newImage, newid);  //需要将图片下载好存在目录中取用
                                ThreadPoolProxyFactory.getDownLoadThreadPoolProxy().execute(thread);
                                //thread.start();
                                Log.d("tag", "insert today news " + newTitle);
                            }
                            numTodayNews++;      //每插入一条数据加一
                            sumTodayNews += Math.pow(2, numTodayNews - 1);        //计算插入数据顺序的累加 1+2+4.。。
                            Log.d("tag", "numTodayNews " + numTodayNews);
                            Log.d("tag", "sumTodayNews " + sumTodayNews);
                            if (numTodayNews == newsCounts && sumTodayNews == Math.pow(2, newsCounts) - 1) {
                                //若插入数据的数量和新闻的真实数量相等，且累加等于2的真实数量次方减1
                                fTodayNewsGet = true;
                                Log.d("tag", "fTodayNewsGet " + true);
                            }
                            if (fTopNewsGet && fTodayNewsGet && fBeforeNewsGet) {
                                //发送广播
                                Intent intent = new Intent("com.mycompany.news.offlinedownload");
                                sendBroadcast(intent);      //发送广播
                                Log.d("tag", "TodayNews发送广播");

                            }
                        } else {
                            Log.d("tag", "今日新闻下载失败");
                            downloadprogress = 101;
                        }
                    }
                };
                ThreadPoolProxyFactory.getDownLoadThreadPoolProxy().execute(runnable);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                downloadprogress = 101;
            }
        });
    }

    public void requestBeforeNews(String lastDate) {
        Log.d("tag", "start requestBeforeNews");
        String latestNewsUrl = "https://zhihu-daily.leanapp.cn/api/v1/before-stories/" + lastDate;
        HttpUtil.sendOkHttpRequest(latestNewsUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();                  //获得原始数据
                final LatestNews latestNews = Utility.handleLatestNewsResponse(responseText);   //解析数据
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Thread reBeforeNews", "run: " + Thread.currentThread().getName());
                        if (latestNews != null) {
                            String BeforeDate = latestNews.date;     //新闻日期
                            Log.d("tag", "get requestBeforeNews " + latestNews.date);
                            beforeNewsCounts += latestNews.LatestStoriesList.size();
                            Log.d("tag", "beforeNewsCounts " + beforeNewsCounts);
                            for (LatestStories latestStories : latestNews.LatestStoriesList) {
                                String newid = latestStories.newId;           //新闻ID
                                requestBeforeNewsContent(newid, BeforeDate);  //请求之前新闻
                            }
                        } else {
                            downloadprogress = 101;
                            Log.d("tag", "最近新闻下载失败");
                        }
                    }
                };
                ThreadPoolProxyFactory.getDownLoadThreadPoolProxy().execute(runnable);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                downloadprogress = 101;
            }
        });
    }

    public void requestBeforeNewsContent(String news_id, final String beforeDate) {

        Log.d("tag", "start request before NewsContent " + news_id);
        String newsContentUrl = "https://zhihu-daily.leanapp.cn/api/v1/contents/" + news_id;
        HttpUtil.sendOkHttpRequest(newsContentUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("tag", "get before NewsContent onResponse");
                final String responseText = response.body().string();                  //获得原始数据
                final com.mycompany.news.gson.NewsContent newsContent = Utility.handleNewsContentResponse(responseText);   //解析数据
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Thread reBefNewsContent", "run: " + Thread.currentThread().getName());
                        if (newsContent != null) {
                            Log.d("tag", "get before NewsContent");
                            String newid = newsContent.NewId;
                            String newTitle = newsContent.NewTitle;
                            String newImage = newsContent.NewImage;
                            String newBody = newsContent.NewBody;
                            String newCss = "file:///android_asset/zhihu_master.css";
                            String newJs = null;
                            for (String newJsa : newsContent.NewJs) {
                                newJs = newJsa;
                            }
                            List<BeforeNews> beforenews = DataSupport.where("newid = ?", newid).find(BeforeNews.class);
                            if (beforenews.size() == 0) {        //如果表中存在一个这个id的数据，证明已经下载了这张照片
                                BeforeNews beforeNews = new BeforeNews();
                                beforeNews.setNewId(newid);
                                beforeNews.setNewTitle(newTitle);
                                beforeNews.setNewImage(newImage);
                                beforeNews.setNewBody(newBody);
                                beforeNews.setNewCss(newCss);
                                beforeNews.setNewJS(newJs);
                                beforeNews.setDate(beforeDate);
                                beforeNews.save();
                                Thread thread = new saveImage(newImage, newid);  //需要将图片下载好存在目录中取用
                                ThreadPoolProxyFactory.getDownLoadThreadPoolProxy().execute(thread);
                                //thread.start();
                                Log.d("tag", "insert before news " + newTitle);
                            }
                            numBeforeNews++;      //每插入一条数据加一
                            sumBeforeNews += Math.pow(2, numBeforeNews - 1);        //计算插入数据顺序的累加 1+2+4.。。
                            Log.d("tag", "numBeforeNews " + numBeforeNews);
                            Log.d("tag", "sumBeforeNews " + sumBeforeNews);
                            if (numBeforeNews == beforeNewsCounts && sumBeforeNews == Math.pow(2, beforeNewsCounts) - 1) {
                                //若插入数据的数量和新闻的真实数量相等，且累加等于2的真实数量次方减1
                                fBeforeNewsGet = true;
                                Log.d("tag", "fBeforeNewsGet " + true);
                            }
                            if (fTopNewsGet && fTodayNewsGet && fBeforeNewsGet) {
                                //发送广播
                                Intent intent = new Intent("com.mycompany.news.offlinedownload");
                                sendBroadcast(intent);        //发送广播
                                Log.d("tag", "BeforeNews发送广播");
                            }
                        } else {
                            downloadprogress = 101;
                        }
                    }
                };
                ThreadPoolProxyFactory.getDownLoadThreadPoolProxy().execute(runnable);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                downloadprogress = 101;
            }
        });
    }
    /*
    保存图片
     */
    public class saveImage extends Thread {

        private String newimage;
        private String newtitle;
        private String shareImg;     //图片存储路径

        saveImage(String newimage, String newtitle) {
            this.newimage = newimage;
            this.newtitle = newtitle;
        }
        public void run() {
            Log.d("Thread saveImg", "run: " + Thread.currentThread().getName());
            try {
                Bitmap bmp = Glide.with(OffLineDownLoad.this)
                        .load(newimage)
                        .asBitmap()
                        .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get();
                shareImg = saveImageToGallery(OffLineDownLoad.this, bmp, newtitle);  //图片存储路径
                Log.d("tag", newtitle + " 图片下载成功 " + shareImg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

    }

    public String saveImageToGallery(Context context, Bitmap bmp, String filename) {
        // 首先保存图片
        String shareImg = null;
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File appDir = new File(file, "news");
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        File currentFile = new File(appDir, filename + ".jpg");
        if (!currentFile.exists()) {
            try {
                currentFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(currentFile, true);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            shareImg = currentFile.getAbsolutePath();    //保存图片的本地地址

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return shareImg;
    }
}
