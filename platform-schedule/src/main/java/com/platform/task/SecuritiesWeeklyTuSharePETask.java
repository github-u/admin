package com.platform.task;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.service.SecuritiesService;
import com.platform.service.impl.SourceService.Source;
import com.platform.utils.DateUtil;
import com.platform.utils.LangUtil;
import com.platform.utils.SecuritiesUtils;

@Component
public class SecuritiesWeeklyTuSharePETask extends AbstractSecuritiesCodesIteratorWeeklyTask{
	
	//private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Resource
	private SecuritiesService securitiesService;
	
	@Override
	public ResultSupport<String> process(String securitiesCode, SimpleTaskParam taskParam, Map<String, String> argMap, Date date) {
		
		Boolean parallel = LangUtil.convert(argMap.get("parallel"), Boolean.class);
		parallel = parallel != null ? parallel : false;
		
		Map<String, Object> conditions = Maps.newHashMap();
		conditions.put("ts_code", SecuritiesUtils.getTuShareSecuritiesCode(securitiesCode));
		conditions.put("trade_date", DateUtil.getDate(date, DateUtil.DAY_FORMATTER_2));
		
		ResultSupport<Long> getRet = securitiesService.get(
				Source.TU_SHARE, 
				"daily_basic", 
				securitiesCode,
				"ts_code,trade_date,close,turnover_rate,turnover_rate_f,volume_ratio,pe,pe_ttm,pb,ps,ps_ttm,total_share,float_share,free_share,total_mv,circ_mv", 
				"code,year,week", 
				conditions,
				new Function<Map<String, Object>, List<Map<String, Object>>>() {

					@Override
					public List<Map<String, Object>> apply(Map<String, Object> paramT) {
						
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
						
						return Lists.newArrayList(paramT);
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

}
