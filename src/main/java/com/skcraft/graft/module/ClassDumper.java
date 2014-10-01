package com.skcraft.graft.module;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassDumper implements AgentModule {

    private static final Logger log = Logger.getLogger(ClassDumper.class.getCanonicalName());
    private final File outputDir;

    public ClassDumper() {
        String property = System.getProperty("graft.classdump.dir");
        if (property != null) {
            this.outputDir = new File(property);
            log.info("Dumping classes to " + outputDir.getAbsolutePath());
        } else {
            this.outputDir = null;
        }
    }

    @Override
    public void registerWith(Instrumentation inst) {
        if (outputDir != null) {
            inst.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classByteCode) throws IllegalClassFormatException {
                    dumpClass(className, classByteCode);
                    return classByteCode;
                }
            });
        }
    }

    public void dumpClass(String name, byte[] classByteCode) {
        if (!name.startsWith("java/")) {
            File path = new File(outputDir, name + ".class");
            path.getParentFile().mkdirs();
            try (BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(path))) {
                bof.write(classByteCode);
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to write Java class dump", e);
            }
        }
    }

}
