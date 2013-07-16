package com.zhangxaochen.huaweiproj;

import java.io.File;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.net.Uri;
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
	int _sampleRate=60;
	String _debugInfo;
	String _fileName=Environment.getExternalStorageDirectory().getAbsolutePath()
			+File.separator+"huawei.xml";;

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
