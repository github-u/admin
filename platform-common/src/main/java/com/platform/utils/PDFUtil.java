package com.platform.utils;

import java.awt.print.PrinterJob;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.text.PDFTextStripper;

import com.alibaba.fastjson.JSON;

import lombok.Getter;
import lombok.Setter;

public class PDFUtil {
    
    public static final String URL = "http://static.cninfo.com.cn/finalpage/2019-10-10/1206970237.PDF";
    
    public static void main(String[] args) throws Exception {
        
        String file = downloadHttpUrl(URL, ".", URL);
        
        File f = new File(file);
        
        
        PDDocument p = PDDocument.load(f);
       
        PDFTextStripper pdfStripper = new PDFTextStripper();
        
        String text = pdfStripper.getText(p);

        reg(text);
        
        p.close();
        
        
                
    }
    
    public static String downloadHttpUrl(String url, String dir, String fileName) {
        try {
            java.net.URL httpurl = new java.net.URL(url);
            File dirfile = new File(dir);  
            if (!dirfile.exists()) {  
                dirfile.mkdirs();
            }
            
            FileUtils.copyURLToFile(httpurl, new File(dir + fileName));
            
            return dir + fileName;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void reg(String line) {
        /**
        String line = "本公司及董事会全体成员保证信息披露的内容真实、准确、完整，没有虚\n" +
                "假记载、误导性陈述或重大遗漏。  \n" +
                "     \n" +
                "    一、本期业绩预计情况 \n" +
                "1. 业绩预告期间：2019年 1月 1日-2019年 9月 30日 \n" +
                "   2. 预计的经营业绩：同向上升 \n" +
                "      项    目 2019年 1-9月 2018年 1-9月 \n" +
                "归属于上市公司股\n" +
                "东的净利润 \n" +
                "盈利：45,913.65万元 盈利：23,627.86万元 \n" +
                "比上年同期增长：94.32%  \n" +
                "基本每股收益 盈利：0.1158元/股 盈利：0.0706元/股 \n" +
                "其中，2019年第三季度业绩预计情况如下： \n" +
                "      项    目 2019年 7-9月 2018年 7-9月 \n" +
                "归属于上市公司股\n" +
                "东的净利润 \n" +
                "盈利：11,126.17万元 盈利：3,218.69万元 \n" +
                "比上年同期增长：245.67%  \n" +
                "基本每股收益 盈利：0.0281元/股 盈利：0.0087元/股 \n" +
                "    二、业绩预告预审计情况 \n" +
                "业绩预告未经过注册会计师预审计。 \n" +
                "三、业绩变动原因说明 \n" +
                "公司本期业绩增长的主要原因是： \n" +
                "1.公司不断优化业务结构，加大自有资金运作力度，相关收入同比增加； \n" +
                "2.公司抓住资本市场回暖的时机积极运作，证券投资相关收益同比大幅增\n" +
                "加； \n" +
                "3.积极应对市场和政策环境等变化，回归本源谋求信托主业转型，着力服\n" +
                "务实体经济，力促信托业务稳健发展。 \n" +
                "四、其他相关说明 \n" +
                "本次业绩预告数据是公司计划财务部初步测算的结果，2019 年三季度实际\n" +
                " 2 \n" +
                "业绩情况及财务数据以公司 2019 年三季度报告为准。敬请投资者注意投资风\n" +
                "险。 \n" +
                "特此公告。 \n" +
                " \n" +
                "                           \n" +
                "                         陕西省国际信托股份有限公司 \n" +
                "                          董  事  会    \n" +
                "                           2019年 10月 10日 \n";
        */
        String[] splits = line.split("\n");
        
        A a = new A();
        int stage = 0;
        for(String spilt : splits) {
            if(stage == 0 && spilt.contains(A.CONCLUSION_KEY)) {
                stage = 1;
                a.setConclusion(spilt);
                continue;
            }
            
            if(stage == 1 && spilt.contains(A.YTD_GROWTH_RATE_KEY)) {
                stage = 2;
                a.setYtdGrowthRate(spilt);
                continue;
            }
            
            if(stage == 2 && spilt.contains(A.STD_GROWTH_RATE_KEY)) {
                stage = 3;
                a.setStdGrowthRate(spilt);
                continue;
            }
        }
        
        Matcher m = A.CONCLUSION_PATTERN.matcher(a.getConclusion());
        if(m.matches()) {
            a.setConclusion(m.group(1));
        }else {
            a.setConclusion(null);
        }
        
        m = A.YTD_GROWTH_KEY_PATTERN.matcher(a.getYtdGrowthRate().trim());
        if(m.matches()) {
            a.setYtdGrowthRate(m.group(1));
        }else {
            a.setYtdGrowthRate(null);
        }
        
        m = A.STD_GROWTH_KEY_PATTERN.matcher(a.getStdGrowthRate().trim());
        if(m.matches()) {
            a.setStdGrowthRate(m.group(1));
        }else {
            a.setStdGrowthRate(null);
        }
        
        System.out.println(JSON.toJSONString(a));
        
        //http://www.cninfo.com.cn/new/disclosure/detail?plate=sse&stockCode=600487&announcementId=1207049111&announcementTime=2019-10-31
    }
    
    public static class A{
        
        public static final String CONCLUSION_KEY = "预计的经营业绩";
        
        public static final String YTD_GROWTH_RATE_KEY = "比上年同期增长";
        
        public static final String STD_GROWTH_RATE_KEY = "比上年同期增长";
        
        public static final Pattern CONCLUSION_PATTERN = Pattern.compile("[\\S|\\s]*预计的经营业绩：(\\S+)\\s*$");
        
        public static final Pattern YTD_GROWTH_KEY_PATTERN = Pattern.compile("比上年同期增长：(\\d+\\.\\d+%)\\s*$");
        
        public static final Pattern STD_GROWTH_KEY_PATTERN = Pattern.compile("比上年同期增长：(\\d+\\.\\d+%)\\s*$");
        
        @Getter @Setter private String conclusion;
        
        @Getter @Setter private String ytdGrowthRate;
        
        @Getter @Setter private String stdGrowthRate;
        
    }
}
