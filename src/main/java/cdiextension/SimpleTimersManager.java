package cdiextension;

import de.clojj.simpletimers.DelayQueueScheduler;
import de.clojj.simpletimers.TimerObjectCron;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

@ApplicationScoped
public class SimpleTimersManager {

    @Resource
    private ManagedThreadFactory managedThreadFactory;

    @Resource
    private ManagedExecutorService managedExecutorService;

    private List<ScheduledMethod> scheduledMethods = new ArrayList<>();

    private DelayQueueScheduler delayQueueScheduler;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {

        delayQueueScheduler = new DelayQueueScheduler();
        delayQueueScheduler.debugPrint("Starting timers...");
        Thread thread = managedThreadFactory.newThread(delayQueueScheduler.timerThreadInstance());
        delayQueueScheduler.startWith(thread);

        // get all bean instances
        for (ScheduledMethod scheduledMethod : scheduledMethods) {
            Object instance = null;
            switch (scheduledMethod.getType()) {
                case EJB:
                    try {
                        InitialContext ctx = new InitialContext();
                        instance = ctx.lookup("java:module/" + scheduledMethod.getClazz().getSimpleName());
                    } catch (NamingException e) {
                        e.printStackTrace();
                    }
                    break;

                case CDI:
                    instance = CDI.current().select(scheduledMethod.getClazz()).get();
                    break;
            }
            scheduledMethod.setInstance(instance);
        }

        // queue all timers
        for (ScheduledMethod scheduledMethod : scheduledMethods) {
            delayQueueScheduler.add(new TimerObjectCron(scheduledMethod.getCron(), now -> {
                Future<?> future = managedExecutorService.submit(() -> {
                    try {
                        Object result = scheduledMethod.getMethod().getJavaMember().invoke(scheduledMethod.getInstance());
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                });
            }));
        }
    }

    public List<ScheduledMethod> getScheduledMethods() {
        return scheduledMethods;
    }

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        System.out.println("STOP simple-timers scheduler");
        delayQueueScheduler.stop();
    }
}