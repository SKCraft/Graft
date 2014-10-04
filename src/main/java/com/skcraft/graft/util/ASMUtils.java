package com.skcraft.graft.util;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class ASMUtils {

    private ASMUtils() {
    }

    public static boolean isMethodCall(AbstractInsnNode insn, String owner, String name, String desc) {
        return insn instanceof MethodInsnNode
                && ((MethodInsnNode) insn).owner.equals(owner)
                && ((MethodInsnNode) insn).name.equals(name)
                && ((MethodInsnNode) insn).desc.equals(desc);
    }

    public static MethodNode findMethod(ClassNode node, String name, String desc) {
        for (MethodNode method : (Iterable<MethodNode>) node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return method;
            }
        }

        return null;
    }

}
