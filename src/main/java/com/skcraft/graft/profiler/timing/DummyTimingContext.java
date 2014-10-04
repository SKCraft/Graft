package com.skcraft.graft.profiler.timing;

public class DummyTimingContext implements TimingContext {

    private static final DummyTimingContext instance = new DummyTimingContext();

    private DummyTimingContext() {
    }

    @Override
    public void stop() {
    }

    public static DummyTimingContext getInstance() {
        return instance;
    }

}
