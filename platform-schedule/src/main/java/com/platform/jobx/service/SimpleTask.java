package com.platform.jobx.service;

import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;

public interface SimpleTask {
	
	ResultSupport<String> process(SimpleTaskParam param);
	
}
