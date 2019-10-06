package com.platform.utils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.platform.annotation.FieldAnnotation;
import com.platform.utils.ReflectUtil.FieldCallback;

import lombok.Getter;
import lombok.Setter;

public class BeanUtil {

	private static Map<Class<?>, Map<String, Field>> fileds = new ConcurrentHashMap<Class<?>, Map<String, Field>>();

	private static Map<Class<?>, Map<String, Method>> readMethods = new ConcurrentHashMap<Class<?>, Map<String, Method>>();

	private static Map<Class<?>, Map<String, Method>> writeMethods = new ConcurrentHashMap<Class<?>, Map<String, Method>>();

	private static final String readMethod = "read", writeMethod = "write";

	public static <Src, Dest> Dest copy(Src src, Dest dest, boolean overwrite) throws Exception {
		return copy(src, dest, overwrite, null, true);
	}

	public static <Src, Dest> Dest copy(Src src, Dest dest, boolean overwrite, Map<String, String> fieldMapping, boolean ignoreNull) throws Exception {

		Class<?> destClz = dest.getClass();
		do {
			ReflectUtil.doWithFields(destClz, new FieldCallback() {
				@Override
				public void doWith(Field field) throws Exception {

					boolean isAccess = field.isAccessible();
					try {
						field.setAccessible(true);
						if (Modifier.isStatic(field.getModifiers())) {
							return;
						}
						;

						Object destFieldVal = field.get(dest);
						if (!overwrite && destFieldVal != null) {
							return;
						}

						String fieldName = field.getName(), aliasFieldName = null, mappingFieldName = null, mappingAliasFieldName = null;
						FieldAnnotation fieldAnnotation =
								AnnotationUtil.findFieldAnnotation(field, FieldAnnotation.class);
						if (fieldAnnotation != null && !"".equals(fieldAnnotation.alias())) {
							aliasFieldName = fieldAnnotation.alias();
						}
						if (fieldMapping != null) {
							mappingFieldName = fieldMapping.get(fieldName);
							mappingAliasFieldName = aliasFieldName != null ? fieldMapping.get(aliasFieldName) : null;
						}

						Method srcReadMethod = null;
						if (fieldMapping != null) {
							srcReadMethod = findMethod(src.getClass(), readMethod, mappingFieldName, mappingAliasFieldName);

						} else {
							srcReadMethod = findMethod(src.getClass(), readMethod, fieldName, aliasFieldName, mappingFieldName, mappingAliasFieldName);
						}
						if (srcReadMethod == null) {
							return;
						}

						Method destWriteMethod = findMethod(dest.getClass(), writeMethod, fieldName);
						if (destWriteMethod == null) {
							return;
						}

						if (destWriteMethod.getParameterTypes().length <= 0) {
							return;
						}

						Class<?> parameterClz = destWriteMethod.getParameterTypes()[0];
						Object srcVal = LangUtil.convert(srcReadMethod.invoke(src), parameterClz);
						if (parameterClz.isPrimitive() && srcVal == null) {
							return;
						}

						if (ignoreNull && srcVal == null) {
							return;
						}

						destWriteMethod.invoke(dest, srcVal);
					} finally {
						field.setAccessible(isAccess);
					}
				}
			});

			destClz = destClz.getSuperclass();
		} while (destClz != Object.class);

		return dest;
	}

	public static <T> T copyPropertiesFrom(Map<String, ? extends Object> map, Class<T> clz) throws Exception {
		return copyPropertiesFrom(map, clz, true);
	}

	public static <T> T copyPropertiesFrom(Map<String, ? extends Object> map, Class<T> clz, boolean ignoreNull) throws Exception {

		T bean = clz.newInstance();

		Class<?> clzT = clz;
		do {
			ReflectUtil.doWithFields(clzT, new FieldCallback() {
				@Override
				public void doWith(Field field) throws Exception {

					boolean isAccess = field.isAccessible();
					try {
						field.setAccessible(true);
						if (Modifier.isStatic(field.getModifiers())) {
							return;
						}
						;

						FieldAnnotation fieldAnnotation = AnnotationUtil.findFieldAnnotation(field, FieldAnnotation.class);

						Object val, aliasVal = null;
						if (fieldAnnotation != null && !"".equals(fieldAnnotation.alias())) {
							aliasVal = map.get(fieldAnnotation.alias());
						}
						val = map.get(field.getName());

						Method destWriteMethod = findMethod(clz, writeMethod, field.getName());
						if (destWriteMethod == null) {
							return;
						}

						if (ignoreNull && val == null && aliasVal == null) {
							return;
						}

						if (destWriteMethod.getParameterTypes().length <= 0) {
							return;
						}

						Class<?> parameterClz = destWriteMethod.getParameterTypes()[0];
						Object srcVal = LangUtil.convert(aliasVal != null ? aliasVal : val, parameterClz);
						if (parameterClz.isPrimitive() && srcVal == null) {
							return;
						}

						destWriteMethod.invoke(bean, srcVal);
					} finally {
						field.setAccessible(isAccess);
					}

				}
			});
			clzT = clzT.getSuperclass();
		} while (clzT != Object.class);

		return bean;

	}

	public static <T> Map<String, Object> populateProperties(T bean) throws Exception {

		Map<String, Object> map = new LinkedHashMap<String, Object>();

		Class<?> clzT = bean.getClass();
		do {
			ReflectUtil.doWithFields(clzT, new FieldCallback() {

				@Override
				public void doWith(Field field) throws Exception {

					boolean isAccess = field.isAccessible();
					try {
						field.setAccessible(true);
						if (Modifier.isStatic(field.getModifiers())) {
							return;
						}

						Method srcReadMethod = findMethod(bean.getClass(), readMethod, field.getName());
						if (srcReadMethod == null) {
							return;
						}

						Object val = srcReadMethod.invoke(bean);
						if (val == null) {
							return;
						}

						FieldAnnotation fieldAnnotation;
						if ((fieldAnnotation = AnnotationUtil.findFieldAnnotation(field, FieldAnnotation.class)) != null) {
							if (fieldAnnotation.mapIgnore()) {
								return;
							} else if (!"".equals(fieldAnnotation.alias())) {
								map.put(field.getName(), val);
								map.put(fieldAnnotation.alias(), val);
							} else {
								map.put(field.getName(), val);
							}
						} else {
							map.put(field.getName(), val);
						}
					} finally {
						field.setAccessible(isAccess);
					}
				}
			});
			clzT = clzT.getSuperclass();
		} while (clzT != Object.class);

		return map;

	}

	/**
	 * private static Field getField(Class<?> clz, String name) {
	 * <p>
	 * Map<String, Field> clzFields = fileds.get(clz);
	 * if(clzFields == null) {
	 * Map<String, Field> tClzFields = new ConcurrentHashMap<String, Field>();
	 * Class<?> clzT = clz;
	 * do {
	 * Field[] fields = clzT.getDeclaredFields();
	 * for(Field field : fields) {
	 * if(Modifier.isStatic(field.getModifiers())) {
	 * continue;
	 * }
	 * boolean isAccess = field.isAccessible();
	 * try {
	 * field.setAccessible(true);
	 * tClzFields.put(field.getName(), field);
	 * FieldAnnotation fieldAnnotation =
	 * AnnotationUtil.findFieldAnnotation(field, FieldAnnotation.class);
	 * if(fieldAnnotation != null && !"".equals(fieldAnnotation.alias())){
	 * tClzFields.put(fieldAnnotation.alias(), field);
	 * }
	 * }finally {
	 * field.setAccessible(isAccess);
	 * }
	 * }
	 * clzT = clzT.getSuperclass();
	 * <p>
	 * }while(clzT != Object.class);
	 * <p>
	 * clzFields = tClzFields;
	 * fileds.put(clz, clzFields);
	 * }
	 * <p>
	 * return clzFields.get(name);
	 * <p>
	 * }
	 */

	private static Method getReadMethod(Class<?> clz, String fieldName) throws IntrospectionException {

		return getMethod(clz, fieldName, readMethods, new PropertyDescriptorMethod() {
			@Override
			public Method get(PropertyDescriptor propertyDescriptor) {
				return propertyDescriptor.getReadMethod();
			}
		});
	}

	private static Method getWriteMethod(Class<?> clz, String fieldName) throws IntrospectionException {

		return getMethod(clz, fieldName, writeMethods, new PropertyDescriptorMethod() {
			@Override
			public Method get(PropertyDescriptor propertyDescriptor) {
				return propertyDescriptor.getWriteMethod();
			}

		});
	}

	private static Method getMethod(Class<?> clz, String fieldName, Map<Class<?>,
			Map<String, Method>> methods, PropertyDescriptorMethod propertyDescriptorMethod) throws IntrospectionException {

		Map<String, Method> clzMethods = methods.get(clz);
		if (clzMethods == null) {
			clzMethods = new ConcurrentHashMap<>();

			BeanInfo beanInfo = Introspector.getBeanInfo(clz);
			PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

			for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
				Method method = propertyDescriptorMethod.get(propertyDescriptor);
				if (method != null) {
					clzMethods.put(propertyDescriptor.getName(), method);
				}
			}

			methods.put(clz, clzMethods);
		}

		return clzMethods.get(fieldName);
	}

	private static Method findMethod(Class<?> clz, String methodType, String... fieldNames) throws IntrospectionException {
		Method method = null;
		if (fieldNames == null) {
			return null;
		}

		for (String fieldName : fieldNames) {
			if (fieldName == null) {
				continue;
			}

			if (readMethod.equals(methodType)) {
				method = getReadMethod(clz, fieldName);
			} else if (writeMethod.equals(methodType)) {
				method = getWriteMethod(clz, fieldName);
			}
			if (method != null) {
				return method;
			}
		}
		return null;
	}

	public static interface PropertyDescriptorMethod {
		Method get(PropertyDescriptor propertyDescriptor);
	}

	public static void checkNotNull(Object obj) {
		if (obj == null) {
			throw new RuntimeException();
		}
	}

	public static void checkArgument(Boolean b) {
		if (b == null || b.booleanValue() == false) {
			throw new RuntimeException();
		}
	}

	public static class A {

	}

	public static class B {
		@Getter
		@Setter
		private Long l1;
		@Setter
		private String s2;
		@Getter
		@Setter
		private Float f3;
		@Getter
		@Setter
		private A a;

		public String getS2() {
			return s2;
		}

	}

    private void a(){}

    public static class C extends B {
        @Getter
		@Setter
		private Object o1;

		public static Object o2;

		private static Object o3;

		@Getter
		@Setter
		private static Object o4;

		private Object n;

        public String getS2() {
			return "C$" + super.getS2();
		}

		public void setN() {
			this.n = new Long(6);
		}
	}

	public static class D {
		@Getter
		@Setter
		private long l1;
	}

	public static void main(String[] args) throws Exception {
		/**
		 BeanInfo beanInfo = Introspector.getBeanInfo(C.class);

		 C c =new C();

		 System.out.println("a");
		 */

		B b = new B();

		testBeanCopyIgnoreNull();

		//testBeanCopyMappingPrimitives();

		//testBeanCopyMapping();

		//testBeanMapCopy();

		//testBeanCopy();
	}

	public static void testBeanCopyMappingPrimitives() throws Exception {
		C c1 = new C();
		D d = new D();
		BeanUtil.copy(c1, d, true);
		System.out.println(d.getL1());
		c1.setL1(1L);
		BeanUtil.copy(c1, d, true);
		System.out.println(d.getL1());

	}

	public static void testBeanCopyMapping() throws Exception {
		C c1 = new C();
		c1.setL1(1L);
		c1.setS2("s2");
		c1.setF3(1.23f);
		c1.setA(new A());
		c1.setO1(new Object());

		C c2 = new C();


		Map<String, String> map = new HashMap<String, String>();
		map.put("o1", "s2");
		BeanUtil.copy(c1, c2, true, map, true);

		System.out.println(c2);
	}

	public static void testBeanCopy() throws Exception {

		C c1 = new C();
		c1.setL1(1L);
		c1.setS2("s2");
		c1.setF3(1.23f);
		c1.setA(new A());
		c1.setO1(new Object());

		C c2 = new C();
		c2.setS2("overwrite");
		C c3 = BeanUtil.copy(c1, c2, false);

		BeanUtil.copy(c1, c2, true);

		System.out.println(c3);

	}

	public static void testBeanMapCopy() throws Exception {
		C c1 = new C();
		c1.setL1(1L);
		c1.setS2("s2");
		c1.setF3(1.23f);
		c1.setA(new A());
		c1.setO1(new Object());

		Map<String, Object> m = BeanUtil.populateProperties(c1);
		System.out.println(m);

		B b2 = BeanUtil.copyPropertiesFrom(m, B.class);
		System.out.println(b2);

		C c3 = BeanUtil.copyPropertiesFrom(m, C.class);
		System.out.println(c3);
	}

	public static void testBeanCopyIgnoreNull() throws Exception {

		C c1 = new C();
		c1.setS2("s2String");

		C c2 = new C();
		c2.setL1(1L);
		c2.setS2("s2");
		c2.setF3(1.23f);
		c2.setA(new A());
		c2.setO1(new Object());

		C c3 = BeanUtil.copy(c1, c2, true, null, true);

		BeanUtil.copy(c1, c2, true, null, false);

		System.out.println(c3);

	}
}
