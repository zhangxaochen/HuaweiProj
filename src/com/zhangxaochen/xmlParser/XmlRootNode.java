package com.zhangxaochen.xmlParser;

import com.example.mysensorlistener.MySensorListener;

public abstract class XmlRootNode {

	public abstract void addNode(MySensorListener.MySensorData sensorData);
	
	public void clear(){}
}//XmlRootNode
