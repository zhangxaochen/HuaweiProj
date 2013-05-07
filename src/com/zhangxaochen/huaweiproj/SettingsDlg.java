package com.zhangxaochen.huaweiproj;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SettingsDlg extends Dialog {
	Context _ctx;

	public SettingsDlg(Context context) {
		super(context);
		_ctx = context;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_dlg);
		final TextView sbTextValue = (TextView) findViewById(R.id.textViewSeekBarValue);
		SeekBar sb = (SeekBar) findViewById(R.id.seekBarPreCd);

		final SharedPreferences sp=_ctx.getSharedPreferences(Consts.prefSettings, Context.MODE_PRIVATE);
		int preCd=sp.getInt(Consts.kPreCd, 0);
		sb.setProgress(preCd);
		sbTextValue.setText(""+preCd);

		sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			public void onStopTrackingTouch(SeekBar seekBar) {
				System.out.println("onStopTrackingTouch");
				int preCd = seekBar.getProgress();
				System.out.println("preCd: " + preCd);
//				SharedPreferences sp = _ctx.getSharedPreferences(prefSettings,
//						Context.MODE_PRIVATE);
				Editor editor = sp.edit();
				editor.putInt(Consts.kPreCd, preCd);
				editor.commit();
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				sbTextValue.setText("" + progress);
			}
		});
	}

}