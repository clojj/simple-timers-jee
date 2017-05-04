package cdiextension;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimpleTimersExtension<R> implements Extension {

    private static Set<Class<? extends Annotation>> EJB_ANNOTATIONS = new HashSet<Class<? extends Annotation>>() {{
        add(Stateless.class);
        add(Singleton.class);
    }};

    private BeanManager beanManager;

    private List<ScheduledMethod> scheduledMethods = new ArrayList<>();

    private CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    public void addTimers(@Observes @WithAnnotations({SimpleTimer.class}) ProcessAnnotatedType<R> pat, BeanManager beanManager) {
        this.beanManager = beanManager;
        AnnotatedType<R> at = pat.getAnnotatedType();
        for (AnnotatedMethod<? super R> method : at.getMethods()) {
            SimpleTimer annotation = method.getAnnotation(SimpleTimer.class);
            if (annotation != null) {
                BeanType type = EJB_ANNOTATIONS.stream().anyMatch(at::isAnnotationPresent) ? BeanType.EJB : BeanType.CDI;
                Class<?> clazz = method.getJavaMember().getDeclaringClass();
                Cron cron = parser.parse(annotation.value());

	            ScheduledMethod scheduledMethod = new ScheduledMethod(type, clazz, method, cron);
	            scheduledMethods.add(scheduledMethod);
            }
        }
    }

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation afterDeploymentValidation) {
        // move list of beans to SimpleTimersInit
        Set<Bean<?>> beans = beanManager.getBeans(SimpleTimersManager.class);
        final Bean<?> appInitBean = beanManager.resolve(beans);
        CreationalContext<?> creationalContext = beanManager.createCreationalContext(appInitBean);
        Object instance = beanManager.getReference(appInitBean, SimpleTimersManager.class, creationalContext);
        ((SimpleTimersManager) instance).getScheduledMethods().addAll(scheduledMethods);
    }
}
