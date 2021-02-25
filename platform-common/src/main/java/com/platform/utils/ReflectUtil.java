package com.platform.utils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.Setter;

public class ReflectUtil {
    
    private static final Map<Class<?>, Field[]> declaredFieldsCache =
            new ConcurrentHashMap<Class<?>, Field[]>(256);
            
    private static final Map<Class<?>, Map<String, Class<?>>> declaredFieldClassesCache =
            new ConcurrentHashMap<Class<?>, Map<String, Class<?>>>(256);
            
    public static Class<?> getFieldClass(Class<?> clazz, String fieldName) {
        Class<?> targetClass = clazz;
        do {
            Map<String, Class<?>> fieldClasses = getDeclaredFieldClasses(targetClass);
            Class<?> fieldClass = fieldClasses.get(fieldName);
            if(fieldClass != null) {
                return fieldClass;
            }
            
            targetClass = targetClass.getSuperclass();
        }while (targetClass != null && targetClass != Object.class);
        
        return null;
    }
   
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
    
    private static Map<String, Class<?>> getDeclaredFieldClasses(Class<?> clazz) {
        checkNotNull(clazz);
        Map<String, Class<?>> result = declaredFieldClassesCache.get(clazz);
        if (result == null) {
            Field[] fields = getDeclaredFields(clazz);
            result = Maps.newConcurrentMap();
            for(Field field : fields) {
                result.put(field.getName(), field.getType());
            }
            declaredFieldClassesCache.put(clazz, result);
        }
        return result;
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
    
    public static class A{
        @Getter @Setter private String city;
        @Getter @Setter private Long area;
    }
    
    public static class B extends A{
        @Getter @Setter private Boolean town;
    }
    
    public static void main(String[] args) {
        System.out.println(ReflectUtil.getFieldClass(A.class, "city"));
        System.out.println(ReflectUtil.getFieldClass(A.class, "area"));
        System.out.println(ReflectUtil.getFieldClass(B.class, "town"));
        System.out.println(ReflectUtil.getFieldClass(B.class, "city"));
        System.out.println(ReflectUtil.getFieldClass(B.class, "abc"));
        
    }
    
}
