package org.acme;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.test.common.PathTestHelper;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Extension that inits QuarkusModel for IntelliJ so Quarkus does not need to run Gradle everytime.
 */
public class IntelliJGradleQuarkusTestExtension implements BeforeAllCallback, InvocationInterceptor {

    private static final IdeGradleModelBuilder IDE_MODEL_BUILDER = new IdeGradleModelBuilder();

    @Override
    public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        initModel(extensionContext);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        initModel(context);
    }

    private void initModel(ExtensionContext context) throws IOException, AppModelResolverException {
        Class<?> requiredTestClass = context.getRequiredTestClass();
        Path testClassLocation = PathTestHelper.getTestClassesLocation(requiredTestClass);
        Path appClassLocation = PathTestHelper.getAppClassLocationForTestLocation(testClassLocation.toString());
        Path projectRoot = Paths.get("").normalize().toAbsolutePath();

        if (System.getProperty(BootstrapConstants.SERIALIZED_TEST_APP_MODEL) == null) {
            // If gradle project running directly with IDE
            if (IDE_MODEL_BUILDER.isRunFromIdea(appClassLocation)) {
                IDE_MODEL_BUILDER.loadModelForIdea(projectRoot);
            }
        }
    }
}
