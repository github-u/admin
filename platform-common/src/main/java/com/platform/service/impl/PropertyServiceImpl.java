package com.platform.service.impl;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import com.platform.entity.ResultSupport;
import com.platform.service.PropertyService;
import com.platform.utils.LangUtil;

public class PropertyServiceImpl implements PropertyService {
	
	private Properties properties;

	private String fileName = "D:\\working_config\\Properties.prop";
	
	public void init() throws Exception {
		FileInputStream fileInputStream = new FileInputStream(fileName);
		InputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
		
		properties = new Properties();
		properties.load(bufferedInputStream);
	}
	
	@Override
	public ResultSupport<String> get(String key) {
		
		ResultSupport<String> ret = new ResultSupport<String>();
		
		return ret.success(LangUtil.safeString(properties.get(key)));
		
	}
	
	public static void main(String[] args) throws Exception {
		
		PropertyServiceImpl p = new PropertyServiceImpl();
		
		p.init();
		
		p.get("a");
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
