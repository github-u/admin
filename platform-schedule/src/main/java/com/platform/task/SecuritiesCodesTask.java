package com.platform.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.jobx.service.SimpleTask;

@Component
public class SecuritiesCodesTask implements SimpleTask{
	
	private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Override
	public ResultSupport<String> process(SimpleTaskParam param) {
		logger.error("com.platform.task.SecuritiesCodesTask start");
		return new ResultSupport<String>().success("SecuritiesCodesTask R");
	}
	
}
