package com.mycompany.news;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.mycompany.news.beans.RecyclerList;

import java.util.ArrayList;
import java.util.List;

class ViewPageAdapter extends PagerAdapter {
    private Context context;
    private ArrayList<View> ivGoodsList;
    private List<RecyclerList> recyclerList;       //所有recyclerView中的数据

    private ArrayList<String> mNewsIdList = new ArrayList<>();


    public ViewPageAdapter(Context context, ArrayList<View> ivGoodsList, List<RecyclerList> recyclerList) {
        this.context = context;
        this.ivGoodsList = ivGoodsList;
        this.recyclerList = recyclerList;

    }

    @Override
    public int getCount() {
        return ivGoodsList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {

        View view = ivGoodsList.get(position);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context, NewsContent.class);
                mNewsIdList.clear();

                for (int i = 0; i < recyclerList.get(0).getList().size(); i++) {           //将该位置之后的newsId传给NewsContentActivity
                    if (!recyclerList.get(0).getList().get(i).getNewId().equals("")) {
                        mNewsIdList.add(recyclerList.get(0).getList().get(i).getNewId());
                    }
                }
                intent.putExtra("new_id", mNewsIdList);
                intent.putExtra("new_position", position);
                context.startActivity(intent);

            }
        });
        container.addView(view);
        return view;

    }
    /*
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        ImageView view = mViewSparseArray.get(position);
        if (view == null) {
            view = new ImageView(context);
            mViewSparseArray.put(position, view);
        }
        Glide.with(context).load(file).into(view);   //将图片路径给imageView
        container.addView(view);
        return view;
    }   */

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = (View) object;
        container.removeView(view);

    }

}

