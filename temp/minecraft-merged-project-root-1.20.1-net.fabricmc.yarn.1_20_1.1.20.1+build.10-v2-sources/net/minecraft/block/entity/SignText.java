/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.block.entity;

import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.dynamic.Codecs;
import org.jetbrains.annotations.Nullable;

public class SignText {
    private static final Codec<Text[]> MESSAGES_CODEC = Codecs.STRINGIFIED_TEXT.listOf().comapFlatMap(messages -> {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * java.lang.UnsupportedOperationException
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.NewAnonymousArray.getDimSize(NewAnonymousArray.java:142)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.isNewArrayLambda(LambdaRewriter.java:466)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:420)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:176)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:107)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper.applyForwards(ExpressionRewriterHelper.java:12)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriterToArgs(AbstractMemberFunctionInvokation.java:101)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriter(AbstractMemberFunctionInvokation.java:88)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:105)
         *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn.rewriteExpressions(StructuredReturn.java:99)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewrite(LambdaRewriter.java:90)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.rewriteLambdas(Op04StructuredStatement.java:1137)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:912)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:538)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1050)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:261)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:143)
         *     at net.fabricmc.loom.decompilers.cfr.LoomCFRDecompiler.decompile(LoomCFRDecompiler.java:89)
         *     at net.fabricmc.loom.task.GenerateSourcesTask$DecompileAction.doDecompile(GenerateSourcesTask.java:272)
         *     at net.fabricmc.loom.task.GenerateSourcesTask$DecompileAction.execute(GenerateSourcesTask.java:237)
         *     at org.gradle.workers.internal.DefaultWorkerServer.execute(DefaultWorkerServer.java:63)
         *     at org.gradle.workers.internal.AbstractClassLoaderWorker$1.create(AbstractClassLoaderWorker.java:54)
         *     at org.gradle.workers.internal.AbstractClassLoaderWorker$1.create(AbstractClassLoaderWorker.java:48)
         *     at org.gradle.internal.classloader.ClassLoaderUtils.executeInClassloader(ClassLoaderUtils.java:100)
         *     at org.gradle.workers.internal.AbstractClassLoaderWorker.executeInClassLoader(AbstractClassLoaderWorker.java:48)
         *     at org.gradle.workers.internal.IsolatedClassloaderWorker.run(IsolatedClassloaderWorker.java:49)
         *     at org.gradle.workers.internal.IsolatedClassloaderWorker.run(IsolatedClassloaderWorker.java:30)
         *     at org.gradle.workers.internal.WorkerDaemonServer.run(WorkerDaemonServer.java:96)
         *     at org.gradle.workers.internal.WorkerDaemonServer.run(WorkerDaemonServer.java:65)
         *     at org.gradle.process.internal.worker.request.WorkerAction$1.call(WorkerAction.java:138)
         *     at org.gradle.process.internal.worker.child.WorkerLogEventListener.withWorkerLoggingProtocol(WorkerLogEventListener.java:41)
         *     at org.gradle.process.internal.worker.request.WorkerAction.run(WorkerAction.java:135)
         *     at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
         *     at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
         *     at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
         *     at java.base/java.lang.reflect.Method.invoke(Method.java:568)
         *     at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:36)
         *     at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
         *     at org.gradle.internal.remote.internal.hub.MessageHubBackedObjectConnection$DispatchWrapper.dispatch(MessageHubBackedObjectConnection.java:182)
         *     at org.gradle.internal.remote.internal.hub.MessageHubBackedObjectConnection$DispatchWrapper.dispatch(MessageHubBackedObjectConnection.java:164)
         *     at org.gradle.internal.remote.internal.hub.MessageHub$Handler.run(MessageHub.java:414)
         *     at org.gradle.internal.concurrent.ExecutorPolicy$CatchAndRecordFailures.onExecute(ExecutorPolicy.java:64)
         *     at org.gradle.internal.concurrent.ManagedExecutorImpl$1.run(ManagedExecutorImpl.java:49)
         *     at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
         *     at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
         *     at java.base/java.lang.Thread.run(Thread.java:842)
         */
        throw new IllegalStateException("Decompilation failed");
    }, messages -> List.of(messages[0], messages[1], messages[2], messages[3]));
    public static final Codec<SignText> CODEC = RecordCodecBuilder.create(instance -> instance.group(((MapCodec)MESSAGES_CODEC.fieldOf("messages")).forGetter(signText -> signText.messages), MESSAGES_CODEC.optionalFieldOf("filtered_messages").forGetter(SignText::getFilteredMessages), ((MapCodec)DyeColor.CODEC.fieldOf("color")).orElse(DyeColor.BLACK).forGetter(signText -> signText.color), ((MapCodec)Codec.BOOL.fieldOf("has_glowing_text")).orElse(false).forGetter(signText -> signText.glowing)).apply((Applicative<SignText, ?>)instance, SignText::create));
    public static final int field_43299 = 4;
    private final Text[] messages;
    private final Text[] filteredMessages;
    private final DyeColor color;
    private final boolean glowing;
    @Nullable
    private OrderedText[] orderedMessages;
    private boolean filtered;

    public SignText() {
        this(SignText.getDefaultText(), SignText.getDefaultText(), DyeColor.BLACK, false);
    }

    public SignText(Text[] messages, Text[] filteredMessages, DyeColor color, boolean glowing) {
        this.messages = messages;
        this.filteredMessages = filteredMessages;
        this.color = color;
        this.glowing = glowing;
    }

    private static Text[] getDefaultText() {
        return new Text[]{ScreenTexts.EMPTY, ScreenTexts.EMPTY, ScreenTexts.EMPTY, ScreenTexts.EMPTY};
    }

    private static SignText create(Text[] messages, Optional<Text[]> filteredMessages, DyeColor color, boolean glowing) {
        Text[] texts = filteredMessages.orElseGet(SignText::getDefaultText);
        SignText.copyMessages(messages, texts);
        return new SignText(messages, texts, color, glowing);
    }

    private static void copyMessages(Text[] from, Text[] to) {
        for (int i = 0; i < 4; ++i) {
            if (!to[i].equals(ScreenTexts.EMPTY)) continue;
            to[i] = from[i];
        }
    }

    public boolean isGlowing() {
        return this.glowing;
    }

    public SignText withGlowing(boolean glowing) {
        if (glowing == this.glowing) {
            return this;
        }
        return new SignText(this.messages, this.filteredMessages, this.color, glowing);
    }

    public DyeColor getColor() {
        return this.color;
    }

    public SignText withColor(DyeColor color) {
        if (color == this.getColor()) {
            return this;
        }
        return new SignText(this.messages, this.filteredMessages, color, this.glowing);
    }

    public Text getMessage(int line, boolean filtered) {
        return this.getMessages(filtered)[line];
    }

    public SignText withMessage(int line, Text message) {
        return this.withMessage(line, message, message);
    }

    public SignText withMessage(int line, Text message, Text filteredMessage) {
        Text[] texts = Arrays.copyOf(this.messages, this.messages.length);
        Text[] texts2 = Arrays.copyOf(this.filteredMessages, this.filteredMessages.length);
        texts[line] = message;
        texts2[line] = filteredMessage;
        return new SignText(texts, texts2, this.color, this.glowing);
    }

    public boolean hasText(PlayerEntity player) {
        return Arrays.stream(this.getMessages(player.shouldFilterText())).anyMatch(text -> !text.getString().isEmpty());
    }

    public Text[] getMessages(boolean filtered) {
        return filtered ? this.filteredMessages : this.messages;
    }

    public OrderedText[] getOrderedMessages(boolean filtered, Function<Text, OrderedText> messageOrderer) {
        if (this.orderedMessages == null || this.filtered != filtered) {
            this.filtered = filtered;
            this.orderedMessages = new OrderedText[4];
            for (int i = 0; i < 4; ++i) {
                this.orderedMessages[i] = messageOrderer.apply(this.getMessage(i, filtered));
            }
        }
        return this.orderedMessages;
    }

    private Optional<Text[]> getFilteredMessages() {
        Text[] texts = new Text[4];
        boolean bl = false;
        for (int i = 0; i < 4; ++i) {
            Text text = this.filteredMessages[i];
            if (!text.equals(this.messages[i])) {
                texts[i] = text;
                bl = true;
                continue;
            }
            texts[i] = ScreenTexts.EMPTY;
        }
        return bl ? Optional.of(texts) : Optional.empty();
    }

    public boolean hasRunCommandClickEvent(PlayerEntity player) {
        for (Text text : this.getMessages(player.shouldFilterText())) {
            Style style = text.getStyle();
            ClickEvent clickEvent = style.getClickEvent();
            if (clickEvent == null || clickEvent.getAction() != ClickEvent.Action.RUN_COMMAND) continue;
            return true;
        }
        return false;
    }
}

