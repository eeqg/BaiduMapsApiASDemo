package wpbaidumap.demo.map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMapOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * 演示MapView的基本用法
 */
public class BaseMapDemo extends Activity {
	
	@SuppressWarnings("unused")
	private static final String LTAG = BaseMapDemo.class.getSimpleName();
	private MapView mMapView;
	FrameLayout layout;
	private boolean mEnableCustomStyle = true;
	private static final int OPEN_ID = 0;
	private static final int CLOSE_ID = 1;
	//用于设置个性化地图的样式文件
	// 精简为1套样式模板:
	// "custom_config_dark.json"
	private static String PATH = "custom_config_dark.json";
	private static int icon_themeId = 1;
	
	private BaiduMap baiduMap;
	private LocationClient mLocationClient;
	//防止每次定位都重新设置中心点和marker
	private boolean isFirstLocation = true;
	//初始化LocationClient定位类
	//BDAbstractLocationListener为7.2版本新增的Abstract类型的监听接口，原有BDLocationListener接口
	private BDLocationListener myListener = new MyLocationListener();
	//经纬度
	private double lat;
	private double lon;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		MapStatus.Builder builder = new MapStatus.Builder();
		LatLng center = new LatLng(39.915071, 116.403907); // 默认 天安门
		float zoom = 11.0f; // 默认 11级
		
		/* 该Intent是OfflineDemo中查看离线地图调起的 */
		Intent intent = getIntent();
		if (null != intent) {
			mEnableCustomStyle = intent.getBooleanExtra("customStyle", true);
			center = new LatLng(intent.getDoubleExtra("y", 39.915071),
					intent.getDoubleExtra("x", 116.403907));
			zoom = intent.getFloatExtra("level", 11.0f);
		}
		builder.target(center).zoom(zoom);
		
		
		/**
		 * MapView (TextureMapView)的
		 * {@link MapView.setCustomMapStylePath(String customMapStylePath)}
		 * 方法一定要在MapView(TextureMapView)创建之前调用。
		 * 如果是setContentView方法通过布局加载MapView(TextureMapView), 那么一定要放置在
		 * MapView.setCustomMapStylePath方法之后执行，否则个性化地图不会显示
		 */
		setMapCustomFile(this, PATH);
		
		mMapView = new MapView(this, new BaiduMapOptions());
		initView(this);
		setContentView(layout);
		
		MapView.setMapCustomEnable(true);
	}
	
	// 初始化View
	private void initView(Context context) {
		layout = new FrameLayout(this);
		layout.addView(mMapView);
		RadioGroup group = new RadioGroup(context);
		group.setBackgroundColor(Color.BLACK);
		final RadioButton openBtn = new RadioButton(context);
		openBtn.setText("开启个性化地图");
		openBtn.setId(OPEN_ID);
		openBtn.setTextColor(Color.WHITE);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT);
		
		group.addView(openBtn, params);
		final RadioButton closeBtn = new RadioButton(context);
		closeBtn.setText("关闭个性化地图");
		closeBtn.setTextColor(Color.WHITE);
		closeBtn.setId(CLOSE_ID);
		group.addView(closeBtn, params);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		
		if (mEnableCustomStyle) {
			openBtn.setChecked(true);
		} else {
			closeBtn.setChecked(true);
		}
		
		layout.addView(group, layoutParams);
		
		group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == OPEN_ID) {
					MapView.setMapCustomEnable(true);
				} else if (checkedId == CLOSE_ID) {
					MapView.setMapCustomEnable(false);
				}
			}
		});
		
		//wp add --> location.
		initMap();
	}
	
	/**
	 * 初始化地图
	 */
	public void initMap() {
		//得到地图实例
		baiduMap = mMapView.getMap();
        /*
        设置地图类型
         */
		//普通地图
		baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
		//卫星地图
		//baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
		//空白地图, 基础地图瓦片将不会被渲染。在地图类型中设置为NONE，将不会使用流量下载基础地图瓦片图层。使用场景：与瓦片图层一起使用，节省流量，提升自定义瓦片图下载速度。
		//baiduMap.setMapType(BaiduMap.MAP_TYPE_NONE);
		//开启交通图
		baiduMap.setTrafficEnabled(true);
		//关闭缩放按钮
		mMapView.showZoomControls(false);
		
		// 开启定位图层
		baiduMap.setMyLocationEnabled(true);
		//声明LocationClient类
		mLocationClient = new LocationClient(this);
		//注册监听函数
		mLocationClient.registerLocationListener(myListener);
		initLocation();
		//开始定位
		mLocationClient.start();
	}
	
	/**
	 * 配置定位参数
	 */
	private void initLocation() {
		LocationClientOption option = new LocationClientOption();
		//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
		option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
		//可选，默认gcj02，设置返回的定位结果坐标系
		option.setCoorType("bd09ll");
		//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
		int span = 5000;
		option.setScanSpan(span);
		//可选，设置是否需要地址信息，默认不需要
		option.setIsNeedAddress(true);
		//可选，默认false,设置是否使用gps
		option.setOpenGps(true);
		//可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
		option.setLocationNotify(true);
		//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
		option.setIsNeedLocationDescribe(true);
		//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
		option.setIsNeedLocationPoiList(true);
		//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
		option.setIgnoreKillProcess(false);
		//可选，默认false，设置是否收集CRASH信息，默认收集
		option.SetIgnoreCacheException(false);
		//可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
		option.setEnableSimulateGps(false);
		mLocationClient.setLocOption(option);
	}
	
	/**
	 * 实现定位监听 位置一旦有所改变就会调用这个方法
	 * 可以在这个方法里面获取到定位之后获取到的一系列数据
	 */
	public class MyLocationListener implements BDLocationListener {
		
		@Override
		public void onReceiveLocation(BDLocation location) {
			//获取定位结果
			location.getTime();    //获取定位时间
			location.getLocationID();    //获取定位唯一ID，v7.2版本新增，用于排查定位问题
			location.getLocType();    //获取定位类型
			location.getLatitude();    //获取纬度信息
			location.getLongitude();    //获取经度信息
			location.getRadius();    //获取定位精准度
			location.getAddrStr();    //获取地址信息
			location.getCountry();    //获取国家信息
			location.getCountryCode();    //获取国家码
			location.getCity();    //获取城市信息
			location.getCityCode();    //获取城市码
			location.getDistrict();    //获取区县信息
			location.getStreet();    //获取街道信息
			location.getStreetNumber();    //获取街道码
			location.getLocationDescribe();    //获取当前位置描述信息
			location.getPoiList();    //获取当前位置周边POI信息
			
			location.getBuildingID();    //室内精准定位下，获取楼宇ID
			location.getBuildingName();    //室内精准定位下，获取楼宇名称
			location.getFloor();    //室内精准定位下，获取当前位置所处的楼层信息
			//经纬度
			lat = location.getLatitude();
			lon = location.getLongitude();
			
			Log.d("-----", "onReceiveLocation-----city : " + location.getCity());
			
			//这个判断是为了防止每次定位都重新设置中心点和marker
			if (isFirstLocation) {
				isFirstLocation = false;
				//设置并显示中心点
				setPosition2Center(baiduMap, location, true);
			}
		}
	}
	
	/**
	 * 设置中心点和添加marker
	 *
	 * @param map
	 * @param bdLocation
	 * @param isShowLoc
	 */
	public void setPosition2Center(BaiduMap map, BDLocation bdLocation, Boolean isShowLoc) {
		MyLocationData locData = new MyLocationData.Builder()
				.accuracy(bdLocation.getRadius())
				.direction(bdLocation.getRadius()).latitude(bdLocation.getLatitude())
				.longitude(bdLocation.getLongitude()).build();
		map.setMyLocationData(locData);
		
		if (isShowLoc) {
			LatLng ll = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
			MapStatus.Builder builder = new MapStatus.Builder();
			builder.target(ll).zoom(18.0f);
			map.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
		}
	}
	
	// 设置个性化地图config文件路径
	private void setMapCustomFile(Context context, String PATH) {
		FileOutputStream out = null;
		InputStream inputStream = null;
		String moduleName = null;
		try {
			inputStream = context.getAssets()
					.open("customConfigdir/" + PATH);
			byte[] b = new byte[inputStream.available()];
			inputStream.read(b);
			
			moduleName = context.getFilesDir().getAbsolutePath();
			File f = new File(moduleName + "/" + PATH);
			if (f.exists()) {
				f.delete();
			}
			f.createNewFile();
			out = new FileOutputStream(f);
			out.write(b);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		MapView.setCustomMapStylePath(moduleName + "/" + PATH);
		
	}
	
	/**
	 * 设置个性化icon
	 *
	 * @param context
	 * @param icon_themeId
	 */
	private void setIconCustom(Context context, int icon_themeId) {
		
		MapView.setIconCustom(icon_themeId);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		// activity 暂停时同时暂停地图控件
		mMapView.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// activity 恢复时同时恢复地图控件
		mMapView.onResume();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// activity 销毁时同时销毁地图控件
		MapView.setMapCustomEnable(false);
		mMapView.onDestroy();
	}
	
}
