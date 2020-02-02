package com.platform.task;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.jobx.service.SimpleTask;
import com.platform.utils.LangUtil;

public abstract class AbstractSecuritiesTask extends SecuritiesTaskSupport implements SimpleTask{

	private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Override
	public ResultSupport<String> process(SimpleTaskParam param) {
		
		Map<String, String> argMap = parseArgs(param.getPlainArgs());
		
		logger.error("title=" + "AbstractSecuritiesTask"
				+ "$mode=" + "process"
				+ "$action=" + "start"
				+ "$name=" + this.getClass().getName());
		
		long start = System.currentTimeMillis();
		ResultSupport<String> processRet;
		try {
			processRet = process(param, Collections.unmodifiableMap(argMap));
			long end = System.currentTimeMillis();
			if(!processRet.isSuccess()) {
				logger.error("title=" + "AbstractSecuritiesTask"
						+ "$mode=" + "process"
						+ "$action=" + "end"
						+ "$name=" + this.getClass().getName()
						+ "$ret=" + "fail"
						+ "$cost=" + (end - start));
				return new ResultSupport<String>().fail(processRet.getErrCode(), processRet.getErrMsg());
			}else {
				logger.error("title=" + "AbstractSecuritiesTask"
						+ "$mode=" + "process"
						+ "$action=" + "end"
						+ "$name=" + this.getClass().getName()
						+ "$ret=" + "success"
						+ "$cost=" + (end - start)
						+ "$msg=" + processRet.getModel()
						);
				return new ResultSupport<String>().success(LangUtil.convert(processRet.getModel(), String.class));
			}
		}catch(Exception e) {
			long end = System.currentTimeMillis();
			logger.error("title=" + "AbstractSecuritiesTask"
					+ "$mode=" + "process"
					+ "$action=" + "end"
					+ "$name=" + this.getClass().getName()
					+ "$ret=" + "exception"
					+ "$cost=" + (end - start),
					e);
			return new ResultSupport<String>().fail(ResultCode.PROCESS_EXCEPTION, e.getMessage());
		}
	}
	
	abstract protected ResultSupport<String> process(SimpleTaskParam param, Map<String, String> argMap);
	
	
	
	public static void main(String[] args) {}
}
