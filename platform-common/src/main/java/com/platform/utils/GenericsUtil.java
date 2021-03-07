package com.platform.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.Setter;

public class GenericsUtil {
    
    private static Map<CacheKey, Class<?>> cache1 = new ConcurrentHashMap<CacheKey, Class<?>>();
    
    private static Map<CacheKey, Class<?>> cache2 = new ConcurrentHashMap<CacheKey, Class<?>>();
    
    public static Class<?> findGenericClass(Class<?> instantiatedClazz, Class<?> xAxis, int yAxis) {
         
         Class<?> ret = null;
         
         if((ret = cache2.get(new CacheKey(instantiatedClazz, yAxis, xAxis))) != null) {
             return ret;
         }else {
             ret = findGenericClassWithoutCache(instantiatedClazz, yAxis, xAxis);
             if(ret != null) {
                 cache2.put(new CacheKey(instantiatedClazz, yAxis, xAxis), ret);
                 return ret;
             }else {
                 return null;
             }
         }
     }
    
    /**
     * @param 
     */
    @SuppressWarnings("rawtypes")
    public static Class<?> findGenericClassWithoutCache(Class<?> instantiatedClazz, Class<?> xAxis, int yAxis) {
        Preconditions.checkArgument(instantiatedClazz != null);
        Preconditions.checkArgument(xAxis != null);
        Preconditions.checkArgument(yAxis >= 0);
        Preconditions.checkArgument(xAxis.isAssignableFrom(instantiatedClazz));
        Preconditions.checkArgument(!instantiatedClazz.isInterface());
        Preconditions.checkArgument(!xAxis.isInterface());
        Preconditions.checkArgument(!instantiatedClazz.equals(xAxis));
        TypeVariable<?>[] instantiatedClazzTypeParameters = instantiatedClazz.getTypeParameters();
        for(Type instantiatedClazzTypeParameter : instantiatedClazzTypeParameters) {
            Preconditions.checkArgument(
                    (instantiatedClazzTypeParameter instanceof Class) 
                    || 
                    (instantiatedClazzTypeParameter instanceof ParameterizedType)
                    );
        }
        
        Map<Class<?>, List<Class<?>>> actualTypes4Class = Maps.newLinkedHashMap();
        Map<String, Class> typeVariablesBuffer = Maps.newLinkedHashMap();
        for(Class<?> tClass = instantiatedClazz; !tClass.equals(xAxis); tClass = tClass.getSuperclass()) {
            Class<?> superClass = tClass.getSuperclass();
            
            Type type = tClass.getGenericSuperclass();
            if(!(type instanceof ParameterizedType)) {
                actualTypes4Class.put(superClass, Lists.newArrayList());
            }
            
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            TypeVariable<?>[] typeParameters = superClass.getTypeParameters();
            
            Preconditions.checkArgument(actualTypeArguments.length == typeParameters.length);
            
            List<Class<?>> actualTypes = Lists.newArrayList();
            for(Type actualTypeArgument : actualTypeArguments) {
                if(actualTypeArgument instanceof Class) {
                    actualTypes.add((Class<?>) actualTypeArgument);
                }else if(actualTypeArgument instanceof ParameterizedType){
                    Type actualTypeArgumentType = ((ParameterizedType) actualTypeArgument).getRawType();
                    if(actualTypeArgumentType instanceof Class) {
                        actualTypes.add((Class<?>) actualTypeArgumentType);
                    }else {
                        throw new RuntimeException("Can not find rawclass of ParameterizedType, type is " + actualTypeArgument.getTypeName());
                    }
                }else if(actualTypeArgument instanceof TypeVariable){
                    Class<?> typeVariableCalss = typeVariablesBuffer.get(((TypeVariable)actualTypeArgument).getName());
                    Preconditions.checkNotNull(typeVariableCalss);
                    actualTypes.add(typeVariableCalss);
                }else {
                    throw new RuntimeException("Can not recognize actualTypeArgument , type is " + actualTypeArgument);
                }
            }
            
            for(int i=0; i< typeParameters.length; i++) {
                TypeVariable typeParameter = typeParameters[i];
                if(typeParameter instanceof TypeVariable) {
                    typeVariablesBuffer.put(typeParameter.getName(), actualTypes.get(i));
                }
            }
            
            actualTypes4Class.put(superClass, actualTypes);
        }
        
        if(yAxis < actualTypes4Class.get(xAxis).size()) {
            return actualTypes4Class.get(xAxis).get(yAxis);
        }else {
            return null;
        }
        
    }
    
    
    /**
     * @param index, 0 based
     */
    public static Class<?> findGenericClass(Class<?> clz, int index, Class<?> root) {
         
         checkNotNull(clz);
         checkNotNull(root);
         checkArgument(index >= 0);
         
         Class<?> ret = null;
         
         if((ret = cache1.get(new CacheKey(clz, index, root))) != null) {
             return ret;
         }else {
             ret = findGenericClassWithoutCache(clz, index, root);
             if(ret != null) {
                 cache1.put(new CacheKey(clz, index, root), ret);
                 return ret;
             }else {
                 return null;
             }
         }
     }
     
    public static Class<?> findGenericClassWithoutCache(Class<?> clz, int index, Class<?> root) {
         
         checkNotNull(clz);
         checkNotNull(root);
         checkArgument(index >= 0);
         
         Class<?> ret = null;
         
         ret = findCurrentClass(clz, index, root);
         if(ret != null) {
             return ret;
         }
         
         return findInterfaces(clz, index, root);
         
     }
    
     protected static Class<?> findCurrentClass(Class<?> clz, int index, Class<?> root){
         
         Class<?> superClass = clz.getSuperclass();
         if(superClass == null) {
             return null;
         }
         
         Type genericSuperClass = clz.getGenericSuperclass();
         
         Class<?> actualTypeArgument = findActualTypeArgument(genericSuperClass, index, root);
         
         if(actualTypeArgument instanceof Class) {
             return (Class<?>) actualTypeArgument;
         }else {
             return findCurrentClass(clz.getSuperclass(), index, root);
         }
         
     }
     
     protected static Class<?> findInterfaces(Class<?> clz, int index, Class<?> root){
         
         Type[] genericInterfaces = clz.getGenericInterfaces();
         if(genericInterfaces == null || genericInterfaces.length <= 0) {
             return null;
         }
         
         for(Type genericInterface : genericInterfaces) {
             
             Class<?> actualTypeArgument = findActualTypeArgument(genericInterface, index, root);
             
             if(actualTypeArgument instanceof Class) {
                 return (Class<?>) actualTypeArgument;
             }else {
                 continue;
             }
             
         }
         
         Class<?>[] interfaces = clz.getInterfaces();
         for(Class<?> interfaceClz : interfaces) {
             
             Class<?> interfaceRet = findInterfaces(interfaceClz, index, root);
             if(interfaceRet != null) {
                 return interfaceRet;
             }else {
                 continue;
             }
         }
             
         
         return null;
         
     }
     
     
     private static Class<?> findActualTypeArgument(Type type, int index, Class<?> root){
         
         if(!(type instanceof ParameterizedType)) {
             return null;
         }

         ParameterizedType parameterizedType = (ParameterizedType) type;
         
         Type rawType = parameterizedType.getRawType();
         Class<?> clz = null;
         if(rawType instanceof Class) {
             clz = (Class<?>) rawType;
         }else {
             return null;
         }
         
         if(!root.isAssignableFrom(clz)) {
             return null;
         }
         
         Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
         if(actualTypeArguments.length <= index) {
             return null;
         }

         Type actualTypeArgument = actualTypeArguments[index];
         if(actualTypeArgument instanceof Class) {
             return (Class<?>) actualTypeArgument;
         }else if(actualTypeArgument instanceof ParameterizedType){
             Type actualTypeArgumentType = ((ParameterizedType) actualTypeArgument).getRawType();
             if(actualTypeArgumentType instanceof Class) {
                 return (Class<?>) actualTypeArgumentType;
             }else {
                 throw new RuntimeException("Can not find rawclass of ParameterizedType, type is " + actualTypeArgument.getTypeName());
             }
         }else {
             return null;
         }
         
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
     
     public static class CacheKey{
         
         @Getter @Setter private Class<?> clz;
         
         @Getter @Setter private Integer index;
         
         @Getter @Setter private Class<?> root;
         
         public CacheKey(Class<?> clz, int index, Class<?> root) {
             this.clz = clz;
             this.index = index;
             this.root = root;
         }
         
         public int hashCode() {
             
             int prime = 17;
             
             int total = clz.hashCode();
             
             total = prime * total + index.hashCode();
             total = prime *total + root.hashCode();
             
             return total;
         }
         
         public boolean equals(Object obj) {
             
             if(!(obj instanceof CacheKey)) {
                 return false;
             }
             
             if(this == obj) {
                 return true;
             }
             
             CacheKey out = (CacheKey) obj;
             
             return clz.equals(out.getClz())
                     && index.equals(out.getIndex())
                     && root.equals(out.getRoot());
             
         }
         
     }
     
     public interface IA<IAnno1, IAnno2, IAnno3>{}
     
     public interface IB extends IA<Float, Double, String>{}
     
     public class A<Anno1, Anno2, Anno3> implements IA<Anno1, Anno2, Anno3>{}
     
     public class C1 extends A<Integer, Long, Number>{}
     
     public class C2 implements IB{}
     
     public class C3<AnnoC3> extends A<Integer, Long, Map<String, Object>>{}
     
     public class C4 extends C3<Boolean>{}
     
     public static void main(String[] args) {
         
         //testFindSuperClass();
         
         //testFindInterfaces();
         
         //testFindSuperClassWithParameterType();
         
         testFindSuperClassNew();
         
         testFindSuperClassNewCache();
         
     }
     
     public static void testFindSuperClassNewCache(){
         System.out.println(GenericsUtil.findGenericClass(C4.class, A.class, 2));
         System.out.println(GenericsUtil.findGenericClass(C4.class, A.class, 2));
     }
     
     public static void testFindSuperClassNew(){
         System.out.println(GenericsUtil.findGenericClassWithoutCache(C4.class, A.class, 2));
     }
     
     public static void testFindSuperClass() {
         
         Class<?> clz = findGenericClass(C1.class, 2, IA.class);
         
         System.out.println(clz);//Number
         
     }
     
     public static void testFindInterfaces() {
         
         Class<?> clz = findGenericClass(C2.class, 2, IA.class);
         
         System.out.println(clz);//String
         
     }
     
     public static void testFindSuperClassWithParameterType() {
         
         Class<?> clz = findGenericClass(C3.class, 2, IA.class);
         
         System.out.println(clz);//Map
         
     }
     
     public static void testCache() {
         
         CacheKey c1 = new CacheKey(Integer.class, 2, Long.class);
         CacheKey c2 = new CacheKey(String.class, 2, Long.class);
         CacheKey c3 = new CacheKey(Integer.class, 3, Long.class);
         CacheKey c4 = new CacheKey(Integer.class, 2, Long.class);
         System.out.println(c1.equals(c2));
         System.out.println(c1.equals(c3));
         System.out.println(c1.equals(c4));
         
         cache1.put(c1, Object.class);
         cache1.put(c2, Object.class);
         cache1.put(c3, Object.class);
         cache1.put(c4, Object.class);
         
         System.out.println(cache1);
         
     }
     
     
}
