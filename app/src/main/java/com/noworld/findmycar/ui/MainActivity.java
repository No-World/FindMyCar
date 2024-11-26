package com.noworld.findmycar.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.noworld.findmycar.R;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity implements AMapLocationListener, com.amap.api.maps.LocationSource, View.OnClickListener {

    private static final String TAG = "MainActivity";

    // 请求权限意图
    private ActivityResultLauncher<String> requestPermission;

    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    // 声明地图控制器
    private AMap aMap = null;
    // 声明地图定位监听
    private LocationSource.OnLocationChangedListener mListener = null;
    // 声明当前坐标
    private LatLng latLng = null;
    // 声明车辆坐标
    private LatLng carLatLng = null;
    private Marker carMarker = null;
    // 声明是否设置缩放级别
    private boolean isSetZoomLevel = false;


    private MapView mp_view;
    private FloatingActionButton fab_marker_car;
    private FloatingActionButton fab_car_del;
    private FloatingActionButton fab_car_location;
    private FloatingActionButton fab_zoom_large;
    private FloatingActionButton fab_zoom_small;
    private FloatingActionButton fab_location;
    private FloatingActionButton fab_navigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        requestPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            // 权限申请结果
            Log.d(TAG, "权限申请结果: " + result);
            showMsg(result ? "已获取到定位权限" : "权限申请失败");

        });


//        MapView mapView = (MapView) findViewById(R.id.map);
//        mapView.onCreate(savedInstanceState);// 此方法必须重写
//        AMap aMap = mapView.getMap();

//        aMap.setTrafficEnabled(true);// 显示实时交通状况
//        //地图模式可选类型：MAP_TYPE_NORMAL,MAP_TYPE_SATELLITE,MAP_TYPE_NIGHT
//        aMap.setMapType(AMap.MAP_TYPE_SATELLITE);// 卫星地图模式

        initView();
        // 初始化定位
        initLocation();
        mp_view.onCreate(savedInstanceState);
        // 初始化地图
        initMap();
        // 初始化车辆标记
        initMarker();

        Log.d(TAG, "moveCamera: " + latLng);
        // 设置地图默认缩放级别
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        Log.d(TAG, "moveEnd!");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //第二个参数表示此menu的id值，在onOptionsItemSelected方法中通过id值判断是哪个menu被点击了
        menu.add(Menu.NONE, 1, 1, "普通视图");
        menu.add(Menu.NONE, 2, 2, "夜景视图");
        menu.add(Menu.NONE, 3, 3, "卫星视图");
        menu.add(Menu.NONE, 4, 4, "导航视图");
        return true;
    }

    //点击实现的操作
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                showMsg("切换为普通视图");
                break;
            case 2:
                aMap.setMapType(AMap.MAP_TYPE_NIGHT);
                showMsg("切换为夜景视图");
                break;
            case 3:
                aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                showMsg("切换为卫星视图");
                break;
            case 4:
                aMap.setMapType(AMap.MAP_TYPE_NAVI);
                showMsg("切换为导航视图");
                break;
        }
        return true;
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        try {
            //初始化定位
            mLocationClient = new AMapLocationClient(getApplicationContext());
            //初始化定位参数
            mLocationOption = new AMapLocationClientOption();
            //设置定位回调监听
            mLocationClient.setLocationListener(this);
            //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //获取最近3s内精度最高的一次定位结果
            mLocationOption.setOnceLocationLatest(true);
            //设置是否返回地址信息（默认返回地址信息）
            mLocationOption.setNeedAddress(true);
            //设置定位超时时间，单位是毫秒
            mLocationOption.setHttpTimeOut(6000);
            //给定位客户端对象设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化地图
     */
    private void initMap() {
        if (aMap == null) {
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

            // 设置定位蓝点展现模式
            // 只定位一次。
            // myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW);
            // 定位一次，且将视角移动到地图中心点。
            // myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE) ;
            // 连续定位、且将视角移动到地图中心点，定位蓝点跟随设备移动。（1秒1次定位）
            // myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW) ;
            // 连续定位、且将视角移动到地图中心点，地图依照设备方向旋转，定位点会跟随设备移动。（1秒1次定位）
            // myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE);
            // 连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）默认执行此种模式。
            // myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
            //以下三种模式从5.1.0版本开始提供
            // 连续定位、蓝点不会移动到地图中心点，定位点依照设备方向旋转，并且蓝点会跟随设备移动。
            // myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
            // 连续定位、蓝点不会移动到地图中心点，并且蓝点会跟随设备移动。
            // myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER);
            // 连续定位、蓝点不会移动到地图中心点，地图依照设备方向旋转，并且蓝点会跟随设备移动。
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE_NO_CENTER);

            // 设置定位蓝点的样式
            aMap.setMyLocationStyle(myLocationStyle);
            // 设置默认定位按钮是否显示
            // aMap.getUiSettings().setMyLocationButtonEnabled(true);
            // 设置定位监听
            aMap.setLocationSource(this);
            // 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
            aMap.setMyLocationEnabled(true);

            //设置最小缩放等级为12 ，缩放级别范围为[3, 20]
            // aMap.setMinZoomLevel(18);
            // 开启室内地图
            aMap.showIndoorMap(true);
            // 地图控件设置
            UiSettings uiSettings = aMap.getUiSettings();
            // 隐藏缩放按钮
            uiSettings.setZoomControlsEnabled(false);
            // 显示比例尺，默认不显示
            uiSettings.setScaleControlsEnabled(true);
            // 显示指南针
            uiSettings.setCompassEnabled(true);

        }
    }


    /**
     * 开始定位
     */
    private void startLocation() {
        if (mLocationClient != null) mLocationClient.startLocation();
    }

    /**
     * 停止定位
     */
    private void stopLocation() {
        if (mLocationClient != null) mLocationClient.stopLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mp_view.onResume();

        // 检查是否已经获取到定位权限
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 获取到权限
            startLocation();
        } else {
            // 请求定位权限
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 绑定生命周期 onPause
        mp_view.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 绑定生命周期 onSaveInstanceState
        mp_view.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 绑定生命周期 onDestroy
        mp_view.onDestroy();
        // 停止定位
        stopLocation();
        // 销毁定位
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
    }


    private void showMsg(CharSequence llw) {
        Toast.makeText(this, llw, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation == null) {
            showMsg("定位失败，aMapLocation 为空");
            Log.e(TAG, "定位失败，aMapLocation 为空");
            return;
        }
        // 获取定位结果
        if (aMapLocation.getErrorCode() == 0) {
            // 定位成功
            Log.i(TAG, "定位成功");
//            showMsg("定位成功");
            latLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude()); // 获取当前latlng坐标
//            aMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
//            double resultx = aMapLocation.getLatitude();//获取纬度
//            double resulty = aMapLocation.getLongitude();//获取经度
//            aMapLocation.getAccuracy();//获取精度信息
//            aMapLocation.getAddress();//详细地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
//            aMapLocation.getCountry();//国家信息
//            aMapLocation.getProvince();//省信息
//            aMapLocation.getCity();//城市信息
//            String result = aMapLocation.getDistrict();//城区信息
//            aMapLocation.getStreet();//街道信息
//            aMapLocation.getStreetNum();//街道门牌号信息
//            aMapLocation.getCityCode();//城市编码
//            aMapLocation.getAdCode();//地区编码
//            aMapLocation.getAoiName();//获取当前定位点的AOI信息
//            aMapLocation.getBuildingId();//获取当前室内定位的建筑物Id
//            aMapLocation.getFloor();//获取当前室内定位的楼层
//            aMapLocation.getGpsAccuracyStatus();//获取GPS的当前状态

            // 停止定位
//            stopLocation();
            // 显示地图定位结果
            if (mListener != null) {
                mListener.onLocationChanged(aMapLocation);
            }
            if (!isSetZoomLevel && latLng != null) {
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
                Log.d(TAG, "moveEnd!");
                isSetZoomLevel = true;
            }
        } else {
            // 定位失败
            showMsg("定位失败，错误：" + aMapLocation.getErrorInfo());
            Log.e(TAG, "location Error, ErrCode:"
                    + aMapLocation.getErrorCode() + ", errInfo:"
                    + aMapLocation.getErrorInfo());
        }
    }

    private void initView() {
        mp_view = findViewById(R.id.mp_view);
        fab_marker_car = findViewById(R.id.fab_marker_car);
        fab_car_del = findViewById(R.id.fab_car_del);
        fab_car_location = findViewById(R.id.fab_car_location);
        fab_zoom_large = findViewById(R.id.fab_zoom_large);
        fab_zoom_small = findViewById(R.id.fab_zoom_small);
        fab_location = findViewById(R.id.fab_location);
        fab_navigation = findViewById(R.id.fab_navigation);
        fab_marker_car.setOnClickListener(this);
        fab_car_del.setOnClickListener(this);
        fab_car_location.setOnClickListener(this);
        fab_zoom_large.setOnClickListener(this);
        fab_zoom_small.setOnClickListener(this);
        fab_location.setOnClickListener(this);
        fab_navigation.setOnClickListener(this);
    }

    private void initMarker() {
        try {
            File file = new File(getFilesDir(), "car.json");
            if (file.exists() && file.length() > 0) {
                byte[] bytes = new byte[(int) file.length()];
                FileInputStream fis = new FileInputStream(file);
                fis.read(bytes);
                fis.close();
                JSONObject carLocation = new JSONObject(new String(bytes));
                double latitude = carLocation.getDouble("latitude");
                double longitude = carLocation.getDouble("longitude");
                carLatLng = new LatLng(latitude, longitude);
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
        } catch (Exception e) {
            e.printStackTrace();
            showMsg("获取车辆位置失败: " + e.getMessage());
        }
    }

    /**
     * 添加车辆标记
     */
    private void addMarker() {
        // 在当前位置添加车辆标记的逻辑
        if (latLng != null) {
            carLatLng = latLng;
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("车辆位置");
            // 缩小ico_marker图标
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ico_marker);
            Bitmap smallBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
            markerOptions.icon(com.amap.api.maps.model.BitmapDescriptorFactory.fromBitmap(smallBitmap));
            carMarker = aMap.addMarker(markerOptions);
            showMsg("车辆标记已添加");
        } else {
            showMsg("无法获取当前位置");
        }
        try {
            JSONObject carLocation = new JSONObject();
            carLocation.put("latitude", carLatLng.latitude);
            carLocation.put("longitude", carLatLng.longitude);

            File file = new File(getFilesDir(), "car.json");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(carLocation.toString().getBytes());
            fos.close();

            Log.d(TAG, "addMarker: 车辆标记已保存");
        } catch (Exception e) {
            e.printStackTrace();
            showMsg("保存车辆位置失败，请稍后重试！");
        }
    }

    /**
     * 删除车辆标记
     */
    private void delMarker() {
        carLatLng = null;
        carMarker.remove();
        carMarker = null;
        File file = new File(getFilesDir(), "car.json");
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        if (mListener == null) {
            mListener = onLocationChangedListener;
        }
        startLocation();
    }

    /**
     * 禁用
     */
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
    public void onClick(View view) {
        if (view.getId() == R.id.fab_marker_car) {
            // 添加车辆标记
            showMsg("添加车辆标记");
            // 弹出弹窗确认是否在当前位置添加车辆标记
            new AlertDialog.Builder(this)
                    .setTitle("添加车辆标记")
                    .setMessage("是否在当前位置添加车辆标记？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        if (carLatLng != null) {
                            new AlertDialog.Builder(this)
                                    .setTitle("添加车辆标记")
                                    .setMessage("车辆标记已存在，是否替换为当前位置？")
                                    .setPositiveButton("确定", (dialog1, which1) -> {
                                        // 删除车辆标记
                                        delMarker();
                                        // 添加车辆标记
                                        addMarker();
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        } else {
                            // 添加车辆标记的逻辑
                            addMarker();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else if (view.getId() == R.id.fab_car_del) {
            // 删除车辆标记
            showMsg("删除车辆标记");
            // 弹出弹窗确认是否在当前位置添加车辆标记
            new AlertDialog.Builder(this)
                    .setTitle("删除车辆标记")
                    .setMessage("是否删除车辆标记？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        // 删除车辆标记的逻辑
                        delMarker();
                        showMsg("车辆标记已删除");
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else if (view.getId() == R.id.fab_car_location) {
            // 车辆定位
            showMsg("车辆定位");
            if (carLatLng != null) {
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLatLng, 18));
                showMsg("车辆定位成功");
            } else {
                showMsg("车辆位置未标记");
            }
        } else if (view.getId() == R.id.fab_navigation) {
            new AlertDialog.Builder(this)
                    .setTitle("导航")
                    .setMessage("是否导航到车辆位置？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        if (carLatLng != null) {
                            // 跳转到导航页面
                            // 创建Intent以启动RouteActivity
                            Intent intent = new Intent(this, RouteActivity.class);
                            // 将车辆位置作为额外参数传递
                            intent.putExtra("latitude", carLatLng.latitude);
                            intent.putExtra("longitude", carLatLng.longitude);
                            // 启动RouteActivity
                            startActivity(intent);
                            showMsg("开始导航，请确保车辆和自身位置均处于地面");
                        } else {
                            showMsg("车辆位置未标记");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else if (view.getId() == R.id.fab_zoom_large) {
            aMap.animateCamera(CameraUpdateFactory.zoomIn());
        } else if (view.getId() == R.id.fab_zoom_small) {
            aMap.animateCamera(CameraUpdateFactory.zoomOut());
        } else if (view.getId() == R.id.fab_location) {
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
        }
    }
}