package cdiextension;

import javax.enterprise.inject.spi.AnnotatedMethod;

class TimersInstance {

    private Class<?> clazz;
    private AnnotatedMethod<?> method;
    private Object instance;

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public AnnotatedMethod<?> getMethod() {
        return method;
    }

    public void setMethod(AnnotatedMethod<?> method) {
        this.method = method;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }
}
