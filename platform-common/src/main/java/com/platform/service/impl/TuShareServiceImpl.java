package com.platform.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Resource;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import com.platform.entity.ResultSupport;
import com.platform.entity.properties.PropertieKeys;
import com.platform.entity.tushare.TuShareData;
import com.platform.entity.tushare.TuShareParam;
import com.platform.entity.tushare.TuShareResult;
import com.platform.service.DataService;
import com.platform.service.PropertyService;
import com.platform.service.SQLService.VelocityContextKey;
import com.platform.service.TuShareService;
import com.platform.utils.BeanUtil;
import com.platform.utils.IOUtils;
import com.platform.utils.LangUtil;
import com.platform.utils.Pair;

import lombok.Setter;

public class TuShareServiceImpl implements TuShareService, SourceService {
    
    public static final String TUSHARE_URL = "http://api.tushare.pro";
    
    public static String TUSHARE_TOKEN = "";
    
    private static Logger logger = LoggerFactory.getLogger(TuShareServiceImpl.class);
    
    /**
    private static ThreadLocal<HttpClient> httpClients = new ThreadLocal<HttpClient>(){
        protected HttpClient initialValue() {
            return HttpClientBuilder.create().build();
        }
    };
    */
    
    @Resource @Setter
    private PropertyService propertyService; 
    
    public void init() throws Exception {
    	propertyService = new PropertyServiceImpl();
        ((PropertyServiceImpl)propertyService).init();
        
    	if("".equals(TUSHARE_TOKEN)) {
    		TUSHARE_TOKEN = propertyService.get(PropertieKeys.TuShare.TOKEN).getModel();
    	}
    }
    
	@Override
	public ResultSupport<List<Map<String, Object>>> source(String sourceName, String securitiesCode, String columnNames,
			Map<String, Object> conditions) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSupport<List<Map<String, Object>>> source(String sourceName, String columnNames,
			Map<String, Object> conditions) {
		
		ResultSupport<List<Map<String, Object>>> ret = new ResultSupport<List<Map<String, Object>>>();
		
        Map<String, String> param = 
        		conditions.entrySet().stream()
        		.map(kv -> {
        			return Pair.of(kv.getKey(), LangUtil.safeString(kv.getValue()));
        		})
        		.collect(Collectors.toMap(pair->pair.fst, pair->pair.snd));
        
        TuShareParam tuShareParam = new TuShareParam(
        		sourceName, 
                TUSHARE_TOKEN, 
                param, 
                columnNames);
        
        ResultSupport<TuShareData> getDataRet = getData(tuShareParam);
        if(!getDataRet.isSuccess()) {
        	return ret.fail(getDataRet.getErrCode(), getDataRet.getErrMsg());
        }
        
        TuShareData tuShareData = getDataRet.getModel();
        
        List<Map<String, Object>> model = 
        		IntStream.range(0, tuShareData.getItems().size())
        		.mapToObj(index -> {
        			return tuShareData.getItem(index);
        		})
        		.collect(Collectors.toList());
        
		return ret.success(model);
	}
	
    @Override
    public ResultSupport<TuShareData> getData(TuShareParam tuShareParam){
        
        ResultSupport<TuShareData> ret = new ResultSupport<TuShareData> ();
        
        HttpPost httpPost = new HttpPost(TUSHARE_URL);
        httpPost.setEntity(new StringEntity(JSON.toJSONString(tuShareParam), ContentType.APPLICATION_JSON));
        
        HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpResponse response = httpClient.execute(httpPost);
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error("title=" + "TuShareService"
                        + "$mode=" + "getData"
                        + "$errCode=" + ResultCode.HTTP_STATUS_ILLEGAL
                        + "$errMsg=" + JSON.toJSONString(response.getStatusLine()));
                return ret.fail(ResultCode.HTTP_STATUS_ILLEGAL, JSON.toJSONString(response.getStatusLine()));
            }
            
            String httpEntity = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            
            JSONObject jsonTuShareResult = JSON.parseObject(httpEntity);
            JSONObject jsonTuShareData = jsonTuShareResult.getJSONObject("data");
            
            TuShareResult tuShareResult = BeanUtil.copyPropertiesFrom(jsonTuShareResult, TuShareResult.class);
            if(tuShareResult.getCode() != TuShareResultCode.SUC) {
                logger.error("title=" + "TuShareService"
                        + "$mode=" + "getData"
                        + "$errCode=" + ResultCode.TUSHARE_SERVER_ERROR
                        + "$errMsg=" + JSON.toJSONString(tuShareResult));
                return ret.fail(ResultCode.TUSHARE_SERVER_ERROR, JSON.toJSONString(tuShareResult));
            }
            TuShareData tuShareData = BeanUtil.copyPropertiesFrom(jsonTuShareData, TuShareData.class);
            
            return ret.success(tuShareData);
            
        } catch (Exception e) {
            logger.error("title=" + "TuShareService"
                        + "$mode=" + "getData"
                        + "$errCode=" + ResultCode.HTTP_CLIENT_EXECUTE_EXCEPTION
                        + "$errMsg=", e);
            return ret.fail(ResultCode.HTTP_CLIENT_EXECUTE_EXCEPTION, e.getMessage());
        } finally {
            HttpClientUtils.closeQuietly(httpClient);
        }
        
    }
    
    public static void main(String[] args) throws Exception {
        
        //sample();
        
    	//stockBasic();
        
    	//dailyBasic();
        
        //income();
        
        //income();
        
        //shareFloat();
        
    	//forecast("000877.SZ");
    	
    	//monthly();
    	
    	weekly();
    	
    	//topInst();
    	
    	//top10FloatHolders();
        
        //System.out.println("");
        
    }
    
    public static void sample() {
        
        TuShareService tuShareService = new TuShareServiceImpl();
        
        Map<String, String> param = Maps.newHashMap();
        param.put("exchange", "");
        param.put("start_date", "20180901");
        param.put("end_date", "20181001");
        param.put("is_open", "0");
        TuShareParam tuShareParam = new TuShareParam("trade_cal", TUSHARE_TOKEN, param, "exchange,cal_date,is_open,pretrade_date");
        
        ResultSupport<TuShareData> r = tuShareService.getData(tuShareParam);
        
        System.out.println(r);
        
    }
    
    public static void stockBasic() throws Exception {
        
        TuShareService tuShareService = new TuShareServiceImpl();
        
        Map<String, String> param = Maps.newHashMap();
        //param.put("is_hs", "");//N H S
        //param.put("list_status", "20180901");//L D P
        //param.put("exchange", "20181001");//SSE SZSE HKEX
        TuShareParam tuShareParam = new TuShareParam(
                "stock_basic", 
                TUSHARE_TOKEN, 
                param, 
                "ts_code,symbol,name,area,industry,fullname,enname,market,exchange,curr_type,list_status,list_date,delist_date,is_hs");
        
        ResultSupport<TuShareData> result = tuShareService.getData(tuShareParam);
        
        Preconditions.checkArgument(result.isSuccess());
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        String tableName = "stock_basic";
        //for(int i = 0; i < 1; i++) {
        for(int i = 0; i < result.getModel().getItems().size(); i++) {
            
            Map<String, Object> insertParams = result.getModel().getItem(i);
            
            Map<String, Object> selectParams = new HashMap<String, Object>();
            selectParams.put("ts_code", insertParams.get("ts_code"));
            ResultSupport<List<Map<String, Object>>> selectRet = dataServiceImpl.select(tableName, selectParams);
            if(selectRet.isSuccess() && selectRet.getModel().size() > 0) {
                insertParams.put("id", selectRet.getModel().get(0).get("id"));
                ResultSupport<Long> updateRet = dataServiceImpl.update(tableName, insertParams);
                Preconditions.checkArgument(updateRet.isSuccess() && updateRet.getModel() > 0);
            }else {
                ResultSupport<Long> insertRet = dataServiceImpl.insert(tableName, insertParams);
                Preconditions.checkArgument(insertRet.isSuccess() && insertRet.getModel() > 0);
            }
        }
        
    }
    
    public static void dailyBasic() throws Exception {
        
        TuShareService tuShareService = new TuShareServiceImpl();
        
        Map<String, String> param = Maps.newHashMap();
        //param.put("ts_code", "");//N H S
        param.put("trade_date", "20190930");
        //param.put("start_date", "20181001");
        //param.put("end_date", "20181001");
        
        TuShareParam tuShareParam = new TuShareParam(
                "daily_basic", 
                TUSHARE_TOKEN, 
                param, 
                "ts_code,trade_date,close,turnover_rate,turnover_rate_f,volume_ratio,pe,pe_ttm,pb,ps,ps_ttm,total_share,float_share,free_share,total_mv,circ_mv");
        
        ResultSupport<TuShareData> result = tuShareService.getData(tuShareParam);
        
        Preconditions.checkArgument(result.isSuccess());
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        String tableName = "daily_basic";
        for(int i = 0; i < result.getModel().getItems().size(); i++) {
            
            Map<String, Object> insertParams = result.getModel().getItem(i);
            
            Map<String, Object> selectParams = new HashMap<String, Object>();
            selectParams.put("ts_code", insertParams.get("ts_code"));
            selectParams.put("trade_date", insertParams.get("trade_date"));
            ResultSupport<List<Map<String, Object>>> selectRet = dataServiceImpl.select(tableName, selectParams);
            if(selectRet.isSuccess() && selectRet.getModel().size() > 0) {
                insertParams.put("id", selectRet.getModel().get(0).get("id"));
                ResultSupport<Long> updateRet = dataServiceImpl.update(tableName, insertParams);
                Preconditions.checkArgument(updateRet.isSuccess() && updateRet.getModel() > 0);
            }else {
                ResultSupport<Long> insertRet = dataServiceImpl.insert(tableName, insertParams);
                Preconditions.checkArgument(insertRet.isSuccess() && insertRet.getModel() > 0);
            }
        }
        
    }
    
    public static void income() throws Exception {
        
        List<String> tsCodes = Lists.newArrayList();
        
        String tableName = "stock_basic";
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        Map<String, Object> selectParams = new HashMap<String, Object>();
        selectParams.put(VelocityContextKey.Limit, 4000);
        ResultSupport<List<Map<String, Object>>> selectRet = dataServiceImpl.select(tableName, selectParams);
        
        Preconditions.checkArgument(selectRet.isSuccess());
        
        tsCodes = selectRet.getModel().stream()
                .map(item->{
                    return LangUtil.convert(item.get("ts_code"), String.class);
                })
                .filter(tsCode -> tsCode != null)
                .collect(Collectors.toList());
        
        RateLimiter rateLimiter = RateLimiter.create(1.29);
        
        for(String tsCode : tsCodes) {
            System.out.println("income=" + tsCode);
            rateLimiter.acquire();
            income(tsCode, dataServiceImpl);
        }
        
    }
    
    public static void income(String tsCode) throws Exception {
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        income(tsCode, dataServiceImpl);
    }
    
    public static void income(String tsCode, DataService dataService) throws Exception {
        
        TuShareService tuShareService = new TuShareServiceImpl();
        
        Map<String, String> param = Maps.newHashMap();
        param.put("ts_code", tsCode);
        //param.put("ann_date", "20190930");
        //param.put("start_date", "20181001");
        //param.put("end_date", "20181001");
        param.put("period", "20180930");
        //param.put("report_type", "20181001");
        //param.put("comp_type", "20181001");
        
        TuShareParam tuShareParam = new TuShareParam(
                "income", 
                TUSHARE_TOKEN, 
                param, 
                "ts_code,ann_date,f_ann_date,end_date,report_type,comp_type,basic_eps,diluted_eps,total_revenue,revenue,"
                        + "int_income,prem_earned,comm_income,n_commis_income,n_oth_income,n_oth_b_income,prem_income,out_prem,"
                        + "une_prem_reser,reins_income,n_sec_tb_income,n_sec_uw_income,n_asset_mg_income,oth_b_income,fv_value_chg_gain,"
                        + "invest_income,ass_invest_income,forex_gain,total_cogs,oper_cost,int_exp,comm_exp,biz_tax_surchg,sell_exp,admin_exp,"
                        + "fin_exp,assets_impair_loss,prem_refund,compens_payout,reser_insur_liab,div_payt,reins_exp,oper_exp,compens_payout_refu,"
                        + "insur_reser_refu,reins_cost_refund,other_bus_cost,operate_profit,non_oper_income,non_oper_exp,nca_disploss,total_profit,"
                        + "income_tax,n_income,n_income_attr_p,minority_gain,oth_compr_income,t_compr_income,compr_inc_attr_p,compr_inc_attr_m_s,ebit,"
                        + "ebitda,insurance_exp,undist_profit,distable_profit,update_flag"
                );
        
        ResultSupport<TuShareData> result = tuShareService.getData(tuShareParam);
        
        Preconditions.checkArgument(result.isSuccess());
        
        String tableName = "income_20180930";
        for(int i = 0; i < result.getModel().getItems().size(); i++) {
            
            Map<String, Object> insertParams = result.getModel().getItem(i);
            
            Map<String, Object> selectParams = new HashMap<String, Object>();
            selectParams.put("ts_code", insertParams.get("ts_code"));
            ResultSupport<List<Map<String, Object>>> selectRet = dataService.select(tableName, selectParams);
            if(selectRet.isSuccess() && selectRet.getModel().size() > 0) {
                insertParams.put("id", selectRet.getModel().get(0).get("id"));
                ResultSupport<Long> updateRet = dataService.update(tableName, insertParams);
                Preconditions.checkArgument(updateRet.isSuccess() && updateRet.getModel() > 0);
            }else {
                ResultSupport<Long> insertRet = dataService.insert(tableName, insertParams);
                Preconditions.checkArgument(insertRet.isSuccess() && insertRet.getModel() > 0);
            }
        }
        
    }
    
    public static void shareFloat() throws Exception {
        
        List<String> tsCodes = Lists.newArrayList();
        
        String tableName = "stock_basic";
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        Map<String, Object> selectParams = new HashMap<String, Object>();
        selectParams.put(VelocityContextKey.Limit, 4000);
        ResultSupport<List<Map<String, Object>>> selectRet = dataServiceImpl.select(tableName, selectParams);
        
        Preconditions.checkArgument(selectRet.isSuccess());
        
        tsCodes = selectRet.getModel().stream()
                .map(item->{
                    return LangUtil.convert(item.get("ts_code"), String.class);
                })
                .filter(tsCode -> tsCode != null)
                .collect(Collectors.toList());
        
        RateLimiter rateLimiter = RateLimiter.create(0.4);
        
        for(String tsCode : tsCodes) {
            System.out.println("income=" + tsCode);
            //rateLimiter.acquire();
            try {
                shareFloat(tsCode, dataServiceImpl);
            }catch(Exception e) {
                System.out.println("income=" + tsCode + e.getMessage());
            }
        }
        
    }
    
    public static void shareFloat(String tsCode) throws Exception {
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        shareFloat(tsCode, dataServiceImpl);
    }
    
    public static void shareFloat(String tsCode, DataService dataService) throws Exception {
        
        TuShareService tuShareService = new TuShareServiceImpl();
        
        Map<String, String> param = Maps.newHashMap();
        param.put("ts_code", tsCode);
        //param.put("ann_date", "20190930");
        //param.put("float_date", "20181001");
        param.put("start_date", "20190930");
        param.put("end_date", "20200930");
        
        TuShareParam tuShareParam = new TuShareParam(
                "share_float", 
                TUSHARE_TOKEN, 
                param, 
                "ts_code,ann_date,float_date,float_share,float_ratio,holder_name,share_type");
        
        ResultSupport<TuShareData> result = tuShareService.getData(tuShareParam);
        
        Preconditions.checkArgument(result.isSuccess());
        
        /**
        for(int i = result.getModel().getItems().size() - 1; i >=0 ; i--) {
            Map<String, Object> itemElem  = result.getModel().getItem(i);
            if(!"首发原始股".equals(itemElem.get("share_type"))) {
                result.getModel().getItems().remove(i);
            }
        }
        */
        
        String tableName = "share_float";
        for(int i = 0; i < result.getModel().getItems().size(); i++) {
            
            Map<String, Object> insertParams = result.getModel().getItem(i);
            
            Map<String, Object> selectParams = new HashMap<String, Object>();
            selectParams.put("ts_code", insertParams.get("ts_code"));
            //selectParams.put("ann_date", insertParams.get("ann_date"));
            selectParams.put("float_date", insertParams.get("float_date"));
            //selectParams.put("float_share", insertParams.get("float_share"));
            //selectParams.put("float_ratio", insertParams.get("float_ratio"));
            selectParams.put("holder_name", insertParams.get("holder_name"));
            selectParams.put("share_type", insertParams.get("share_type"));
            
            ResultSupport<List<Map<String, Object>>> selectRet = dataService.select(tableName, selectParams);
            if(selectRet.isSuccess() && selectRet.getModel().size() > 0) {
                insertParams.put("id", selectRet.getModel().get(0).get("id"));
                ResultSupport<Long> updateRet = dataService.update(tableName, insertParams);
                Preconditions.checkArgument(updateRet.isSuccess() && updateRet.getModel() > 0);
            }else {
                ResultSupport<Long> insertRet = dataService.insert(tableName, insertParams);
                Preconditions.checkArgument(insertRet.isSuccess() && insertRet.getModel() > 0);
            }
        }
        
    }
    
    public static void forecast() throws Exception {
        
        List<String> tsCodes = Lists.newArrayList();
        
        String tableName = "stock_basic";
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        Map<String, Object> selectParams = new HashMap<String, Object>();
        selectParams.put(VelocityContextKey.Limit, 4000);
        ResultSupport<List<Map<String, Object>>> selectRet = dataServiceImpl.select(tableName, selectParams);
        
        Preconditions.checkArgument(selectRet.isSuccess());
        
        tsCodes = selectRet.getModel().stream()
                .map(item->{
                    return LangUtil.convert(item.get("ts_code"), String.class);
                })
                .filter(tsCode -> tsCode != null)
                .collect(Collectors.toList());
        
        RateLimiter rateLimiter = RateLimiter.create(0.4);
        
        for(String tsCode : tsCodes) {
            System.out.println("income=" + tsCode);
            rateLimiter.acquire();
            try {
                forecast(tsCode, dataServiceImpl);
            }catch(Exception e) {
                System.out.println("income=" + tsCode + e.getMessage());
            }
        }
        
    }
    
    public static void monthly() throws Exception {

        TuShareService tuShareService = new TuShareServiceImpl();
        
        Map<String, String> param = Maps.newHashMap();
        //param.put("ts_code", "");//N H S
        param.put("trade_date", "20190430");
        //param.put("start_date", "20181001");
        //param.put("end_date", "20181001");
        
        TuShareParam tuShareParam = new TuShareParam(
                "monthly", 
                TUSHARE_TOKEN, 
                param, 
                "ts_code,trade_date,close,open,high,low,pre_close,change,pct_chg,vol,amount");
        
        ResultSupport<TuShareData> result = tuShareService.getData(tuShareParam);
        
        Preconditions.checkArgument(result.isSuccess());
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        String tableName = "monthly";
        int total = result.getModel().getItems().size();
        int processed = 0;
        for(int i = 0; i < result.getModel().getItems().size(); i++) {
            
            Map<String, Object> insertParams = result.getModel().getItem(i);
            
            Map<String, Object> selectParams = new HashMap<String, Object>();
            selectParams.put("ts_code", insertParams.get("ts_code"));
            selectParams.put("trade_date", insertParams.get("trade_date"));
            ResultSupport<List<Map<String, Object>>> selectRet = dataServiceImpl.select(tableName, selectParams);
            if(selectRet.isSuccess() && selectRet.getModel().size() > 0) {
                insertParams.put("id", selectRet.getModel().get(0).get("id"));
                ResultSupport<Long> updateRet = dataServiceImpl.update(tableName, insertParams);
                Preconditions.checkArgument(updateRet.isSuccess() && updateRet.getModel() > 0);
            }else {
                ResultSupport<Long> insertRet = dataServiceImpl.insert(tableName, insertParams);
                Preconditions.checkArgument(insertRet.isSuccess() && insertRet.getModel() > 0);
            }
            
            processed = processed + 1;
            System.out.println("monthly processing "  + processed + "/" + total + ", code " + insertParams.get("ts_code"));
            
        }
        
    
    }
    
    public static void weekly() throws Exception {

        TuShareService tuShareService = new TuShareServiceImpl();
        ((TuShareServiceImpl)tuShareService).init();
        
        
        Map<String, String> param = Maps.newHashMap();
        //param.put("ts_code", "");//N H S
        param.put("trade_date", "20200110");
        //param.put("start_date", "20181001");
        //param.put("end_date", "20181001");
        
        TuShareParam tuShareParam = new TuShareParam(
                "weekly", 
                TUSHARE_TOKEN, 
                param, 
                "ts_code,trade_date,close,open,high,low,pre_close,change,pct_chg,vol,amount");
        
        ResultSupport<TuShareData> result = tuShareService.getData(tuShareParam);
        
        Preconditions.checkArgument(result.isSuccess());
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        String tableName = "weekly";
        for(int i = 0; i < result.getModel().getItems().size(); i++) {
            
            Map<String, Object> insertParams = result.getModel().getItem(i);
            
            Map<String, Object> selectParams = new HashMap<String, Object>();
            selectParams.put("ts_code", insertParams.get("ts_code"));
            selectParams.put("trade_date", insertParams.get("trade_date"));
            ResultSupport<List<Map<String, Object>>> selectRet = dataServiceImpl.select(tableName, selectParams);
            if(selectRet.isSuccess() && selectRet.getModel().size() > 0) {
                insertParams.put("id", selectRet.getModel().get(0).get("id"));
                ResultSupport<Long> updateRet = dataServiceImpl.update(tableName, insertParams);
                Preconditions.checkArgument(updateRet.isSuccess() && updateRet.getModel() > 0);
            }else {
                ResultSupport<Long> insertRet = dataServiceImpl.insert(tableName, insertParams);
                Preconditions.checkArgument(insertRet.isSuccess() && insertRet.getModel() > 0);
            }
        }
        
    
    }
    
    public static void topInst() throws Exception {
    	
    	List<String> tsCodes = Lists.newArrayList();
    	
    	String tableName = "stock_basic";
    	
    	DataServiceImpl dataServiceImpl = new DataServiceImpl();
    	dataServiceImpl.init();
    	
    	Map<String, Object> selectParams = new HashMap<String, Object>();
    	selectParams.put(VelocityContextKey.Limit, 4000);
    	ResultSupport<List<Map<String, Object>>> selectRet = dataServiceImpl.select(tableName, selectParams);
    	
    	Preconditions.checkArgument(selectRet.isSuccess());
    	
    	tsCodes = selectRet.getModel().stream()
    			.map(item->{
    				return LangUtil.convert(item.get("ts_code"), String.class);
    			})
    			.filter(tsCode -> tsCode != null)
    			.collect(Collectors.toList());
    	
    	RateLimiter rateLimiter = RateLimiter.create(3);
    	
    	for(String tsCode : tsCodes) {
    		System.out.println("topInst=" + tsCode);
    		rateLimiter.acquire();
    		try {
    			topInst(tsCode, dataServiceImpl);
            }catch(Exception e) {
                System.out.println("topInst=" + tsCode + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public static void topInst(String tsCode, DataServiceImpl dataService) throws Exception {

    	TuShareService tuShareService = new TuShareServiceImpl();

    	Map<String, String> param = Maps.newHashMap();
    	param.put("ts_code", tsCode);
    	param.put("trade_date", "20191219");

    	TuShareParam tuShareParam = new TuShareParam(
    			"top_inst", 
    			TUSHARE_TOKEN, 
    			param, 
    			"trade_date,ts_code,exalter,buy,buy_rate,sell,sell_rate,net_buy");

    	ResultSupport<TuShareData> result = tuShareService.getData(tuShareParam);

    	Preconditions.checkArgument(result.isSuccess(), result.getErrCode() + "$" + result.getErrMsg());

    	/**
        for(int i = result.getModel().getItems().size() - 1; i >=0 ; i--) {
            Map<String, Object> itemElem  = result.getModel().getItem(i);
            if(!"首发原始股".equals(itemElem.get("share_type"))) {
                result.getModel().getItems().remove(i);
            }
        }
    	 */

    	String tableName = "top_inst";
    	for(int i = 0; i < result.getModel().getItems().size(); i++) {

    		Map<String, Object> insertParams = result.getModel().getItem(i);

    		Map<String, Object> selectParams = new HashMap<String, Object>();
    		selectParams.put("ts_code", insertParams.get("ts_code"));
    		selectParams.put("trade_date", insertParams.get("trade_date"));
    		selectParams.put("exalter", insertParams.get("exalter"));

    		ResultSupport<List<Map<String, Object>>> selectRet = dataService.select(tableName, selectParams);
    		if(selectRet.isSuccess() && selectRet.getModel().size() > 0) {
    			insertParams.put("id", selectRet.getModel().get(0).get("id"));
    			ResultSupport<Long> updateRet = dataService.update(tableName, insertParams);
    			Preconditions.checkArgument(updateRet.isSuccess() && updateRet.getModel() > 0);
    		}else {
    			ResultSupport<Long> insertRet = dataService.insert(tableName, insertParams);
    			Preconditions.checkArgument(insertRet.isSuccess() && insertRet.getModel() > 0);
    		}
    	}
    }
    
public static void top10FloatHolders() throws Exception {
    	
    	List<String> tsCodes = Lists.newArrayList();
    	
    	String tableName = "stock_basic";
    	
    	DataServiceImpl dataServiceImpl = new DataServiceImpl();
    	dataServiceImpl.init();
    	
    	Map<String, Object> selectParams = new HashMap<String, Object>();
    	selectParams.put(VelocityContextKey.Limit, 4000);
    	ResultSupport<List<Map<String, Object>>> selectRet = dataServiceImpl.select(tableName, selectParams);
    	
    	Preconditions.checkArgument(selectRet.isSuccess());
    	
    	tsCodes = selectRet.getModel().stream()
    			.map(item->{
    				return LangUtil.convert(item.get("ts_code"), String.class);
    			})
    			.filter(tsCode -> tsCode != null)
    			.collect(Collectors.toList());
    	
    	RateLimiter rateLimiter = RateLimiter.create(3);
    	
    	for(String tsCode : tsCodes) {
    		System.out.println("top10FloatHolders=" + tsCode);
    		//rateLimiter.acquire();
    		try {
    			top10FloatHolders(tsCode, dataServiceImpl);
            }catch(Exception e) {
                System.out.println("top10FloatHolders=" + tsCode + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public static void top10FloatHolders(String tsCode, DataServiceImpl dataService) throws Exception {

    	TuShareService tuShareService = new TuShareServiceImpl();

    	Map<String, String> param = Maps.newHashMap();
    	param.put("ts_code", tsCode);
    	param.put("start_date", "20190701");
    	param.put("end_date", "20191031");

    	TuShareParam tuShareParam = new TuShareParam(
    			"top10_floatholders", 
    			TUSHARE_TOKEN, 
    			param, 
    			"ts_code,ann_date,end_date,holder_name,hold_amount");

    	ResultSupport<TuShareData> result = tuShareService.getData(tuShareParam);

    	Preconditions.checkArgument(result.isSuccess(), result.getErrCode() + "$" + result.getErrMsg());

    	/**
        for(int i = result.getModel().getItems().size() - 1; i >=0 ; i--) {
            Map<String, Object> itemElem  = result.getModel().getItem(i);
            if(!"首发原始股".equals(itemElem.get("share_type"))) {
                result.getModel().getItems().remove(i);
            }
        }
    	 */

    	String tableName = "top10_floatholders";
    	for(int i = 0; i < result.getModel().getItems().size(); i++) {

    		Map<String, Object> insertParams = result.getModel().getItem(i);

    		Map<String, Object> selectParams = new HashMap<String, Object>();
    		selectParams.put("ts_code", insertParams.get("ts_code"));
    		selectParams.put("end_date", insertParams.get("end_date"));
    		selectParams.put("holder_name", insertParams.get("holder_name"));

    		ResultSupport<List<Map<String, Object>>> selectRet = dataService.select(tableName, selectParams);
    		if(selectRet.isSuccess() && selectRet.getModel().size() > 0) {
    			insertParams.put("id", selectRet.getModel().get(0).get("id"));
    			ResultSupport<Long> updateRet = dataService.update(tableName, insertParams);
    			Preconditions.checkArgument(updateRet.isSuccess() && updateRet.getModel() > 0);
    		}else {
    			ResultSupport<Long> insertRet = dataService.insert(tableName, insertParams);
    			Preconditions.checkArgument(insertRet.isSuccess() && insertRet.getModel() > 0, JSON.toJSONString(insertRet));
    		}
    	}
    }

    public static void forecast(String tsCode) throws Exception {
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        forecast(tsCode, dataServiceImpl);
    }
    
    public static void forecast(String tsCode, DataService dataService) throws Exception {
        
        TuShareService tuShareService = new TuShareServiceImpl();
        
        Map<String, String> param = Maps.newHashMap();
        param.put("ts_code", tsCode);
        //param.put("ann_date", "20190930");
        //param.put("start_date", "20190920");
        //param.put("end_date", "20191110");
        //param.put("period", "20181001");
        //param.put("type", "20181001");
        
        TuShareParam tuShareParam = new TuShareParam(
                "forecast", 
                TUSHARE_TOKEN, 
                param, 
                "ts_code,ann_date,end_date,type,p_change_min,p_change_max,net_profit_min,net_profit_max,last_parent_net,first_ann_date,summary,change_reason");
        
        ResultSupport<TuShareData> result = tuShareService.getData(tuShareParam);
        
        Preconditions.checkArgument(result.isSuccess());
        
        String tableName = "forecast";
        for(int i = 0; i < result.getModel().getItems().size(); i++) {
            
            Map<String, Object> insertParams = result.getModel().getItem(i);
            
            Map<String, Object> selectParams = new HashMap<String, Object>();
            selectParams.put("ts_code", insertParams.get("ts_code"));
            //selectParams.put("ann_date", insertParams.get("ann_date"));
            //selectParams.put("float_date", insertParams.get("float_date"));
            //selectParams.put("float_share", insertParams.get("float_share"));
            //selectParams.put("float_ratio", insertParams.get("float_ratio"));
            //selectParams.put("holder_name", insertParams.get("holder_name"));
            //selectParams.put("share_type", insertParams.get("share_type"));
            
            ResultSupport<List<Map<String, Object>>> selectRet = dataService.select(tableName, selectParams);
            if(selectRet.isSuccess() && selectRet.getModel().size() > 0) {
                insertParams.put("id", selectRet.getModel().get(0).get("id"));
                ResultSupport<Long> updateRet = dataService.update(tableName, insertParams);
                Preconditions.checkArgument(updateRet.isSuccess() && updateRet.getModel() > 0);
            }else {
                ResultSupport<Long> insertRet = dataService.insert(tableName, insertParams);
                Preconditions.checkArgument(insertRet.isSuccess() && insertRet.getModel() > 0);
            }
        }
        
    }

}
