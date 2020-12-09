package com.platform.utils;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.runtime.type.AviatorFunction;
import com.googlecode.aviator.runtime.type.AviatorJavaType;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.IExpressContext;

import lombok.Getter;
import lombok.Setter;

public class QLExpressUtil {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(QLExpressUtil.class);
    
    public static final Map<String, Expression> EXPRESSIONS = Maps.newConcurrentMap();
    
    
    /**
     * 
     * 
     * {
"Condition": "avaitor",
"Executor": "HSF",
"Param": {
"copy": "a,b,c",
"trans": "e",
"logic": [
"f=a+b",
"g=a+e"
]
}
}
    
    public static Pair<String, Map<String, Object>> a(String ruleId, Map<String, Object> context) {
        
        Expression expression = AviatorEvaluator.compile("");
        
        Map<String, Object> env = Maps.newHashMap();
        
        expression.execute(env);
    }
     */
    public static boolean registerExpression(String ruleId, String sExpression) {
        Preconditions.checkNotNull(ruleId);
        Preconditions.checkNotNull(sExpression);
        
        if(EXPRESSIONS.get(ruleId) != null) {
            return Boolean.FALSE;
        }
        
        try {
            Expression expression = AviatorEvaluator.compile(sExpression);
            EXPRESSIONS.put(ruleId, expression);
        }catch(Exception e) {
            LOGGER.error("title=AviatorUtil"
                    + "$mode=" + "registerExpression"
                    + "$errCode=" + "COMPILE_EXCEPTION" , e);
            return Boolean.FALSE;
        }
        
        return Boolean.TRUE;
    }
    
    public static final class TestCase{
        
        @Test
        public void _1_test_registerExpression() {
            Preconditions.checkArgument(registerExpression("1", "a + b"));
        }
        
        @Test
        public void _2_test_registerExpression_repeate() {
            Preconditions.checkArgument(registerExpression("2", "a + b"));
            Preconditions.checkArgument(!registerExpression("2", "a + b"));
        }
        
        @Test
        public void _3_test_registerExpression_buffer() {
            Preconditions.checkArgument(EXPRESSIONS.get("3") == null);
            Preconditions.checkArgument(registerExpression("3", "a + b"));
            Preconditions.checkArgument(EXPRESSIONS.get("3") != null);
            
        }
    }
    
    public static final class A{
        @Getter @Setter private String a = "a";
        @Getter @Setter private String b = "b";
    }
    
    public static void main(String[] args) throws Exception {
        
        ExpressRunner runner = new ExpressRunner(false, true);
        IExpressContext<String, Object> context = new DefaultContext<String, Object>();
        System.out.println(new A());
        //String script = "sum=0;for(i=0;i<10;i=i+1){sum=sum+i;};return sum;";
        //String script = "import com.platform.utils.QLExpressUtil;"
                //+ "a = new QLExpressUtil();return a;";
        
        String script = "a + b;"
                ;
        
        
        Object result = runner.execute(script, context, null,
                true, false);
        System.out.println(result);
        
        
        
    }
    
}
