package dev.aetherclient.agent;

import dev.aetherclient.agent.transformer.AetherClassTransformer;
import dev.aetherclient.bridge.AetherBridge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AetherClassTransformerTest {

    @AfterEach
    void cleanup() {
        AetherBridge.clearHooks();
    }

    @Test
    void transformsAllMinecraft12111HooksWithoutVerifyErrorsAndDispatchesCorrectArgs() throws Exception {
        byte[] dummyTracy = emptyClass("net/minecraft/class_10219");
        byte[] dummyKeyInput = emptyClass("net/minecraft/class_11908");
        byte[] dummyCharInput = emptyClass("net/minecraft/class_11905");
        byte[] dummyMouseInput = emptyClass("net/minecraft/class_11910");

        byte[] windowRaw = classWithCounterMethod(
                "net/minecraft/class_1041",
                "method_15998",
                "(Lnet/minecraft/class_10219;)V"
        );
        byte[] keyboardRaw = keyboardClassWithKeyAndChar();
        byte[] mouseRaw = mouseClassWithThreeMethods();
        byte[] mcRaw = classWithCounterMethod(
                "net/minecraft/class_310",
                "method_1574",
                "()V"
        );

        ByteClassLoader loader = new ByteClassLoader(getClass().getClassLoader());
        loader.define("net.minecraft.class_10219", dummyTracy);
        loader.define("net.minecraft.class_11908", dummyKeyInput);
        loader.define("net.minecraft.class_11905", dummyCharInput);
        loader.define("net.minecraft.class_11910", dummyMouseInput);

        AetherClassTransformer transformer = new AetherClassTransformer();
        byte[] windowTx = transformer.transform(loader, "net/minecraft/class_1041", null, null, windowRaw);
        byte[] keyboardTx = transformer.transform(loader, "net/minecraft/class_309", null, null, keyboardRaw);
        byte[] mouseTx = transformer.transform(loader, "net/minecraft/class_312", null, null, mouseRaw);
        byte[] mcTx = transformer.transform(loader, "net/minecraft/class_310", null, null, mcRaw);

        assertNotNull(windowTx);
        assertNotNull(keyboardTx);
        assertNotNull(mouseTx);
        assertNotNull(mcTx);

        // Second transform pass must be a strict idempotent no-op (returns null)
        assertNull(transformer.transform(loader, "net/minecraft/class_1041", null, null, windowTx));
        assertNull(transformer.transform(loader, "net/minecraft/class_309", null, null, keyboardTx));
        assertNull(transformer.transform(loader, "net/minecraft/class_312", null, null, mouseTx));
        assertNull(transformer.transform(loader, "net/minecraft/class_310", null, null, mcTx));

        verifyBytecode(windowTx, loader);
        verifyBytecode(keyboardTx, loader);
        verifyBytecode(mouseTx, loader);
        verifyBytecode(mcTx, loader);

        Class<?> winCls = loader.define("net.minecraft.class_1041", windowTx);
        Class<?> kbCls = loader.define("net.minecraft.class_309", keyboardTx);
        Class<?> msCls = loader.define("net.minecraft.class_312", mouseTx);
        Class<?> mcCls = loader.define("net.minecraft.class_310", mcTx);

        AtomicBoolean frameTickArg = new AtomicBoolean(false);
        AtomicInteger frameCalls = new AtomicInteger(0);
        AtomicBoolean cancelNext = new AtomicBoolean(true);
        AtomicInteger keyActionSeen = new AtomicInteger(-1);
        AtomicInteger charCalls = new AtomicInteger(0);
        AtomicReference<Double> cursorXSeen = new AtomicReference<>(0.0);
        AtomicReference<Double> cursorYSeen = new AtomicReference<>(0.0);
        AtomicInteger tickCalls = new AtomicInteger(0);

        AetherBridge.registerHooks(
                (rtc, tick) -> {
                    frameCalls.incrementAndGet();
                    frameTickArg.set(tick);
                },
                (dc, rtc) -> {},
                mc -> tickCalls.incrementAndGet(),
                (win, action, keyInput) -> {
                    keyActionSeen.set(action);
                    return cancelNext.get();
                },
                (win, mouseInput, action) -> cancelNext.get(),
                (win, x, y) -> {
                    cursorXSeen.set(x);
                    cursorYSeen.set(y);
                },
                (win, h, v) -> cancelNext.get(),
                (win, charInput) -> {
                    charCalls.incrementAndGet();
                    return cancelNext.get();
                }
        );

        Object tracy = loader.loadClass("net.minecraft.class_10219").getConstructor().newInstance();
        Object win = winCls.getConstructor().newInstance();
        winCls.getMethod("method_15998", tracy.getClass()).invoke(win, tracy);
        assertEquals(1, frameCalls.get());
        assertTrue(frameTickArg.get());

        Object ki = loader.loadClass("net.minecraft.class_11908").getConstructor().newInstance();
        Object ci = loader.loadClass("net.minecraft.class_11905").getConstructor().newInstance();
        Object kb = kbCls.getConstructor().newInstance();
        kbCls.getMethod("method_1466", long.class, int.class, ki.getClass()).invoke(kb, 99L, 1, ki);
        kbCls.getMethod("method_1457", long.class, ci.getClass()).invoke(kb, 99L, ci);
        assertEquals(1, keyActionSeen.get());
        assertEquals(1, charCalls.get());
        assertEquals(0, kbCls.getField("calls").getInt(kb), "Cancelled key and char events should short-circuit body");

        cancelNext.set(false);
        kbCls.getMethod("method_1466", long.class, int.class, ki.getClass()).invoke(kb, 99L, 0, ki);
        kbCls.getMethod("method_1457", long.class, ci.getClass()).invoke(kb, 99L, ci);
        assertEquals(2, kbCls.getField("calls").getInt(kb), "Uncancelled key and char events should run body");

        Object mi = loader.loadClass("net.minecraft.class_11910").getConstructor().newInstance();
        Object ms = msCls.getConstructor().newInstance();
        msCls.getMethod("method_1600", long.class, double.class, double.class).invoke(ms, 7L, 320.5, 180.25);
        assertEquals(320.5, cursorXSeen.get(), 0.0001);
        assertEquals(180.25, cursorYSeen.get(), 0.0001);
        msCls.getMethod("method_1601", long.class, mi.getClass(), int.class).invoke(ms, 7L, mi, 1);
        msCls.getMethod("method_1598", long.class, double.class, double.class).invoke(ms, 7L, 0.0, -1.0);
        assertEquals(3, msCls.getField("calls").getInt(ms));

        cancelNext.set(true);
        msCls.getMethod("method_1601", long.class, mi.getClass(), int.class).invoke(ms, 7L, mi, 1);
        msCls.getMethod("method_1598", long.class, double.class, double.class).invoke(ms, 7L, 0.0, -1.0);
        assertEquals(3, msCls.getField("calls").getInt(ms), "Cancelled mouse events should short-circuit body");

        Object mc = mcCls.getConstructor().newInstance();
        mcCls.getMethod("method_1574").invoke(mc);
        assertEquals(1, tickCalls.get());
    }

    @Test
    void transformsScoreboardAndTabListSuppression() throws Exception {
        byte[] dummyCtx = emptyClass("net/minecraft/class_332");
        byte[] dummyObjective = emptyClass("net/minecraft/class_266");
        byte[] dummyDim = emptyClass("net/minecraft/class_269");

        byte[] hudRaw = classWithCounterMethod(
                "net/minecraft/class_329",
                "method_1757",
                "(Lnet/minecraft/class_332;Lnet/minecraft/class_266;)V"
        );
        byte[] listRaw = classWithCounterMethod(
                "net/minecraft/class_355",
                "method_1919",
                "(Lnet/minecraft/class_332;ILnet/minecraft/class_269;Lnet/minecraft/class_266;)V"
        );

        ByteClassLoader loader = new ByteClassLoader(getClass().getClassLoader());
        loader.define("net.minecraft.class_332", dummyCtx);
        loader.define("net.minecraft.class_266", dummyObjective);
        loader.define("net.minecraft.class_269", dummyDim);

        AetherClassTransformer transformer = new AetherClassTransformer();
        byte[] hudTx = transformer.transform(loader, "net/minecraft/class_329", null, null, hudRaw);
        byte[] listTx = transformer.transform(loader, "net/minecraft/class_355", null, null, listRaw);
        assertNotNull(hudTx);
        assertNotNull(listTx);

        // Strict idempotency: second pass is a no-op
        assertNull(transformer.transform(loader, "net/minecraft/class_329", null, null, hudTx));
        assertNull(transformer.transform(loader, "net/minecraft/class_355", null, null, listTx));

        verifyBytecode(hudTx, loader);
        verifyBytecode(listTx, loader);

        Class<?> hudCls = loader.define("net.minecraft.class_329", hudTx);
        Class<?> listCls = loader.define("net.minecraft.class_355", listTx);

        Object ctx = loader.loadClass("net.minecraft.class_332").getConstructor().newInstance();
        Object objective = loader.loadClass("net.minecraft.class_266").getConstructor().newInstance();
        Object dim = loader.loadClass("net.minecraft.class_269").getConstructor().newInstance();

        Object hud = hudCls.getConstructor().newInstance();
        AetherBridge.setHideScoreboard(false);
        hudCls.getMethod("method_1757", ctx.getClass(), objective.getClass()).invoke(hud, ctx, objective);
        assertEquals(1, hudCls.getField("calls").getInt(hud), "Scoreboard renders when not hidden");

        AetherBridge.setHideScoreboard(true);
        hudCls.getMethod("method_1757", ctx.getClass(), objective.getClass()).invoke(hud, ctx, objective);
        assertEquals(1, hudCls.getField("calls").getInt(hud), "Scoreboard suppressed when hidden");

        Object list = listCls.getConstructor().newInstance();
        AetherBridge.setHideTabList(false);
        listCls.getMethod("method_1919", ctx.getClass(), int.class, dim.getClass(), objective.getClass())
                .invoke(list, ctx, 0, dim, objective);
        assertEquals(1, listCls.getField("calls").getInt(list), "Tab list renders when not hidden");

        AetherBridge.setHideTabList(true);
        listCls.getMethod("method_1919", ctx.getClass(), int.class, dim.getClass(), objective.getClass())
                .invoke(list, ctx, 0, dim, objective);
        assertEquals(1, listCls.getField("calls").getInt(list), "Tab list suppressed when hidden");
    }

    @Test
    void transformsItemEntityRendererSpinInjection() throws Exception {
        byte[] qfc = stubInterface("org/joml/Quaternionfc");
        byte[] qf = stubClass("org/joml/Quaternionf", "java/lang/Object", new String[]{"org/joml/Quaternionfc"});
        byte[] axis = rotationAxisStub();
        byte[] mstack = matrixStackStub();
        byte[] state = stubClass("net/minecraft/class_10039", "java/lang/Object", null);
        byte[] vcp = stubClass("net/minecraft/class_11659", "java/lang/Object", null);
        byte[] light = stubClass("net/minecraft/class_12075", "java/lang/Object", null);

        byte[] itemRendererRaw = classWithCounterMethod(
                "net/minecraft/class_916",
                "method_3996",
                "(Lnet/minecraft/class_10039;Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;Lnet/minecraft/class_12075;)V"
        );

        ByteClassLoader loader = new ByteClassLoader(getClass().getClassLoader());
        loader.define("org.joml.Quaternionfc", qfc);
        loader.define("org.joml.Quaternionf", qf);
        loader.define("net.minecraft.class_7833", axis);
        loader.define("net.minecraft.class_4587", mstack);
        loader.define("net.minecraft.class_10039", state);
        loader.define("net.minecraft.class_11659", vcp);
        loader.define("net.minecraft.class_12075", light);

        AetherClassTransformer transformer = new AetherClassTransformer();
        byte[] tx = transformer.transform(loader, "net/minecraft/class_916", null, null, itemRendererRaw);
        assertNotNull(tx, "ItemEntityRenderer should be transformed for spin injection");
        assertNull(transformer.transform(loader, "net/minecraft/class_916", null, null, tx), "Spin injection must be idempotent");

        verifyBytecode(tx, loader);

        // Spin-on path is injected: assert the bridge call + matrix multiply are present in the method.
        assertHasMethodCall(tx, "dev/aetherclient/bridge/AetherBridge", "getItemSpinAngle", "()F");
        assertHasMethodCall(tx, "net/minecraft/class_4587", "method_22907", "(Lorg/joml/Quaternionfc;)V");

        Class<?> rendererCls = loader.define("net.minecraft.class_916", tx);
        Object renderer = rendererCls.getConstructor().newInstance();
        Object stateObj = loader.loadClass("net.minecraft.class_10039").getConstructor().newInstance();
        Object mstackObj = loader.loadClass("net.minecraft.class_4587").getConstructor().newInstance();
        Object vcpObj = loader.loadClass("net.minecraft.class_11659").getConstructor().newInstance();
        Object lightObj = loader.loadClass("net.minecraft.class_12075").getConstructor().newInstance();

        // Spin off (angle 0): conditional short-circuits the multiply, body still runs.
        AetherBridge.setItemSpinAngle(0f);
        rendererCls.getMethod("method_3996", stateObj.getClass(), mstackObj.getClass(), vcpObj.getClass(), lightObj.getClass())
                .invoke(renderer, stateObj, mstackObj, vcpObj, lightObj);
        assertEquals(1, rendererCls.getField("calls").getInt(renderer), "Item render body runs with spin off");
    }

    private static void assertHasMethodCall(byte[] bytes, String owner, String name, String desc) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);
        for (MethodNode mn : cn.methods) {
            for (AbstractInsnNode insn : mn.instructions) {
                if (insn instanceof MethodInsnNode min
                        && owner.equals(min.owner) && name.equals(min.name) && desc.equals(min.desc)) {
                    return;
                }
            }
        }
        fail("Expected injected method call " + owner + "." + name + desc);
    }

    private static byte[] stubInterface(String name) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, name, null, "java/lang/Object", null);
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] stubClass(String name, String superName, String[] interfaces) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, name, null, superName, interfaces);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] rotationAxisStub() {
        String n = "net/minecraft/class_7833";
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, n, null, "java/lang/Object", null);
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "field_40714", "Lnet/minecraft/class_7833;", null, null).visitEnd();
        MethodVisitor rot = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "rotation", "(F)Lorg/joml/Quaternionf;", null, null);
        rot.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] matrixStackStub() {
        String n = "net/minecraft/class_4587";
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, n, null, "java/lang/Object", null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        MethodVisitor mult = cw.visitMethod(Opcodes.ACC_PUBLIC, "method_22907", "(Lorg/joml/Quaternionfc;)V", null, null);
        mult.visitCode();
        mult.visitInsn(Opcodes.RETURN);
        mult.visitMaxs(0, 0);
        mult.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void verifyBytecode(byte[] bytes, ClassLoader loader) {
        StringWriter sw = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(bytes), loader, false, new PrintWriter(sw));
        assertTrue(sw.toString().isEmpty(), "ASM CheckClassAdapter reported errors:\n" + sw);
    }

    private static byte[] emptyClass(String internalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] classWithCounterMethod(String internalName, String methodName, String desc) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        cw.visitField(Opcodes.ACC_PUBLIC, "calls", "I", null, 0).visitEnd();

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, desc, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.DUP);
        mv.visitFieldInsn(Opcodes.GETFIELD, internalName, "calls", "I");
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IADD);
        mv.visitFieldInsn(Opcodes.PUTFIELD, internalName, "calls", "I");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] mouseClassWithThreeMethods() {
        String name = "net/minecraft/class_312";
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        cw.visitField(Opcodes.ACC_PUBLIC, "calls", "I", null, 0).visitEnd();

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        String[][] methods = {
                {"method_1601", "(JLnet/minecraft/class_11910;I)V"},
                {"method_1600", "(JDD)V"},
                {"method_1598", "(JDD)V"}
        };
        for (String[] m : methods) {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, m[0], m[1], null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.DUP);
            mv.visitFieldInsn(Opcodes.GETFIELD, name, "calls", "I");
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitInsn(Opcodes.IADD);
            mv.visitFieldInsn(Opcodes.PUTFIELD, name, "calls", "I");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] keyboardClassWithKeyAndChar() {
        String name = "net/minecraft/class_309";
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        cw.visitField(Opcodes.ACC_PUBLIC, "calls", "I", null, 0).visitEnd();

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        String[][] methods = {
                {"method_1466", "(JILnet/minecraft/class_11908;)V"},
                {"method_1457", "(JLnet/minecraft/class_11905;)V"}
        };
        for (String[] m : methods) {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, m[0], m[1], null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.DUP);
            mv.visitFieldInsn(Opcodes.GETFIELD, name, "calls", "I");
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitInsn(Opcodes.IADD);
            mv.visitFieldInsn(Opcodes.PUTFIELD, name, "calls", "I");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static final class ByteClassLoader extends ClassLoader {
        ByteClassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(String binaryName, byte[] bytes) {
            return defineClass(binaryName, bytes, 0, bytes.length);
        }
    }
}
