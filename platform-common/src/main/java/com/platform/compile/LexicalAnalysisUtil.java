package com.platform.compile;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.base.Preconditions;
import com.platform.entity.ResultSupport;
import com.platform.utils.Pair;

import lombok.Getter;
import lombok.Setter;

public class LexicalAnalysisUtil {
    
    public static class Token{
        
        @Getter @Setter private String token;
        @Getter @Setter private int line;
        @Getter @Setter private int i;
        @Getter @Setter private int index;
        
        public Token(String token,int line,int i){
            this.token = token;
            this.line = line;
            this.i = i;
        }
        
        public String toString(){
            return this.token;
        }
        
    }
    
    public static enum RC{
        TOKEN_ANALYSIS_STRING_ILLEGAL("字符串未闭合"),
        
        TOKEN_ANALYSIS_OPERATOR_NOT_FOUND("未发现操作符"),
        
        TOKEN_ANALYSIS_COMMON_TOKEN_NOT_FOUND("未发现普通token"),
        
        TOKEN_ANALYSIS_COMMON_TOKEN_ILLEGAL("词法分析异常"),
        
        ;
        
        @Getter private String desc;
        
        private RC(String desc){
            this.desc = desc;
        }
    }
    
    public static ResultSupport<List<Token>> parse(String[] operators,String str) throws Exception{
        ResultSupport<List<Token>> ret = new ResultSupport<List<Token>>();
        if(str == null || "".equals(str) || "".equals(str.trim())) {
            return ret.success(new ArrayList<Token>());
        }
        
        List<Token> tokens = new ArrayList<Token>();
        int i = 0, line = 1, commonI = i;
        while(i < str.length()) {
            char charAt = str.charAt(i);
            if(charAt == '"') {
                ResultSupport<Pair<Integer, Token>> getStringTokenRet = getStringToken(str, i+1, line);
                if(!getStringTokenRet.isSuccess()) {
                    return ret.fail(getStringTokenRet.getErrCode(), getStringTokenRet.getErrMsg());
                }
                
                tokens.add(getStringTokenRet.getModel().snd);
                i = getStringTokenRet.getModel().fst; 
                commonI = i;
            }
            else if(charAt == ' ' || charAt == '\r' || charAt == '\n') {
                
                ResultSupport<Pair<Integer, Token>> getCommonTokenRet = getCommonToken(str, i, line, commonI);
                if(!getCommonTokenRet.isSuccess() 
                        && !RC.TOKEN_ANALYSIS_COMMON_TOKEN_NOT_FOUND.toString().equals(getCommonTokenRet.getErrCode())) {
                    return ret.fail(getCommonTokenRet.getErrCode(), getCommonTokenRet.getErrMsg());
                }else if(!getCommonTokenRet.isSuccess() 
                        && RC.TOKEN_ANALYSIS_COMMON_TOKEN_NOT_FOUND.toString().equals(getCommonTokenRet.getErrCode())) {
                    //ignore
                }else {
                    tokens.add(getCommonTokenRet.getModel().snd);
                }
                
                if(charAt == '\n') {
                    line = line + 1;
                }
                
                i = i+1;
                commonI = i;
            }
            else {
                ResultSupport<Pair<Integer, Token>> getOperatorTokenRet = getOperatorToken(operators, str, i, line);
                if(!getOperatorTokenRet.isSuccess() 
                        && !RC.TOKEN_ANALYSIS_OPERATOR_NOT_FOUND.toString().equals(getOperatorTokenRet.getErrCode())) {
                    return ret.fail(getOperatorTokenRet.getErrCode(), getOperatorTokenRet.getErrMsg());
                }else if(!getOperatorTokenRet.isSuccess() 
                        && RC.TOKEN_ANALYSIS_OPERATOR_NOT_FOUND.toString().equals(getOperatorTokenRet.getErrCode())) {
                    i = i + 1;
                }else {
                    ResultSupport<Pair<Integer, Token>> getCommonTokenRet = getCommonToken(str, i, line, commonI);
                    if(!getCommonTokenRet.isSuccess() 
                            && !RC.TOKEN_ANALYSIS_COMMON_TOKEN_NOT_FOUND.toString().equals(getCommonTokenRet.getErrCode())) {
                        return ret.fail(getCommonTokenRet.getErrCode(), getCommonTokenRet.getErrMsg());
                    }else if(!getCommonTokenRet.isSuccess() 
                            && RC.TOKEN_ANALYSIS_COMMON_TOKEN_NOT_FOUND.toString().equals(getCommonTokenRet.getErrCode())) {
                        //ignore
                    }else {
                        tokens.add(getCommonTokenRet.getModel().snd);
                    }
                    
                    tokens.add(getOperatorTokenRet.getModel().snd);
                    i = getOperatorTokenRet.getModel().fst;
                    commonI = i;
                }
            }
        }
        
        ResultSupport<Pair<Integer, Token>> getCommonTokenRet = getCommonToken(str, i, line, commonI);
        if(!getCommonTokenRet.isSuccess() 
                && !RC.TOKEN_ANALYSIS_COMMON_TOKEN_NOT_FOUND.toString().equals(getCommonTokenRet.getErrCode())) {
            return ret.fail(getCommonTokenRet.getErrCode(), getCommonTokenRet.getErrMsg());
        }else if(!getCommonTokenRet.isSuccess() 
                && RC.TOKEN_ANALYSIS_COMMON_TOKEN_NOT_FOUND.toString().equals(getCommonTokenRet.getErrCode())) {
            //ignore
        }else {
            tokens.add(getCommonTokenRet.getModel().snd);
        }
        
        return ret.success(tokens);
    }
    
    /***
     * @param
     *  str         : 原始字符串
     *  i           : 词法分析指针（绝对位置）
     *  line        : 行号
     *  commonI     : 未识别文本开始指针（绝对位置）
     *  @return
     *   pair.fst   : 词法分析指针（绝对位置）
     *   pair.snd   : operator token
     *  @throws Exception 
     */
    protected static ResultSupport<Pair<Integer, Token>> getCommonToken(String str, int i, int line, int commonI) throws Exception {
        ResultSupport<Pair<Integer, Token>> ret = new ResultSupport<Pair<Integer, Token>>();
        
        if(i - commonI > 0) {
            return ret.success(Pair.of(
                    i, 
                    new Token(str.substring(commonI, i), line, commonI)
                    ));
        }else {
            return ret.fail(RC.TOKEN_ANALYSIS_COMMON_TOKEN_NOT_FOUND.toString(), RC.TOKEN_ANALYSIS_COMMON_TOKEN_NOT_FOUND.getDesc());
        }
        
    }
    /***
     * @param
     *  str         : 原始字符串
     *  i           : 词法分析指针（绝对位置）
     *  line        : 行号
     *  @return
     *   pair.fst   : 词法分析指针（绝对位置）
     *   pair.snd   : str token
     *  @throws Exception 
     */
    protected static ResultSupport<Pair<Integer, Token>> getStringToken(String str, int i, int line) throws Exception {
        
        ResultSupport<Pair<Integer, Token>> ret = new ResultSupport<Pair<Integer, Token>>();
        
        String quotesChar = "\"";
        StringBuilder sb = new StringBuilder();
        
        int quotesIndex = str.indexOf(quotesChar, i + 1);
        if (quotesIndex < 0)
            return ret.fail(
                    RC.TOKEN_ANALYSIS_STRING_ILLEGAL.toString(), 
                    RC.TOKEN_ANALYSIS_STRING_ILLEGAL.toString() + ", line " + line
                    );
        
        //sb.append(str.substring(i, endIndex));
        // "\"部分字符1\"部分字符2处理\"部分字符3处理"
        int lastQuotesIndex = i;
        while(str.charAt(quotesIndex - 1) == '\\') {
            sb.append(str.substring(lastQuotesIndex, quotesIndex - 1));
            lastQuotesIndex = quotesIndex;
            quotesIndex = str.indexOf(quotesChar, quotesIndex + 1);
        }
        
        if(quotesIndex < 0) {
            return ret.fail(
                    RC.TOKEN_ANALYSIS_STRING_ILLEGAL.toString(), 
                    RC.TOKEN_ANALYSIS_STRING_ILLEGAL.toString() + ", line " + line
                    );
        }
        
        sb.append(str.substring(lastQuotesIndex, quotesIndex));
        
        return ret.success(Pair.of(quotesIndex + 1,new Token(sb.toString(), line, i)));
    }
    
    /***
     * @param
     *  operators   : 操作符
     *  str         : 原始字符串
     *  i           : 词法分析指针（绝对位置）
     *  line        : 行号
     *  @return
     *   pair.fst   : 词法分析指针（绝对位置）
     *   pair.snd   : operator token
     *  @throws Exception 
     */
    protected static ResultSupport<Pair<Integer, Token>> getOperatorToken(String[] operators, String str, int i, int line) throws Exception {
        ResultSupport<Pair<Integer, Token>> ret = new ResultSupport<Pair<Integer, Token>>();
        
        for(String operator : operators) {
            if(i + operator.length() <= str.length() && operator.equals(str.substring(i, i + operator.length()))) {
                return ret.success(
                        Pair.of(
                                i + operator.length(),
                                new Token(str.substring(i, i + operator.length()), line, i)
                        ));
            }
        }
        
        return ret.fail(RC.TOKEN_ANALYSIS_OPERATOR_NOT_FOUND.toString(), RC.TOKEN_ANALYSIS_OPERATOR_NOT_FOUND.getDesc());
    }
    
    
    
    public static class TestCase{
        
        @Test
        public void _1_test_Token_Analysis_String() throws Exception{
            String str = "\"StringToken1\""
                    + "\"StringToken2\"";
            
            ResultSupport<List<Token>> parseRet = LexicalAnalysisUtil.parse(null, str);
            Preconditions.checkArgument(parseRet.isSuccess());
            Preconditions.checkArgument("StringToken1".equals(parseRet.getModel().get(0).getToken()));
            Preconditions.checkArgument("StringToken2".equals(parseRet.getModel().get(1).getToken()));
            
        }
        
        @Test
        public void _2_test_Token_Analysis_String_With_Slash() throws Exception{
            String str = "\"StringToken1\""
                    + "\"StringToken2\""
                    + "\"String\\\"Token3\"";
            
            ResultSupport<List<Token>> parseRet = LexicalAnalysisUtil.parse(null, str);
            Preconditions.checkArgument(parseRet.isSuccess());
            Preconditions.checkArgument("String\"Token3".equals(parseRet.getModel().get(2).getToken()));
            
        }
        
        @Test
        public void _3_test_Token_Analysis_String_Illegal_Gramma() throws Exception{
            String str = "\"StringToken1";
            
            ResultSupport<List<Token>> parseRet = LexicalAnalysisUtil.parse(null, str);
            Preconditions.checkArgument(!parseRet.isSuccess() && parseRet.getErrCode().equals(RC.TOKEN_ANALYSIS_STRING_ILLEGAL.toString()));
            
        }
        
        @Test
        public void _4_test_Token_Analysis_Operator() throws Exception{
            String str = "c = a + b;";
            
            ResultSupport<List<Token>> parseRet = LexicalAnalysisUtil.parse(Constants.OPERATOR, str);
            Preconditions.checkArgument(parseRet.isSuccess() 
                    && "c".equals(parseRet.getModel().get(0).getToken())
                    && "=".equals(parseRet.getModel().get(1).getToken())
                    && "a".equals(parseRet.getModel().get(2).getToken())
                    && "+".equals(parseRet.getModel().get(3).getToken())
                    && "b".equals(parseRet.getModel().get(4).getToken())
                    && ";".equals(parseRet.getModel().get(5).getToken())
                    );
            
        }
        
        @Test
        public void _5_test_Token_Analysis_Operator_String() throws Exception{
            String str = "c = a + b; "
                    + "e = \"hello world\""
                    + "return e;";
            
            ResultSupport<List<Token>> parseRet = LexicalAnalysisUtil.parse(Constants.OPERATOR, str);
            Preconditions.checkArgument(parseRet.isSuccess() 
                    && "c".equals(parseRet.getModel().get(0).getToken())
                    && "=".equals(parseRet.getModel().get(1).getToken())
                    && "a".equals(parseRet.getModel().get(2).getToken())
                    && "+".equals(parseRet.getModel().get(3).getToken())
                    && "b".equals(parseRet.getModel().get(4).getToken())
                    && ";".equals(parseRet.getModel().get(5).getToken())
                    && "e".equals(parseRet.getModel().get(6).getToken())
                    && "=".equals(parseRet.getModel().get(7).getToken())
                    && "hello world".equals(parseRet.getModel().get(8).getToken())
                    && "return".equals(parseRet.getModel().get(9).getToken())
                    && "e".equals(parseRet.getModel().get(10).getToken())
                    && ";".equals(parseRet.getModel().get(11).getToken())
                    );
            
        }
    }
    
}
