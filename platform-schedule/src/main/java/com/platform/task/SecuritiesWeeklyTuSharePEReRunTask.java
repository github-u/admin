package com.platform.task;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.service.SecuritiesService;
import com.platform.service.impl.SourceService.Source;
import com.platform.utils.DateUtil;
import com.platform.utils.LangUtil;

@Component
public class SecuritiesWeeklyTuSharePEReRunTask extends AbstractSecuritiesBatchWeeklyTask{
	
	private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Resource
	private SecuritiesService securitiesService;
	
	public ResultSupport<String> process(SimpleTaskParam taskParam, Map<String, String> argMap) {
		
		String start = LangUtil.safeString(argMap.get("start"));
		String end = LangUtil.safeString(argMap.get("end"));
		
		Date s = DateUtil.getDate(start);
		Date e = DateUtil.getDate(end);
		
		Preconditions.checkArgument(s.before(e));
		
		LocalDateTime localDateTimeStart = LocalDateTime.ofInstant(s.toInstant(), ZoneId.systemDefault());
		LocalDateTime localDateTimeEnd = LocalDateTime.ofInstant(e.toInstant(), ZoneId.systemDefault());
		
		LocalDateTime index;
		StringBuffer notTriggerDay = new StringBuffer();
		StringBuffer triggerDay = new StringBuffer();
		for(index = localDateTimeStart; index.isBefore(localDateTimeEnd); index = index.plusDays(1)) {
			ResultSupport<Date> weekTriggerDayRet = weekTriggerDay(argMap, "batch" + this.getClass().getSimpleName());
			if(!weekTriggerDayRet.isSuccess()) {
				if(ResultCode.NOT_WEEK_TRIGGER_DAY.equals(weekTriggerDayRet.getErrCode())) {
					logger.error("title=" + "SecuritiesWeeklyTuSharePEReRunTask"
							+ "$mode=" + "process"
							+ "$errCode=" + ResultCode.NOT_WEEK_TRIGGER_DAY
							+ "$errMsg=" + DateUtil.getDate(Date.from(index.atZone(ZoneId.systemDefault()).toInstant())));
					notTriggerDay.append(DateUtil.getDate(Date.from(index.atZone(ZoneId.systemDefault()).toInstant()))).append(",");
					continue;
				}else {
					return new ResultSupport<String>().fail(weekTriggerDayRet.getErrCode(), weekTriggerDayRet.getErrMsg());
				}
			}else {
				//ResultSupport<String> processRet = process(taskParam, argMap, weekTriggerDayRet.getModel());
				triggerDay
				.append(DateUtil.getDate(Date.from(index.atZone(ZoneId.systemDefault()).toInstant())))
				//.append("^").append(processRet.getModel())
				.append(",");
			}
			
		}
		
		logger.error("notTriggerDay=" + notTriggerDay);;
		logger.error("triggerDay=" + triggerDay);
		
		return new ResultSupport<String>().success(triggerDay.toString());
	}

	@Override
	public ResultSupport<String> process(SimpleTaskParam taskParam, Map<String, String> argMap, Date date) {
		
		Boolean parallel = LangUtil.convert(argMap.get("parallel"), Boolean.class);
		parallel = parallel != null ? parallel : false;
		
		Map<String, Object> conditions = Maps.newHashMap();
		conditions.put("trade_date", DateUtil.getDate(date, DateUtil.DAY_FORMATTER_2));
		
		ResultSupport<Long> getRet = securitiesService.getBatch(
				Source.TU_SHARE, 
				"daily_basic", 
				"ts_code,trade_date,close,turnover_rate,turnover_rate_f,volume_ratio,pe,pe_ttm,pb,ps,ps_ttm,total_share,float_share,free_share,total_mv,circ_mv", 
				"code,year,week", 
				conditions,
				new Function<Map<String, Object>, Map<String, Object>>() {

					@Override
					public Map<String, Object> apply(Map<String, Object> paramT) {
						
						String tsCode = LangUtil.convert(paramT.get("ts_code"), String.class);
						String code = tsCode.split("\\.")[0];
						
						long year = DateUtil.getYear(date);
						long week = DateUtil.getWeekOfYear(date);
						
						String tradeDate = DateUtil.getDate(date, DateUtil.DAY_FORMATTER_1);
						
						paramT.put("code", code);
						paramT.put("year", year);
						paramT.put("week", week);
						paramT.put("trade_date", tradeDate);
						paramT.put("ts_pe_ttm", paramT.get("pe_ttm"));
						
						return paramT;
					}
					
				},
				new Function<String, String>() {
					@Override
					public String apply(String t) {
						return "securities_weekly";
					}
				}
				,parallel
				);
		
		if(!getRet.isSuccess()) {
			return new ResultSupport<String>().fail(getRet.getErrCode(), getRet.getErrMsg());
		}else {
			return new ResultSupport<String>().success(LangUtil.convert(getRet.getModel(), String.class));
		}
		
	}
	
	public static void main(String[] args) {

	}

}
