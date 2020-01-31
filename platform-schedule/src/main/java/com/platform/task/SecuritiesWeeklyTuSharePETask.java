package com.platform.task;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.jobx.domain.SimpleTaskParam;
import com.platform.service.SecuritiesService;
import com.platform.service.impl.SourceService.Source;
import com.platform.utils.LangUtil;
import com.platform.utils.SecuritiesUtils;

@Component
public class SecuritiesWeeklyTuSharePETask extends AbstractSecuritiesCodesIteratorTask{
	
	//private static Logger logger = LoggerFactory.getLogger(SecuritiesCodesTask.class);
	
	@Resource
	private SecuritiesService securitiesService;
	
	@Override
	public ResultSupport<String> process(String securitiesCode, SimpleTaskParam taskParam, Map<String, String> argMap) {
		
		Map<String, Object> conditions = Maps.newHashMap();
		conditions.put("ts_code", SecuritiesUtils.getTuShareSecuritiesCode(securitiesCode));
		conditions.put("trade_date", "20190930");
		
		ResultSupport<Long> getRet = securitiesService.get(
				Source.TU_SHARE, 
				"daily_basic", 
				securitiesCode,
				"ts_code,trade_date,close,turnover_rate,turnover_rate_f,volume_ratio,pe,pe_ttm,pb,ps,ps_ttm,total_share,float_share,free_share,total_mv,circ_mv", 
				"code,year,week", 
				conditions
				);
		
		if(!getRet.isSuccess()) {
			return new ResultSupport<String>().fail(getRet.getErrCode(), getRet.getErrMsg());
		}else {
			return new ResultSupport<String>().success(LangUtil.convert(getRet.getModel(), String.class));
		}
		
	}
	
}
