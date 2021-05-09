package com.platform.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class AnnotationUtil {
    
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A findFieldAnnotation(Field field, Class<A> specifyAnna){
        
        Annotation[] annas = field.getDeclaredAnnotations();
        
        for(Annotation anna : annas){
            if(specifyAnna.equals(anna.annotationType())){
                return (A) anna;
            }
        }
        
        return null;
    }
     
}