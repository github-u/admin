package com.platform.task;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.service.SecuritiesService;
import com.platform.utils.DateUtil;
import com.platform.utils.LangUtil;
import com.platform.utils.Pair;

public class SecuritiesTaskSupport{

	private static Logger logger = LoggerFactory.getLogger(SecuritiesTaskSupport.class);
	
	@Resource
	private SecuritiesService securitiesService;
	
	public static final class ResultCode{
		
		public static final String PROCESS_EXCEPTION = "PROCESS_EXCEPTION";
		
		public static final String NOT_WEEK_TRIGGER_DAY = "NOT_WEEK_TRIGGER_DAY";
		
	}
	
	protected static Map<String, String> parseArgs(String args){
		Map<String, String> ret = Maps.newHashMap();
		if(args == null || args.trim().length() == 0) {
			return ret;
		}
		
		String[] argsToken = StringUtils.tokenizeToStringArray(args, ",;\t\n");
		Map<String, String> argMap = Lists.newArrayList(argsToken).stream()
				.map(token -> {
					if(token == null || token.trim().length() == 0) {
						return null;
					}
					String[] kv = token.split("=");
					return Pair.of(kv[0].trim(), kv.length > 1 ? kv[1].trim() : null);
				})
				.filter(pair -> pair != null)
				.collect(Collector.of(
						()->{
							Map<String, String> s = new LinkedHashMap<String, String>();
							return s;
						},
						(s, e)->{
							s.put(e.fst, e.snd);
						}, 
						(s1, s2)->{
							s1.putAll(s2);
							return s1;
						},
						Collector.Characteristics.IDENTITY_FINISH
						));
		
		ret.putAll(argMap);
		return ret;
	}
	
	public ResultSupport<Date> weekTriggerDay(Map<String, String> argMap, String logCode){
		
		Preconditions.checkNotNull(argMap);
		
		ResultSupport<Date> ret = new ResultSupport<Date>();
		
		Date date = null;
		String dateString = LangUtil.safeString(argMap.get("date"));
		if(dateString != null) {
			try {
				date = DateUtil.getDate(dateString);
			}catch(Exception e) {
				logger.error("title=" + "AbstractSecuritiesCodesIteratorWeeklySupportTask"
						+ "$mode=" + "process"
						+ "$code=" + logCode
						+ "$errCode=" + "DATE_PARSE_EXCEPTION",e);
			}
		}
		date = date != null ? date : new Date();

		if(!weekTrigger(date)) {
			logger.error("title=" + "AbstractSecuritiesCodesIteratorWeeklySupportTask"
					+ "$mode=" + "process"
					+ "$code=" + logCode
					+ "$errCode=" + ResultCode.NOT_WEEK_TRIGGER_DAY
					+ "$date=" + date);
			return new ResultSupport<Date>().fail(ResultCode.NOT_WEEK_TRIGGER_DAY, DateUtil.getDate(date)); 
		}
		
		return ret.success(date);
	}
	
	protected boolean weekTrigger(Date date) {
		return lastWeekTradeDay(date);
	}
	
	protected boolean lastWeekTradeDay(Date date) {
		List<Date> weekDays = DateUtil.getWeekDays(date);
		
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
	
	public static void main(String[] args) {}
}
