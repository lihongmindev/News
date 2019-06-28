package com.mycompany.news;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.mycompany.news.beans.RecyclerList;
import com.mycompany.news.db.BeforeNews;
import com.mycompany.news.db.TodayNews;
import com.mycompany.news.db.TopNews;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static java.lang.Integer.valueOf;
import static java.lang.String.format;


public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private final int IMAGE_VIEW_TYPE = 0;//轮播图
    private final int DATE_VIEW_TYPE = 1; //日期
    private final int NORMAL_VIEW_TYPE = 2;//正常布局
    private ArrayList<View> mivViewsList = new ArrayList<>();  //顶部ViewPager图片
    private ArrayList<String> mTopTitleList = new ArrayList<>();    //顶部图片Title
    private List<RecyclerList> recyclerList;       //所有recyclerView中的数据
    private Drawable drawable;

    private ArrayList<String> mNewsIdList = new ArrayList<>();

    private int mNum = 0;
    private int dateNum =0;

    RecyclerAdapter() {

    }
    void UpdateRecyclerAdapter(Context context, List<RecyclerList> recyclerList) {
        this.context = context;
        this.recyclerList = recyclerList;
    }

    /**
     * 获取item的类型
     *
     * @param position item的位置
     * @return item的类型
     * 有几种type就回在onCreateViewHolder方法中引入几种布局,也就是创建几个ViewHolder
     */
    @Override
    public int getItemViewType(int position) {
        /*
        区分item类型,返回不同的int类型的值
        在onCreateViewHolder方法中用viewType来创建不同的ViewHolder
         */
        if (recyclerList.get(position).getType()==0){
            return IMAGE_VIEW_TYPE;
        }else if (recyclerList.get(position).getType()==1){
            return DATE_VIEW_TYPE;
        }else {
            return NORMAL_VIEW_TYPE;
        }
    }

    /**
     * 创建ViewHolder,根据getItemViewType方法里面返回的几种类型来创建
     *
     * @param viewType 就是getItemViewType返回的type
     * @return 返回自己创建的ViewHolder
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == IMAGE_VIEW_TYPE) {      //如果顶部新闻就去创建轮播图的viewHolder
            view = getView(R.layout.image_item);
            return new ImageHolder(view);
        } else if (viewType == DATE_VIEW_TYPE) { //日期
            view = getView(R.layout.date_item);
            return new DateHolder(view);
        } else {                                 //正常
            view = getView(R.layout.news_item);
            return new NormalHolder(view);
        }
    }

    /**
     * 用来引入布局的方法
     */
    private View getView(int view) {
        return View.inflate(context, view, null);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        //判断不同的ViewHolder做不同的处理
        if (holder instanceof ImageHolder) {   //轮播图
            final ImageHolder imageHolder = (ImageHolder) holder;

                setViewPager(imageHolder);     //设置顶部viewpager

        } else if (holder instanceof DateHolder) {//日期

            DateHolder dateHolder = (DateHolder) holder;
            //设置date
            Calendar c = Calendar.getInstance(); // 默认得到的是当前的日期
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH)+1;
            int day = c.get(Calendar.DAY_OF_MONTH);
            String today = format(Locale.CHINA,"%s%02d%02d",year,month,day);
            if (today.equals(recyclerList.get(position).getDate()) )
                dateHolder.textView.setText("今日热闻");
            else {
                int normaldate = valueOf(recyclerList.get(position).getDate());
                String time = format(Locale.CHINA,"%d月%d日",normaldate/100%100,normaldate%100);
                String time1 = format(Locale.CHINA,"%s-%02d-%02d",normaldate/10000,
                        normaldate/100%100,normaldate%100);
                int dayofweek = getDayofWeek(time1);
                String dayofweek1;
                if (dayofweek == 1)
                    dayofweek1 = "日";
                else if (dayofweek == 2)
                    dayofweek1 = "一";
                else if (dayofweek == 3)
                    dayofweek1 = "二";
                else if (dayofweek == 4)
                    dayofweek1 = "三";
                else if (dayofweek == 5)
                    dayofweek1 = "四";
                else if (dayofweek == 6)
                    dayofweek1 = "五";
                else
                    dayofweek1 = "六";
                String beforeDay = format(Locale.CHINA,"%s "+ "星期"+"%s",time,dayofweek1);
                dateHolder.textView.setText(beforeDay);
            }

        } else if (holder instanceof NormalHolder) {//正常布局
            NormalHolder normalHolder = (NormalHolder) holder;
            String newTitle = recyclerList.get(position).getTitle();
            normalHolder.newTitle.setText(newTitle);
            List<TodayNews> todayNews = DataSupport.where("newid = ?",recyclerList.get(position).getNewId()).find(TodayNews.class);
            List<BeforeNews> beforeNews = DataSupport.where("newid = ?",recyclerList.get(position).getNewId()).find(BeforeNews.class);
            if (todayNews.size() != 0 || beforeNews.size() != 0 ){        //如果两个表中存在一个这个id的数据，证明已经下载了这张照片
                    //本地文件
                    File file = new File("/storage/emulated/0/Pictures/news/", recyclerList.get(position).getNewId()+".jpg");
                    //加载图片
                    Glide.with(context).load(file).into(normalHolder.newImage);
                    Log.d("首页新闻 ","news加载本地图片");
            }else {
                Glide.with(context).load(recyclerList.get(position).getImageUrl()).into(normalHolder.newImage);
                Log.d("首页新闻 ","news请求网络图片");
            }
            normalHolder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {        //卡片的点击事件
                    Intent intent = new Intent(context,NewsContent.class);
                    mNewsIdList.clear();
                    for (int i = 0; i< recyclerList.size();i++){            //将所有news的newsId传给NewsContentActivity
                        if (!recyclerList.get(i).getNewId().equals("")){
                            mNewsIdList.add(recyclerList.get(i).getNewId()); //将其中没有newsId的项剔除
                        }
                    }
                    dateNum = 0;
                    for (int i=0; i<position;i++){                          //计算出所点击位置之前的date类型的数量
                        if (recyclerList.get(i).getType() == 1){
                            dateNum++;
                        }
                    }
                    intent.putExtra("new_position",position -dateNum-1);     //将该点击位置在传递数组中的位置传递
                    intent.putExtra("new_id",mNewsIdList);
                    context.startActivity(intent);
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        if (recyclerList!=null)
        return recyclerList.size();  //除正常布局还有两个不同的布局，返回的是整个recyclerView的Item数
        else return 0;
    }

    private int getDayofWeek(String dateTime) {
        Calendar cal = Calendar.getInstance();
        if (dateTime.equals("")) {
            cal.setTime(new Date(System.currentTimeMillis()));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = null;
            try {
                date = sdf.parse(dateTime);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (java.text.ParseException e) {
                e.printStackTrace();
            }
            if (date != null) {
                cal.setTime(new Date(date.getTime()));
            }
        }
        return cal.get(Calendar.DAY_OF_WEEK);
    }


    /**
     * 图片的ViewHolder
     */
    public static class ImageHolder extends RecyclerView.ViewHolder {
        RelativeLayout topnewslayout;
        LinearLayout linearLayout;
        ViewPager viewPager;
        ImageHolder(View itemView) {
            super(itemView);
            topnewslayout = itemView.findViewById(R.id.top_news_layout);
            viewPager = itemView.findViewById(R.id.viewpager);
            linearLayout = itemView.findViewById(R.id.main_linear);
        }
    }

    /**
     * 日期的ViewHolder
     */
    public static class DateHolder extends RecyclerView.ViewHolder {
        LinearLayout linearLayout;
        TextView textView;
        DateHolder(View itemView) {
            super(itemView);
            linearLayout = itemView.findViewById(R.id.date);
            textView = itemView.findViewById(R.id.new_date);
        }
    }

    /**
     * 正常布局的ViewHolder
     */
    public static class NormalHolder extends RecyclerView.ViewHolder {
        RelativeLayout Layout;
        CardView cardView;
        ImageView newImage;
        TextView newTitle;
        NormalHolder(View itemView) {
            super(itemView);
            Layout = itemView.findViewById(R.id.card_layout);
            cardView = itemView.findViewById(R.id.news);
            newImage = itemView.findViewById(R.id.new_image);
            newTitle = itemView.findViewById(R.id.new_title);
        }
    }

    /**
     * 设置ViewPager
     */
    private void setViewPager(final ImageHolder imageHolder){
        View view;
        imageHolder.linearLayout.removeAllViews();    //防止每刷新一次就多一组小圆点
        mivViewsList.clear();
        mNum = 0;
        LayoutInflater inflater = LayoutInflater.from(context);      //布局解析器
        drawable = ContextCompat.getDrawable(context,R.drawable.imgcover);  //前景图片
        for ( int i=0;i<recyclerList.get(0).getList().size();i++) {
            View topnewsView = inflater.inflate(R.layout.viewpager_topnews, null);
            ImageView newsimage = topnewsView.findViewById(R.id.image_topnews);
            newsimage.setScaleType(ImageView.ScaleType.CENTER_CROP);  //使图片充满布局
            TextView newstitle = topnewsView.findViewById(R.id.top_new_title);
            newstitle.setText(recyclerList.get(0).getList().get(i).getTitle());
            List<TopNews> topNews = DataSupport.where("newid = ?",recyclerList.get(0).getList().get(i).getNewId()).find(TopNews.class);
            if (topNews.size()>0){
                File file = new File("/storage/emulated/0/Pictures/news/", recyclerList.get(0).getList().get(i).getNewId()+".jpg");
                Glide.with(context).load(file).into(newsimage);
                Log.d("首页新闻 ","top news加载本地图片");
            }else{
                Glide.with(context).load(recyclerList.get(0).getList().get(i).getImageUrl()).into(newsimage);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                newsimage.setForeground(drawable);
            }
            //mTopTitleList.add(recyclerList.get(0).getList().get(i).getTitle());   //将题目取到数组中
            mivViewsList.add(topnewsView);
            //创建底部指示器(小圆点)
            view = new View(context);
            view.setBackgroundResource(R.drawable.background);
            view.setEnabled(false);
            //设置宽高
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(20, 20);
            //设置间隔
            layoutParams.leftMargin = 10;
            //添加到LinearLayout
            imageHolder.linearLayout.addView(view, layoutParams);
        }
        imageHolder.linearLayout.getChildAt(0).setEnabled(true);         //第一次显示小白点
        ViewPageAdapter mAdapter = new ViewPageAdapter(context,mivViewsList,recyclerList);
        imageHolder.viewPager.setAdapter(mAdapter);
        imageHolder.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }
            @Override
            public void onPageSelected(int position) {
                imageHolder.linearLayout.getChildAt(mNum).setEnabled(false);
                imageHolder.linearLayout.getChildAt(position).setEnabled(true);
                mNum = position;
            }
            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
        mAdapter.notifyDataSetChanged();
    }
}
