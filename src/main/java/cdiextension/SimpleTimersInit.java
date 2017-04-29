package cdiextension;

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

        for (TimersInstance timersInstance : timersInstances) {
            String name = timersInstance.getClazz().getSimpleName();
            try {
                InitialContext ctx = new InitialContext();
                Object instance = ctx.lookup("java:module/" + name);

                try {
                    Object result = timersInstance.getMethod().getJavaMember().invoke(instance, null);
                    System.out.println("result = " + result);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

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