package cdi;

import cdiextension.SimpleTimer;

import javax.inject.Named;

@Named
public class MyCdiBean {

    @SimpleTimer(value = "cdiMethod")
    public void cdiMethod() {
        System.out.println("cdiMethod");
    }
}
