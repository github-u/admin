package com.platform.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.entity.tushare.TuShareData;
import com.platform.entity.tushare.TuShareParam;
import com.platform.entity.tushare.TuShareResult;
import com.platform.service.TuShareService;
import com.platform.utils.BeanUtil;
import com.platform.utils.IOUtils;

public class TuShareServiceImpl implements TuShareService {
    
    public static final String TUSHARE_URL = "http://api.tushare.pro";
    
    public static final String TUSHARE_TOKEN = "";
    
    private static Logger logger = LoggerFactory.getLogger(TuShareServiceImpl.class);
            
    private static ThreadLocal<HttpClient> httpClients = new ThreadLocal<HttpClient>(){
        protected HttpClient initialValue() {
            return HttpClientBuilder.create().build();
        }
    };
    
    @Override
    public ResultSupport<TuShareData> getData(TuShareParam tuShareParam){
        
        ResultSupport<TuShareData> ret = new ResultSupport<TuShareData> ();
        
        HttpPost httpPost = new HttpPost(TUSHARE_URL);
        httpPost.setEntity(new StringEntity(JSON.toJSONString(tuShareParam), ContentType.APPLICATION_JSON));
        
        HttpClient httpClient =  httpClients.get();
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
        }
        
    }
    
    public static void main(String[] args) throws Exception {
        
        //sample();
        
        stockBasic();
        
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
    
    public static void income(String tsCode) throws Exception {
        
        TuShareService tuShareService = new TuShareServiceImpl();
        
        Map<String, String> param = Maps.newHashMap();
        param.put("ts_code", tsCode);
        //param.put("ann_date", "20190930");
        //param.put("start_date", "20181001");
        //param.put("end_date", "20181001");
        //param.put("period", "20181001");
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
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        String tableName = "income";
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
    
}
