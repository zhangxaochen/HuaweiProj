package com.zhangxaochen.huaweiproj;

import java.io.File;

import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.mysensorlistener.MySensorListener;
import com.example.mysensorlistener.MySensorListener.MySensorData;
import com.zhangxaochen.xmlParser.CaptureSessionNode;

@SuppressLint("NewApi")
public class HuaweiProj extends Activity {
	private String _debugInfo;
	
	// intent putExtra keys:
	public static final String kSampleRate = "sampleRate";

	public static final int aMillion = 1000 * 1000;

	private static long _exitTimeStamp = -1;

	// 非 UI 线程写文件：
	class WriteXmlTask extends AsyncTask<Void, Void, Void> {
		public WriteXmlTask() {
		}

		@Override
		protected Void doInBackground(Void... params) {
			System.out.println("doInBackground()");
			if (_captureSessionNode == null || _file == null
					|| _persister == null) {
				System.out
						.println("_captureSessionNode==null || _file==null || _persister == null");
				return null;
			}

			try {
				_persister.write(_captureSessionNode, _file);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			System.out.println("onPostExecute");

			super.onPostExecute(result);
			// _savingDlg.hide(); //导致第二次 show 错误
			_savingDlg.dismiss(); // √
			_captureSessionNode.clearAllNodes();

			// 居然一样：
			// System.out.println("_file.getPath: " + _file.getPath());
			// System.out.println("_file.getAbsolutePath: "
			// + _file.getAbsolutePath());
			// System.out.println("_file.getCanonicalPath: "
			// + _file.getCanonicalPath());

			Toast.makeText(HuaweiProj.this, "已存到: " + _file.getAbsolutePath(),
					Toast.LENGTH_SHORT).show();
		}

	}// WriteXmlTask

	// xml 存储
	// String _fileName="huawei.xml"; //若不加绝对路径， 无权限写文件
	// String _fileName = "/data/data/com.zhangxaochen.huaweiproj/huawei.xml";

	String _fileName = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + File.separator + "huawei.xml"; // 即
																// /mnt/sdcard/huawei.xml

	CaptureSessionNode _captureSessionNode = new CaptureSessionNode();
	// File _file = new File(_fileName);
	File _file;
	Format _format = new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>");
	Persister _persister = new Persister(_format);

	// 传感器：
	SensorManager _sm;
	MySensorListener _listener = new MySensorListener();

	// 允许倒计时：
	private boolean _isCdEnabled = false;
	private static String _isNotEnabledString = "已关闭";
	private static String _isEnabledString = "已开启";
	CountDownTimer _timer;
	int _cdDuration = 0;

	// 采样率, 改成使用 hard coded number
	// int _rate = SensorManager.SENSOR_DELAY_FASTEST;
	int _rate = -1;

	// UI:
	EditText _editTextCD;
	Button _buttonEnableCD;
	ToggleButton _toggleButtonSampling;
	MyRelativeLayout _relativeLayoutEnableCD;
	// MyRelativeLayout _relativeLayoutSampleRate;
	Spinner _spinnerSampleRate;
	Spinner _spinnerSampleMode;
	Button _buttonSaveAndClear;
	Dialog _savingDlg;

	EditText _editTextOperator;
	TextView _textViewFnameHint;
	private String _fnameHintStub = "文件命名为：";

	private String _dataFolderName = "huaweiproj-data";
	private File _dataFolder;

	// UI 组件实例化
	void initWidgets() {

		_editTextCD = (EditText) findViewById(R.id.editTextCD);
		_editTextCD.setText("0");
		_editTextCD.setEnabled(_isCdEnabled);

		_buttonEnableCD = (Button) findViewById(R.id.buttonEnableCD);
		_buttonEnableCD.append(_isNotEnabledString);

		_toggleButtonSampling = (ToggleButton) findViewById(R.id.toggleButtonSampling);
		// System.out.println("_toggleButtonSampling.isChecked():= "+_toggleButtonSampling.isChecked()
		// ); //default: false

		_relativeLayoutEnableCD = (MyRelativeLayout) findViewById(R.id.relativeLayoutEnableCD);

		// _relativeLayoutSampleRate = (MyRelativeLayout)
		// findViewById(R.id.relativeLayoutSampleRate);
		_spinnerSampleRate = (Spinner) findViewById(R.id.spinnerSampleRate);
		_spinnerSampleMode = (Spinner) findViewById(R.id.spinnerSampleMode);

		_buttonSaveAndClear = (Button) findViewById(R.id.buttonSaveAndClear);

		_savingDlg = new Dialog(this);
		// -------------------方式 ①
		_savingDlg.setContentView(R.layout.saving_dlg);
		_savingDlg.setTitle(R.string.savingTitle);
		_savingDlg.setCancelable(false); // 不可手动取消

		// -----------------------方式 ②
		// LayoutInflater inflater = getLayoutInflater();
		// View layout = inflater.inflate(R.layout.saving_dlg, null);
		// new AlertDialog.Builder(this)
		// .setTitle(getResources().getString(R.string.savingTitle))
		// .setView(layout)
		// // .setPositiveButton("确定", null)
		// // .setNegativeButton("取消", null)
		// .show();

		_editTextOperator = (EditText) findViewById(R.id.editTextOperator);
		_textViewFnameHint = (TextView) findViewById(R.id.textViewFnameHint);
	}

	// 各种事件响应
	void respondEvents() {
		// --------倒计时
		_editTextCD.addTextChangedListener(new TextWatcher() {

			@TargetApi(9)
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// System.out.println("s:= "+s);
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void afterTextChanged(Editable s) {
				// System.out.println("afterTextChanged");
				if (s.length() == 0) {
					// _editTextCD.setText("0");
					// _editTextCD.setSelection(1);
					// 或者这样：
					_editTextCD.append("0");
					return;
				}

				int value = Integer.parseInt(s.toString());
				if (value == 0) {
					_toggleButtonSampling.setEnabled(false);
				} else
					_toggleButtonSampling.setEnabled(true);

			}

		});

		// -------------采样率选择
		_spinnerSampleRate
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						// 程序启动时自动 call这里一次

						_rate = Integer.parseInt(parent.getSelectedItem()
								.toString());
						// _rate = position;

						Toast.makeText(parent.getContext(), "rate: " + _rate,
								Toast.LENGTH_SHORT).show();

					}

					public void onNothingSelected(AdapterView<?> parent) {
						// 无效
					}
				});

		_spinnerSampleMode
				.setOnItemSelectedListener(new OnItemSelectedListener() {

					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {

//						File dataFolder = Environment
//								.getExternalStoragePublicDirectory(_dataFolderName);
//						if (!dataFolder.exists()) {
//							System.out
//									.println("!dataFolder.exists()-----------------------");
//							dataFolder.mkdirs();
//						}
						_fileName = parent.getSelectedItem().toString()
								+ "_"+_editTextOperator.getText() + ".xml";
						_textViewFnameHint.setText(_fnameHintStub + _fileName);

						_fileName = _dataFolder.getAbsolutePath()
								+ File.separator + _fileName;
						System.out.println("_fileName: " + _fileName);

						_file = new File(_fileName);

					}

					public void onNothingSelected(AdapterView<?> parent) {
					}
				});

		// ----------
		_editTextOperator.addTextChangedListener(new TextWatcher() {

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				System.out.println("s, start, before, count:= " + s + ", "
						+ start + ", " + before + ", " + count);
				
				_fileName=_spinnerSampleMode.getSelectedItem().toString()+"_"+s+".xml";
				_textViewFnameHint.setText(_fnameHintStub + _fileName);
				_fileName = _dataFolder.getAbsolutePath()
						+ File.separator + _fileName;
				System.out.println("_fileName: " + _fileName);

				_file = new File(_fileName);

//				_textViewFnameHint.setText(_fnameHintStub+)
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void afterTextChanged(Editable s) {
			}
		});

	}// respondEvents

	@Override
	public void onCreate(Bundle savedInstanceState) {
		System.out.println("onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getActionBar().setHomeButtonEnabled(true);
		// getActionBar().setDisplayHomeAsUpEnabled(true); //区别在于左侧会多一个小箭头

		_dataFolder = Environment
				.getExternalStoragePublicDirectory(_dataFolderName);
		if (!_dataFolder.exists()) {
			System.out.println("!_dataFolder.exists()-----------------------");
			_dataFolder.mkdirs();
		}

		initWidgets();
		respondEvents();

		_sm = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
		// _file = new File(_fileName);

	}// onCreate

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
		case android.R.id.home: // 应用左上角的图标， 非手机 home 键
			System.out.println("android.R.id.home");
			break;
		case R.id.menu_settings:
			Toast.makeText(this, "no settings~~", Toast.LENGTH_SHORT).show();
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
			// aboutDlg.setCancelable(false); // 不可手动取消
			aboutDlg.show();

			break;
		case R.id.menu_debug:
			Toast.makeText(this, _debugInfo, Toast.LENGTH_SHORT).show();
			break;
		case R.id.menuItemViewXml:
			System.out.println("R.id.menuItemViewXml");

			System.out.println(_fileName);
			System.out.println("_file.exists():= " + _file.exists());

			// 用默认程序打开 xml:
			if (!_file.exists()) {
				// _file.getAbsolutePath()
				Toast.makeText(this, _file.getAbsolutePath() + " 可能尚未生成，或已被删除",
						Toast.LENGTH_SHORT).show();
				break;
			}
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			// File file = new File(
			// "/data/data/com.zhangxaochen.huaweiproj/curve.png");

			// intent.setDataAndType(Uri.fromFile(file), "image/*");
			intent.setDataAndType(Uri.fromFile(_file), "text/*");
			startActivity(intent); // √
			// startActivity(Intent.createChooser(intent, getResources()
			// .getString(R.string.menuViewXml))); // √

			break;

		case R.id.menuItemViewCurve:
			Intent intentViewCurve = new Intent(this, ViewCurveActivity.class);
			intentViewCurve.putExtra(HuaweiProj.kSampleRate, _rate);

			startActivity(intentViewCurve);
			break;

		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	// 必须 public
	public void on_buttonEnableCD_clicked(View view) {
		System.out.println("on_buttonEnableCD_clicked");

		String txt = _editTextCD.getText().toString();
		if (txt.equals("") || Integer.parseInt(txt) == 0)
			_toggleButtonSampling.setEnabled(_isCdEnabled);
		else
			_toggleButtonSampling.setEnabled(true);

		String btnText = this.getString(R.string.buttonEnableCD);
		if (_isCdEnabled) {
			// 本来 true， 现在关闭：
			_buttonEnableCD.setText(btnText + _isNotEnabledString);
		} else {
			// 本来 false， 现在打开：
			_buttonEnableCD.setText(btnText + _isEnabledString);
		}
		_isCdEnabled = !_isCdEnabled;
		_editTextCD.setEnabled(_isCdEnabled);

	}

	public void on_buttonSaveAndClear_clicked(View view) {
		System.out.println("on_buttonSaveAndClear_clicked");

		_savingDlg.show();
		new WriteXmlTask().execute();
	}

	public void on_toggleButtonSampling_clicked(View view) {
		System.out.println("on_toggleButtonSampling_clicked");

		boolean isOn = _toggleButtonSampling.isChecked();
		// System.out.println("isOn:= "+isOn); //进回调之前系统已经 on/off 了
		if (isOn) {
			uiStartSampling();
			_listener.registerWithSensorManager(_sm, aMillion / _rate);

			if (_isCdEnabled) {
				// 秒：
				_cdDuration = Integer
						.parseInt(_editTextCD.getText().toString());

				// +30ms， 弥补CountDownTimer 的bug：
				_timer = new CountDownTimer(_cdDuration * 1000 + 30, 1000) {

					@Override
					public void onTick(long millisUntilFinished) {
						_editTextCD.setText("" + millisUntilFinished / 1000);
					}

					@Override
					public void onFinish() {
						System.out.println("onFinish");

						stopSampling();
					}
				};
				_timer.start();

			}

		} else { // !isOn
			stopSampling();

			if (_isCdEnabled)
				_timer.cancel();
		}
	}// on_toggleButtonSampling_clicked

	private void stopSampling() {
		uiStopSampling();
		_listener.unregisterWithSensorManager(_sm);

		MySensorData mySensorData=_listener.getSensorData();
//		System.out.println("abuf: "+mySensorData.getAbuf().size());
//		System.out.println("mbuf: "+mySensorData.getMbuf().size());
//		System.out.println("gbuf: "+mySensorData.getGbuf().size());
//		System.out.println("rbuf: "+mySensorData.getRbuf().size());
		_debugInfo=
		"abuf:\t"+mySensorData.getAbuf().size()+"\n"+
		"mbuf:\t"+mySensorData.getMbuf().size()+"\n"+
		"gbuf:\t"+mySensorData.getGbuf().size()+"\n"+
		"rbuf:\t"+mySensorData.getRbuf().size()+"\n";

		
		// 加一条数据，（还没存文件）
		_captureSessionNode.addNode(mySensorData);
		mySensorData.clearAllBuf(); // 连那些没用到的 buf 也清空，以免内存泄露

	}

	private void uiStopSampling() {
		_toggleButtonSampling.setChecked(false);

		if (_isCdEnabled) // 重要！！
			_editTextCD.setText("" + _cdDuration);
		_relativeLayoutEnableCD.enablePanel();
		// _relativeLayoutSampleRate.enablePanel();

	}

	private void uiStartSampling() {
		// 采样时disable闲杂组件
		_relativeLayoutEnableCD.disablePanel();
		// _relativeLayoutSampleRate.disablePanel();

		ActionBar bar = getActionBar();
		bar.setDisplayShowHomeEnabled(false);
		bar.setDisplayShowCustomEnabled(false);
	}

	@Override
	public void onBackPressed() {
		// IME shown 的时候仍然检测不到 back key！！！！！！！
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		System.out.println("onKeyDown, keyCode:= " + keyCode);

		return super.onKeyDown(keyCode, event);
	}

}
