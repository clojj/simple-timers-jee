package cdiextension;

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
                TimersInstance timersInstance = new TimersInstance();
                timersInstance.setClazz(method.getJavaMember().getDeclaringClass());
                timersInstance.setMethod(method);
                timersInstances.add(timersInstance);
                break;
            }
        }
    }

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation after) {
        Class<?> clazz = SimpleTimersInit.class;
        Set<Bean<?>> beans = beanManager.getBeans(clazz);
        final Bean<?> appInitBean = beanManager.resolve(beans);
        CreationalContext<?> creationalContext = beanManager.createCreationalContext(appInitBean);
        Object instance = beanManager.getReference(appInitBean, clazz, creationalContext);
        ((SimpleTimersInit) instance).getTimersInstances().addAll(timersInstances);
    }

}
