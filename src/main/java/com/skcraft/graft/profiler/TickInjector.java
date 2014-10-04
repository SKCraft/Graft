package com.skcraft.graft.profiler;

import com.skcraft.graft.profiler.timing.DummyTimingContext;
import com.skcraft.graft.profiler.timing.TimingContext;
import com.skcraft.graft.util.ASMUtils;
import com.skcraft.graft.util.SimpleLogFormatter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

public class TickInjector {

    private static final String TIME_TILE_ENTITY = "tickInjector$timeTileEntity";
    private static final String TIME_ENTITY = "tickInjector$timeEntity";

    private static final Logger log = Logger.getLogger(TickInjector.class.getCanonicalName());
    private static TickInjector instance;

    private final SpecificProfiler specificProfiler = SpecificProfiler.getInstance();

    private TickInjector() {
    }

    @SuppressWarnings("unchecked")
    private static byte[] transformWorld(byte[] buffer) {
        SimpleLogFormatter.configureGlobalLogger();

        ClassReader cr = new ClassReader(buffer);
        ClassNode node = new ClassNode();
        cr.accept(node, 0);

        node.methods.add(createTimeTileEntityMethod());
        node.methods.add(createTimeEntityMethod());

        MethodNode method = ASMUtils.findMethod(node, "func_72939_s", "()V");
        ListIterator<AbstractInsnNode> it = method.instructions.iterator();

        while (it.hasNext()) {
            AbstractInsnNode insn = it.next();

            if (ASMUtils.isMethodCall(insn, "net/minecraft/tileentity/TileEntity", "func_145845_h", "()V")) {
                log.info("Hooking into tile entity tick...");

                it.remove();

                it.add(new InsnNode(Opcodes.DUP));
                it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/world/World", TIME_TILE_ENTITY,
                        "(Lnet/minecraft/tileentity/TileEntity;)Lcom/skcraft/graft/profiler/timing/TimingContext;"));
                it.add(new InsnNode(Opcodes.SWAP));
                it.add(insn);
                it.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "com/skcraft/graft/profiler/timing/TimingContext", "stop", "()V"));

            } else if (ASMUtils.isMethodCall(insn, "net/minecraft/world/World", "func_72870_g", "(Lnet/minecraft/entity/Entity;)V")) {
                log.info("Hooking into entity tick...");

                it.remove();

                it.add(new InsnNode(Opcodes.DUP));
                it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/world/World", TIME_ENTITY,
                        "(Lnet/minecraft/entity/Entity;)Lcom/skcraft/graft/profiler/timing/TimingContext;"));
                it.add(new InsnNode(Opcodes.SWAP));
                it.add(new VarInsnNode(Opcodes.ALOAD, 0));
                it.add(new InsnNode(Opcodes.SWAP));
                it.add(insn);
                it.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "com/skcraft/graft/profiler/timing/TimingContext", "stop", "()V"));
                it.add(new InsnNode(Opcodes.POP));
            }
        }

        ClassWriter cw = new ClassWriter(COMPUTE_FRAMES | COMPUTE_MAXS);
        node.accept(cw);
        return cw.toByteArray();
    }

    private static MethodNode createTimeTileEntityMethod() {
        MethodNode m = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, TIME_TILE_ENTITY,
                "(Lnet/minecraft/tileentity/TileEntity;)Lcom/skcraft/graft/profiler/timing/TimingContext;", null, null);
        InsnList inst = m.instructions;
        inst.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inst.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inst.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/tileentity/TileEntity", "func_145831_w", "()Lnet/minecraft/world/World;"));
        inst.add(createWorldNameInsnList());
        inst.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inst.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/tileentity/TileEntity", "field_145851_c", "I"));
        inst.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inst.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/tileentity/TileEntity", "field_145848_d", "I"));
        inst.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inst.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/tileentity/TileEntity", "field_145849_e", "I"));
        inst.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/skcraft/graft/profiler/TickInjector", "timeTileEntity",
                "(Ljava/lang/Object;Ljava/lang/String;III)Lcom/skcraft/graft/profiler/timing/TimingContext;"));
        inst.add(new InsnNode(Opcodes.ARETURN));
        return m;
    }

    private static MethodNode createTimeEntityMethod() {
        MethodNode m = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, TIME_ENTITY,
                "(Lnet/minecraft/entity/Entity;)Lcom/skcraft/graft/profiler/timing/TimingContext;", null, null);
        InsnList inst = m.instructions;
        inst.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inst.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inst.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/entity/Entity", "field_70170_p", "Lnet/minecraft/world/World;"));
        inst.add(createWorldNameInsnList());
        inst.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inst.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/entity/Entity", "field_70165_t", "D"));
        inst.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inst.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/entity/Entity", "field_70163_u", "D"));
        inst.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inst.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/entity/Entity", "field_70161_v", "D"));
        inst.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/skcraft/graft/profiler/TickInjector", "timeEntity",
                "(Ljava/lang/Object;Ljava/lang/String;DDD)Lcom/skcraft/graft/profiler/timing/TimingContext;"));
        inst.add(new InsnNode(Opcodes.ARETURN));
        return m;
    }

    private static InsnList createWorldNameInsnList() {
        InsnList inst = new InsnList();
        inst.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/world/World", "func_72912_H", "()Lnet/minecraft/world/storage/WorldInfo;"));
        inst.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/world/storage/WorldInfo", "func_76065_j", "()Ljava/lang/String;"));
        return inst;
    }

    public TimingContext timeObject(final String className, final String world, final int x, final int y, final int z) {
        if (specificProfiler.isEnabled()) {
            return new TimingContext() {
                private final long start = System.nanoTime();

                @Override
                public void stop() {
                    specificProfiler.record(className, world, x, y, z, System.nanoTime() - start);
                }
            };
        } else {
            return DummyTimingContext.getInstance();
        }
    }

    public static TimingContext timeTileEntity(Object object, String world, int x, int y, int z) {
        if (object == null) {
            return DummyTimingContext.getInstance();
        }
        return getInstance().timeObject(object.getClass().getName(), world, x, y, z);
    }

    public static TimingContext timeEntity(Object object, String world, double x, double y, double z) {
        if (object == null) {
            return DummyTimingContext.getInstance();
        }
        return getInstance().timeObject(object.getClass().getName(), world, (int) x, (int) y, (int) z);
    }

    public static void register(Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                try {
                    if (className.equals("net/minecraft/world/World")) {
                        return transformWorld(classfileBuffer);
                    }
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Failed to transform", t);
                }

                return classfileBuffer;
            }
        });
    }

    private static TickInjector getInstance() {
        if (instance == null) {
            synchronized (TickInjector.class) {
                //noinspection ConstantConditions
                if (instance == null) {
                    instance = new TickInjector();
                }
            }
        }

        return instance;
    }

}
