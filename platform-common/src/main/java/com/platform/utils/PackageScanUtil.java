package com.platform.utils;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;

public class PackageScanUtil{
    
    public static final String FILE_PROTOCOL = "file";
    public static final String UTF8_CHARSET = "UTF-8";
    public static final String CLASS_SUFFIX = ".class";
    public static final String CLASS_PATH_STUB = "classes";
    public static final int OFFSET_CLASS_PATH_STUB = 8;

    @SuppressWarnings("rawtypes")
    public static Set<Class> scan(String packageName) throws RuntimeException{
        return scan(packageName, null, UTF8_CHARSET);
    }
    
    @SuppressWarnings("rawtypes")
    public static Set<Class> scan(String packageName, ClassLoader classLoader, String charset) throws RuntimeException{
        
        Preconditions.checkNotNull(packageName);
        
        Set<Class> sets = new HashSet<Class>();
        
        String packageDirName = packageName.replace('.', '/');
        ClassLoader currentClassLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        
        Enumeration<URL> resources;
        try {
            resources = currentClassLoader.getResources(packageDirName);
            if(!resources.hasMoreElements()) {
                return null;
            }
            
            URL url = resources.nextElement();
            if(!FILE_PROTOCOL.equals(url.getProtocol())) {
                throw new RuntimeException("Illegal file, url is " + url.toString());
            }
            
            File file = new File(URLDecoder.decode(url.getFile(), charset));
            if(!file.isDirectory()) {
                throw new RuntimeException("Illegal package, package is " + file.getAbsolutePath());
            }
            
            File[] files = file.listFiles();
            for(File fileE : files) {
                
                String absolutePath = fileE.getAbsolutePath();
                if(!absolutePath.endsWith(CLASS_SUFFIX)) {
                    continue;
                }
                
                String classPath = absolutePath.substring(
                        absolutePath.lastIndexOf(CLASS_PATH_STUB) + OFFSET_CLASS_PATH_STUB, 
                        absolutePath.indexOf(CLASS_SUFFIX)
                        );
                
                String className = classPath.replaceAll("/", ".");
                Class clazz = Class.forName(className);
                sets.add(clazz);
            }
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return sets;
    }
    
}

