package com.skcraft.graft;

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

public class ClassDumper {

    private static final Logger log = Logger.getLogger(ClassDumper.class.getCanonicalName());
    private static File outputDir;

    private static void dumpClass(String name, byte[] classByteCode) {
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

    public static void register(Instrumentation inst) {
        String property = System.getProperty("graft.classdump.dir");
        if (property != null) {
            outputDir = new File(property);

            inst.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classByteCode) throws IllegalClassFormatException {
                    dumpClass(className, classByteCode);
                    return classByteCode;
                }
            });

            log.info("Dumping classes to " + outputDir.getAbsolutePath());
        }
    }

}
