package cdiextension;

import de.clojj.simpletimers.DelayQueueScheduler;
import de.clojj.simpletimers.TimerObjectCron;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
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

    @Inject
    Event<TimerFiredEvent> timerFiredEvent;

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
                        throw new RuntimeException("EJB not found: ", e);
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
            TimerObjectCron timerObject = new TimerObjectCron(scheduledMethod.getCron());
            timerObject.setConsumer(now -> {
                timerFiredEvent.fire(new TimerFiredEvent(timerObject, now));
                Future<?> future = managedExecutorService.submit(() -> {
                    try {
                        Object result = scheduledMethod.getMethod().getJavaMember().invoke(scheduledMethod.getInstance());
                        // TODO handle result...

                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("method call failed: ", e);
                    }
                });
            });
            delayQueueScheduler.add(timerObject);
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