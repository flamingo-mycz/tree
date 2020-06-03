package com.mycz.tree.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.baidu.mapapi.model.LatLng;
import com.mycz.tree.R;
import com.mycz.tree.activity.MapActivity;

/**
 * @author 木已成舟
 * @date 2019/11/5
 */
public class MapToolsFragment extends Fragment {

    public static double LONGITUDE = 0.0;
    public static double LATITUDE = 0.0;
    private RelativeLayout mRlRoute;
    private RelativeLayout mRlMark;
    private Button mBtOpen;


    /**
     * 当Fragment被创建时回调此方法，
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map_tools, container, false);
    }

    /**
     * 当Fragment创建完成时回调此方法
     *
     * @param view
     * @param savedInstanceState
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView(view);
        initEvent();
    }

    /**
     * 初始化事件
     */
    private void initEvent() {
        // 以轨迹方式startActivity
        mRlRoute.setOnClickListener(v -> {
            Toast.makeText(getContext(), "trace", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), MapActivity.class);
            intent.putExtra("method", "trace");
            startActivity(intent);
        });

        // 以标注方式startActivity
        mRlMark.setOnClickListener(v -> {
            Toast.makeText(getContext(), "mark", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), MapActivity.class);
            intent.putExtra("method", "mark");
            startActivity(intent);
        });

        mBtOpen.setOnClickListener(v -> {
            try {
                LatLng startLatLng = new LatLng(39.940387, 116.29446);
                LatLng endLatLng = new LatLng(39.87397, 116.529025);
                String uri = String.format("baidumap://map/direction?origin=%s,%s&destination=" +
                                "%s,%s&mode=driving&src=com.34xian.demo", startLatLng.latitude, startLatLng.longitude,
                        endLatLng.latitude, endLatLng.longitude);
                Intent intent = new Intent();
                intent.setData(Uri.parse(uri));
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getActivity(), "请先安装百度地图", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 初始化界面
     */
    private void initView(View root) {
        mRlRoute = root.findViewById(R.id.rl_route);
        mRlMark = root.findViewById(R.id.rl_mark);
        mBtOpen = root.findViewById(R.id.bt_open_map);

    }

}
