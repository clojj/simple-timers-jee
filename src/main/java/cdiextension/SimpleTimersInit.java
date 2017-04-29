package cdiextension;

import de.clojj.simpletimers.DelayQueueScheduler;
import de.clojj.simpletimers.TimerObjectMillis;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SimpleTimersInit {

    private List<TimersInstance> timersInstances = new ArrayList<>();

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {

        DelayQueueScheduler delayQueueScheduler = new DelayQueueScheduler(true, true);
        delayQueueScheduler.debugPrint("initialization...");

        for (TimersInstance timersInstance : timersInstances) {

            // TODO CDI beans
            // instance already in timersInstance

            // TODO EJBs
            try {
                InitialContext ctx = new InitialContext();
                String name = timersInstance.getClazz().getSimpleName();
                Object instance = ctx.lookup("java:module/" + name);

                delayQueueScheduler.add(new TimerObjectMillis(2000, true, aLong -> {
                    try {
                        Object result = timersInstance.getMethod().getJavaMember().invoke(instance, null);
                        System.out.println("result = " + result);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }));
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }
    }

    public List<TimersInstance> getTimersInstances() {
        return timersInstances;
    }

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init) {
    }
}