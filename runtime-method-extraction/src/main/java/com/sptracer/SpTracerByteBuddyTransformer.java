package com.sptracer;

import com.sptracer.configuration.ConfigurationRegistry;
import com.sptracer.util.ClassUtils;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.List;

import static com.sptracer.CachedClassLoaderMatcher.cached;
import static com.sptracer.SpTracerClassNameMatcher.isInsideMonitoredProject;
import static com.sptracer.TimedElementMatcherDecorator.timed;
import static net.bytebuddy.matcher.ElementMatchers.*;

public abstract class SpTracerByteBuddyTransformer {

    protected static final ConfigurationRegistry configuration = SpTracer.getConfiguration();

    protected static final boolean DEBUG_INSTRUMENTATION = configuration.getConfig(CorePlugin.class).isDebugInstrumentation();

    private static final Logger logger = LoggerFactory.getLogger(SpTracerByteBuddyTransformer.class);

    private static final ElementMatcher.Junction<ClassLoader> applicationClassLoaderMatcher = cached(new ApplicationClassLoaderMatcher());

    protected final String transformerName = getClass().getSimpleName();

    public final AgentBuilder.RawMatcher getMatcher() {
        return new AgentBuilder.RawMatcher() {
            @Override
            public boolean matches(TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule, Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
                final boolean matches = filterCoreJavaClasses(classLoader, typeDescription) &&
                        timed("type", transformerName, getTypeMatcher()).matches(typeDescription) &&
                        getRawMatcher().matches(typeDescription, classLoader, javaModule, classBeingRedefined, protectionDomain) &&
                        timed("classloader", "application", getClassLoaderMatcher()).matches(classLoader);
                if (!matches) {
                    onIgnored(typeDescription, classLoader);
                }
                return matches;
            }
        };
    }

    private boolean filterCoreJavaClasses(ClassLoader classLoader, TypeDescription typeDescription) {
        if (transformsCoreJavaClasses()) {
            return true;
        } else {
            final boolean isCoreJavaClass = isBootstrapClassLoader().matches(classLoader) || nameStartsWith("java")
                    .or(nameStartsWith("com.sun."))
                    .or(nameStartsWith("sun."))
                    .or(nameStartsWith("jdk.")).matches(typeDescription);
            return !isCoreJavaClass;
        }
    }

    /**
     * Makes sure that no classes of the same class loader are instrumented twice, even if multiple stagemonitored
     * applications are deployed on one application server
     */
    protected AgentBuilder.RawMatcher getRawMatcher() {
        if (isPreventDuplicateTransformation()) {
            return new AvoidDuplicateTransformationsRawMatcher();
        }
        return NoOpRawMatcher.INSTANCE;
    }

    private class AvoidDuplicateTransformationsRawMatcher implements AgentBuilder.RawMatcher {
        @Override
        public boolean matches(TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule, Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
            final String key = getClassAlreadyTransformedKey(typeDescription, classLoader);
            final boolean hasAlreadyBeenTransformed = Dispatcher.getValues().containsKey(key);
            if (DEBUG_INSTRUMENTATION) {
                logger.info("{}: {}", key, hasAlreadyBeenTransformed);
            }
            return !hasAlreadyBeenTransformed;
        }
    }

    private String getClassAlreadyTransformedKey(TypeDescription typeDescription, ClassLoader classLoader) {
        return getAdviceClass() + typeDescription.getName() + ClassUtils.getIdentityString(classLoader) + ".transformed";
    }

    protected ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
        return getIncludeTypeMatcher()
                .and(not(isInterface()))
                .and(not(isSynthetic()))
                .and(not(getExtraExcludeTypeMatcher()));
    }

    public boolean isActive() {
        return true;
    }

    protected boolean transformsCoreJavaClasses() {
        return false;
    }

    protected ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
        return isInsideMonitoredProject().or(getExtraIncludeTypeMatcher()).and(getNarrowTypesMatcher());
    }

    protected ElementMatcher.Junction<TypeDescription> getExtraIncludeTypeMatcher() {
        return none();
    }

    protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
        return any();
    }

    protected ElementMatcher.Junction<TypeDescription> getExtraExcludeTypeMatcher() {
        return none();
    }

    protected ElementMatcher.Junction<ClassLoader> getClassLoaderMatcher() {
        return applicationClassLoaderMatcher;
    }

    public AgentBuilder.Transformer getTransformer() {
        final AsmVisitorWrapper.ForDeclaredMethods advice = getAdvice();
        if (advice == null) {
            return AgentBuilder.Transformer.NoOp.INSTANCE;
        } else {
            return new AgentBuilder.Transformer() {
                @Override
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                                                        ClassLoader classLoader, JavaModule module) {
                    beforeTransformation(typeDescription, classLoader);
                    return builder.visit(advice);
                }
            };
        }
    }

    private AsmVisitorWrapper.ForDeclaredMethods getAdvice() {
        try {
            return registerDynamicValues()
                    .to(getAdviceClass())
                    .on(timed("method", transformerName, getMethodElementMatcher()));
        } catch (NoClassDefFoundError error) {
            logger.debug("Error while creating advice. This usually means that an optional type is not present " +
                    "so this is nothing wo worry about. Error message: {}", error.getMessage());
            return null;
        }
    }

    private Advice.WithCustomMapping registerDynamicValues() {
        List<Advice.OffsetMapping.Factory<? extends Annotation>> offsetMappingFactories = getOffsetMappingFactories();
        Advice.WithCustomMapping withCustomMapping = Advice.withCustomMapping();
        for (Advice.OffsetMapping.Factory<? extends Annotation> offsetMappingFactory : offsetMappingFactories) {
            withCustomMapping = withCustomMapping.bind((Advice.OffsetMapping.Factory<? super Annotation>) offsetMappingFactory);
        }
        return withCustomMapping;
    }

    protected List<Advice.OffsetMapping.Factory<? extends Annotation>> getOffsetMappingFactories() {
        return Collections.emptyList();
    }

    protected ElementMatcher.Junction<MethodDescription> getMethodElementMatcher() {
        return not(isConstructor())
                .and(not(isAbstract()))
                .and(not(isNative()))
                .and(not(isFinal()))
                .and(not(isSynthetic()))
                .and(not(isTypeInitializer()))
                .and(getExtraMethodElementMatcher());
    }

    protected ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
        return any();
    }

    protected Class<? extends SpTracerByteBuddyTransformer> getAdviceClass() {
        return getClass();
    }

    /**
     * Returns the order of this transformer when multiple transformers match a method.
     * <p>
     * Higher orders will be applied first
     *
     * @return the order
     */
    protected int getOrder() {
        return 0;
    }

    protected boolean isPreventDuplicateTransformation() {
        return false;
    }

    /**
     * This method is called before the transformation.
     * You can stop the transformation from happening by returning false from this method.
     *
     * @param typeDescription The type that is being transformed.
     * @param classLoader     The class loader which is loading this type.
     */
    public void beforeTransformation(TypeDescription typeDescription, ClassLoader classLoader) {
        if (isPreventDuplicateTransformation()) {
            Dispatcher.put(getClassAlreadyTransformedKey(typeDescription, classLoader), Boolean.TRUE);
        }

        if (DEBUG_INSTRUMENTATION && logger.isDebugEnabled()) {
            logger.debug("TRANSFORM {} ({})", typeDescription.getName(), getClass().getSimpleName());
        }
    }

    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader) {
        if (DEBUG_INSTRUMENTATION && logger.isDebugEnabled() && getTypeMatcher().matches(typeDescription)) {
            logger.debug("IGNORE {} ({})", typeDescription.getName(), getClass().getSimpleName());
        }
    }

    private static class NoOpRawMatcher implements AgentBuilder.RawMatcher {
        public static final NoOpRawMatcher INSTANCE = new NoOpRawMatcher();
        @Override
        public boolean matches(TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule, Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
            return true;
        }
    }

}
