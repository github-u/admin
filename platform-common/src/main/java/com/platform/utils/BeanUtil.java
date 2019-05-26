package com.platform.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.platform.annotation.FieldAnnotation;
import com.platform.utils.ReflectUtil.FieldCallback;

import lombok.Getter;
import lombok.Setter;

public class BeanUtil {
    
    private static Map<Class<?>, Map<String, Field>> fileds = new ConcurrentHashMap<Class<?>, Map<String, Field>>();
    
    public static <Src, Dest> Dest copy(Src src, Dest dest, boolean overwrite) {
        
        Class<?> destClz = dest.getClass();
        do {
            ReflectUtil.doWithFields(destClz, new FieldCallback() {
                @Override
                public void doWith(Field field) throws Exception {

                    boolean isAccess = field.isAccessible();
                    try {
                        field.setAccessible(true);
                        if(Modifier.isStatic(field.getModifiers())){
                            return;
                        };

                        String fieldName = field.getName(), aliasFieldName = null;
                        FieldAnnotation fieldAnnotation = 
                                AnnotationUtil.findFieldAnnotation(field, FieldAnnotation.class);
                        if(fieldAnnotation != null && !"".equals(fieldAnnotation.alias())){
                            aliasFieldName = fieldAnnotation.alias();
                        }

                        Field srcField = getField(src.getClass(), fieldName), aliasSrcField = null;
                        if(aliasFieldName != null) {
                            aliasSrcField = getField(src.getClass(), aliasFieldName);
                        }

                        Field desisionField = aliasSrcField != null ? aliasSrcField : srcField;
                        if(desisionField == null) {
                            return;
                        }

                        Object destFieldVal = field.get(dest);
                        if(!overwrite && destFieldVal != null) {
                            return;
                        }
                        
                        boolean desisionFieldIsAccess = desisionField.isAccessible();
                        try {
                            desisionField.setAccessible(true);
                            field.set(dest, desisionField.get(src));
                        }finally {
                            desisionField.setAccessible(desisionFieldIsAccess);
                        }
                    }finally {
                        field.setAccessible(isAccess);
                    }
                }
            });
            
            destClz = destClz.getSuperclass();
        }while(destClz !=  Object.class);
        
        return dest;
    }
    
    public static <T> T copyPropertiesFrom(Map<String, Object> map, Class<T> clz) throws Exception{
        
        T bean = clz.newInstance();
        
        Class<?> clzT = clz;
        do {
            ReflectUtil.doWithFields(clzT, new FieldCallback() {
                @Override
                public void doWith(Field field) throws Exception {

                    boolean isAccess = field.isAccessible();
                    try {
                        field.setAccessible(true);
                        if(Modifier.isStatic(field.getModifiers())){
                            return;
                        };

                        FieldAnnotation fieldAnnotation = AnnotationUtil.findFieldAnnotation(field, FieldAnnotation.class);

                        Object val, aliasVal = null;
                        if(fieldAnnotation != null && !"".equals(fieldAnnotation.alias())){
                            aliasVal = map.get(fieldAnnotation.alias());
                        }
                        val = map.get(field.getName());

                        field.set(bean, aliasVal != null ? aliasVal : val);
                    }finally {
                        field.setAccessible(isAccess);
                    }

                }
            });
            clzT = clzT.getSuperclass();
        }while(clzT != Object.class) ;
        
        return bean;
        
    }
    
    public static <T> Map<String, Object> poputeProperties(T bean) throws Exception{
        
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        
        Class<?> clzT = bean.getClass();
        do {
            ReflectUtil.doWithFields(clzT, new FieldCallback() {

                @Override
                public void doWith(Field field) throws Exception {

                    boolean isAccess = field.isAccessible();
                    try {
                        field.setAccessible(true);
                        if(Modifier.isStatic(field.getModifiers())){
                            return;
                        }

                        Object val = field.get(bean);
                        FieldAnnotation fieldAnnotation;
                        if((fieldAnnotation = AnnotationUtil.findFieldAnnotation(field, FieldAnnotation.class)) != null){
                            if(fieldAnnotation.mapIgnore()){
                                return;
                            }else if(!"".equals(fieldAnnotation.alias())){
                                map.put(fieldAnnotation.alias(), val);
                            }else{
                                map.put(field.getName(), val);
                            }
                        }else{
                            map.put(field.getName(), val);
                        }
                    }finally {
                        field.setAccessible(isAccess);
                    }
                }
            });
            clzT = clzT.getSuperclass();
        }while(clzT != Object.class) ;
        
        return map;
        
    }
    
    private static Field getField(Class<?> clz, String name) {
        
        Map<String, Field> clzFields = fileds.get(clz);
        if(clzFields == null) {
            Map<String, Field> tClzFields = new ConcurrentHashMap<String, Field>();
            Class<?> clzT = clz;
            do {
                Field[] fields = clzT.getDeclaredFields();
                for(Field field : fields) {
                    if(Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    boolean isAccess = field.isAccessible();
                    try {
                        field.setAccessible(true);
                        tClzFields.put(field.getName(), field);
                        FieldAnnotation fieldAnnotation = 
                                AnnotationUtil.findFieldAnnotation(field, FieldAnnotation.class);
                        if(fieldAnnotation != null && !"".equals(fieldAnnotation.alias())){
                            tClzFields.put(fieldAnnotation.alias(), field);
                        } 
                    }finally {
                        field.setAccessible(isAccess);
                    }
                }
                clzT = clzT.getSuperclass();
                
            }while(clzT != Object.class);
            
            clzFields = tClzFields;
            fileds.put(clz, clzFields);
        }
        
        return clzFields.get(name);
        
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
        
    }
    
    public static class B{
        @Getter @Setter private Long l1;
        @Getter @Setter private String s2;
        @Getter @Setter private Float f3;
        @Getter @Setter private A a;
        
    }
    
    public static class C extends B{
        @Getter @Setter private Object o1;
        
        public static Object o2 ;
        
        private static Object o3 ;
        
        @Getter @Setter private static Object o4;
        
    }
    
    public static void main(String[] args) throws Exception {
        
        C c1 = new C();
        c1.setL1(1L);
        c1.setS2("s2");
        c1.setF3(1.23f);
        c1.setA(new A());
        c1.setO1(new Object());
        
        C c2 = new C();
        c2.setS2("overwrite");
        C c3 = BeanUtil.copy(c1, c2, false);
        
        System.out.println(c3);
        
    }
    
    public static void testBeanMapCopy() throws Exception {
        C c1 = new C();
        c1.setL1(1L);
        c1.setS2("s2");
        c1.setF3(1.23f);
        c1.setA(new A());
        c1.setO1(new Object());
        
        Map<String, Object> m = BeanUtil.poputeProperties(c1);
        System.out.println(m);
        
        B b2 = BeanUtil.copyPropertiesFrom(m, B.class);
        System.out.println(b2);
        
        C c3 = BeanUtil.copyPropertiesFrom(m, C.class);
        System.out.println(c3);
    }
}
