package com.zhangxaochen.huaweiproj;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class BaseActivity extends Activity {
	static long _exitTimeStamp = -1;
//	int _sampleRate=60;
	int _sampleRate=SensorManager.SENSOR_DELAY_FASTEST;
	String _debugInfo;
	String _fileName=Environment.getExternalStorageDirectory().getAbsolutePath()
			+File.separator+"huawei.xml";
	
				
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		//------------------------��� android ϵͳ�汾
		if(android.os.Build.VERSION.SDK_INT<Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			System.out.println("shit~~~~~~~~~~~~~~~~~~");
			String errorMsg="��Ҫ Android OS4.0 ���ϰ汾��ϵͳ";
			AlertDialog.Builder builder=new AlertDialog.Builder(this);
			builder.setTitle("ϵͳҪ��")
			.setMessage(errorMsg)
			.setCancelable(false)
			.setPositiveButton("�˳�����", new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					BaseActivity.super.onBackPressed();
				}
			})
			.show();
		}
		
		
		//------------------------��⴫����Ӳ���Ƿ����
		PackageManager pm=this.getPackageManager();
		boolean hasAcc=pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
		boolean hasGyro=pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
		boolean hasMag=pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
		
		if(!(hasAcc && hasGyro && hasMag)){
			String errorMsg=
					"������Ӧ��\t"+(hasAcc?"��":"ȱʧ")+"\n"+
					"�����ǣ�\t\t"+(hasGyro?"��":"ȱʧ")+"\n"+
					"�������̣�\t"+(hasMag?"��":"ȱʧ")+"\n\n"+
					"~~~��ʹ��ͬʱ�߱���������������ģ����豸�������ݲɼ�~~~";
			AlertDialog.Builder builder=new AlertDialog.Builder(this);
			builder.setTitle("��ȱ���˶�������ģ�飡")
			.setMessage(errorMsg)
			.setCancelable(false)
			.setPositiveButton("�˳�����", new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
//					BaseActivity.this.finish();
					BaseActivity.super.onBackPressed();					
					
				}
			});
			
			Dialog lackSensorDlg=builder.create();
			lackSensorDlg.show();	
			
		}
			
			
		
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		System.out.println("onOptionsItemSelected");
		int itemId = item.getItemId();

		// System.out.println("itemId:= " + itemId);

		switch (itemId) {
		case android.R.id.home: // Ӧ�����Ͻǵ�ͼ�꣬ ���ֻ� home ��
			System.out.println("android.R.id.home");
			break;
		case R.id.menu_settings:
//			Toast.makeText(this, "no settings~~", Toast.LENGTH_SHORT).show();
			SettingsDlg settingsDlg=new SettingsDlg(this);
			settingsDlg.setOnDismissListener(new OnDismissListener() {
				
				public void onDismiss(DialogInterface dialog) {
//					Toast.makeText(HuaweiProj.this, "settingsDlg...onDismiss", Toast.LENGTH_SHORT).show();
					
				}
			});
			settingsDlg.setTitle(R.string.settingsTitle);
			settingsDlg.show();
			break;
		case R.id.menu_about:
			Dialog aboutDlg = new Dialog(this) {

				@Override
				protected void onCreate(Bundle savedInstanceState) {
					super.onCreate(savedInstanceState);
					setContentView(R.layout.about_dlg);
					TextView textViewAbout = (TextView) findViewById(R.id.textViewAbout);
					textViewAbout.setText(Html
							.fromHtml(getString(R.string.about_text)));
					textViewAbout.setMovementMethod(LinkMovementMethod
							.getInstance());
				}

			};
			// aboutDlg.setContentView(R.layout.about_dlg);
			aboutDlg.setTitle(R.string.aboutTitle);
			// aboutDlg.setCancelable(false); // �����ֶ�ȡ��
			aboutDlg.show();

			break;
		case R.id.menu_debug:
			Toast.makeText(this, _debugInfo, Toast.LENGTH_SHORT).show();
			break;
		case R.id.menuItemViewXml:
			System.out.println("R.id.menuItemViewXml");

			System.out.println(_fileName);
			File file=new File(_fileName);
			System.out.println("_file.exists():= " + file.exists());

			// ��Ĭ�ϳ���� xml:
			if (!file.exists()) {
				// _file.getAbsolutePath()
				Toast.makeText(this, file.getAbsolutePath() + " ������δ���ɣ����ѱ�ɾ��",
						Toast.LENGTH_SHORT).show();
				break;
			}
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			// File file = new File(
			// "/data/data/com.zhangxaochen.huaweiproj/curve.png");

			// intent.setDataAndType(Uri.fromFile(file), "image/*");
			intent.setDataAndType(Uri.fromFile(file), "text/*");
			startActivity(intent); // ��
			// startActivity(Intent.createChooser(intent, getResources()
			// .getString(R.string.menuViewXml))); // ��

			break;

		case R.id.menuItemViewCurve:
			Intent intentViewCurve = new Intent(this, ViewCurveActivity.class);
			intentViewCurve.putExtra(HuaweiProj.kSampleRate, _sampleRate);

			startActivity(intentViewCurve);
			break;

		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
		// IME shown ��ʱ����Ȼ��ⲻ�� back key��������������
		System.out.println("onBackPressed");

		long curTime = SystemClock.uptimeMillis();

		System.out.println("curTime:= " + curTime);

		if (curTime - _exitTimeStamp < 1000) {
			System.out.println("curTime - _exitTimeStamp < 1000");
			super.onBackPressed(); // ==finish()
			// finish();
		} else {
			Toast.makeText(this, "press BACK again to exit", Toast.LENGTH_SHORT)
					.show();
			_exitTimeStamp = curTime;
		}
	}


}
