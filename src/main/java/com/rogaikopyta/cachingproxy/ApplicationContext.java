package com.rogaikopyta.cachingproxy;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A trivial implementation of a dependency injection framework.
 */
public class ApplicationContext {

    private final Map<Class<?>, Object> class2singleton = new ConcurrentHashMap<>();

    private final Map<String, Object> name2value = new ConcurrentHashMap<>();

    public <T> void registerBean(Class<? super T> clazz, T bean) {
        class2singleton.put(clazz, bean);
    }

    public void registerValue(String name, Object value) {
        name2value.put(name, value);
    }

    public void autowireBean(Object bean) {
        for (Field f : bean.getClass().getDeclaredFields()) {
            Object value = null;

            Value valueAnn = f.getAnnotation(Value.class);
            if (valueAnn != null) {
                value = name2value.get(valueAnn.value());
            } else {
                Inject injectAnn = f.getAnnotation(Inject.class);
                if (injectAnn != null) {
                    value = class2singleton.get(f.getDeclaringClass());
                }
            }
            if (value != null) {
                f.setAccessible(true);
                try {
                    f.set(bean, value);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException();
                }
            }
        }
    }
}
