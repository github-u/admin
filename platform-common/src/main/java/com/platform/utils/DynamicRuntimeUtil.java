package com.platform.utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

//import org.junit.FixMethodOrder;
//import org.junit.Test;
//import org.junit.runners.MethodSorters;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import lombok.Getter;
import lombok.Setter;

public class DynamicRuntimeUtil {
    
    public static final String          DYNAMIC_CLASS_PATH          = "com.platform.utils.self.define.571451.";
    
    public static final String          DYNAMIC_CLASS_NAME          = "DynamicClass";
    
    public static final AtomicInteger   DYNAMIC_CLASS_COUNTER       = new AtomicInteger(0);
    
    public static final Class<?>        DYNAMIC_CLASS_PARAM_CLASS   = Map.class;
    
    public static final Class<?>        DYNAMIC_RETURN_CLASS        = Object.class;
    
    public static final String          DYNAMIC_INVOKE_METHOD       = "$invoke";
    
    public static final String          DYNAMIC_CONTEXT_METHOD      = "context";
    
    public static final ClassPool       CLASS_POOL                  = ClassPool.getDefault();
    
    public static final Map<String, Object> DYNAMIC_CLASS_OBJECTS   = Maps.newConcurrentMap();
    
    public Object $invoke(Object service, String methodName, Class<? extends Object>[] parameterTypes, Object[] args) throws Exception{
        Method method = service.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(service, args);
    }
    
    protected static String getDynamicClassName() {
        return DYNAMIC_CLASS_PATH + "class" + DYNAMIC_CLASS_COUNTER.incrementAndGet();
    }
    
    public static Object evaluate(String scriptName, String script, Map<String, Object> context) throws Exception{
        
        registerScript(scriptName, script);
        
        Object dynamicClassInstance = getDynamicClassInstance(scriptName);
        
        Method $invoke = dynamicClassInstance.getClass().getMethod(DYNAMIC_INVOKE_METHOD, new Class[] {DYNAMIC_CLASS_PARAM_CLASS});
        
        return $invoke.invoke(dynamicClassInstance, new Object[] {context});
        
    }
    
    public static Object getDynamicClassInstance(String outterScriptName) {
        
        Preconditions.checkNotNull(outterScriptName); 
        
        return DYNAMIC_CLASS_OBJECTS.get(outterScriptName);
        
    }
    
    public static String registerScript(String outterScriptName, String script) throws Exception {
        
        Preconditions.checkArgument(script != null && !"".equals(script) && !"".equals(script.trim()));
        
        if(outterScriptName != null && DYNAMIC_CLASS_OBJECTS.containsKey(outterScriptName)) {
            return outterScriptName;
        }
        
        String fullPathClassName = DYNAMIC_CLASS_PATH + getDynamicClassName();
        
        CtClass ctClass = CLASS_POOL.makeClass(fullPathClassName);
        
        CtMethod ctInvokeMethod = CtNewMethod.make(
                CLASS_POOL.get(DYNAMIC_RETURN_CLASS.getName()), 
                DYNAMIC_INVOKE_METHOD, 
                new CtClass[] {CLASS_POOL.get(DYNAMIC_CLASS_PARAM_CLASS.getName())}, 
                new CtClass[] {}, 
                "{"
                        + script +
                "}",
                ctClass);
        
        ctClass.addMethod(ctInvokeMethod);
        
        ctClass.writeFile();
        
        Object ctClassObj = ctClass.toClass().newInstance();
        
        String scriptName = outterScriptName != null ? outterScriptName : fullPathClassName;
        
        DYNAMIC_CLASS_OBJECTS.put(scriptName, ctClassObj);
        
        return scriptName;
        
    }
    
    
    //@FixMethodOrder(MethodSorters.NAME_ASCENDING)
    public static final class TestCase{
        //@Test
        public void _1_testFramwork() throws Exception {
            Preconditions.checkArgument(
                    "hello world".equals(
                            DynamicRuntimeUtil.evaluate(
                                    "1", 
                                    "return new String(\"hello world\");", 
                                    new HashMap<String, Object>())
                            )
                    );
            ;
        }
        
        //@Test
        public void _2_testRegisterScript() throws Exception {
            
            String scriptName = "2";
            
            Preconditions.checkArgument(!DYNAMIC_CLASS_OBJECTS.containsKey(scriptName));
            
            DynamicRuntimeUtil.evaluate(
                    scriptName, 
                    "return new String(\"hello world\");", 
                    new HashMap<String, Object>()
                    );
            
            Preconditions.checkArgument(DYNAMIC_CLASS_OBJECTS.containsKey(scriptName));
            
        }
        
        //@Test
        public void _3_testGetDynamicClassInstance() throws Exception {
            
            String scriptName = "3";
            
            Preconditions.checkArgument(getDynamicClassInstance(scriptName) == null);
            
            DynamicRuntimeUtil.evaluate(
                    scriptName, 
                    "return new String(\"hello world\");", 
                    new HashMap<String, Object>()
                    );
            
            Preconditions.checkArgument(getDynamicClassInstance(scriptName) != null);
            
        }
        
        public static final class ContextInnerClass{
            
            @Getter @Setter private String name;
            
            public String getString() {
                return "context hello world";
            }
            
        }
        
        //@Test
        public void _4_testContext_getMethod() throws Exception {
            
            String scriptName = "4";
            
            Map<String, Object> context = Maps.newHashMap();
            context.put("innerClass", new ContextInnerClass());
            
            Preconditions.checkArgument(
                    "context hello world".equals(DynamicRuntimeUtil.evaluate(
                            scriptName, 
                            "return ((com.platform.utils.DynamicRuntimeUtil.TestCase.ContextInnerClass)$1.get(\"innerClass\")).getString();", 
                            context
                            ))
                    );
            
        }
        
        //@Test
        public void _5_testContext_setMethod() throws Exception {
            
            String scriptName = "5";
            
            Map<String, Object> context = Maps.newHashMap();
            context.put("innerClass", new ContextInnerClass());
            
            Preconditions.checkArgument(
                    "TC4".equals(
                            DynamicRuntimeUtil.evaluate(
                                    scriptName, 
                                    "com.platform.utils.DynamicRuntimeUtil.TestCase.ContextInnerClass innerClass = ((com.platform.utils.DynamicRuntimeUtil.TestCase.ContextInnerClass)$1.get(\"innerClass\"));"
                                            + "innerClass.setName(\"TC4\");"
                                            + "return innerClass.getName();", 
                                            context
                                    )
                            )
                    );
        }
        
    }
    
    public static void main(String[] args) throws Exception {}
    
}
