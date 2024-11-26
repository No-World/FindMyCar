package com.noworld.findmycar.ui;

import static com.noworld.findmycar.utils.MapUtil.convertToLatLonPoint;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.noworld.findmycar.R;
import com.noworld.findmycar.overlay.WalkRouteOverlay;
import com.noworld.findmycar.utils.MapUtil;

public class RouteActivity extends AppCompatActivity implements
        AMapLocationListener, LocationSource, RouteSearch.OnRouteSearchListener {


    private static final String TAG = "RouteActivity";
    //    private ActivityRouteBinding binding;
    //地图控制器
    private AMap aMap = null;
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    //位置更改监听
    private LocationSource.OnLocationChangedListener mListener;
    //定义一个UiSettings对象
    private UiSettings mUiSettings;
    //定位样式
    private MyLocationStyle myLocationStyle = new MyLocationStyle();

    //起点
    private LatLonPoint mStartPoint;
    //终点
    private LatLonPoint mEndPoint;
    //车辆位置
    private LatLng carLatLng = null;
    private Marker carMarker = null;

    //路线搜索对象
    private RouteSearch routeSearch;

    private MapView mp_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_route);
        EdgeToEdge.enable(this);

        // 从Intent中获取车辆位置
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("latitude") && intent.hasExtra("longitude")) {
            double latitude = intent.getDoubleExtra("latitude", 0);
            double longitude = intent.getDoubleExtra("longitude", 0);
            carLatLng = new LatLng(latitude, longitude);
            mEndPoint = convertToLatLonPoint(carLatLng);
            Log.d(TAG, "车辆位置: " + latitude + ", " + longitude);
            showMsg("车辆位置: " + latitude + ", " + longitude);
        }

        initView();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //初始化定位
        initLocation();
        //初始化地图
        initMap(savedInstanceState);
        //初始化车辆标记
        // initMarker();

        //启动定位
        mLocationClient.startLocation();

        //初始化路线
        initRoute();
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        //初始化定位
        try {
            mLocationClient = new AMapLocationClient(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mLocationClient != null) {
            mLocationClient.setLocationListener(this);
            mLocationOption = new AMapLocationClientOption();
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setOnceLocationLatest(true);
            mLocationOption.setNeedAddress(true);
            mLocationOption.setHttpTimeOut(20000);
            mLocationOption.setLocationCacheEnable(false);
            mLocationClient.setLocationOption(mLocationOption);
        }
    }

    /**
     * 初始化车辆标记
     */
    private void initMarker() {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(carLatLng);
        markerOptions.title("车辆位置");
        // 缩小ico_marker图标
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ico_marker);
        Bitmap smallBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
        markerOptions.icon(com.amap.api.maps.model.BitmapDescriptorFactory.fromBitmap(smallBitmap));
        carMarker = aMap.addMarker(markerOptions);
        Log.d(TAG, "initMarker: 车辆标记已初始化");
    }

    /**
     * 初始化地图
     */
    private void initMap(Bundle savedInstanceState) {

        mp_view.onCreate(savedInstanceState);
        //初始化地图控制器对象
        aMap = mp_view.getMap();
        // 创建定位蓝点的样式
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        // 自定义定位蓝点图标
        // Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.gps_point);
        // Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 50, 50, false);
        // myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.gps_point));
        // 自定义精度范围的圆形边框颜色  都为0则透明
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));
        // 自定义精度范围的圆形边框宽度  0 无宽度
        myLocationStyle.strokeWidth(0);
        // 设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        myLocationStyle.interval(1000);
        // 设置圆形的填充颜色  都为0则透明
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));
        //设置最小缩放等级为12 ，缩放级别范围为[3, 20]
        aMap.setMinZoomLevel(12);
        //开启室内地图
        aMap.showIndoorMap(true);
        //实例化UiSettings类对象
        mUiSettings = aMap.getUiSettings();
        //隐藏缩放按钮 默认显示
        mUiSettings.setZoomControlsEnabled(false);
        //显示比例尺 默认不显示
        mUiSettings.setScaleControlsEnabled(true);
        // 自定义定位蓝点图标
        // myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.gps_point));
        //设置定位蓝点的Style
        aMap.setMyLocationStyle(myLocationStyle);
        // 设置定位监听
        aMap.setLocationSource(this);
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationEnabled(true);
    }

    /**
     * 初始化路线
     */
    private void initRoute() {
        try {
            routeSearch = new RouteSearch(this);
        } catch (AMapException e) {
            e.printStackTrace();
        }
        routeSearch.setRouteSearchListener(this);
    }


    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                //地址
                String address = aMapLocation.getAddress();
                //获取纬度
                double latitude = aMapLocation.getLatitude();
                //获取经度
                double longitude = aMapLocation.getLongitude();
                Log.d(TAG, aMapLocation.getCity());
                Log.d(TAG, address);

                //起点
                mStartPoint = new LatLonPoint(latitude, longitude);

                //开始路线搜索
                startRouteSearch();

                //停止定位后，本地定位服务并不会被销毁
                //mLocationClient.stopLocation();

                //显示地图定位结果
                if (mListener != null) {
                    // 显示系统图标
                    mListener.onLocationChanged(aMapLocation);
                }

            } else {
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }
    }

    @Override
    public void activate(LocationSource.OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mLocationClient == null) {
            mLocationClient.startLocation();//启动定位
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }


    @Override
    protected void onResume() {
        super.onResume();
        mp_view.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mp_view.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mp_view.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //销毁定位客户端，同时销毁本地定位服务。
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
        mp_view.onDestroy();
    }

    private void showMsg(CharSequence llw) {
        Toast.makeText(this, llw, Toast.LENGTH_SHORT).show();
    }

    private void initView() {
        mp_view = findViewById(R.id.mp_view);
    }

    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int code) {

    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int code) {

    }

    /**
     * 步行规划路径结果
     *
     * @param walkRouteResult 结果
     * @param code            结果码
     */
    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int code) {
        aMap.clear();// 清理地图上的所有覆盖物
        if (code != AMapException.CODE_AMAP_SUCCESS) {
            showMsg("错误码；" + code);
            return;
        }
        if (walkRouteResult == null || walkRouteResult.getPaths() == null) {
            showMsg("对不起，没有搜索到相关数据！");
            return;
        }
        if (walkRouteResult.getPaths().isEmpty()) {
            showMsg("对不起，没有搜索到相关数据！");
            return;
        }
        final WalkPath walkPath = walkRouteResult.getPaths().get(0);
        if (walkPath == null) {
            return;
        }
        //绘制路线
        WalkRouteOverlay walkRouteOverlay = new WalkRouteOverlay(
                this, aMap, walkPath,
                walkRouteResult.getStartPos(),
                walkRouteResult.getTargetPos());
        walkRouteOverlay.removeFromMap();
        walkRouteOverlay.addToMap();
        walkRouteOverlay.zoomToSpan();

        int dis = (int) walkPath.getDistance();
        int dur = (int) walkPath.getDuration();
        String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
        Log.d(TAG, des);
    }


    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int code) {

    }

    /**
     * 开始路线搜索
     */
    private void startRouteSearch() {
//        showMsg("mStartPoint: " + mStartPoint + ", mEndPoint: " + mEndPoint);
        //在地图上添加起点Marker
//        aMap.addMarker(new MarkerOptions()
//                .position(convertToLatLng(mStartPoint))
//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.start)));
        //在地图上添加终点Marker
//        aMap.addMarker(new MarkerOptions()
//                .position(convertToLatLng(mEndPoint))
//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.end)));

        //搜索路线 构建路径的起终点
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                mStartPoint, mEndPoint);
        //构建步行路线搜索对象
        RouteSearch.WalkRouteQuery query = new RouteSearch.WalkRouteQuery(fromAndTo, RouteSearch.WalkDefault);
        // 异步路径规划步行模式查询
        routeSearch.calculateWalkRouteAsyn(query);
    }

}
