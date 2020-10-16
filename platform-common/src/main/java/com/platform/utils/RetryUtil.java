package com.platform.utils;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;
import com.platform.entity.ResultSupport;

public class RetryUtil {
    
    private static Logger log = Logger.getLogger(RetryUtil.class); 
            
    public static <T> ResultSupport<T> retry(Callable<ResultSupport<T>> call, int n){
        return retry(call, n, 0);
    }
    
    public static <T> ResultSupport<T> retry(Callable<ResultSupport<T>> call, int n, int sleepMills){
        
        Preconditions.checkArgument(n >= 0);
        
        ResultSupport<T> callRet = null;
        for(int i=0; i<n; i++) {
            //System.out.println("turn = " + i);
            try {
                callRet = call.call();
            }catch(Exception e) {
                log.error("title=RetryUtil"
                        + "$mode=Retry"
                        + "$errCode=RetryException"
                        + "$errMsg=" + e.getMessage(), e);
                callRet = new ResultSupport<T>().fail("RETRY_EXCEPTION", e.getMessage());
                try {
                    Thread.sleep(sleepMills);
                } catch (InterruptedException ie) {
                    callRet = new ResultSupport<T>().fail("RETRY_INTERRUPTED_$2", ie.getMessage());
                    break;
                }
                continue;
            }
            
            if(callRet.isSuccess()) {
                return callRet;
            }else {
                 try {
                    Thread.sleep(sleepMills);
                } catch (InterruptedException e) {
                    callRet = new ResultSupport<T>().fail("RETRY_INTERRUPTED_$1", e.getMessage());
                    break;
                }
                continue;
            }
        }
        
        Preconditions.checkArgument(callRet != null);
        return callRet;
    }
    
    public static void main(String[] args) throws Exception {
        
        //TestCase1:suc
        System.out.println("TestCase1>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Callable<ResultSupport<Object>> c1 = new Callable<ResultSupport<Object>>() {
            @Override
            public ResultSupport<Object> call() throws Exception {
                return new ResultSupport<Object>().success("suc");
            }
        };
        System.out.println(RetryUtil.retry(c1, 10));
        //TestCase2:fail
        System.out.println("TestCase2>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Callable<ResultSupport<Object>> c2 = new Callable<ResultSupport<Object>>() {
            @Override
            public ResultSupport<Object> call() throws Exception {
                return new ResultSupport<Object>().fail("fail", "");
            }
        };
        System.out.println(RetryUtil.retry(c2, 10));
        //TestCase3:exception
        System.out.println("TestCase3>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Callable<ResultSupport<Object>> c3 = new Callable<ResultSupport<Object>>() {
            @Override
            public ResultSupport<Object> call() throws Exception {
                throw new RuntimeException("RuntimeException");
            }
        };
        System.out.println(RetryUtil.retry(c3, 10));
        
        //TestCase4: suc at 2
        System.out.println("TestCase4>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Callable<ResultSupport<Object>> c4 = new Callable<ResultSupport<Object>>() {
            private int n = 0;
            @Override
            public ResultSupport<Object> call() throws Exception {
                ResultSupport<Object> ret = new ResultSupport<Object>();
                System.out.println("call at " + n);
                n++;
                if(n == 1) {
                    return ret.fail("", "");
                }else if(n == 2) {
                    return ret.success("");
                }else {
                    throw new RuntimeException("RuntimeException");
                }
            }
        };
        System.out.println(RetryUtil.retry(c4, 10));
        //TestCase5: add sleep
        System.out.println("TestCase5>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Callable<ResultSupport<Object>> c5 = new Callable<ResultSupport<Object>>() {
            private int n = 0;
            @Override
            public ResultSupport<Object> call() throws Exception {
                ResultSupport<Object> ret = new ResultSupport<Object>();
                n++;
                if(n == 1) {
                    return ret.fail("", "");
                }else if(n == 2) {
                    return ret.success("");
                }else {
                    throw new RuntimeException("RuntimeException");
                }
            }
        };
        
        System.out.println(RetryUtil.retry(c5, 10, 10*1000));
        
        //TestCase6: exception interrupt test
        System.out.println("TestCase6>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Thread t1 = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Callable<ResultSupport<Object>> c6 = new Callable<ResultSupport<Object>>() {
                            @Override
                            public ResultSupport<Object> call() throws Exception {
                                throw new InterruptedException();
                            }
                        };
                        System.out.println(RetryUtil.retry(c6, 10, 20*1000));
                    }
                }
                );
        t1.start();
        Thread.sleep(15*1000);
        t1.interrupt();
        System.out.println("before interrupt");
        //TestCase7: fail interrupt test
        System.out.println("TestCase7>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Thread t2 = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Callable<ResultSupport<Object>> c7 = new Callable<ResultSupport<Object>>() {
                            
                            @Override
                            public ResultSupport<Object> call() throws Exception {
                                ResultSupport<Object> ret = new ResultSupport<Object>();
                                return ret.fail("InterruptTest", "");
                            }
                        };
                        
                        System.out.println(RetryUtil.retry(c7, 10, 20*1000));
                    }
                }
                );
        t2.start();
        Thread.sleep(15*1000);
        t2.interrupt();
        System.out.println("before interrupt");
    }
    
}
