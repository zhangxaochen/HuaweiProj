package com.zhangxaochen.huaweiproj.whole;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.mysensorlistener.Consts;
import com.example.mysensorlistener.MySensorListener;
import com.example.mysensorlistener.MySensorListener.MySensorData;
import com.zhangxaochen.sensordataxml.NewSessionNode;
import com.zhangxaochen.sensordataxml.XmlRootNode;

public class DrivingUI extends BaseActivity{
	final String nan="NAN";
	
	//----------------------xml data file
//	String _fileName;
//	String _fileName=Environment.getExternalStorageDirectory().getAbsolutePath()
//			+File.separator+"huawei.xml";
	
//	CaptureSessionNode _captureSessionNode = new CaptureSessionNode();
	XmlRootNode _newSessionNode=new NewSessionNode();
	
	File _file;
	Format _format = new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>");
	Persister _persister = new Persister(_format);

	
	//----------------------sensor
	SensorManager _sm;
	MySensorListener _listener=new MySensorListener();
	
	//----------------------audio
	MediaPlayer _mpTick;
	MediaPlayer _mpStart;
	MediaPlayer _mpStop;

	//----------------------prefs
	SharedPreferences _sp;
	Editor _spEditor;
	TreeSet<String> _usersSet;
	String _currentUser;
	int _curActType, _curPosition, _curAction;
	
	
	//----------------------UI
//	MyRelativeLayout _relativeLayoutOptions;
//	MyLinearLayout _linearLayoutOptions;
	
	Switch _switchCd;
	EditText _editTextCd;
	Spinner _spinnerUsers;
	Button _buttonAddUser;
	Spinner _spinnerActType;
	Spinner _spinnerPosition;
	Spinner _spinnerActions;
	ToggleButton _toggleButtonSampling;
	ScrollView _scrollViewOptions;
	Dialog _savingDlg;
	AlertDialog _addUserDlg;
	
	
	boolean _isCdEnabled=false;
	int _cdDuration=0;
	CountDownTimer _timer;
//	int _sampleRate=60;
	final String _dataFolderName="huaweiproj-whole";
	File _dataFolder;
//	String _debugInfo;
	
	//用 _userSet 重新填充 _spinnerUsers; 设置 currentUser
	private void setUsersSpinner(){
		if(_usersSet==null)
			return;
		ArrayList<String> usersList=new ArrayList<String>(_usersSet);
		ArrayAdapter<String> usersArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, usersList);
		usersArrayAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		_spinnerUsers.setAdapter(usersArrayAdapter);
		
		String currentUser=_sp.getString(Consts.kCurrentUser, "");
		if(!currentUser.isEmpty()){
			_spinnerUsers.setSelection(usersList.indexOf(currentUser), true);	//× animate true 似乎没啥用。。 
		}
//		_spinnerUsers.select
	}
	
	private boolean userExists(String uname){
		System.out.println("in userExists");
		if(_usersSet==null)
			return false;
		
		return _usersSet.contains(uname.toLowerCase());
	}

	// UI 组件实例化
	/**
	 * 
	 */
	void initWidgets() {
//		_linearLayoutOptions=(MyLinearLayout) findViewById(R.id.linearLayoutOptions);
		
		_switchCd=(Switch) findViewById(R.id.switchCD);
		_editTextCd=(EditText) findViewById(R.id.editTextCD);
		_editTextCd.setText("0");
		
		_spinnerUsers=(Spinner) findViewById(R.id.spinnerUser);
		setUsersSpinner();
//		_spinnerUsers.getSelectedItem()==null?
		
		_buttonAddUser=(Button) findViewById(R.id.buttonAddUser);
		
		_spinnerActType=(Spinner) findViewById(R.id.spinnerActTypes);
		_spinnerActType.setSelection(_curActType, true);
		
		_spinnerPosition=(Spinner) findViewById(R.id.spinnerPosition);
		_spinnerPosition.setSelection(_curPosition, true);
		
		_spinnerActions=(Spinner) findViewById(R.id.spinnerActions);
//		_spinnerActions.setSelection(_spinnerActions.getCount()-1); //默认置为 “混合数据”
		_spinnerActions.setSelection(_curAction, true);
		
		_toggleButtonSampling=(ToggleButton) findViewById(R.id.toggleButtonSampling);
		//首次安装， spinnerUser 为空， 则 disable 开始按钮
		// _toggleButtonSampling.setEnabled(_spinnerUsers.getSelectedItem()!=null);
		
		//如果 spinnerAction text==NAN， 则 disable 开始按钮
//		Toast.makeText(this, _spinnerActions.getSelectedItem().toString(), Toast.LENGTH_SHORT).show();
		
		// _toggleButtonSampling.setEnabled(_spinnerActions.getSelectedItem().toString()!=nan);
		setToggleBtnState();
		
		
		_scrollViewOptions=(ScrollView) findViewById(R.id.scrollViewOptions);
		
		_savingDlg=new Dialog(this);
		_savingDlg.setContentView(R.layout.saving_dlg);
		_savingDlg.setTitle(R.string.savingTitle);
		_savingDlg.setCancelable(false);

		
		
		//----------------------add user dialog
		AlertDialog.Builder builder=new AlertDialog.Builder(this);
		builder
		.setTitle("请输入用户名(大小写不敏感)")
		.setPositiveButton("确定添加", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//添加到 spinner && prefs
				EditText editTextAddUser=(EditText) _addUserDlg.findViewById(R.id.editTextAddUser);	//√, 
				String uname=editTextAddUser.getText().toString();
				if(_usersSet==null)
					_usersSet=new TreeSet<String>();
				if(_usersSet.contains(uname))
					return;//其实此判断多余
				_usersSet.add(uname);
				
				_spEditor.putString(Consts.kCurrentUser, uname);
				_spEditor.commit();
				
				setUsersSpinner();
				// _toggleButtonSampling.setEnabled(_spinnerUsers.getSelectedItem()!=null);
				setToggleBtnState();
				
				System.out.println("BUTTON_POSITIVE:: _usersSet: "+_usersSet);
				_spEditor.remove(Consts.kUsers);
				_spEditor.commit();//管用了
				_spEditor.putStringSet(Consts.kUsers, _usersSet);
				boolean putSuccess=_spEditor.commit();
				System.out.println("putSuccess: "+putSuccess);
			}

		})
		.setView(getLayoutInflater().inflate(R.layout.add_user_dlg, null));
		
		_addUserDlg=builder.create();
		_addUserDlg.show();
		_addUserDlg.dismiss();
		
	}
	
	void respondEvents(){
		//--------------------倒计时开关
		_switchCd.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				System.out.println("_switchCd.onCheckedChanged");
				
				_editTextCd.setEnabled(isChecked);
				_switchCd.setText(DrivingUI.this.getString(isChecked?R.string.cn_enabled:R.string.cn_disabled));
				// int cdDt=Integer.parseInt(_editTextCd.getText().toString());
				// if(isChecked && cdDt==0)
					// _toggleButtonSampling.setEnabled(false);
				// else
					// _toggleButtonSampling.setEnabled(true);
				setToggleBtnState();
//				_editTextCd.performClick(); // 无法控制输入法弹出与否
			}
		});
		
		//---------------------倒计时输入框
		_editTextCd.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				if(s.length()==0){
					_editTextCd.append("0");
					return;
				}
				
				// int value=Integer.parseInt(s.toString());
				// _toggleButtonSampling.setEnabled(value!=0);
				setToggleBtnState();
			}
		});
		
		//--------------------------------_spinnerUsers，选中时顺便写到 prefs
		_spinnerUsers.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String uname=parent.getSelectedItem().toString();
				 _spEditor.putString(Consts.kCurrentUser, uname);
				 _spEditor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		
		// --------------------------------_spinnerActType，选中时顺便写到 prefs
		_spinnerActType.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String actTypeName=parent.getSelectedItem().toString();
				Toast.makeText(DrivingUI.this, actTypeName+position, Toast.LENGTH_SHORT).show();
				
				//set the 2nd sublevel spinner content accordingly:
				int[] resids = { R.array.actDrive, 
						R.array.actWalkRun, 
						R.array.actWalkRun, 
						R.array.actFall, 
						R.array.actDaily
						};
				
				ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
						DrivingUI.this, resids[position], android.R.layout.simple_spinner_item);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				_spinnerActions.setAdapter(adapter);
				
				_spEditor.putInt(Consts.kCurActType, position);
				_spEditor.commit();
			}//onItemSelected

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
			
		});
		
		// --------------------------------_spinnerActType，选中时顺便写到 prefs
		_spinnerPosition.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				_spEditor.putInt(Consts.kCurPosition, position);
				_spEditor.commit();
			}//onItemSelected

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		//--------------------------spinnerActions	如果 text==NAN, disable 开始按钮, 选中时顺便写到 prefs
		_spinnerActions.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String actionName=parent.getSelectedItem().toString();
				Toast.makeText(DrivingUI.this, actionName, Toast.LENGTH_SHORT).show();
				System.out.println("----------------"+actionName+", "+_spinnerActions.getSelectedItem().toString().length());
				// if(actionName.contains(nan) )
					// _toggleButtonSampling.setEnabled(false);
				setToggleBtnState();
				
				_spEditor.putInt(Consts.kCurAction, position);
				_spEditor.commit();
			}//onItemSelected

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		
		//----------------------add user button clicked
		//见 on_buttonAddUser_click()
		
		//----------------------editTextAddUser
		final EditText editTextAddUser=(EditText) _addUserDlg.findViewById(R.id.editTextAddUser);
//		System.out.println("editTextAddUser: "+editTextAddUser+", "+_addUserDlg);
		if (editTextAddUser !=null)
		editTextAddUser.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				//检查是否已经存在此用户名
				boolean isExist=userExists(s.toString());
				_addUserDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(!isExist&&!s.toString().isEmpty());
				if(isExist)
					Toast.makeText(getApplicationContext(), 
							"\""+s.toString()+"\" 已存在", Toast.LENGTH_SHORT).show();
			}
		});
		
		//----------------------add user dialog positiveButton	移到了 initWidgets, 因为 View.OnClickListener 不好
//		_addUserDlg.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				//添加到 spinner && prefs
//				String uname=editTextAddUser.getText().toString();
//				if(_usersSet==null)
//					_usersSet=new HashSet<String>();
//				if(_usersSet.contains(uname))
//					return;//其实此判断多余
//				_usersSet.add(uname);
//				setUsersSpinner();
//				
////				_spEditor=_sp.edit();
//				System.out.println("BUTTON_POSITIVE:: _usersSet: "+_usersSet);
//				_spEditor.remove(Consts.kUsers);
//				_spEditor.commit();//管用了
//				_spEditor.putStringSet(Consts.kUsers, _usersSet);
//				boolean putSuccess=_spEditor.commit();
//				System.out.println("putSuccess: "+putSuccess);
//				
//				//dlg 居然不能自动 dismiss。。。
//				_addUserDlg.dismiss();
//			}
//		});
		
		//----------------------开始按钮
		_toggleButtonSampling.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				System.out.println("onCheckedChanged===========");
				if(isChecked){
//					final Dialog tickDlg=new Dialog(getBaseContext());
					final Dialog tickDlg=new Dialog(DrivingUI.this);
					tickDlg.setContentView(R.layout.saving_dlg);
					tickDlg.setTitle("准备开始!");
					tickDlg.setCancelable(false);
					tickDlg.show();
					
					int preCd=_sp.getInt(Consts.kPreCd, 0);
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
				}
				else{
					stopSampling();
					
					if(_switchCd.isChecked())
						_timer.cancel();
				}
			}
		});
		
	}//respondEvents
	
	private void startSampling(){
		_mpStart.start();
		uiStartSampling();
		
		_listener.reset();
		_listener.registerWithSensorManager(_sm, Consts.aMillion/_sampleRate);
		
		((NewSessionNode)_newSessionNode).setBeginTime(System.currentTimeMillis()*Consts.MS2S);
		System.out.println("setBeginTime: "+System.currentTimeMillis()*Consts.MS2S);
		
		if(_switchCd.isChecked()){
			// 秒：
			_cdDuration = Integer
					.parseInt(_editTextCd.getText().toString());

			// +30ms， 弥补CountDownTimer 的bug：
			_timer = new CountDownTimer(_cdDuration * 1000 + 30, 1000) {

				@Override
				public void onTick(long millisUntilFinished) {
					_editTextCd.setText("" + millisUntilFinished / 1000);
				}

				@Override
				public void onFinish() {
					System.out.println("onFinish");

//					stopSampling();
					_toggleButtonSampling.setChecked(false);
				}
			};
			_timer.start();
		}
		
	}//startSampling
	
	private void uiStartSampling(){
//		_linearLayoutOptions.disablePanel();
		_scrollViewOptions.setVisibility(View.GONE);
		
		if(VERSION.SDK_INT>VERSION_CODES.HONEYCOMB){
			ActionBar bar=getActionBar();
			bar.setDisplayShowHomeEnabled(false);
			bar.setDisplayShowCustomEnabled(false);
		}
	}
	
	private void stopSampling() {
		System.out.println("in stopSampling======");
		_mpStop.start();	//停止音乐
		uiStopSampling();
		_listener.unregisterWithSensorManager(_sm);

		final MySensorData mySensorData = _listener.getSensorData();
		int asz=mySensorData.getAbuf().size();
		int gsz=mySensorData.getGbuf().size();
		int msz=mySensorData.getMbuf().size();
		int rsz=mySensorData.getRbuf().size();
		
		_debugInfo = "abuf:\t" + asz + "\n"
					+ "mbuf:\t" + msz + "\n" 
					+ "gbuf:\t" + gsz + "\n" 
					+ "rbuf:\t" + rsz + "\n";

		List<Integer> bufSizes=Arrays.asList(asz, gsz, msz, rsz);
		if (Collections.min(bufSizes)<Collections.max(bufSizes)/2.0)
			_debugInfo+="!!! some buf size too small\n";
		
		LinkedList<Double> ats=mySensorData.getATsBuf();
		LinkedList<Double> gts=mySensorData.getGTsBuf();
		LinkedList<Double> mts=mySensorData.getMTsBuf();
		LinkedList<Double> rts=mySensorData.getRTsBuf();

//		调试用：
//		ats.offerFirst(86400.);
		
		List<Double> tss=Arrays.asList(ats.peek(), gts.peek(), mts.peek(), rts.peek());
		_debugInfo+=String.format("%-5s\t%.2f", "ats:", ats.peek())+"\n"
				+String.format("%-5s\t%.2f", "gts:", gts.peek())+"\n"
				+String.format("%-5s\t%.2f", "mts:", mts.peek())+"\n"
				+String.format("%-5s\t\t%.2f", "rts:", rts.peek())+"\n";
		
		if(Collections.max(tss)-Collections.min(tss)>24*3600)
			_debugInfo+="!!! TimeStamp are not consistent\n";
		
		double afps=ats.size()*1./(ats.peekLast()-ats.peek()),
				gfps=gts.size()*1./(gts.peekLast()-gts.peek()),
				mfps=mts.size()*1./(mts.peekLast()-mts.peek()),
				rfps=rts.size()*1./(rts.peekLast()-rts.peek());
		
		_debugInfo+="afps:\t"+String.format("%.2f", afps)+"\n"
					+"gfps:\t"+String.format("%.2f", gfps)+"\n"
					+"mfps:\t"+String.format("%.2f", mfps)+"\n"
					+"rfps:\t"+String.format("%.2f", rfps)+"\n";
		
		if(Collections.min(Arrays.asList(afps, gfps, mfps, rfps))<30)
			_debugInfo+="!!! low FPS\n";

		
		if(asz*gsz*msz*rsz==0){
			String errMsg="ERROR: 某传感元件数据丢失!\n"+_debugInfo;
//			Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
			AlertDialog.Builder builder=new AlertDialog.Builder(this);
			builder
			.setTitle("传感器故障！")
			.setMessage(errMsg)
			.setNegativeButton("放弃本次采集", null);
			
			Dialog errDlg=builder.create();
			errDlg.show();

		}
		else{
			AlertDialog.Builder builder=new AlertDialog.Builder(this);
			builder.setTitle("是否保存本次数据")
			.setCancelable(false)
			.setPositiveButton("保存", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// 加一条数据，
//					_captureSessionNode.addNode(mySensorData);
					_newSessionNode.addNode(mySensorData);
					((NewSessionNode)_newSessionNode).setEndTime(System.currentTimeMillis()*Consts.MS2S);
					System.out.println("setEndTime: "+System.currentTimeMillis()*Consts.MS2S);
					
//					mySensorData.clearAllBuf(); // 连那些没用到的 buf 也清空，以免内存泄露
					
					//数据存文件, 构造文件名：
					_currentUser=_sp.getString(Consts.kCurrentUser, "");
					_curActType=_sp.getInt(Consts.kCurActType, 0);
					_curPosition=_sp.getInt(Consts.kCurPosition, 0);
					_curAction=_sp.getInt(Consts.kCurAction, 0);

					_fileName=_currentUser+"_"+"abcde".charAt(_curActType)+"_"+_curPosition+"_"+_curAction;

//					File dir=new File(_dataFolder.getAbsolutePath());
//					for(String f:dir.list())
//						System.out.println(f);
//					int fcnt=dir.list(new FilenameFilter() {
					int fcnt = _dataFolder.list(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String filename) {
							return filename.contains(_fileName)&&filename.endsWith(".xml");
						}
					}).length;
					
					_fileName=_dataFolder.getAbsolutePath()+File.separator
							+_fileName+"_"+fcnt+".xml";
					_file=new File(_fileName);
					_savingDlg.show();
					WriteXmlTask task=new WriteXmlTask(){
						@Override
						protected void onPostExecute(Void result) {
							super.onPostExecute(result);
							
							_savingDlg.dismiss();
							
							//===================检查数据存储xml的完整性
							boolean fileIntegrity=true;
//							try {
////								_file=new File("/mnt/sdcard/huaweiproj-driving/dailyLX_a9_1.xml");
//								_persister.read(_newSessionNode.getClass(), _file);
//							} catch (Exception e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//								
//								fileIntegrity=false;
//								_file.delete();
//								
//								//捕获异常，这里最可能是文件不完整导致， javax.xml.stream.XMLStreamException
//								//Message: XML 文档结构必须从头至尾包含在同一个实体内。
//								AlertDialog.Builder builder= new AlertDialog.Builder(DrivingUI.this);
//								builder.setTitle("保存错误！");
//								builder.setMessage("由于文件保存不完整，已删除此数据！");
//								builder.setCancelable(false);
//								builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
//									@Override
//									public void onClick(DialogInterface dialog, int which) {
//									}
//								});
//								Dialog dlg=builder.create();
//								dlg.show();								
//							}
							
							if(fileIntegrity){
								Toast.makeText(getApplicationContext(), 
										"已存到: " + _file.getAbsolutePath(),
										Toast.LENGTH_SHORT).show();
							}
						}
					};
//					task.setCsNode(_captureSessionNode)
					task.setXmlRootNode(_newSessionNode)
					.setFile(_file)
					.setPersister(_persister)
					.execute();
				}
			})
			.setNegativeButton("放弃", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
//					mySensorData.clearAllBuf(); // 连那些没用到的 buf 也清空，以免内存泄露
				}
			} );
			Dialog addNodeDlg=builder.create();
			addNodeDlg.show();
		}
	}//stopSampling
	
	private void uiStopSampling() {
//		_toggleButtonSampling.setChecked(false);

		if (_switchCd.isChecked()) // 重要！！
			_editTextCd.setText("" + _cdDuration);
//		_linearLayoutOptions.enablePanel();
		_scrollViewOptions.setVisibility(View.VISIBLE);
	}

	
	void loadAudio(){
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
		am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/2, 0);
	}
	
	void loadPrefs(){
		_sp=this.getSharedPreferences(Consts.prefSettings, MODE_PRIVATE);
		_spEditor=_sp.edit();
		
		_usersSet=new TreeSet<String>();
		_usersSet.addAll( _sp.getStringSet(Consts.kUsers, null));
		System.out.println("loadPrefs:: _usersSet: "+_usersSet);
		
		_currentUser=_sp.getString(Consts.kCurrentUser, "");
		
		_curActType=_sp.getInt(Consts.kCurActType, 0);
		_curPosition=_sp.getInt(Consts.kCurPosition, 0);
		_curAction=_sp.getInt(Consts.kCurAction, 0);
	}
	
	
	public void on_buttonAddUser_clicked(View view){
		System.out.println("on_buttonAddUser_clicked");
		
		_addUserDlg.show();

		//若文本框为空， disable 确定按钮
		EditText editTextAddUser=(EditText) _addUserDlg.findViewById(R.id.editTextAddUser);	//√, 
		String uname=editTextAddUser.getText().toString();
		System.out.println("uname.isEmpty(): " + uname.isEmpty());
		if(uname.isEmpty())
			_addUserDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);

//		_addUserDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(
//				!uname.isEmpty() && !_usersSet.contains(uname.toLowerCase()));
		
	}
	
	//toggleButtonSampling enable 状态设置
	void setToggleBtnState(){
		//判断条件： spinnerUser ==null？ spinnerActions ==nan？ cd isChecked && cdDt==0？
		boolean isChecked=_switchCd.isChecked();
		int cdDt=Integer.parseInt(_editTextCd.getText().toString());
		if(_spinnerUsers.getSelectedItem()==null ||
				_spinnerActions.getSelectedItem().toString().contains(nan) ||
				(isChecked && cdDt==0)
				){
			_toggleButtonSampling.setEnabled(false);
		}
		else
			_toggleButtonSampling.setEnabled(true);
				
	}//setToggleBtnState
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("onCreate~~~~~~~~~~~");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.driving_ui);
		_sm=(SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
		
		_dataFolder=Environment.getExternalStoragePublicDirectory(_dataFolderName);
		if(!_dataFolder.exists()){
			_dataFolder.mkdirs();
		}
		loadAudio();
		loadPrefs();
		initWidgets();
		respondEvents();
	}//onCreate

	@Override
	protected void onDestroy() {
		System.out.println("onDestroy================");
		
		super.onDestroy();
	}//onDestroy

}
