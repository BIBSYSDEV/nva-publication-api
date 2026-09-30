package no.unit.nva.publication.adapter;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import nva.commons.apigateway.ApiGatewayHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HandlerContainer {

    private static final Logger logger = LoggerFactory.getLogger(HandlerContainer.class);

    private final Map<Class<?>, Object> services = new LinkedHashMap<>();
    private final Map<Class<?>, HandlerFactory> overrides = new HashMap<>();
    // Constructor lookup is reflection-heavy and the answer never changes, so it is cached.
    // The handler instances themselves are not: RestRequestHandler.init() stores the request's
    // OutputStream on the instance, so a shared handler would corrupt concurrent responses.
    private final Map<Class<?>, Constructor<?>> constructors = new ConcurrentHashMap<>();

    public <T> HandlerContainer register(Class<T> type, T instance) {
        services.put(type, instance);
        return this;
    }

    public HandlerContainer registerFactory(Class<? extends ApiGatewayHandler<?, ?>> handlerClass,
                                            HandlerFactory factory) {
        overrides.put(handlerClass, factory);
        return this;
    }

    public ApiGatewayHandler<?, ?> create(Class<?> handlerClass) {
        HandlerFactory override = overrides.get(handlerClass);
        if (override != null) {
            return override.create(this);
        }
        return invokeConstructor(constructors.computeIfAbsent(handlerClass, this::resolveConstructor));
    }

    public <T> Optional<T> lookup(Class<T> type) {
        return Optional.ofNullable(type.cast(services.get(type)));
    }

    int resolvedConstructorCount() {
        return constructors.size();
    }

    private Constructor<?> resolveConstructor(Class<?> handlerClass) {
        var constructor = bestMatchingConstructor(handlerClass)
                              .orElseThrow(() -> new IllegalStateException(
                                  "No constructor of " + handlerClass.getName()
                                  + " could be satisfied by registered services. Registered: "
                                  + services.keySet()));
        constructor.setAccessible(true);
        return constructor;
    }

    private Optional<Constructor<?>> bestMatchingConstructor(Class<?> handlerClass) {
        return Arrays.stream(handlerClass.getDeclaredConstructors())
                   .filter(this::allParameterTypesRegistered)
                   .filter(ctor -> takesArguments(ctor) || onlyHasNoArgConstructor(handlerClass))
                   .max(Comparator.comparingInt(Constructor::getParameterCount));
    }

    private static boolean takesArguments(Constructor<?> ctor) {
        return ctor.getParameterCount() > 0;
    }

    // The no-arg constructors reach for real AWS services, so falling back to one
    // silently turns an unwired handler into a live call
    private static boolean onlyHasNoArgConstructor(Class<?> handlerClass) {
        return Arrays.stream(handlerClass.getDeclaredConstructors())
                   .noneMatch(HandlerContainer::takesArguments);
    }

    private boolean allParameterTypesRegistered(Constructor<?> ctor) {
        for (Class<?> paramType : ctor.getParameterTypes()) {
            if (!services.containsKey(paramType)) {
                return false;
            }
        }
        return true;
    }

    private ApiGatewayHandler<?, ?> invokeConstructor(Constructor<?> ctor) {
        var args = new Object[ctor.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            args[i] = services.get(ctor.getParameterTypes()[i]);
        }
        try {
            var instance = ctor.newInstance(args);
            logger.debug("Instantiated {} via {}-arg constructor", ctor.getDeclaringClass().getSimpleName(),
                         ctor.getParameterCount());
            return (ApiGatewayHandler<?, ?>) instance;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate " + ctor.getDeclaringClass().getName(), e);
        }
    }
}
