package com.zhangxaochen.huaweiproj;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class MyLinearLayout extends LinearLayout {

	private ArrayList<Boolean> childrenEnabled=new ArrayList<Boolean>();

	public MyLinearLayout(Context context) {
		super(context);
	}

	public MyLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MyLinearLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	void disablePanel() {
		if(!this.isEnabled())
			return;
		this.setEnabled(false); //这句不是真的 disable 了面板， 只是个 flag 作用
		for (int i = 0; i < this.getChildCount(); i++) {
			View view = this.getChildAt(i);
			childrenEnabled.add(view.isEnabled());
			view.setEnabled(false);
		}
	}

	void enablePanel() {
		if(this.isEnabled())
			return;
		this.setEnabled(true);
		
		for (int i = 0; i < this.getChildCount(); i++) {
			View view = this.getChildAt(i);

			view.setEnabled(childrenEnabled.get(i));
		}
		childrenEnabled.clear();
	}

}
