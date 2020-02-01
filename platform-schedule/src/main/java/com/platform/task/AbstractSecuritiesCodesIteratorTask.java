package com.platform.task;

import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.service.SecuritiesService;
import com.platform.utils.LangUtil;

public abstract class AbstractSecuritiesCodesIteratorTask extends AbstractSecuritiesTask{
	
	private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Resource
	private SecuritiesService securitiesService;
	
	private RateLimiter rateLimiter = RateLimiter.create(Double.MAX_VALUE);
			
	@Override
	public ResultSupport<String> process(SimpleTaskParam taskParam, Map<String, String> argMap) {
		
		ResultSupport<String> ret = new ResultSupport<String>();
		
		String paramSecuritiesCodes = LangUtil.safeString(argMap.get("securitiesCodes"));
		String queryPerSeconds = LangUtil.safeString(argMap.get("QPS"));
		try {
			rateLimiter.setRate(Double.parseDouble(queryPerSeconds));
		}catch(Exception e) {
			logger.error("title=" + "AbstractSecuritiesCodesIteratorTask"
					+ "$mode=" + "process"
					+ "$errCode=" + "RATE_LIMITER_SET_EXCEPTION", e);
			rateLimiter.setRate(Double.MAX_VALUE);
		}
		
		ResultSupport<List<String>> securitiesCodesRet = null; 
		if(paramSecuritiesCodes != null) {
			List<String> securitiesCodes = Lists.newArrayList(paramSecuritiesCodes.split("^"));
			securitiesCodesRet = new ResultSupport<List<String>>().success(securitiesCodes);
		}else {
			securitiesCodesRet = securitiesService.getSecuritiesCodes();
		}
		
		if(!securitiesCodesRet.isSuccess()) {
			return ret.fail(securitiesCodesRet.getErrCode(), securitiesCodesRet.getErrMsg());
		}
		
		Boolean interrupt = LangUtil.safeBoolean(argMap.get("interrupt"));
		for(String securitiesCode : securitiesCodesRet.getModel()) {
			rateLimiter.acquire();
			ResultSupport<String> processRet = process(securitiesCode, taskParam, argMap);
			if(!processRet.isSuccess() && interrupt) {
				return ret.fail(processRet.getErrCode(), processRet.getErrMsg());
			}
		}
		
		return ret.success(String.valueOf(securitiesCodesRet.getModel().size()));
		
	}
	
	abstract protected ResultSupport<String> process(String securitiesCode, SimpleTaskParam taskParam, Map<String, String> argMap);
	
	public static void main(String[] args) {
		
	}

}
