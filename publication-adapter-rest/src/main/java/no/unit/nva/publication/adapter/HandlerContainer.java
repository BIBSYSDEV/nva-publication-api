package no.unit.nva.publication.adapter;

import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import nva.commons.apigateway.ApiGatewayHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HandlerContainer {

    private static final Logger logger = LoggerFactory.getLogger(HandlerContainer.class);

    private final Map<Class<?>, Object> services = new LinkedHashMap<>();
    private final Map<Class<?>, HandlerFactory> overrides = new HashMap<>();

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
        return bestMatchingConstructor(handlerClass)
                   .map(this::invokeConstructor)
                   .orElseThrow(() -> new IllegalStateException(
                       "No constructor of " + handlerClass.getName()
                       + " could be satisfied by registered services. Registered: " + services.keySet()));
    }

    public <T> Optional<T> lookup(Class<T> type) {
        return Optional.ofNullable(type.cast(services.get(type)));
    }

    private Optional<Constructor<?>> bestMatchingConstructor(Class<?> handlerClass) {
        return java.util.Arrays.stream(handlerClass.getDeclaredConstructors())
                   .filter(this::allParameterTypesRegistered)
                   .max(Comparator.comparingInt(Constructor::getParameterCount));
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
            ctor.setAccessible(true);
            var instance = ctor.newInstance(args);
            logger.debug("Instantiated {} via {}-arg constructor", ctor.getDeclaringClass().getSimpleName(),
                         ctor.getParameterCount());
            return (ApiGatewayHandler<?, ?>) instance;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate " + ctor.getDeclaringClass().getName(), e);
        }
    }
}
