package cdiextension;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import javax.ejb.Stateless;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SimpleTimersExtension<R> implements Extension {

    private BeanManager beanManager;

    private List<ScheduledMethod> scheduledMethods = new ArrayList<>();

    private CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    public void addTimers(@Observes ProcessAnnotatedType<R> pat, BeanManager beanManager) {
        this.beanManager = beanManager;
        AnnotatedType<R> at = pat.getAnnotatedType();
        for (AnnotatedMethod<? super R> method : at.getMethods()) {
            if (method.isAnnotationPresent(SimpleTimer.class)) {

                // TODO allow CDI, Stateless, Singleton, .... ?
                BeanType type = at.isAnnotationPresent(Stateless.class) ? BeanType.EJB : BeanType.CDI;
                Class<?> clazz = method.getJavaMember().getDeclaringClass();

                SimpleTimer annotation = method.getAnnotation(SimpleTimer.class);
                Cron cron = parser.parse(annotation.value());
	            ScheduledMethod scheduledMethod = new ScheduledMethod(type, clazz, method, cron);
	            scheduledMethods.add(scheduledMethod);

            }
        }
    }

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation afterDeploymentValidation) {
        // move list of beans to SimpleTimersInit
        Class<?> clazz = SimpleTimersManager.class;
        Set<Bean<?>> beans = beanManager.getBeans(clazz);
        final Bean<?> appInitBean = beanManager.resolve(beans);
        CreationalContext<?> creationalContext = beanManager.createCreationalContext(appInitBean);
        Object instance = beanManager.getReference(appInitBean, clazz, creationalContext);
        ((SimpleTimersManager) instance).getScheduledMethods().addAll(scheduledMethods);
    }
}
