package com.mycompany.news;

import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class ContentViewAdapter extends PagerAdapter {

    private ArrayList<View> listViews ;// content
    private int size;// 页数


    public ContentViewAdapter(ArrayList<View> listViews){
        this.listViews = listViews;
        size = listViews == null ? 0 : listViews.size();
    }


    //数据源的数目
    public int getCount() {
        return size;
    }


    //view是否由对象产生，官方写arg0==arg1即可
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0==arg1;

    }

    //对应页卡添加上数据

    @Override
    public Object instantiateItem(ViewGroup container,  int position)
    {
        try {
            container.addView(listViews.get( position ));
            Log.d("Tag", String.valueOf(position));
        } catch (Exception e) {
            Log.e("Tag", "exception：" + e.getMessage());
        }
        return listViews.get( position );
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(listViews.get(position % size));

    }


}
