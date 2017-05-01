package cdiextension;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.expression.Every;
import com.cronutils.parser.CronParser;

import javax.ejb.Stateless;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static utils.Util.caze;
import static utils.Util.switchType;

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
                /* TODO use this for absolute time ?
                ExecutionTime executionTime = ExecutionTime.forCron(cron);
                Duration duration = executionTime.timeToNextExecution(ZonedDateTime.now());
                */

                // TODO use complete cron info
                Map<CronFieldName, CronField> fieldMap = cron.retrieveFieldsAsMap();
                fieldMap.forEach((key, value) -> {
                    switchType(value.getExpression(),
                            caze(Every.class, every -> {
                                switch (key) {
                                    case SECOND:
                                        int delayMillis = every.getPeriod().getValue() * 1000;
                                        ScheduledMethod scheduledMethod = new ScheduledMethod(type, clazz, method, delayMillis);
                                        scheduledMethods.add(scheduledMethod);
                                        break;
                                    case MINUTE:
                                        break;
                                    case HOUR:
                                        break;
                                    case DAY_OF_MONTH:
                                        break;
                                    case MONTH:
                                        break;
                                    case DAY_OF_WEEK:
                                        break;
                                    case YEAR:
                                        break;
                                }
                            })
                            // TODO: caze(Always.class, always -> {}),
                            // TODO: caze(QuestionMark.class, questionMark -> {})
                    );
                });
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
