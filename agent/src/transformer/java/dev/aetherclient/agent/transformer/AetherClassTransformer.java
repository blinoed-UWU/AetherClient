package dev.aetherclient.agent.transformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Set;

/**
 * Bytecode transformer for Minecraft 1.21.11 Fabric (intermediary mappings) that routes
 * frame rendering and input events into Bootstrap-resident AetherBridge.
 */
public final class AetherClassTransformer implements ClassFileTransformer {
    private static final String BRIDGE = "dev/aetherclient/bridge/AetherBridge";

    public static final String TARGET_WINDOW = "net/minecraft/class_1041";
    public static final String TARGET_KEYBOARD = "net/minecraft/class_309";
    public static final String TARGET_MOUSE = "net/minecraft/class_312";
    public static final String TARGET_MINECRAFT = "net/minecraft/class_310";
    public static final String TARGET_INGAME_HUD = "net/minecraft/class_329";
    public static final String TARGET_PLAYER_LIST_HUD = "net/minecraft/class_355";
    public static final String TARGET_ITEM_ENTITY_RENDERER = "net/minecraft/class_916";
    public static final String TARGET_HELD_ITEM_RENDERER = "net/minecraft/class_759";
    public static final String TARGET_SKY_RENDERING = "net/minecraft/class_9975";

    private static final Set<String> TARGETS = Set.of(
            TARGET_WINDOW,
            TARGET_KEYBOARD,
            TARGET_MOUSE,
            TARGET_MINECRAFT,
            TARGET_INGAME_HUD,
            TARGET_PLAYER_LIST_HUD,
            TARGET_ITEM_ENTITY_RENDERER,
            TARGET_HELD_ITEM_RENDERER,
            TARGET_SKY_RENDERING
    );

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer
    ) {
        if (className == null || !TARGETS.contains(className)) {
            return null;
        }
        try {
            return transformBytes(className, classfileBuffer, loader);
        } catch (Throwable t) {
            System.err.println("[AetherAgent] Failed to transform " + className + ": " + t.getMessage());
            t.printStackTrace(System.err);
            return null;
        }
    }

    public byte[] transformBytes(String internalName, byte[] classfileBuffer, ClassLoader targetLoader) {
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassLoaderAwareWriter(cr, targetLoader);
        int[] hooksApplied = {0};

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor downstream = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (downstream == null) return null;

                AdviceFactory adapterFactory = selectHook(internalName, name, descriptor);
                if (adapterFactory == null) {
                    return downstream;
                }

                // Buffer via MethodNode first to guarantee strict idempotency across retransformations
                return new MethodNode(Opcodes.ASM9, access, name, descriptor, signature, exceptions) {
                    @Override
                    public void visitEnd() {
                        super.visitEnd();
                        if (alreadyInjected(this)) {
                            accept(downstream);
                        } else {
                            hooksApplied[0]++;
                            MethodVisitor advised = adapterFactory.create(downstream, access, name, descriptor);
                            accept(advised);
                        }
                    }
                };
            }
        };

        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        if (hooksApplied[0] == 0) {
            return null;
        }
        return cw.toByteArray();
    }

    @FunctionalInterface
    private interface AdviceFactory {
        MethodVisitor create(MethodVisitor mv, int access, String name, String desc);
    }

    private static AdviceFactory selectHook(String internalName, String name, String descriptor) {
        return switch (internalName) {
            case TARGET_WINDOW -> ("method_15998".equals(name) && "(Lnet/minecraft/class_10219;)V".equals(descriptor))
                    ? FrameRenderAdapter::new
                    : null;
            case TARGET_KEYBOARD -> {
                if ("method_1466".equals(name) && "(JILnet/minecraft/class_11908;)V".equals(descriptor)) {
                    yield KeyboardAdapter::new;
                }
                if ("method_1457".equals(name) && "(JLnet/minecraft/class_11905;)V".equals(descriptor)) {
                    yield CharTypedAdapter::new;
                }
                yield null;
            }
            case TARGET_MOUSE -> {
                if ("method_1601".equals(name) && "(JLnet/minecraft/class_11910;I)V".equals(descriptor)) {
                    yield MouseButtonAdapter::new;
                }
                if ("method_1600".equals(name) && "(JDD)V".equals(descriptor)) {
                    yield CursorPosAdapter::new;
                }
                if ("method_1598".equals(name) && "(JDD)V".equals(descriptor)) {
                    yield MouseScrollAdapter::new;
                }
                yield null;
            }
            case TARGET_MINECRAFT -> ("method_1574".equals(name) && "()V".equals(descriptor))
                    ? ClientTickAdapter::new
                    : null;
            case TARGET_INGAME_HUD -> {
                if ("method_1757".equals(name) && "(Lnet/minecraft/class_332;Lnet/minecraft/class_266;)V".equals(descriptor)) {
                    yield (mv, a, n, d) -> new SuppressAdapter(mv, a, n, d, "shouldHideScoreboard");
                }
                if ("method_55803".equals(name) && "(Lnet/minecraft/class_332;Lnet/minecraft/class_9779;)V".equals(descriptor)) {
                    yield (mv, a, n, d) -> new SuppressAdapter(mv, a, n, d, "shouldHideScoreboard");
                }
                if ("method_1736".equals(name)) {
                    yield (mv, a, n, d) -> new SuppressAdapter(mv, a, n, d, "shouldHideCrosshair");
                }
                yield null;
            }
            case TARGET_PLAYER_LIST_HUD -> ("method_1919".equals(name) && "(Lnet/minecraft/class_332;ILnet/minecraft/class_269;Lnet/minecraft/class_266;)V".equals(descriptor))
                    ? (mv, a, n, d) -> new SuppressAdapter(mv, a, n, d, "shouldHideTabList")
                    : null;
            case TARGET_ITEM_ENTITY_RENDERER -> ("method_3996".equals(name) && "(Lnet/minecraft/class_10039;Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;Lnet/minecraft/class_12075;)V".equals(descriptor))
                    ? ItemSpinAdapter::new
                    : null;
            case TARGET_HELD_ITEM_RENDERER -> {
                if ("method_3217".equals(name) && "(Lnet/minecraft/class_4587;Lnet/minecraft/class_1306;F)V".equals(descriptor)) {
                    yield SwingOffsetAdapter::new;
                }
                if ("method_3224".equals(name) && "(Lnet/minecraft/class_4587;Lnet/minecraft/class_1306;F)V".equals(descriptor)) {
                    yield EquipOffsetAdapter::new;
                }
                yield null;
            }
            case TARGET_SKY_RENDERING -> {
                if ("method_74926".equals(name) && "(Lnet/minecraft/class_638;FLnet/minecraft/class_4184;Lnet/minecraft/class_12076;)V".equals(descriptor)) {
                    yield SkyStateUpdateAdapter::new;
                }
                if ("method_62302".equals(name) && "(I)V".equals(descriptor)) {
                    yield SkyColorTopAdapter::new;
                }
                if ("method_62306".equals(name) && "(Lnet/minecraft/class_4587;FI)V".equals(descriptor)) {
                    yield SkyColorGlowAdapter::new;
                }
                yield null;
            }
            default -> null;
        };
    }

    private static boolean alreadyInjected(MethodNode mn) {
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode min && BRIDGE.equals(min.owner)) {
                return true;
            }
        }
        return false;
    }

    private static final class FrameRenderAdapter extends AdviceAdapter {
        FrameRenderAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            super.visitVarInsn(ALOAD, 1);
            super.visitInsn(ICONST_1);
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, "dispatchFrameRender", "(Ljava/lang/Object;Z)V", false);
        }
    }

    private static final class KeyboardAdapter extends AdviceAdapter {
        KeyboardAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            super.visitVarInsn(LLOAD, 1);
            super.visitVarInsn(ILOAD, 3);
            super.visitVarInsn(ALOAD, 4);
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, "dispatchKey", "(JILjava/lang/Object;)Z", false);
            Label cont = new Label();
            super.visitJumpInsn(IFEQ, cont);
            super.visitInsn(RETURN);
            super.visitLabel(cont);
        }
    }

    private static final class CharTypedAdapter extends AdviceAdapter {
        CharTypedAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            super.visitVarInsn(LLOAD, 1);
            super.visitVarInsn(ALOAD, 3);
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, "dispatchChar", "(JLjava/lang/Object;)Z", false);
            Label cont = new Label();
            super.visitJumpInsn(IFEQ, cont);
            super.visitInsn(RETURN);
            super.visitLabel(cont);
        }
    }

    private static final class MouseButtonAdapter extends AdviceAdapter {
        MouseButtonAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            super.visitVarInsn(LLOAD, 1);
            super.visitVarInsn(ALOAD, 3);
            super.visitVarInsn(ILOAD, 4);
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, "dispatchMouseButton", "(JLjava/lang/Object;I)Z", false);
            Label cont = new Label();
            super.visitJumpInsn(IFEQ, cont);
            super.visitInsn(RETURN);
            super.visitLabel(cont);
        }
    }

    private static final class CursorPosAdapter extends AdviceAdapter {
        CursorPosAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            super.visitVarInsn(LLOAD, 1);
            super.visitVarInsn(DLOAD, 3);
            super.visitVarInsn(DLOAD, 5);
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, "dispatchCursorPos", "(JDD)V", false);
        }
    }

    private static final class MouseScrollAdapter extends AdviceAdapter {
        MouseScrollAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            super.visitVarInsn(LLOAD, 1);
            super.visitVarInsn(DLOAD, 3);
            super.visitVarInsn(DLOAD, 5);
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, "dispatchMouseScroll", "(JDD)Z", false);
            Label cont = new Label();
            super.visitJumpInsn(IFEQ, cont);
            super.visitInsn(RETURN);
            super.visitLabel(cont);
        }
    }

    private static final class ClientTickAdapter extends AdviceAdapter {
        ClientTickAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            super.visitVarInsn(ALOAD, 0);
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, "dispatchClientTick", "(Ljava/lang/Object;)V", false);
        }
    }

    private static final class SuppressAdapter extends AdviceAdapter {
        private final String bridgeMethod;

        SuppressAdapter(MethodVisitor mv, int access, String name, String desc, String bridgeMethod) {
            super(Opcodes.ASM9, mv, access, name, desc);
            this.bridgeMethod = bridgeMethod;
        }

        @Override
        protected void onMethodEnter() {
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, bridgeMethod, "()Z", false);
            Label cont = new Label();
            super.visitJumpInsn(IFEQ, cont);
            super.visitInsn(RETURN);
            super.visitLabel(cont);
        }
    }

    /**
     * Applies a client-side Y-axis spin to dropped item entities by pre-multiplying the model
     * matrix with RotationAxis.POSITIVE_Y.rotation(AetherBridge.getItemSpinAngle()). The angle is
     * 0 when the ItemPhysics module is off, making the transform a cheap identity.
     */
    private static final class ItemSpinAdapter extends AdviceAdapter {
        ItemSpinAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            // if (AetherBridge.getItemSpinAngle() != 0f) { matrices.multiply(POSITIVE_Y.rotation(angle)); }
            Label cont = new Label();
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, "getItemSpinAngle", "()F", false);
            super.visitInsn(FCONST_0);
            super.visitInsn(FCMPL);
            super.visitJumpInsn(IFEQ, cont);
            super.visitVarInsn(ALOAD, 2); // matrices (class_4587)
            super.visitFieldInsn(GETSTATIC, "net/minecraft/class_7833", "field_40714", "Lnet/minecraft/class_7833;");
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, "getItemSpinAngle", "()F", false);
            super.visitMethodInsn(INVOKEINTERFACE, "net/minecraft/class_7833", "rotation", "(F)Lorg/joml/Quaternionf;", true);
            super.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/class_4587", "method_22907", "(Lorg/joml/Quaternionfc;)V", false);
            super.visitLabel(cont);
        }
    }

    private static final class SwingOffsetAdapter extends AdviceAdapter {
        SwingOffsetAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            super.visitVarInsn(ALOAD, 1);
            super.visitVarInsn(ALOAD, 2);
            super.visitVarInsn(FLOAD, 3);
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, "dispatchSwingOffset", "(Ljava/lang/Object;Ljava/lang/Object;F)Z", false);
            Label cont = new Label();
            super.visitJumpInsn(IFEQ, cont);
            super.visitInsn(RETURN);
            super.visitLabel(cont);
        }
    }

    private static final class EquipOffsetAdapter extends AdviceAdapter {
        EquipOffsetAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            super.visitVarInsn(ALOAD, 1);
            super.visitVarInsn(ALOAD, 2);
            super.visitVarInsn(FLOAD, 3);
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, "dispatchEquipOffset", "(Ljava/lang/Object;Ljava/lang/Object;F)Z", false);
            Label cont = new Label();
            super.visitJumpInsn(IFEQ, cont);
            super.visitInsn(RETURN);
            super.visitLabel(cont);
        }
    }

    private static final class SkyColorTopAdapter extends AdviceAdapter {
        SkyColorTopAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            super.visitVarInsn(ILOAD, 1);
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, "getCustomSkyColor", "(I)I", false);
            super.visitVarInsn(ISTORE, 1);
        }
    }

    private static final class SkyColorGlowAdapter extends AdviceAdapter {
        SkyColorGlowAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            super.visitVarInsn(ILOAD, 3);
            super.visitMethodInsn(INVOKESTATIC, BRIDGE, "getCustomSkyColor", "(I)I", false);
            super.visitVarInsn(ISTORE, 3);
        }
    }

    private static final class SkyStateUpdateAdapter extends AdviceAdapter {
        SkyStateUpdateAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }

        @Override
        protected void onMethodExit(int opcode) {
            if (opcode == RETURN) {
                super.visitVarInsn(ALOAD, 4);
                super.visitInsn(DUP);
                super.visitFieldInsn(GETFIELD, "net/minecraft/class_12076", "field_63097", "I");
                super.visitMethodInsn(INVOKESTATIC, BRIDGE, "getCustomSkyColor", "(I)I", false);
                super.visitFieldInsn(PUTFIELD, "net/minecraft/class_12076", "field_63097", "I");
            }
        }
    }

    private static final class ClassLoaderAwareWriter extends ClassWriter {
        private final ClassLoader targetLoader;

        ClassLoaderAwareWriter(ClassReader reader, ClassLoader targetLoader) {
            super(reader, COMPUTE_FRAMES | COMPUTE_MAXS);
            this.targetLoader = targetLoader != null ? targetLoader : ClassLoader.getSystemClassLoader();
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            try {
                Class<?> c1 = Class.forName(type1.replace('/', '.'), false, targetLoader);
                Class<?> c2 = Class.forName(type2.replace('/', '.'), false, targetLoader);
                if (c1.isAssignableFrom(c2)) return type1;
                if (c2.isAssignableFrom(c1)) return type2;
                if (c1.isInterface() || c2.isInterface()) return "java/lang/Object";
                do {
                    c1 = c1.getSuperclass();
                } while (c1 != null && !c1.isAssignableFrom(c2));
                return c1 != null ? c1.getName().replace('.', '/') : "java/lang/Object";
            } catch (Throwable ignored) {
                return "java/lang/Object";
            }
        }
    }
}
