package com.mycz.tree.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.TextOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.navi.BaiduMapAppNotSupportNaviException;
import com.baidu.mapapi.navi.BaiduMapNavigation;
import com.baidu.mapapi.navi.NaviParaOption;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.utils.AreaUtil;
import com.baidu.mapapi.utils.DistanceUtil;
import com.mycz.tree.R;
import com.mycz.tree.factory.NavInfoWindowFactory;
import com.mycz.tree.overlayutil.WalkingRouteOverlay;
import com.mycz.tree.util.RectangleUtil;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.baidu.mapapi.map.TextOptions.ALIGN_BOTTOM;
import static com.baidu.mapapi.map.TextOptions.ALIGN_LEFT;

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
    private ConstraintLayout mClTools;

    // 进入到此Activity的方法
    private static String mStartMethod;

    private ImageButton mBtMark;
    private ImageButton mBtRuler;

    // 允许标注
    private static boolean mMarkable;

    private RoutePlanSearch mRoutePlanSearch;
    private ConstraintLayout mClMeasure;
    private boolean mEnableMeasure;
    private List<LatLng> mPoints = new ArrayList<>();
    private TextView mTvResult;
    private ImageButton mBtCloseLayer;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取启动方式
        mStartMethod = getIntent().getStringExtra("method");
        Toast.makeText(this, mStartMethod, Toast.LENGTH_SHORT).show();

        // 这句话要放在setContentView前面，否则会报错
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_map);

        // 初始化界面
        initView();

        // 申请权限
        requestLocatePermission();

    }

    /**
     * 初始化界面
     */
    private void initView() {
        // 工具图层
        mClTools = findViewById(R.id.cl_tools);
        if ("mark".equals(mStartMethod)) {
            mClTools.setVisibility(View.VISIBLE);
        } else {
            mClTools.setVisibility(View.INVISIBLE);
        }
        // 测量图层
        mClMeasure = findViewById(R.id.cl_measure);

        mMapView = findViewById(R.id.mapView);
        mBaiduMap = mMapView.getMap();

        mBtBack = findViewById(R.id.bt_back);
        mBtAim = findViewById(R.id.bt_aim);
        mBtMark = findViewById(R.id.bt_mark);
        mBtRuler = findViewById(R.id.bt_ruler);

        mTvResult = findViewById(R.id.tv_result);

        mBtCloseLayer = findViewById(R.id.bt_close_layer);


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
            initRoutePlanSearch();
            initEvents();
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

        // 点击标注（旗子）按钮，在地图上标注点
        mBtMark.setOnClickListener(v -> {
            mMarkable = true;
        });

        // 点击尺子按钮，测量面积
        mBtRuler.setOnClickListener(v -> {
            // 显示测量的图层
            mClMeasure.setVisibility(View.VISIBLE);
            // 启用测量模式
            mEnableMeasure = true;
            // 设置默认的tip信息
            mTvResult.setText(R.string.str_measure_tip);
        });

        // 给测量图层的关闭按钮设置监听
        mBtCloseLayer.setOnClickListener(v -> {
            mClMeasure.setVisibility(View.INVISIBLE);
        });

        // marker点击监听
        mBaiduMap.setOnMarkerClickListener(marker -> {
            View view = new NavInfoWindowFactory(getApplicationContext()).getNavInfoWindowView();
            showInfoWindowOnMarker(marker, view);
            return false;
        });

        // 设置地图点击监听
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                if (mMarkable) {
                    addMarker(point);
                    mMarkable = false;
                }

                if (mEnableMeasure) {
                    mPoints.add(point);
                    Log.v("MapAPP", "points=" + mPoints.size());
                    if (mPoints.size() == 2) {
                        LatLng p1 = mPoints.get(0);
                        LatLng p2 = mPoints.get(1);
                        measure(p1, p2);
                        mPoints.clear();
                        mEnableMeasure = false;
                    }
                }
            }

            @Override
            public void onMapPoiClick(MapPoi mapPoi) {
            }
        });
    }


    /*****************************地图有关方法************************************************/

    /**
     * 获取我的位置
     *
     * @return
     */
    private LatLng getMyLocation() {
        double latitude = mLocationClient.getLastKnownLocation().getLatitude();
        double longitude = mLocationClient.getLastKnownLocation().getLongitude();
        return new LatLng(latitude, longitude);
    }

    /**
     * 检索规划步行路线
     *
     * @param end
     */
    private void searchWakingRoute(LatLng end) {
        PlanNode stNode = PlanNode.withLocation(getMyLocation());
        PlanNode enNode = PlanNode.withLocation(end);

        mRoutePlanSearch.walkingSearch((new WalkingRoutePlanOption())
                .from(stNode)
                .to(enNode));
    }


    /**
     * 在地图上添加Marker
     *
     * @param point
     */
    private void addMarker(LatLng point) {
        // 构建Marker图标
        BitmapDescriptor bitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.ic_marker);

        // 构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option = new MarkerOptions()
                .position(point)
                .icon(bitmap)
                .animateType(MarkerOptions.MarkerAnimateType.grow);

        // 在地图上添加Marker，并显示
        mBaiduMap.addOverlay(option);
    }

    /**
     * 初始化路线规划及搜索
     */
    private void initRoutePlanSearch() {
        mRoutePlanSearch = RoutePlanSearch.newInstance();
        MyRoutePlanResultListener routePlanResultListener = new MyRoutePlanResultListener();
        mRoutePlanSearch.setOnGetRoutePlanResultListener(routePlanResultListener);
    }

    /**
     * 则初始化定位服务
     */
    private void initLocateService() {
        // 开启地图的定位图层
        mBaiduMap.setMyLocationEnabled(true);

        // 配置定位图标(跟随模式，有方向，默认图标)
        MyLocationConfiguration locationConfiguration =
                new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, null);
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
     * 在marker上显示InfoWindow
     */
    @SuppressLint("DefaultLocale")
    private void showInfoWindowOnMarker(Marker marker, View view) {
        LatLng position = marker.getPosition();

        // 构造InfoWindow
        // point 描述的位置点
        // -100 InfoWindow相对于point在y轴的偏移量
        InfoWindow infoWindow = new InfoWindow(view, position, -100);

        // 给关闭按钮设置监听
        Button btCloseWindow = view.findViewById(R.id.bt_close_window);
        btCloseWindow.setOnClickListener(v -> mBaiduMap.hideInfoWindow());

        // 给路线规划按钮设置监听
        Button btRoutePlanning = view.findViewById(R.id.bt_route_planning);
        LatLng destination = marker.getPosition();
        btRoutePlanning.setOnClickListener(v -> searchWakingRoute(destination));

        // 给导航按钮设置点击监听
        Button btNav = view.findViewById(R.id.bt_nav);
        btNav.setOnClickListener(v -> navigate(destination));

        // 计算距离
        double distance = DistanceUtil.getDistance(getMyLocation(), destination);
        TextView tvDistance = view.findViewById(R.id.tv_distance);
        tvDistance.setText(String.format("%.2fkm", distance / 1000));

        // 显示infoWindow
        mBaiduMap.showInfoWindow(infoWindow);
    }

    /**
     * 调起百度地图步行导航
     * @param destination
     */
    private void navigate(LatLng destination) {
        //定义起终点坐标（天安门和百度大厦）
        LatLng startPoint = getMyLocation();
        LatLng endPoint = destination;

        //构建导航参数
        NaviParaOption para = new NaviParaOption()
                .startPoint(startPoint)
                .endPoint(endPoint);
        //调起百度地图
        try {
            BaiduMapNavigation.openBaiduMapWalkNavi(para, this);
        } catch (BaiduMapAppNotSupportNaviException e) {
            //调起失败的处理
            Toast.makeText(this,"没有安装百度地图，请先安装", Toast.LENGTH_SHORT).show();
        }

    }


    /**
     * 测量两点之间的距离和两点确定的矩形的面积
     *
     * @param p1
     * @param p2
     */
    @SuppressLint("DefaultLocale")
    private void measure(LatLng p1, LatLng p2) {
        double distance = DistanceUtil.getDistance(p1, p2);
        double area = AreaUtil.calculateArea(p1, p2);

        String text = String.format("距离为：%.2fkm,面积为：%.2fkm²", distance / 1e3, area / 1e6);

        drawLine(p1, p2);
        drawRectangle(p1, p2);
        drawText(RectangleUtil.leftTopVertex(p1, p2), text);
        mTvResult.setText(text);

    }

    /**
     * 在地图上画一条线
     *
     * @param p1
     * @param p2
     */
    private void drawLine(LatLng p1, LatLng p2) {
        //构建折线点坐标
        List<LatLng> points = new ArrayList<>();
        points.add(p1);
        points.add(p2);

        //设置折线的属性
        OverlayOptions mOverlayOptions = new PolylineOptions()
                .width(10)
                .color(0xAAFF0000)
                .points(points);

        //在地图上绘制折线
        mBaiduMap.addOverlay(mOverlayOptions);
    }

    /**
     * 在地图上画一个矩形
     *
     * @param p1
     * @param p2
     */
    private void drawRectangle(LatLng p1, LatLng p2) {
        //构建折线点坐标
        List<LatLng> points = RectangleUtil.vertices(p1, p2);

        //设置折线的属性
        OverlayOptions mOverlayOptions = new PolylineOptions()
                .width(10)
                .color(0xAAFF0000)
                .points(points)
                .dottedLine(true);

        //在地图上绘制折线
        mBaiduMap.addOverlay(mOverlayOptions);
    }

    /**
     * 在地图上描绘文字
     * @param point
     * @param text
     */
    private void drawText(LatLng point, String text) {
        //构建TextOptions对象
        OverlayOptions mTextOptions = new TextOptions()
                .text(text) //文字内容
                .fontSize(24) //字号
                .fontColor(0xFFFF00FF) //文字颜色
                .position(point)
                .align(ALIGN_LEFT, ALIGN_BOTTOM);

        //在地图上显示文字覆盖物
        mBaiduMap.addOverlay(mTextOptions);
    }

    /**********************************************************************************************/

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
        initRoutePlanSearch();
        initEvents();
    }

    /**
     * 权限申请拒绝的回调
     *
     * @param requestCode
     * @param perms
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Toast.makeText(this, "没有授予权限", Toast.LENGTH_SHORT).show();
    }


    /**
     * 实现定位监听
     */
    private class MyLocationListener extends BDAbstractLocationListener {

        /**
         * 当接收到位置信号时回调此方法
         *
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
         *
         * @param zoomTo
         * @param latLng
         */
        private void zoomAndLocate(float zoomTo, LatLng latLng) {
            mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(zoomTo));
            mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(latLng));
        }
    }

    /**
     * 实现路线规划检索结果监听器
     */
    private class MyRoutePlanResultListener implements OnGetRoutePlanResultListener {

        @Override
        public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {
            //创建WalkingRouteOverlay实例
            WalkingRouteOverlay overlay = new WalkingRouteOverlay(mBaiduMap);
            if (walkingRouteResult.getRouteLines().size() > 0) {
                //获取路径规划数据,(以返回的第一条数据为例)
                //为WalkingRouteOverlay实例设置路径数据
                overlay.setData(walkingRouteResult.getRouteLines().get(0));
                //在地图上绘制WalkingRouteOverlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

        }

        @Override
        public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

        }

        @Override
        public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {

        }

        @Override
        public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

        }

        @Override
        public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

        }
    }
}
