package com.mycz.tree.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.mycz.tree.R;
import com.mycz.tree.fragments.InvestFragment;
import com.mycz.tree.fragments.MapToolsFragment;
import com.mycz.tree.fragments.QueryFragment;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    private ViewPager mVpContent;
    private TabLayout mTabLayout;
    private ArrayList<String> mTitles;
    private ArrayList<Fragment> mFragments;
    private ArrayList<Integer> mIcons;
    private Toolbar mToolBar;
    private InvestFragment mInvestFragment;
    private QueryFragment mQueryFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    /**
     * 初始化界面
     */
    private void initView() {
        mVpContent = findViewById(R.id.vp_content);
        mTabLayout = findViewById(R.id.tab_layout);
        mToolBar = findViewById(R.id.toolbar);


        setSupportActionBar(mToolBar);
        // 显示标题和子标题
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        mTitles = new ArrayList<>();
        mTitles.add("调查");
        mTitles.add("查询");
        mTitles.add("我的");

        mFragments = new ArrayList<>();
        mInvestFragment = new InvestFragment();
        mFragments.add(mInvestFragment);
        mQueryFragment = new QueryFragment();
        mFragments.add(mQueryFragment);
        mFragments.add(new MapToolsFragment());

        mIcons = new ArrayList<>();
        mIcons.add(R.drawable.tab_img_invest);
        mIcons.add(R.drawable.tab_img_upload);
        mIcons.add(R.drawable.tab_img_self);

        mVpContent.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int i) {
                return mFragments.get(i);
            }

            @Override
            public int getCount() {
                return mFragments.size();
            }

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                return mTitles.get(position);
            }
        });

        mTabLayout.setupWithViewPager(mVpContent);

        mVpContent.setOffscreenPageLimit(2);

        for (int i = 0; i < 3; i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            tab.setIcon(mIcons.get(i));
        }


    }

    /**
     * 设置app加载menu
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        setIconEnable(menu, true);
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    /**
     * 设置menu显示图标
     * @param menu
     * @param isVisible
     */
    private void setIconEnable(Menu menu, boolean isVisible) {
        if(menu != null) {
            try {
                Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", boolean.class);
                method.setAccessible(true);
                method.invoke(menu, isVisible);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.scanner:
                Toast.makeText(this,"跳转到扫一扫",Toast.LENGTH_SHORT).show();
                IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
                intentIntegrator.setBeepEnabled(true);
                /*设置启动我们自定义的扫描活动，若不设置，将启动默认活动*/
                intentIntegrator.setCaptureActivity(ScanActivity.class);
                intentIntegrator.initiateScan();
                break;
            default:
                break;
        }

        return true;
    }


    /**
     * 当从ScanActivity回退到MainActivity时回调此方法
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            String contents = result.getContents();
            if(contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + contents, Toast.LENGTH_LONG).show();
                EditText editText = mInvestFragment.getView().findViewById(R.id.et_tree_id);
                try {
                    String value = contents.substring(contents

                            .lastIndexOf("_") + 1);
                    int treeId = Integer.parseInt(value) - 1;
                    editText.setText(treeId + "");
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                //加载二维码的网页
                WebView webView = mQueryFragment.getView().findViewById(R.id.web_view);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.setWebViewClient(new WebViewClient());
                try {
                    webView.loadUrl(contents);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
