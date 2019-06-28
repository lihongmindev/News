package com.mycompany.news;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mycompany.news.beans.News;
import com.mycompany.news.beans.RecyclerList;
import com.mycompany.news.db.BeforeNews;
import com.mycompany.news.db.TodayNews;
import com.mycompany.news.db.TopNews;
import com.mycompany.news.gson.LatestNews;
import com.mycompany.news.gson.LatestStories;
import com.mycompany.news.gson.Top_Stories;
import com.mycompany.news.service.OffLineDownLoad;
import com.mycompany.news.util.HttpUtil;
import com.mycompany.news.util.Utility;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static java.lang.Integer.valueOf;
import static java.lang.String.format;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MyBroadcastReceiver myBroadcastReceiver;


    private List<News> rlatestNews = new ArrayList<>();
    private List<News> rbeforeNews = new ArrayList<>();
    private List<News> rtopNews = new ArrayList<>();
    private List<RecyclerList> allRecycler = new ArrayList<>();
    private RecyclerAdapter adapter;
    private Context context;
    private int lastVisibleItem = 0;
    private int Nextdate;
    private final int Head_Type = 0;
    private final int date_Type = 1;
    private final int New_Type = 2;
    private int m = 0;                  //allRecycler的顺序
    private int downloadprogress = 0;
    private Handler handler;
    private Timer timer;

    /**
     * TopNews列表
     */
    private List<TopNews> topNewsList;
    /**
     * TodayNews列表
     */
    private List<TodayNews> todayNewsList;
    /**
     * BeforeNews列表
     */
    private List<BeforeNews> beforeNewsList;

    private OffLineDownLoad.DownloadBinder downloadBinder;
    private ServiceConnection connection = new ServiceConnection() {
        //这两个方法在活动与服务绑定和解除绑定时调用
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder = (OffLineDownLoad.DownloadBinder) service;  //可以在活动中调用DownloadBinder的任何public方法
            // 初始化定时器
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.d("bindService ", "开启定时器");
                    downloadprogress = downloadBinder.getProgress();
                    Message message = new Message();
                    message.what = downloadprogress;
                    handler.sendMessage(message);
                    Log.d("bindService ", "downloadprogress " + downloadprogress);
                }
            }, 0, 200);   //200ms
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = MainActivity.this;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        handler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                //获得NavigationView的list view,从而绑定里面的控件
                NavigationView navigationView = findViewById(R.id.nav_view);
                //获得NavigationView的头部view,从而绑定里面的控件
                View view = navigationView.getHeaderView(0);
                TextView textView = view.findViewById(R.id.lixian);
                if (msg.what != 100) {
                    if (msg.what == 101){
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.latest_news_download_fail), Toast.LENGTH_SHORT).show();
                        timer.cancel();                        //Timer停止
                        timer = null;
                        Log.d("bindService ", "关闭定时器");
                        unbindService(connection);              //对服务进行解绑，否则会造成内存泄漏
                        //获得NavigationView的头部view,从而绑定里面的控件
                        textView.setText(getResources().getString(R.string.fail));
                    }else {
                        textView.setText(String.format("%s%s", String.valueOf(msg.what), getString(R.string.baifenhao)));
                        Log.d("bindService ", "textView " + textView + "%");
                    }
                } else {
                    timer.cancel();                        //Timer停止
                    timer = null;
                    Log.d("bindService ", "关闭定时器");
                    unbindService(connection);              //对服务进行解绑，否则会造成内存泄漏
                }
            }
        };
        /*
        下拉刷新设置
         */
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                allRecycler.clear();      //将数组中存储的数据清空
                rbeforeNews.clear();
                rlatestNews.clear();
                rtopNews.clear();
                m = 0;                      //allRecycler的引索清空
                topNewsList = DataSupport.findAll(TopNews.class);    //必须有，离线下载后的数据库发生了变化
                todayNewsList = DataSupport.findAll(TodayNews.class);
                beforeNewsList = DataSupport.findAll(BeforeNews.class);
                //如果网络可用就网络请求新闻
                if (isNetworkAvailable(MainActivity.this)) {
                    Log.d("首页新闻 ", "网络可用");
                    requestLatestNews();
                    swipeRefreshLayout.setRefreshing(false);    //将刷新动作去除
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.request_from_net), Toast.LENGTH_SHORT).show();
                } else if (topNewsList.size() > 0 && beforeNewsList.size() > 0) {  //这里没有判断todaynews是因为里面可能是昨天离线下载的，已经被删除了
                    Log.d("首页新闻 ", "有离线下载，从数据库查询数据");
                    requestDataNews(getToday());
                    swipeRefreshLayout.setRefreshing(false);    //将刷新动作去除
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.show_news_from_local), Toast.LENGTH_SHORT).show();
                } else {
                    swipeRefreshLayout.setRefreshing(false);    //将刷新动作去除
                    adapter.UpdateRecyclerAdapter(context, allRecycler);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.cannot_get_news), Toast.LENGTH_SHORT).show();
                }
            }
        });
        //侧滑菜单
        mDrawerLayout = findViewById(R.id.drawer_layout);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        //获得NavigationView的list view,从而绑定里面的控件
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setCheckedItem(R.id.nav_call);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                mDrawerLayout.closeDrawers();
                return true;
            }
        });

        //获得NavigationView的头部view,从而绑定里面的控件
        View view = navigationView.getHeaderView(0);
        TextView textView = view.findViewById(R.id.lixian);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {                   //离线下载监听事件
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.
                            WRITE_EXTERNAL_STORAGE}, 1);     //权限请求
                } else {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.start_download), Toast.LENGTH_SHORT).show();
                    Intent bindIntent = new Intent(MainActivity.this, OffLineDownLoad.class);
                    bindService(bindIntent, connection, BIND_AUTO_CREATE);   //绑定服务
                    //BIND_AUTO_CREATE表示活动与服务绑定后自动创建服务
                }
            }
        });

        TextView textView1 = view.findViewById(R.id.username);
        textView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {        //请登录的监听事件
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);               //跳转到登陆活动
            }
        });

        EventBus.getDefault().register(this);   //注册EventBus

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.mycompany.news.offlinedownload");
        myBroadcastReceiver = new MyBroadcastReceiver();
        registerReceiver(myBroadcastReceiver, intentFilter);       //注册动态广播

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new RecyclerAdapter();
        recyclerView.setAdapter(adapter);                     //设置RecyclerView的adapter
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // 在newState为滑到底部时
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Log.d("MainActivity", "滑到底部");
                    // 如果没有隐藏footView，那么最后一个条目的位置就比我们的getItemCount少1
                    if (lastVisibleItem + 1 == adapter.getItemCount()) {

                        Nextdate = valueOf(getNextDay(Nextdate));        //下一天的日期，用于网络请求，也用于下一轮使用

                        String beforedate2 = getNextDay(Nextdate);    //再下一天的日期，只用于数据查询
                        //同一天的消息在数据库中存储的date是实际date，而网络请求时需要前一天的时间
                        List<BeforeNews> lastDayNews = DataSupport.where("date = ?", beforedate2).find(BeforeNews.class);
                        if (lastDayNews.size() > 0) {         //如果数据库有存储就拿出来
                            int n = 0;
                            RecyclerList beforeDate = new RecyclerList(date_Type, lastDayNews.get(0).getDate(), "", "", "", rtopNews);
                            allRecycler.add(m++, beforeDate);    //日期
                            Log.d("首页新闻 ", "添加前天的日期到allRecycler");
                            for (BeforeNews lastdaynews : lastDayNews) {
                                News test = new News(lastdaynews.getDate(), lastdaynews.getNewId(), lastdaynews.getNewTitle(), lastdaynews.getNewImage());
                                rbeforeNews.add(n++, test);
                                RecyclerList news = new RecyclerList(New_Type, lastdaynews.getDate(), lastdaynews.getNewId(), lastdaynews.getNewTitle(), lastdaynews.getNewImage(), rbeforeNews);
                                allRecycler.add(m++, news);      //新闻
                                Log.d("首页新闻 ", "添加前天的新闻到allRecycler");
                            }
                            adapter.UpdateRecyclerAdapter(context, allRecycler);
                            adapter.notifyDataSetChanged();
                        } else {
                            Log.d("首页新闻 ", String.valueOf(Nextdate));
                            requestBeforeNews(Nextdate);
                            Log.d("首页新闻 ", "前天新闻无离线下载，网络请求");
                        }
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // 在滑动完成后，拿到最后一个可见的item的位置
                lastVisibleItem = layoutManager.findLastVisibleItemPosition();
            }
        });

        topNewsList = DataSupport.findAll(TopNews.class);
        todayNewsList = DataSupport.findAll(TodayNews.class);
        beforeNewsList = DataSupport.findAll(BeforeNews.class);

        if (todayNewsList.size() > 0) {
            if (!getToday().equals(todayNewsList.get(0).getDate())) {    //如果离线下载是之前几天下载的
                for (TodayNews todayNews : todayNewsList) {
                    String newid = todayNews.getNewId();           //将todaynews中的数据添加到BeforeNews中，并且将TodayNews中数据删掉
                    String newTitle = todayNews.getNewTitle();
                    String newImage = todayNews.getNewImage();
                    String newBody = todayNews.getNewBody();
                    String newCss = todayNews.getNewCss();
                    String newJs = todayNews.getNewJS();
                    String date = todayNews.getDate();
                    BeforeNews beforeNews = new BeforeNews();
                    beforeNews.setDate(date);
                    beforeNews.setNewId(newid);
                    beforeNews.setNewTitle(newTitle);
                    beforeNews.setNewImage(newImage);  //需要将图片下载好存在目录中取用
                    beforeNews.setNewBody(newBody);
                    beforeNews.setNewCss(newCss);
                    beforeNews.setNewJS(newJs);
                    beforeNews.save();
                    Log.d("日期 ", "将昨天的新闻加入到BeforeNews");

                }
                DataSupport.deleteAll(TodayNews.class);
            }
        }

        if (isNetworkAvailable(MainActivity.this)) {
            Log.d("首页新闻 ", "网络可用");
            requestLatestNews();
            Toast.makeText(MainActivity.this, "网络请求新闻", Toast.LENGTH_SHORT).show();
        } else if (topNewsList.size() > 0 && beforeNewsList.size() > 0) {
            Log.d("首页新闻 ", "有离线下载，从数据库查询数据");
            requestDataNews(getToday());
            Toast.makeText(MainActivity.this, "展示离线下载新闻", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("首页新闻 ", "无离线下载，网络不可用");
            Toast.makeText(MainActivity.this, "无离线下载，网络不可用", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 事件响应方法
     * 接收消息
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LoginEvent event) {
        String msg = event.getMessage();
        NavigationView navigationView = findViewById(R.id.nav_view);
        //获得NavigationView的头部view,从而绑定里面的控件
        View view = navigationView.getHeaderView(0);
        ImageView pic = view.findViewById(R.id.icon_image);
        TextView textView1 = view.findViewById(R.id.username);
        textView1.setText(msg);              //登陆后用户名和头像变化
        pic.setImageResource(R.drawable.pic);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);   //注销EventBus
        unregisterReceiver(myBroadcastReceiver);
        if (handler != null){
            handler.removeCallbacksAndMessages(null);  //防止内存泄露，将handler所持有的message对象从主线程的消息队列中清除。
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.backup:
                Toast.makeText(this, "You clicked Backup", Toast.LENGTH_SHORT).show();
                break;
            case R.id.settings:
                Toast.makeText(this, "You clicked Settings", Toast.LENGTH_SHORT).show();
                break;
            default:
        }
        return true;
    }

    /**
     * 请求LastestNews信息
     */
    public void requestLatestNews() {
        String latestNewsUrl = "https://zhihu-daily.leanapp.cn/api/v1/last-stories";
        HttpUtil.sendOkHttpRequest(latestNewsUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();                  //获得原始数据
                final LatestNews latestNews = Utility.handleLatestNewsResponse(responseText);   //解析数据
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (latestNews != null) {   //判断请求信息是否成功
                            //将最近最新消息显示出来
                            showLatestNewsInfo(latestNews);
                        } else {
                            Toast.makeText(MainActivity.this, getResources().getString(R.string.fail_get_news), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.bad_net), Toast.LENGTH_SHORT).show();
                    }


                });
            }
        });
    }

    /**
     * 请求BeforeNews信息
     */
    public void requestBeforeNews(int nextdate) {
        String latestNewsUrl = "https://zhihu-daily.leanapp.cn/api/v1/before-stories/" + nextdate;
        HttpUtil.sendOkHttpRequest(latestNewsUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();                  //获得原始数据
                final LatestNews latestNews = Utility.handleLatestNewsResponse(responseText);   //解析数据
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (latestNews != null) {   //判断请求信息是否成功
                            showBeforeNewsInfo(latestNews);
                        } else {
                            Nextdate = valueOf(getBeforeDay(Nextdate));   //使下一次网络请求还是之前的日期
                        }
                    }
                });
            }
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.bad_net), Toast.LENGTH_SHORT).show();
                        Nextdate = valueOf(getBeforeDay(Nextdate));       //使下一次网络请求还是之前的日期
                    }
                });
            }
        });
    }

    public void requestDataNews(String today){
        int j = 0;
        int i = 0;
        int n = 0;
        for (TopNews topnews : topNewsList) {
            News topNews = new News("", topnews.getNewId(), topnews.getNewTitle(), topnews.getNewImage());
            rtopNews.add(j++, topNews);
        }
        //TopNews
        RecyclerList head = new RecyclerList(Head_Type, "", "", "", "", rtopNews);
        allRecycler.add(m++, head);
        Log.d("首页新闻 ", "添加顶部新闻到allRecycler");

        //TodayDate
        RecyclerList date = new RecyclerList(date_Type, today, "", "", "", rtopNews);
        allRecycler.add(m++, date);
        Log.d("首页新闻 ", "添加今天的日期到allRecycler");
        for (TodayNews todaynews : todayNewsList) {
            News test = new News(today, todaynews.getNewId(), todaynews.getNewTitle(), todaynews.getNewImage());
            rlatestNews.add(i++, test);
            //TodayNews
            RecyclerList news = new RecyclerList(New_Type, todaynews.getDate(), todaynews.getNewId(), todaynews.getNewTitle(), todaynews.getNewImage(), rlatestNews);
            allRecycler.add(m++, news);
            Log.d("首页新闻 ", "添加今日新闻到allRecycler");
        }

        Date date1 = new Date(); // 新建一个日期
        Calendar c = Calendar.getInstance(); // 默认得到的是当前的日期
        c.setTime(date1);
        for (int k=1;k<50;k++){                    //将50天内的离线下载新闻显示
            c.set(Calendar.DATE, c.get(Calendar.DATE) - 1);
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH) + 1;
            int day = c.get(Calendar.DAY_OF_MONTH);
            String lastDate = format(Locale.CHINA, "%s%02d%02d", year, month, day);  //昨天的日期
            List<BeforeNews> lastDayNews = DataSupport.where("date = ?", lastDate).find(BeforeNews.class);

            if (lastDayNews.size()>0){
                Nextdate = valueOf(lastDate);     //得到最后有新闻的一天，用于滑到recyclerView底部使用
                //LastDate
                RecyclerList date2 = new RecyclerList(date_Type, lastDate, "", "", "", rtopNews);
                allRecycler.add(m++, date2);
                Log.d("首页新闻 ", "添加昨天的日期到allRecycler");
                for (BeforeNews lastdaynews : lastDayNews) {
                    News test = new News(lastdaynews.getDate(), lastdaynews.getNewId(), lastdaynews.getNewTitle(), lastdaynews.getNewImage());
                    rbeforeNews.add(n++, test);
                    RecyclerList news = new RecyclerList(New_Type, lastdaynews.getDate(), lastdaynews.getNewId(), lastdaynews.getNewTitle(), lastdaynews.getNewImage(), rbeforeNews);
                    allRecycler.add(m++, news);
                    Log.d("首页新闻 ", "添加昨天的新闻到allRecycler");
                }
            }
        }
        adapter.UpdateRecyclerAdapter(context, allRecycler);
        adapter.notifyDataSetChanged();
    }

    /**
     * 处理并展示LatestNews实体类中的数据
     */
    public void showLatestNewsInfo(LatestNews latestNews) {

        String LatestDate = latestNews.date;     //最新日期
        Nextdate = valueOf(LatestDate);  //beforeNews请求今天的数据，返回的是昨天的新闻
        int i = 0;
        int j = 0;

        for (Top_Stories topStories : latestNews.LatestTopStoriesList) {
            String newid = topStories.newId_top;
            String topNewTitle = topStories.newTitle_top;    //Top新闻题目
            String topNewImage = topStories.newImage_top;    //Top新闻图片
            News topnews = new News(LatestDate, newid, topNewTitle, topNewImage);
            rtopNews.add(j++, topnews);
        }
        RecyclerList head = new RecyclerList(Head_Type, "", "", "", "", rtopNews);
        allRecycler.add(m++, head);

        RecyclerList date = new RecyclerList(date_Type, LatestDate, "", "", "", rtopNews);
        allRecycler.add(m++, date);
        for (LatestStories latestStories : latestNews.LatestStoriesList) {
            String newtitle = latestStories.newTitle;    //最近新闻题目
            String newid = latestStories.newId;           //最近新闻ID
            for (String newsImage : latestStories.NewsImageList) {
                News test = new News(LatestDate, newid, newtitle, newsImage);
                rlatestNews.add(i++, test);
                RecyclerList news = new RecyclerList(New_Type, LatestDate, newid, newtitle, newsImage, rlatestNews);
                allRecycler.add(m++, news);
            }
        }
        adapter.UpdateRecyclerAdapter(context, allRecycler);
        adapter.notifyDataSetChanged();
        /*
        防止出现最新的新闻条数太少无法充满首页的情况
         */
        requestBeforeNews(Nextdate);   //beforeNews请求今天的数据，返回的是昨天的新闻

    }

    /**
     * 处理并展示BeforeNews实体类中的数据
     */
    public void showBeforeNewsInfo(LatestNews latestNews) {

        String LatestDate = latestNews.date;     //最新日期
        int i = 0;
        News test;
        RecyclerList date = new RecyclerList(date_Type, LatestDate, "", "", "", rtopNews);
        allRecycler.add(m++, date);

        for (LatestStories latestStories : latestNews.LatestStoriesList) {
            String newtitle = latestStories.newTitle;    //最近新闻题目
            String newid = latestStories.newId;           //最近新闻ID
            for (String newsImage : latestStories.NewsImageList) {
                test = new News(LatestDate, newid, newtitle, newsImage);   //最近新闻图片
                rbeforeNews.add(i++, test);
                RecyclerList news = new RecyclerList(New_Type, LatestDate, newid, newtitle, newsImage, rbeforeNews);
                allRecycler.add(m++, news);
            }
        }
        adapter.UpdateRecyclerAdapter(context, allRecycler);
        adapter.notifyDataSetChanged();
    }

    /**
     * 调此方法输入所要转换的时间输入例如（"2014-06-14-16-09-00"）返回时间戳
     *
     */
    public static String dataOne(String time) {
        SimpleDateFormat sdr = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss",
                Locale.CHINA);
        Date date;
        String times = null;
        try {
            date = sdr.parse(time);
            long l = date.getTime();
            String stf = String.valueOf(l);
            times = stf.substring(0, 10);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return times;
    }

    /**
     * 调用此方法输入所要转换的时间戳输入例如（1402733340）输出（"2014-06-14  16:09:00"）
     *
     */
    public static String timedate(String time) {

        SimpleDateFormat sdr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        @SuppressWarnings("unused")
        long lcc = Long.valueOf(time);
        int i = Integer.parseInt(time);
        return sdr.format(new Date(i * 1000L));
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent bindIntent = new Intent(MainActivity.this, OffLineDownLoad.class);
                    bindService(bindIntent, connection, BIND_AUTO_CREATE);        //绑定服务
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.start_download), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getResources().getString(R.string.deny_permission), Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("tag", "接收广播");
            Toast.makeText(context, getResources().getString(R.string.download_success), Toast.LENGTH_SHORT).show();
            //获得NavigationView的list view,从而绑定里面的控件
            NavigationView navigationView = findViewById(R.id.nav_view);
            //获得NavigationView的头部view,从而绑定里面的控件
            View view = navigationView.getHeaderView(0);
            TextView textView = view.findViewById(R.id.lixian);
            textView.setText(getResources().getString(R.string.complete));
        }
    }

    /**
     * 检测当的网络（WLAN、3G/2G）状态
     *
     * @param context Context
     * @return true 表示网络可用
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                // 当前网络是连接的
                return info.getState() == NetworkInfo.State.CONNECTED;
            }
        }
        return false;
    }

    public String getToday(){
        Calendar t = Calendar.getInstance(); // 默认得到的是当前的日期
        int tyear = t.get(Calendar.YEAR);
        int tmonth = t.get(Calendar.MONTH) + 1;
        int tday = t.get(Calendar.DAY_OF_MONTH);
        String today = format(Locale.CHINA, "%s%02d%02d", tyear, tmonth, tday);
        return today;
    }
    public String getNextDay(int date){
        String time = format(Locale.CHINA, "%s-%02d-%02d-00-00-00", date / 10000, date / 100 % 100, date % 100);
        int times = valueOf(dataOne(time)) - 60 * 60 * 24;
        String sleepDatess = timedate(String.valueOf(times));
        String date1 = sleepDatess.replace("-", "");
        String date2 = date1.replace(" 00:00:00", "");
        return date2;
    }
    public String getBeforeDay(int date){
        String time = format(Locale.CHINA, "%s-%02d-%02d-00-00-00", date / 10000, date / 100 % 100, date % 100);
        int times = valueOf(dataOne(time)) + 60 * 60 * 24;
        String sleepDatess = timedate(String.valueOf(times));
        String date1 = sleepDatess.replace("-", "");
        String date2 = date1.replace(" 00:00:00", "");
        return date2;
    }

}


