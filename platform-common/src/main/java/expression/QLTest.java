package expression;

import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;

public class QLTest {


    public static void main(String[] args){

        ExpressRunner runner = new ExpressRunner();

        DefaultContext<String, Object> context = new DefaultContext<String, Object>();
        context.put("a",1);
        context.put("b",2);
        context.put("c",3);
        String express = "a+b*c";
        Object r = null;
        try {
            r = runner.execute(express, context, null, true, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(r);


    }


}
