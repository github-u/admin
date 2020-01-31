package com.platform.task;

import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.service.SecuritiesService;
import com.platform.utils.LangUtil;

public abstract class AbstractSecuritiesCodesIteratorTask extends AbstractSecuritiesTask{
	
	//private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Resource
	private SecuritiesService securitiesService;
	
	@Override
	public ResultSupport<String> process(SimpleTaskParam taskParam, Map<String, String> argMap) {
		
		ResultSupport<String> ret = new ResultSupport<String>();
		
		ResultSupport<List<String>> securitiesCodesRet = securitiesService.getSecuritiesCodes();
		if(!securitiesCodesRet.isSuccess()) {
			return ret.fail(securitiesCodesRet.getErrCode(), securitiesCodesRet.getErrMsg());
		}
		
		Boolean interrupt = LangUtil.safeBoolean(argMap.get("interrupt"));
		for(String securitiesCode : securitiesCodesRet.getModel()) {
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
