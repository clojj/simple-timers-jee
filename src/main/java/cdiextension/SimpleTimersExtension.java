package cdiextension;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.expression.Every;
import com.cronutils.model.field.expression.FieldExpression;
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

    private List<TimersInstance> timersInstances = new ArrayList<>();

    private CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    public void addTimers(@Observes ProcessAnnotatedType<R> pat, BeanManager beanManager) {
        this.beanManager = beanManager;
        AnnotatedType<R> at = pat.getAnnotatedType();
        for (AnnotatedMethod<? super R> method : at.getMethods()) {
            if (method.isAnnotationPresent(SimpleTimer.class)) {
                SimpleTimer annotation = method.getAnnotation(SimpleTimer.class);
                Cron cron = parser.parse(annotation.value());

                TimersInstance timersInstance = new TimersInstance();

                // TODO read timer setting
                // TODO use visitor or instanceof ?
                CronField field = cron.retrieve(CronFieldName.SECOND);
                FieldExpression expression = field.getExpression();
                if (expression instanceof Every) {
                    Every every = (Every) expression;
                    Integer seconds = every.getPeriod().getValue();
                    System.out.println("seconds = " + seconds);
                    timersInstance.setSeconds(seconds);
                }

                Class<?> clazz = method.getJavaMember().getDeclaringClass();

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
        Class<?> clazz = SimpleTimersManager.class;
        Set<Bean<?>> beans = beanManager.getBeans(clazz);
        final Bean<?> appInitBean = beanManager.resolve(beans);
        CreationalContext<?> creationalContext = beanManager.createCreationalContext(appInitBean);
        Object instance = beanManager.getReference(appInitBean, clazz, creationalContext);
        ((SimpleTimersManager) instance).getTimersInstances().addAll(timersInstances);
    }

}
