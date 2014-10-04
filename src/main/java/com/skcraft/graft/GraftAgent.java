package com.skcraft.graft;

import com.skcraft.graft.profiler.TickInjector;
import com.skcraft.graft.util.SimpleLogFormatter;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class GraftAgent {

    private static final Logger log = Logger.getLogger(GraftAgent.class.getCanonicalName());

    public static void premain(String args, Instrumentation inst) {
        setupLogger();

        log.info("Graft is enabled.");

        TickInjector.register(inst);
        ClassDumper.register(inst);
    }

    private static void setupLogger() {
        SimpleLogFormatter.configureGlobalLogger();
    }

}
