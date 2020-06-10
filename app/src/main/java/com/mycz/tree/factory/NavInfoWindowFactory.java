package com.mycz.tree.factory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.mycz.tree.R;

/**
 * @author 木已成舟
 * @date 2020/6/5
 */
public class NavInfoWindowFactory {

    private Context mContext;

    public NavInfoWindowFactory(Context context) {
        mContext = context;
    }

    public View getNavInfoWindowView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.info_window, null, false);
        return view;
    }

}
