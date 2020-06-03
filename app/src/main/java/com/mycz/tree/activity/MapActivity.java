package com.mycz.tree.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.mycz.tree.R;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * @author 木已成舟
 * @date 2020/5/11
 */
public class MapActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final int REQUEST_PERMISSION_LOCATE_CODE = 0x01;

    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private LocationClient mLocationClient;
    private MyLocationListener mLocationListener;
    private static boolean isFirst = true;
    private ImageButton mBtAim;
    private ImageButton mBtBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 这句话要放在setContentView前面，否则会报错
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_map);

        // 初始化界面
        initView();

        // 申请权限
        requestLocatePermission();

        // 初始化事件
        initEvents();
    }

    /**
     * 初始化界面
     */
    private void initView() {
        mMapView = findViewById(R.id.mapView);
        mBaiduMap = mMapView.getMap();

        mBtBack = findViewById(R.id.bt_back);
        mBtAim = findViewById(R.id.bt_aim);
    }

    /**
     * 申请定位权限
     */
    public void requestLocatePermission() {
        // 权限列表
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};

        // 判断是否有权限
        if (EasyPermissions.hasPermissions(this, permissions)) {
            // 如果有权限，则初始化定位服务
            initLocateService();
        } else {
            // 如果没有，则去申请权限
            EasyPermissions.requestPermissions(this, "定位服务需要授予App访问地理位置权限", REQUEST_PERMISSION_LOCATE_CODE, permissions);
        }
    }

    /**
     * 初始化事件
     */
    private void initEvents() {

        // 点击返回按钮，返回MainActivity
        mBtBack.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        });

        // 点击瞄准按钮，回到自身位置
        mBtAim.setOnClickListener(v -> {
            BDLocation location = mLocationClient.getLastKnownLocation();
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(latLng));
            mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(17));
        });
    }

    /**
     * 则初始化定位服务
     */
    private void initLocateService() {
        // 开启地图的定位图层
        mBaiduMap.setMyLocationEnabled(true);

        // 配置定位图标(跟随模式，有方向，默认图标)
        MyLocationConfiguration locationConfiguration = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, null);
        mBaiduMap.setMyLocationConfiguration(locationConfiguration);

        // 实例化定位客户端
        mLocationClient = new LocationClient(this);

        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        // 打开gps
        option.setOpenGps(true);
        // 设置坐标类型
        option.setCoorType("bd09ll");
        // 每隔1s定位1次
        option.setScanSpan(1000);

        mLocationClient.setLocOption(option);

        // 注册LocationListener监听器
        mLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(mLocationListener);

        // 开启地图定位图层
        mLocationClient.start();

    }

    /**
     * 申请权限时回调此方法
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * 重写此方法、点击back键后activity不finish，而是返回桌面
     *
     * @param event
     * @return
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.HOME");
            startActivity(intent);
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    /**
     * 退出时释放资源
     */
    @Override
    protected void onDestroy() {
        // 退出时销毁定位
        mLocationClient.unRegisterLocationListener(mLocationListener);
        mLocationClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mBaiduMap.clear();
        super.onDestroy();
        mMapView.onDestroy();
        mMapView = null;
    }

    /**
     * 权限申请成功的回调
     *
     * @param requestCode
     * @param perms
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        initLocateService();
    }

    /**
     * 权限申请拒绝的回调
     *
     * @param requestCode
     * @param perms
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Toast.makeText(this,"没有授予权限", Toast.LENGTH_SHORT).show();
    }

    /**
     * 实现定位监听
     */
    private class MyLocationListener extends BDAbstractLocationListener {

        /**
         * 当接收到位置信号时回调此方法
         * @param location
         */
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }

            // 在地图上标定位置
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    .direction(location.getDirection())
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .build();
            mBaiduMap.setMyLocationData(locData);

            // 首次定位设置地图缩放和中心点
            if (isFirst) {
                LatLng latLng = new LatLng(locData.latitude, locData.longitude);
                float zoomTo = 17;
                zoomAndLocate(zoomTo, latLng);
                isFirst = false;
            }

        }

        /**
         * 设置地图缩放和中心点
         * @param zoomTo
         * @param latLng
         */
        private void zoomAndLocate(float zoomTo, LatLng latLng) {
            mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(zoomTo));
            mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(latLng));
        }
    }
}
