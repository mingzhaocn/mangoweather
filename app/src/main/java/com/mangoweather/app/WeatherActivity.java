package com.mangoweather.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mangoweather.app.service.AutoUpdateService;
import com.mangoweather.app.util.HttpCallbackListener;
import com.mangoweather.app.util.HttpUtil;
import com.mangoweather.app.util.Utility;

public class WeatherActivity extends Activity implements OnClickListener{

    private static final String TAG = "WeatherActivity";

	private RelativeLayout weatherInfoLayout;
	/**
	 * 用于显示城市名
	 */
	private TextView cityNameText;
	/**
	 * 用于显示发布时间
	 */
	private TextView publishText;
	/**
	 * 用于显示天气描述信息
	 */
	private TextView weatherDespText;
	/**
	 * 用于显示气温1
	 */
	private TextView temp1Text;
	/**
	 * 用于显示气温2
	 */
	private TextView temp2Text;
	/**
	 * 用于显示当前日期
	 */
	private TextView currentDateText;
	/**
	 * 切换城市按钮
	 */
	private Button switchCity;
	/**
	 * 更新天气按钮
	 */
	private Button refreshWeather;

	/**
	 *闹钟跳转
	 */
	private ImageButton alarmManage;

	/**
	 *天气图
	 */
	private ImageView imageViewDes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(com.mangoweather.app.R.layout.weather_layout);
        initView();


        String countyCode = getIntent().getStringExtra("county_code");
		if (!TextUtils.isEmpty(countyCode)) {
			// 有县级代号时就去查询天气
			publishText.setText("同步中...");
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			queryWeatherCode(countyCode);
		} else {
			// 没有县级代号时就直接显示本地天气
			showWeather();
		}
		switchCity.setOnClickListener(this);
		refreshWeather.setOnClickListener(this);
	}

    private void initView() {
        // 初始化各控件
        weatherInfoLayout = (RelativeLayout) findViewById(com.mangoweather.app.R.id.weather_info_layout);
        cityNameText = (TextView) findViewById(com.mangoweather.app.R.id.city_name);
        publishText = (TextView) findViewById(com.mangoweather.app.R.id.publish_text);
        weatherDespText = (TextView) findViewById(com.mangoweather.app.R.id.weather_desp);
        temp1Text = (TextView) findViewById(com.mangoweather.app.R.id.temp1);
        temp2Text = (TextView) findViewById(com.mangoweather.app.R.id.temp2);
        currentDateText = (TextView) findViewById(com.mangoweather.app.R.id.current_date);
        switchCity = (Button) findViewById(com.mangoweather.app.R.id.switch_city);
        imageViewDes = (ImageView) findViewById(com.mangoweather.app.R.id.imageViewDes);
		refreshWeather = (Button) findViewById(com.mangoweather.app.R.id.refresh_weather);
//        alarmManage = (ImageButton) findViewById(R.id.alarmManage);
    }

    @Override
	public void onClick(View v) {
		switch (v.getId()) {
		case com.mangoweather.app.R.id.switch_city:
			Intent intent = new Intent(this, ChooseAreaActivity.class);
			intent.putExtra("from_weather_activity", true);
			startActivity(intent);
			finish();
			break;
		case com.mangoweather.app.R.id.refresh_weather:
			publishText.setText("同步中...");
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String weatherCode = prefs.getString("weather_code", "");
			if (!TextUtils.isEmpty(weatherCode)) {
				queryWeatherInfo(weatherCode);
			}
			break;
            default:
			break;
		}
	}
	
	/**
	 * 查询县级代号所对应的天气代号。
	 */
	private void queryWeatherCode(String countyCode) {
		String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
		queryFromServer(address, "countyCode");
	}

	/**
	 * 查询天气代号所对应的天气。
	 */
	private void queryWeatherInfo(String weatherCode) {
		String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
		queryFromServer(address, "weatherCode");
	}
	
	/**
	 * 根据传入的地址和类型去向服务器查询天气代号或者天气信息。
	 */
	private void queryFromServer(final String address, final String type) {
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			@Override
			public void onFinish(final String response) {
				if ("countyCode".equals(type)) {
					if (!TextUtils.isEmpty(response)) {
						// 从服务器返回的数据中解析出天气代号
						String[] array = response.split("\\|");
						if (array != null && array.length == 2) {
							String weatherCode = array[1];
							queryWeatherInfo(weatherCode);
						}
					}
				} else if ("weatherCode".equals(type)) {
					// 处理服务器返回的天气信息
					Utility.handleWeatherResponse(WeatherActivity.this, response);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							showWeather();
						}
					});
				}
			}
			
			@Override
			public void onError(Exception e) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						publishText.setText("同步失败");
					}
				});
			}
		});
	}
	
	/**
	 * 从SharedPreferences文件中读取存储的天气信息，并显示到界面上。
	 */
	private void showWeather() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		cityNameText.setText( prefs.getString("city_name", ""));
		temp1Text.setText(prefs.getString("temp1", ""));
		temp2Text.setText(prefs.getString("temp2", ""));
		weatherDespText.setText(prefs.getString("weather_desp", ""));
		publishText.setText("今天" + prefs.getString("publish_time", "") + "发布");
		currentDateText.setText(prefs.getString("current_date", ""));
        String img1 = prefs.getString("img1", "d0.gif");
        String img = img1.substring(1, 2);
        Log.d(TAG, "img>"+img);
        switch (Integer.parseInt(img)) {
            case 0:
                imageViewDes.setImageResource(com.mangoweather.app.R.drawable.d00);
                break;
            case 2:
                imageViewDes.setImageResource(com.mangoweather.app.R.drawable.d02);
                break;
            case 3:
                imageViewDes.setImageResource(com.mangoweather.app.R.drawable.d04);
                break;
            case 4:
                imageViewDes.setImageResource(com.mangoweather.app.R.drawable.d04);
                break;
            case 5:
                imageViewDes.setImageResource(com.mangoweather.app.R.drawable.d05);
                break;
            case 6:
                imageViewDes.setImageResource(com.mangoweather.app.R.drawable.d06);
                break;
            case 7:
                imageViewDes.setImageResource(com.mangoweather.app.R.drawable.d07);
                break;
            case 8:
                imageViewDes.setImageResource(com.mangoweather.app.R.drawable.d08);
                break;
            case 9:
                imageViewDes.setImageResource(com.mangoweather.app.R.drawable.d09);
                break;
            default:
                break;
        }



		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
		Intent intent = new Intent(this, AutoUpdateService.class);
		startService(intent);
	}

}