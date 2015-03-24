package com.zhangxaochen.huaweiproj;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Toast;

import com.zhangxaochen.mysensor.MyView;

public class ViewCurveActivity extends Activity {
	//sensor
	SensorManager _sm;
	int _sampleRate=-1;
	int _sensorType=-1;
	
	//UI
	Spinner _spinnerSensorList;
	MyView _myViewCurve;

	private void initWidgets(){
		_spinnerSensorList=(Spinner) findViewById(R.id.spinnerSensorList);
		//怎么设置 custom id? 
//		_spinnerSensorList
		
		_myViewCurve=(MyView) findViewById(R.id.viewCurve);

	}
	
	private void respondEvents(){
		//----------传感器选择:
		_spinnerSensorList.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				System.out.println(parent.getSelectedItem());
				
				_sensorType=getSensorType(position);
				_myViewCurve.setGap(_sensorType);
				
				_sm.unregisterListener(_myViewCurve);
				_sm.registerListener(_myViewCurve, _sm.getDefaultSensor(_sensorType), _sampleRate<4 ? _sampleRate: HuaweiProj.aMillion/_sampleRate);
				
				Toast.makeText(ViewCurveActivity.this, "_sensorType, _sampleRate:= "+_sensorType+", "+_sampleRate, Toast.LENGTH_SHORT).show();
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

	}
	
	private int getSensorType(int spinnerPos){
		int type=-1;
		switch (spinnerPos) {
		case 0:
			type=Sensor.TYPE_ACCELEROMETER;
			break;
		case 1:
			type=Sensor.TYPE_MAGNETIC_FIELD;
			break;
		case 2:
			type=Sensor.TYPE_GYROSCOPE;
			break;
		case 3:
			type=Sensor.TYPE_ROTATION_VECTOR;
			break;
		default:
			break;
		}
		return type;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_curve);
		initWidgets();
		respondEvents();
		
		Intent intent=this.getIntent();
		_sampleRate=intent.getIntExtra(HuaweiProj.kSampleRate, -1);
		System.out.println("_sampleRate:= " + _sampleRate);		

		_sm=(SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		System.out.println("ViewCurveActivity.onResume, _sampleRate: "+_sampleRate);
		_sm.registerListener(_myViewCurve, _sm.getDefaultSensor(_sensorType), _sampleRate);

	}

	@Override
	protected void onPause() {
		super.onPause();
		
		System.out.println("onPause");
		_sm.unregisterListener(_myViewCurve);
	}
	

}
