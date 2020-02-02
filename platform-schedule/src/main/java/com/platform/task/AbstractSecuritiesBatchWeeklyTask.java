package com.platform.task;

import java.util.Date;
import java.util.Map;

import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;

public abstract class AbstractSecuritiesBatchWeeklyTask extends AbstractSecuritiesTask{
	
	@Override
	public ResultSupport<String> process(SimpleTaskParam taskParam, Map<String, String> argMap) {
		
		ResultSupport<Date> weekTriggerDayRet = weekTriggerDay(argMap, "batch" + this.getClass().getSimpleName());
		if(!weekTriggerDayRet.isSuccess()) {
			if(ResultCode.NOT_WEEK_TRIGGER_DAY.equals(weekTriggerDayRet.getErrCode())) {
				return new ResultSupport<String>().success(ResultCode.NOT_WEEK_TRIGGER_DAY);
			}else {
				return new ResultSupport<String>().fail(weekTriggerDayRet.getErrCode(), weekTriggerDayRet.getErrMsg());
			}
		}
		
		return process(taskParam, argMap, weekTriggerDayRet.getModel());
	}
	
	public abstract ResultSupport<String> process(SimpleTaskParam taskParam, Map<String, String> argMap, Date date);
	
}
