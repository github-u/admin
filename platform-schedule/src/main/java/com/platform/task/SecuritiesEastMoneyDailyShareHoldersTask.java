package com.platform.task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.service.DataService;
import com.platform.service.SecuritiesService;
import com.platform.service.impl.SourceService.Source;
import com.platform.utils.DateUtil;
import com.platform.utils.LangUtil;
import com.platform.utils.SecuritiesUtils;

import lombok.Setter;

@Component
public class SecuritiesEastMoneyDailyShareHoldersTask extends AbstractSecuritiesCodesIteratorTask{
	
	//private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Resource
	private SecuritiesService securitiesService;
	
	@Resource @Setter
	private DataService dataService; 
	
	@Override
	public ResultSupport<String> process(String securitiesCode, SimpleTaskParam taskParam, Map<String, String> argMap) {
		
		Boolean parallel = LangUtil.convert(argMap.get("parallel"), Boolean.class);
		parallel = parallel != null ? parallel : false;
		
		Map<String, Object> conditions = Maps.newHashMap();
		conditions.put("code", 
				SecuritiesUtils.getSecuritiesType(securitiesCode) == 0 ? "SZ" + securitiesCode :
				SecuritiesUtils.getSecuritiesType(securitiesCode) == 1 ? "SH" + securitiesCode :
				"");
		
		ResultSupport<Long> getRet = securitiesService.get(
				Source.EAST_MONEY, 
				"", 
				securitiesCode,
				"", 
				"code,year,quarter,holder_name", 
				conditions,
				new Function<String, String>(){

					@Override
					public String apply(String t) {
						return "http://f10.eastmoney.com/ShareholderResearch/ShareholderResearchAjax";
					}
					
				},
				new Function<Map<String, Object>, List<Map<String, Object>>>() {

					@Override
					public List<Map<String, Object>> apply(Map<String, Object> paramT) {
						
						List<Map<String, Object>> quarterlyShareHolders = (List<Map<String, Object>>) paramT.get("sdltgd");
						
						return quarterlyShareHolders.stream()
						.flatMap(oneQuarter ->{
							List<Map<String, Object>> oneQuarterShareHolder = (List<Map<String, Object>>) oneQuarter.get("sdltgd");
							
							return oneQuarterShareHolder.stream()
									.map(oneShareHolder ->{
										Map<String, Object> oneShareHolderTuple = Maps.newHashMap();

										Date quarterEndDay = DateUtil.getDate(LangUtil.safeString(oneQuarter.get("rq")), DateUtil.DAY_FORMATTER_1);
										long year = DateUtil.getYear(quarterEndDay);
										long quarter = DateUtil.getQuarterOfYear(quarterEndDay);
										String code = securitiesCode;
										String holder_name = LangUtil.safeString(oneShareHolder.get("gdmc"));
										BigDecimal holder_amount = new BigDecimal(LangUtil.safeString(oneShareHolder.get("cgs")).replace(",", ""));

										oneShareHolderTuple.put("code", code);
										oneShareHolderTuple.put("year", year);
										oneShareHolderTuple.put("quarter", quarter);
										oneShareHolderTuple.put("ann_date", quarterEndDay);
										oneShareHolderTuple.put("end_date", quarterEndDay);
										oneShareHolderTuple.put("holder_name", holder_name);
										oneShareHolderTuple.put("hold_amount", holder_amount);

										Map<String, Object> selectParams = Maps.newHashMap();
										selectParams.put("code", code);
										selectParams.put("year", year);
										selectParams.put("quarter", quarter);
										selectParams.put("holder_name", holder_name);

										ResultSupport<List<Map<String, Object>>> selectRet = dataService.select("securities_quarterly_holders", selectParams);
										if(selectRet.isSuccess() && selectRet.getModel().size() > 0) {
											return null;
										}

										return oneShareHolderTuple;
									})
									.filter(oneShareHolderTuple -> oneShareHolderTuple != null);

						})
						.collect(Collectors.toList());

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