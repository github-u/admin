package com.platform.task;

import java.util.Date;
import java.util.Map;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;

@Component
public abstract class AbstractSecuritiesCodesIteratorWeeklyTask extends AbstractSecuritiesCodesIteratorTask{
	
	//private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Override
	public ResultSupport<String> process(String securitiesCode, SimpleTaskParam taskParam, Map<String, String> argMap) {
		
		ResultSupport<Date> weekTriggerDayRet = weekTriggerDay(argMap, securitiesCode);
		if(!weekTriggerDayRet.isSuccess()) {
			if(ResultCode.NOT_WEEK_TRIGGER_DAY.equals(weekTriggerDayRet.getErrCode())) {
				return new ResultSupport<String>().success(ResultCode.NOT_WEEK_TRIGGER_DAY);
			}else {
				return new ResultSupport<String>().fail(weekTriggerDayRet.getErrCode(), weekTriggerDayRet.getErrMsg());
			}
		}
		
		return process(securitiesCode, taskParam, argMap, weekTriggerDayRet.getModel());
		
	}
	
	public abstract ResultSupport<String> process(String securitiesCode, SimpleTaskParam taskParam, Map<String, String> argMap, Date date);
	
	public static void main(String[] args) {
		
	}
	
}
