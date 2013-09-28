package com.zhangxaochen.huaweiproj;

import java.io.File;
import java.io.IOException;

import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.mysensorlistener.Consts;
import com.example.mysensorlistener.MySensorListener;
import com.example.mysensorlistener.MySensorListener.MySensorData;
import com.zhangxaochen.sensordataxml.CaptureSessionNode;
import com.zhangxaochen.sensordataxml.XmlRootNode;

@SuppressLint("NewApi")
public class HuaweiProj extends BaseActivity{
	private String _driveDpString="";
	private String _driveSubFname = "";
	Dialog _drivingSubDlg;
	RadioGroup _rgDrive;
	RadioGroup _rgDp;
	MediaPlayer _mpTick;
	MediaPlayer _mpStart;
	MediaPlayer _mpStop;

	private String _debugInfo;

	// intent putExtra keys:
	public static final String kSampleRate = "sampleRate";

	public static final int aMillion = 1000 * 1000;

//	private static long _exitTimeStamp = -1;


	// xml 存储
	// String _fileName="huawei.xml"; //若不加绝对路径， 无权限写文件
	// String _fileName = "/data/data/com.zhangxaochen.huaweiproj/huawei.xml";

	String _fileName = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + File.separator + "huawei.xml"; // 即
																// /mnt/sdcard/huawei.xml

	CaptureSessionNode _captureSessionNode = new CaptureSessionNode();
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
		// ---------------------------driving_sub_dlg 对话框，2013年3月26日添加
		_drivingSubDlg = new Dialog(HuaweiProj.this) {

			private void initDlg() {
				_rgDp=(RadioGroup) findViewById(R.id.radioGroupDp);
				_rgDp.clearCheck();
				final RadioButton rbDpDriver=(RadioButton) findViewById(R.id.radioDpDriver);
				final RadioButton rbDpPassenger=(RadioButton) findViewById(R.id.radioDpPassenger);
				
				
//				RadioGroup rgDrive = (RadioGroup) findViewById(R.id.radioGroupDrive);
				_rgDrive = (RadioGroup) findViewById(R.id.radioGroupDrive);
				_rgDrive.clearCheck(); // √
				final RadioButton rbDriveAcc = (RadioButton) findViewById(R.id.radioDriveAccelerate);
				final RadioButton rbDriveDec = (RadioButton) findViewById(R.id.radioDriveDecelerate);
				final RadioButton rbDriveTurn = (RadioButton) findViewById(R.id.radioDriveTurn);
				final RadioButton rbDriveOther = (RadioButton) findViewById(R.id.radioDriveOther);
				// rbDriveOther.setChecked(true);
				final EditText editTextDriveOther = (EditText) findViewById(R.id.editTextDriveOther);
				final Button btnOk = (Button) findViewById(R.id.buttonDriveOK);

				btnOk.setOnClickListener(new View.OnClickListener() {

					public void onClick(View v) {
						_drivingSubDlg.dismiss();
				
						_fileName = _spinnerSampleMode.getSelectedItem()
								.toString()
								+ "_"
								+ _editTextOperator.getText()
								+ "_" +_driveDpString+"_"+ _driveSubFname + ".xml";

						_textViewFnameHint.setText(_fnameHintStub + _fileName);
						_fileName = _dataFolder.getAbsolutePath()
								+ File.separator + _fileName;
						System.out.println("_fileName: " + _fileName);

					}
				});

				_rgDp.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						System.out.println("_rgDp: onCheckedChanged");
						
						if(checkedId==rbDpDriver.getId())
							_driveDpString="driver";
						else if(checkedId==rbDpPassenger.getId())
							_driveDpString="passenger";
						
						if(_rgDrive.getCheckedRadioButtonId()!=-1)
							btnOk.setEnabled(true);
						

					} //onCheckedChanged
				});

				_rgDrive.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					public void onCheckedChanged(RadioGroup group, int checkedId) {
						System.out.println("rgDrive: onCheckedChanged");

						if (checkedId != rbDriveOther.getId()) {
							findViewById(R.id.textViewDriveOther).setEnabled(
									false);
							editTextDriveOther.setEnabled(false);
							if (_rgDp.getCheckedRadioButtonId() != -1)
								btnOk.setEnabled(true);

							if (checkedId == rbDriveAcc.getId())
								_driveSubFname = "accelerate";
							else if (checkedId == rbDriveDec.getId())
								_driveSubFname = "decelerate";
							else if (checkedId == rbDriveTurn.getId())
								_driveSubFname = "turn";
						} else if (checkedId == rbDriveOther.getId()) {
							findViewById(R.id.textViewDriveOther).setEnabled(
									true);
							editTextDriveOther.setEnabled(true);
							if (editTextDriveOther.getText().length() == 0)
								btnOk.setEnabled(false);
						}
//						_fileName = _spinnerSampleMode.getSelectedItem()
//								.toString()
//								+ "_"
//								+ _editTextOperator.getText()
//								+ "_" + _driveSubFname + ".xml";
//
//						_textViewFnameHint.setText(_fnameHintStub + _fileName);
//						_fileName = _dataFolder.getAbsolutePath()
//								+ File.separator + _fileName;
//						System.out.println("_fileName: " + _fileName);

					} // onCheckedChanged
				});

				editTextDriveOther.addTextChangedListener(new TextWatcher() {

					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
						_driveSubFname = s.toString();
						if (s.length() == 0) {
							btnOk.setEnabled(false);
						} else {
							btnOk.setEnabled(true);
						}
					}

					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					public void afterTextChanged(Editable s) {
					}
				});
			} // initDlg

			@Override
			protected void onCreate(Bundle savedInstanceState) {
				System.out.println("_drivingSubDlg: onCreate");
				
				super.onCreate(savedInstanceState);
				setContentView(R.layout.driving_sub_dlg);
				initDlg();
			} // onCreate
		};
//		_drivingSubDlg.setCanceledOnTouchOutside(false);
		_drivingSubDlg.setTitle("驾驶模式设置");
		_drivingSubDlg.setCancelable(false);
		_drivingSubDlg.setOnShowListener(new OnShowListener() {
			
			public void onShow(DialogInterface dialog) {
//				RadioGroup rgDrive=(RadioGroup)findViewById(R.id.radioGroupDrive); //×, 必须设成全局
//				_rgDrive.clearCheck(); //会导致 checkChanged
//				_rgDp.clearCheck();
			}
		});

		// ======================================================
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
		_editTextOperator.setSelectAllOnFocus(true);
		_textViewFnameHint = (TextView) findViewById(R.id.textViewFnameHint);
	} // initWidgets

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

						// File dataFolder = Environment
						// .getExternalStoragePublicDirectory(_dataFolderName);
						// if (!dataFolder.exists()) {
						// System.out
						// .println("!dataFolder.exists()-----------------------");
						// dataFolder.mkdirs();
						// }
						String prefix = parent.getSelectedItem().toString();
						if (prefix.equals("driving")) {
//							RadioGroup rgDrive=(RadioGroup) findViewById(R.id.radioGroupDrive);
//							_rgDrive.clearCheck(); //每次clear
							
							_drivingSubDlg.show();
						}
						_fileName = prefix + "_" + _editTextOperator.getText()
								+ ".xml";
						// _fileName = prefix + "_" +
						// _editTextOperator.getText();
						// if (!_driveSubFname.equals(""))
						// _fileName += "_" + _driveSubFname;
						// _fileName += ".xml";
						_textViewFnameHint.setText(_fnameHintStub + _fileName);

						_fileName = _dataFolder.getAbsolutePath()
								+ File.separator + _fileName;
						System.out.println("_fileName: " + _fileName);

						// _file = new File(_fileName);

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

				String prefix=_spinnerSampleMode.getSelectedItem().toString();
				_fileName = prefix + "_" + s;
//				if (!_driveSubFname.equals("") && prefix.equals("driving")){
				if (prefix.equals("driving")) {
					_fileName += "_" +_driveDpString +"_"+_driveSubFname;
				}
				_fileName += ".xml";
				_textViewFnameHint.setText(_fnameHintStub + _fileName);
				_fileName = _dataFolder.getAbsolutePath() + File.separator
						+ _fileName;
				System.out.println("_fileName: " + _fileName);

				// _file = new File(_fileName);

				// _textViewFnameHint.setText(_fnameHintStub+)
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void afterTextChanged(Editable s) {
				System.out.println("_editTextOperator: afterTextChanged");
			}
		});

	}// respondEvents

	@Override
	public void onCreate(Bundle savedInstanceState) {
		System.out.println("onCreate");

		// new Dialog(this).show(); //没按钮

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		int ver = Build.VERSION.SDK_INT;
		System.out.println("version: " + ver);
		// Build.VERSION_CODES.JELLY_BEAN;

		_dataFolder = Environment
				.getExternalStoragePublicDirectory(_dataFolderName);
		if (!_dataFolder.exists()) {
			System.out.println("!_dataFolder.exists()-----------------------");
			_dataFolder.mkdirs();
		}

		initWidgets();
		respondEvents();

		_sm = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

		_mpTick=MediaPlayer.create(this, R.raw.tick);
		_mpStart=MediaPlayer.create(this, R.raw.start);
		_mpStop=MediaPlayer.create(this, R.raw.stop);
		try {
			_mpTick.prepare();
			_mpStart.prepare();
			_mpStop.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		AudioManager am=(AudioManager) getSystemService(Context.AUDIO_SERVICE);
		am.setStreamVolume(AudioManager.STREAM_MUSIC, 10, 0);
		
	}// onCreate

	@Override
	protected void onDestroy() {
		_mpStart.release();
		_mpStop.release();
		_mpTick.release();

		super.onDestroy();
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		getMenuInflater().inflate(R.menu.activity_main, menu);
//		return true;
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		System.out.println("onOptionsItemSelected");
//		int itemId = item.getItemId();
//
//		// System.out.println("itemId:= " + itemId);
//
//		switch (itemId) {
//		case android.R.id.home: // 应用左上角的图标， 非手机 home 键
//			System.out.println("android.R.id.home");
//			break;
//		case R.id.menu_settings:
////			Toast.makeText(this, "no settings~~", Toast.LENGTH_SHORT).show();
//			SettingsDlg settingsDlg=new SettingsDlg(this);
//			settingsDlg.setOnDismissListener(new OnDismissListener() {
//				
//				public void onDismiss(DialogInterface dialog) {
////					Toast.makeText(HuaweiProj.this, "settingsDlg...onDismiss", Toast.LENGTH_SHORT).show();
//					
//				}
//			});
//			settingsDlg.setTitle(R.string.settingsTitle);
//			settingsDlg.show();
//			break;
//		case R.id.menu_about:
//			Dialog aboutDlg = new Dialog(this) {
//
//				@Override
//				protected void onCreate(Bundle savedInstanceState) {
//					super.onCreate(savedInstanceState);
//					setContentView(R.layout.about_dlg);
//					TextView textViewAbout = (TextView) findViewById(R.id.textViewAbout);
//					textViewAbout.setText(Html
//							.fromHtml(getString(R.string.about_text)));
//					textViewAbout.setMovementMethod(LinkMovementMethod
//							.getInstance());
//				}
//
//			};
//			// aboutDlg.setContentView(R.layout.about_dlg);
//			aboutDlg.setTitle(R.string.aboutTitle);
//			// aboutDlg.setCancelable(false); // 不可手动取消
//			aboutDlg.show();
//
//			break;
//		case R.id.menu_debug:
//			Toast.makeText(this, _debugInfo, Toast.LENGTH_SHORT).show();
//			break;
//		case R.id.menuItemViewXml:
//			System.out.println("R.id.menuItemViewXml");
//
//			System.out.println(_fileName);
//			_file=new File(_fileName);
//			System.out.println("_file.exists():= " + _file.exists());
//
//			// 用默认程序打开 xml:
//			if (!_file.exists()) {
//				// _file.getAbsolutePath()
//				Toast.makeText(this, _file.getAbsolutePath() + " 可能尚未生成，或已被删除",
//						Toast.LENGTH_SHORT).show();
//				break;
//			}
//			Intent intent = new Intent();
//			intent.setAction(Intent.ACTION_VIEW);
//			// File file = new File(
//			// "/data/data/com.zhangxaochen.huaweiproj/curve.png");
//
//			// intent.setDataAndType(Uri.fromFile(file), "image/*");
//			intent.setDataAndType(Uri.fromFile(_file), "text/*");
//			startActivity(intent); // √
//			// startActivity(Intent.createChooser(intent, getResources()
//			// .getString(R.string.menuViewXml))); // √
//
//			break;
//
//		case R.id.menuItemViewCurve:
//			Intent intentViewCurve = new Intent(this, ViewCurveActivity.class);
//			intentViewCurve.putExtra(HuaweiProj.kSampleRate, _rate);
//
//			startActivity(intentViewCurve);
//			break;
//
//		default:
//			break;
//		}
//
//		return super.onOptionsItemSelected(item);
//	}

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
		_file = new File(_fileName);
//		new WriteXmlTask().execute();
		WriteXmlTask task=new WriteXmlTask(){
			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				
				_savingDlg.dismiss();
				Toast.makeText(getApplicationContext(), 
						"已存到: " + _file.getAbsolutePath(),
						Toast.LENGTH_SHORT).show();
			}
		};
//		task.setCsNode(_captureSessionNode)
		task.setXmlRootNode(_captureSessionNode)
		.setFile(_file)
		.setPersister(_persister)
		.execute();

	}

	public void on_toggleButtonSampling_clicked(View view) {
		System.out.println("on_toggleButtonSampling_clicked");

		boolean isOn = _toggleButtonSampling.isChecked();
		// System.out.println("isOn:= "+isOn); //进回调之前系统已经 on/off 了
		if (isOn) {
			final Dialog tickDlg=new Dialog(this);
			tickDlg.setContentView(R.layout.saving_dlg);
			tickDlg.setTitle("tick-tick");
			tickDlg.setCancelable(false);
			tickDlg.show();
			
			SharedPreferences sp=this.getSharedPreferences(Consts.prefSettings, MODE_PRIVATE);
			int preCd=sp.getInt(Consts.kPreCd, 0);
			CountDownTimer preTimer=new CountDownTimer(preCd*1000+50, 1000) {
				
				@Override
				public void onTick(long millisUntilFinished) {
					_mpTick.start();
				}
				
				@Override
				public void onFinish() {
					tickDlg.dismiss();
					startSampling();
				}
			};
			preTimer.start();
			
		} else { // !isOn
			
			stopSampling();

			if (_isCdEnabled)
				_timer.cancel();
		}
	}// on_toggleButtonSampling_clicked

	private void stopSampling() {
		_mpStop.start();	//停止音乐
		uiStopSampling();
		_listener.unregisterWithSensorManager(_sm);

		final MySensorData mySensorData = _listener.getSensorData();
		// System.out.println("abuf: "+mySensorData.getAbuf().size());
		// System.out.println("mbuf: "+mySensorData.getMbuf().size());
		// System.out.println("gbuf: "+mySensorData.getGbuf().size());
		// System.out.println("rbuf: "+mySensorData.getRbuf().size());
		_debugInfo = "abuf:\t" + mySensorData.getAbuf().size() + "\n"
				+ "mbuf:\t" + mySensorData.getMbuf().size() + "\n" + "gbuf:\t"
				+ mySensorData.getGbuf().size() + "\n" + "rbuf:\t"
				+ mySensorData.getRbuf().size() + "\n";

		AlertDialog.Builder builder=new AlertDialog.Builder(this);
		builder.setTitle("是否保存本次数据")
		.setCancelable(false)
		.setPositiveButton("保存", new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				// 加一条数据，（还没存文件）
				_captureSessionNode.addNode(mySensorData);
//				mySensorData.clearAllBuf(); // 连那些没用到的 buf 也清空，以免内存泄露
			}
		})
		.setNegativeButton("放弃", new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
//				mySensorData.clearAllBuf(); // 连那些没用到的 buf 也清空，以免内存泄露
			}
		} );
		Dialog addNodeDlg=builder.create();
		addNodeDlg.show();

		// 加一条数据，（还没存文件）
//		_captureSessionNode.addNode(mySensorData);
//		mySensorData.clearAllBuf(); // 连那些没用到的 buf 也清空，以免内存泄露

	}

	private void uiStopSampling() {
		_toggleButtonSampling.setChecked(false);

		if (_isCdEnabled) // 重要！！
			_editTextCD.setText("" + _cdDuration);
		_relativeLayoutEnableCD.enablePanel();
		// _relativeLayoutSampleRate.enablePanel();

	}

	private void startSampling(){
		_mpStart.start();
		uiStartSampling();
		
		_listener.reset();
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

	}
	
	private void uiStartSampling() {
		// 采样时disable闲杂组件
		_relativeLayoutEnableCD.disablePanel();
		// _relativeLayoutSampleRate.disablePanel();

		if (VERSION.SDK_INT > VERSION_CODES.HONEYCOMB) {
			ActionBar bar = getActionBar();
			bar.setDisplayShowHomeEnabled(false);
			bar.setDisplayShowCustomEnabled(false);
		}
	}

//	@Override
//	public void onBackPressed() {
//		// IME shown 的时候仍然检测不到 back key！！！！！！！
//		System.out.println("onBackPressed");
//
//		long curTime = SystemClock.uptimeMillis();
//
//		System.out.println("curTime:= " + curTime);
//
//		if (curTime - _exitTimeStamp < 1000) {
//			System.out.println("curTime - _exitTimeStamp < 1000");
//			super.onBackPressed(); // ==finish()
//			// finish();
//		} else {
//			Toast.makeText(this, "press BACK again to exit", Toast.LENGTH_SHORT)
//					.show();
//			_exitTimeStamp = curTime;
//		}
//	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		System.out.println("onKeyDown, keyCode:= " + keyCode);

		return super.onKeyDown(keyCode, event);
	}

}//HuaweiProj

// 非 UI 线程写文件：
class WriteXmlTask extends AsyncTask<Void, Void, Void> {
	XmlRootNode _xmlRootNode;
//	CaptureSessionNode _captureSessionNode;
	File _file;
	Persister _persister;
//	Activity _mainActivity;
	
	public WriteXmlTask() {
	}
	
//	public WriteXmlTask setCsNode(CaptureSessionNode csNode){
//		_captureSessionNode=csNode;
//		return this;
//	}
	
	public WriteXmlTask setXmlRootNode(XmlRootNode rootNode){
		_xmlRootNode=rootNode;
		return this;
	}
	
	public WriteXmlTask setFile(File file){
		_file=file;
		return this;
	}
	
	public WriteXmlTask setPersister(Persister persister){
		_persister=persister;
		return this;
	}
	
//	public WriteXmlTask setMainActivity(Activity activity){
//		_mainActivity=activity;
//		return this;
//	}

	@Override
	protected Void doInBackground(Void... params) {
		System.out.println("doInBackground()");
//		if (_captureSessionNode == null || _file == null
//				|| _persister == null) {
//			System.out
//					.println("_captureSessionNode==null || _file==null || _persister == null");
//			return null;
//		}
		if (_xmlRootNode == null || _file == null
				|| _persister == null) {
			System.out
					.println("_xmlRootNode==null || _file==null || _persister == null");
			return null;
		}

		try {
//			_persister.write(_captureSessionNode, _file);
			_persister.write(_xmlRootNode, _file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		System.out.println("onPostExecute");

		super.onPostExecute(result);
		// _savingDlg.hide(); //导致第二次 show 错误
//		_savingDlg.dismiss(); // √

//		_captureSessionNode.clearAllNodes();
		_xmlRootNode.clear();

		//TODO: to be overrided...
	}

}// WriteXmlTask


