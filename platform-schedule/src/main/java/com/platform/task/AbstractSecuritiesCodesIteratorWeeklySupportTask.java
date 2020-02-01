package com.platform.task;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.service.SecuritiesService;
import com.platform.utils.DateUtil;
import com.platform.utils.LangUtil;

@Component
public abstract class AbstractSecuritiesCodesIteratorWeeklySupportTask extends AbstractSecuritiesCodesIteratorTask{
	
	private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Resource
	private SecuritiesService securitiesService;
	
	@Override
	public ResultSupport<String> process(String securitiesCode, SimpleTaskParam taskParam, Map<String, String> argMap) {
		Date date = null;
		
		String dateString = LangUtil.safeString(argMap.get("date"));
		if(dateString != null) {
			try {
				date = DateUtil.getDate(dateString);
			}catch(Exception e) {
				logger.error("title=" + "AbstractSecuritiesCodesIteratorWeeklySupportTask"
						+ "$mode=" + "process"
						+ "$code=" + securitiesCode
						+ "$errCode=" + "DATE_PARSE_EXCEPTION",e);
			}
		}
		date = date != null ? date : new Date();
		
		if(!trigger(date)) {
			logger.error("title=" + "AbstractSecuritiesCodesIteratorWeeklySupportTask"
					+ "$mode=" + "process"
					+ "$code=" + securitiesCode
					+ "$errCode=" + "NOT_TRIGGER_DAY"
					+ "$date=" + date);
			return new ResultSupport<String>().success("NOT_TRIGGER_DAY"); 
		}
		
		return process(securitiesCode, taskParam, argMap, date);
		
	}
	
	public abstract ResultSupport<String> process(String securitiesCode, SimpleTaskParam taskParam, Map<String, String> argMap, Date date);

	protected boolean trigger(Date date) {
		return lastWeekTradeDay(date);
	}
	
	protected boolean lastWeekTradeDay(Date date) {
		List<Date> weekDays = DateUtil.getWeekDays();
		
		for(int i=weekDays.size()-1; i>=0 ; i--) {
			Date weekDay = weekDays.get(i);
			ResultSupport<Boolean> tradeDayRet = securitiesService.isTradeDay(weekDay);
			if(!tradeDayRet.isSuccess()) {
				throw new RuntimeException(tradeDayRet.getErrCode() + "$" + tradeDayRet.getErrMsg());
			}
			
			if(!tradeDayRet.getModel().booleanValue()) {
				weekDays.remove(i);
			}
		}
		
		if(weekDays.size() <= 0) {
			return false;
		}
		
		Date lastWeekTradeDay = weekDays.get(weekDays.size() - 1);
		return DateUtil.isSameDay(date, lastWeekTradeDay);
	}
	
	public static void main(String[] args) {
		
	}
	
}
