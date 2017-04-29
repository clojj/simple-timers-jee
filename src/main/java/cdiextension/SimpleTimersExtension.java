package cdiextension;

import javax.ejb.Stateless;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SimpleTimersExtension<R> implements Extension {

    private BeanManager beanManager;

    private List<TimersInstance> timersInstances = new ArrayList<>();

    public void addTimers(@Observes ProcessAnnotatedType<R> pat, BeanManager beanManager) {
        this.beanManager = beanManager;
        AnnotatedType<R> at = pat.getAnnotatedType();
        for (AnnotatedMethod<? super R> method : at.getMethods()) {
            if (method.isAnnotationPresent(SimpleTimer.class)) {

                // TODO read timer setting

                Class<?> clazz = method.getJavaMember().getDeclaringClass();

                TimersInstance timersInstance = new TimersInstance();
                if (at.isAnnotationPresent(Stateless.class)) {
                    timersInstance.setType(BeanType.EJB);
                } else {
                    timersInstance.setType(BeanType.CDI);
                }

                timersInstance.setClazz(clazz);
                timersInstance.setMethod(method);
                timersInstances.add(timersInstance);
                break;
            }
        }
    }

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation after) {

        // get CDI instances
        for (TimersInstance timersInstance : timersInstances) {
            Class<?> clazz = timersInstance.getClazz();
            Set<Bean<?>> beans = beanManager.getBeans(clazz);
            final Bean<?> bean = beanManager.resolve(beans);
            CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
            Object instance = beanManager.getReference(bean, clazz, creationalContext);
            timersInstance.setCdiInstance(instance);
        }

        // move list of beans to SimpleTimersInit
        Class<?> clazz = SimpleTimersInit.class;
        Set<Bean<?>> beans = beanManager.getBeans(clazz);
        final Bean<?> appInitBean = beanManager.resolve(beans);
        CreationalContext<?> creationalContext = beanManager.createCreationalContext(appInitBean);
        Object instance = beanManager.getReference(appInitBean, clazz, creationalContext);
        ((SimpleTimersInit) instance).getTimersInstances().addAll(timersInstances);
    }

}
