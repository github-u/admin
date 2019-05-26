package com.platform.utils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectUtil {
    
    private static final Map<Class<?>, Field[]> declaredFieldsCache =
            new ConcurrentHashMap<Class<?>, Field[]>(256);
    
    public static void doWithFields(Class<?> clazz, FieldCallback fieldCallback) {
        Class<?> targetClass = clazz;
        do {
            Field[] fields = getDeclaredFields(targetClass);
            for (Field field : fields) {
                try {
                    fieldCallback.doWith(field);
                }
                catch (Exception ex) {
                    throw new RuntimeException("fieldCallback exception  '" + field.getName() + "': " + ex);
                }
            }
            targetClass = targetClass.getSuperclass();
        }
        while (targetClass != null && targetClass != Object.class);
    }
    
    private static Field[] getDeclaredFields(Class<?> clazz) {
        checkNotNull(clazz);
        Field[] result = declaredFieldsCache.get(clazz);
        if (result == null) {
            result = clazz.getDeclaredFields();
            declaredFieldsCache.put(clazz, (result.length == 0 ? new Field[] {} : result));
        }
        return result;
    }
    
    public interface FieldCallback {
        
        void doWith(Field field) throws Exception;
        
    }
    
    public static void checkNotNull(Object obj) {
        if(obj == null) {
            throw new RuntimeException();
        }
    }
    
    public static void checkArgument(Boolean b) {
        if(b == null || b.booleanValue() == false) {
            throw new RuntimeException();
        }
    }
    
}
