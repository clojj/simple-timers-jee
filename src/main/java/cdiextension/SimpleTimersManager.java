package cdiextension;

import de.clojj.simpletimers.DelayQueueScheduler;
import de.clojj.simpletimers.TimerObjectMillis;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
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

    private List<TimersInstance> timersInstances = new ArrayList<>();

    private DelayQueueScheduler delayQueueScheduler;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        delayQueueScheduler = new DelayQueueScheduler();
        delayQueueScheduler.debugPrint("Starting timers...");
        Thread thread = managedThreadFactory.newThread(delayQueueScheduler.timerThreadInstance());
        delayQueueScheduler.startWith(thread);

        for (TimersInstance timersInstance : timersInstances) {

            switch (timersInstance.getType()) {
                case EJB:
                    try {
                        // TODO EJBs
                        InitialContext ctx = new InitialContext();
                        String name = timersInstance.getClazz().getSimpleName();
                        Object instance = ctx.lookup("java:module/" + name);

                        // TODO get timer setting from TimersInstance
                        // TODO refactor common code
                        delayQueueScheduler.add(new TimerObjectMillis(timersInstance.getSeconds() * 1000, true, aLong -> {
                            Future<?> future = managedExecutorService.submit(() -> {
                                try {
                                    Object result = timersInstance.getMethod().getJavaMember().invoke(instance, null);
                                    System.out.println("result EJB = " + result);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            });
                        }));
                    } catch (NamingException e) {
                        e.printStackTrace();
                    }
                    break;

                case CDI:
                    // TODO CDI beans
                    Object instance = timersInstance.getCdiInstance();

                    // TODO get timer setting from TimersInstance
                    // TODO refactor common code
                    delayQueueScheduler.add(new TimerObjectMillis(timersInstance.getSeconds() * 1000, true, aLong -> {
                        Future<?> future = managedExecutorService.submit(() -> {
                            try {
                                Object result = timersInstance.getMethod().getJavaMember().invoke(instance, null);
                                System.out.println("result CDI = " + result);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        });
                    }));
                    break;
            }
        }
    }

    public List<TimersInstance> getTimersInstances() {
        return timersInstances;
    }

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init) {
    }
}