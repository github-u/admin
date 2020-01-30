package com.platform.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.JobHandler;

@Component
@JobHandler("SecuritiesCodesTask")



public class SecuritiesCodesTask extends IJobHandler{
	
	private static Logger log = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Override
	public ReturnT<String> execute(String param) throws Exception {
		log.error("com.platform.task.SecuritiesCodesTask start");
		return new ReturnT<String>("");
	}

}
