package com.platform.task;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
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
public class SecuritiesDailyShareHoldersTask extends AbstractSecuritiesCodesIteratorTask{
	
	//private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Resource
	private SecuritiesService securitiesService;
	
	@Override
	public ResultSupport<String> process(String securitiesCode, SimpleTaskParam taskParam, Map<String, String> argMap) {
		
		Boolean parallel = LangUtil.convert(argMap.get("parallel"), Boolean.class);
		parallel = parallel != null ? parallel : false;
		
		Date today = new Date(); 
		
		LocalDateTime from = LocalDateTime.ofInstant(today.toInstant(), ZoneId.systemDefault()).minusMonths(3);
		LocalDateTime to = LocalDateTime.ofInstant(today.toInstant(), ZoneId.systemDefault());
		
		Map<String, Object> conditions = Maps.newHashMap();
		conditions.put("ts_code", SecuritiesUtils.getTuShareSecuritiesCode(securitiesCode));
		conditions.put("start_date", DateUtil.getDate(Date.from(from.atZone(ZoneId.systemDefault()).toInstant()), DateUtil.DAY_FORMATTER_2));
		conditions.put("end_date", DateUtil.getDate(Date.from(to.atZone(ZoneId.systemDefault()).toInstant()), DateUtil.DAY_FORMATTER_2));
		
		ResultSupport<Long> getRet = securitiesService.get(
				Source.TU_SHARE, 
				"top10_floatholders", 
				securitiesCode,
				"ts_code,ann_date,end_date,holder_name,hold_amount", 
				"code,year,quarter", 
				conditions,
				null,
				new Function<Map<String, Object>, List<Map<String, Object>>>() {

					@Override
					public List<Map<String, Object>> apply(Map<String, Object> paramT) {
						
						String tsCode = LangUtil.convert(paramT.get("ts_code"), String.class);
						String code = tsCode.split("\\.")[0];
						
						String sEndDate = LangUtil.convert(paramT.get("end_date"), String.class);
						Date endDate = DateUtil.getDate(sEndDate, DateUtil.DAY_FORMATTER_2);
						long year = DateUtil.getYear(endDate);
						long quater = DateUtil.getQuarterOfYear(endDate);
						
						paramT.put("code", code);
						paramT.put("year", year);
						paramT.put("quarter", quater);

						//System.out.println(JSON.toJSONString(paramT));
						return Lists.newArrayList(paramT);
					}
					
				},
				new Function<String, String>() {
					@Override
					public String apply(String t) {
						return "securities_quarterly_holders";
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
