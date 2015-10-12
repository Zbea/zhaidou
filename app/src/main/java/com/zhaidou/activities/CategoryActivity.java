package com.zhaidou.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.Window;
import android.widget.TextView;


import com.umeng.analytics.MobclickAgent;
import com.viewpagerindicator.TabPageIndicator;
import com.zhaidou.R;
import com.zhaidou.fragments.GoodsSingleListFragment;
import com.zhaidou.fragments.SearchArticleListFragment;

import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends FragmentActivity {

    private TextView mTextView;
    private TabPageIndicator mIndicator;
    private ViewPager mViewPager;
    private List<Fragment> mFragments;
    private GoodsSingleListFragment mGoodsSingleListFragment;
    private SearchArticleListFragment mStrategyFragment;
    private CategoryFragmentAdapter mFragmentAdapter;

    private int categoryId;
    private String title;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        findViewById(R.id.rl_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        categoryId=getIntent().getIntExtra("id",0);
        title=getIntent().getStringExtra("title");

        mTextView=(TextView)findViewById(R.id.tv_category_name);
        mTextView.setText(title);

        mIndicator=(TabPageIndicator)findViewById(R.id.indicator);
        mViewPager=(ViewPager)findViewById(R.id.vp_category);
        mFragments=new ArrayList<Fragment>();
        mFragmentAdapter=new CategoryFragmentAdapter(getSupportFragmentManager());

        mViewPager.setAdapter(mFragmentAdapter);
        mIndicator.setViewPager(mViewPager);

        if (mFragments.size()<2){
            mGoodsSingleListFragment = GoodsSingleListFragment.newInstance(categoryId + "", "category");
//            mStrategyFragment=StrategyFragment1.newInstance(categoryId+"","category");
            mFragments.add(mGoodsSingleListFragment);
//            mFragments.add(mStrategyFragment);
        }

        mFragmentAdapter.notifyDataSetChanged();
        mIndicator.notifyDataSetChanged();
    }

    private class CategoryFragmentAdapter extends FragmentPagerAdapter {
        public CategoryFragmentAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }
        @Override
        public Fragment getItem(int i) {
            return mFragments.get(i);
        }

        @Override
        public int getCount() {
//            return mFragments.size();
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position==0)
                return "单品";
            return "攻略";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onPageStart("单品列表页");
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd("单品列表页");
        MobclickAgent.onPause(this);
    }
}
