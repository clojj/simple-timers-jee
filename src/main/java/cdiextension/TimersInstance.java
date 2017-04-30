package cdiextension;

import javax.enterprise.inject.spi.AnnotatedMethod;

class TimersInstance {

    private Class<?> clazz;
    private AnnotatedMethod<?> method;
    private Object cdiInstance;
    private BeanType type;

    // TODO generalize !
    private int seconds;

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

    public Object getCdiInstance() {
        return cdiInstance;
    }

    public void setCdiInstance(Object cdiInstance) {
        this.cdiInstance = cdiInstance;
    }

    public void setType(BeanType type) {
        this.type = type;
    }

    public BeanType getType() {
        return type;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }
}
