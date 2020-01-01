package com.platform.service;

import com.platform.entity.ResultSupport;

public interface PropertyService {

	ResultSupport<String> get(String key);

	public class ResultCode{
		
	}

	public class PropertyResultCode{

		public static final long SUC = 0;

	}
}
