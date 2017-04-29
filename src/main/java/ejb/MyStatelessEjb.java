package ejb;

import cdiextension.SimpleTimer;

import javax.ejb.Stateless;

@Stateless
public class MyStatelessEjb {

    public MyStatelessEjb() {
    }

    @SimpleTimer(value = "statelessEjbMethod")
    public void statelessEjbMethod() {
        System.out.println("statelessEjbMethod");
    }

}
