package com.skcraft.graft.module;

import java.lang.instrument.Instrumentation;

public interface AgentModule {

    void registerWith(Instrumentation inst);

}
