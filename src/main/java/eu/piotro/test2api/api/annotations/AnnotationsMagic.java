package eu.piotro.test2api.api.annotations;

import eu.piotro.test2api.api.*;
import eu.piotro.test2api.http.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Annotation processing
 * @see eu.piotro.test2api.api.annotations
 */
public class AnnotationsMagic {
    /**
     * Parses class and registers annotated methods to forwarder.
     * @param clazz Class to parse
     * @param forwarder Forwarder to which register class methods
     */
    public static void registerClass(Class<?> clazz, APIForwarder forwarder){
        Method[] methods = clazz.getDeclaredMethods();
        Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(RESTHandler.class))
                .forEach(method -> {
                    checkValidHandler(method);
                    RESTHandler annotation = method.getAnnotation(RESTHandler.class);

                    APIHandler handler = new APIHandler() {
                        @Override
                        public HTTPResponse handle(HTTPRequest request) throws HTTPException {
                            try {
                                return (HTTPResponse) method.invoke(null, request); //can cast because checked before that returns HTTPResponse. Obj may be null because method is static and don't matter from where we are calling it
                            } catch (IllegalAccessException e){
                                e.printStackTrace();
                                throw new HTTPException(500, HTTPCodes.C500);
                            } catch (InvocationTargetException e){ //invoked method returned exception
                                if (e.getTargetException() instanceof HTTPException){
                                    throw (HTTPException) e.getTargetException(); //pass HTTPException
                                } else {
                                    e.printStackTrace();
                                    throw new HTTPException(500, HTTPCodes.C500); //This should not happen
                                }
                            }
                        }
                    };

                    forwarder.register(annotation.method(), annotation.URI(), handler);
        });
    }

    private static void checkValidHandler(Method method){
        if (method.getReturnType() != HTTPResponse.class) {
            throw new IllegalArgumentException("@RESTHandler method '" + method + "' must return HTTPResponse type");
        }
        if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != HTTPRequest.class) {
            throw new IllegalArgumentException("@RESTHandler method '" + method + "' must take HTTPRequest as only parameter");
        }
        if(!Modifier.isStatic(method.getModifiers())){
            throw new IllegalArgumentException("@RESTHandler method '" + method + "' must be static");
        }
        if(!method.canAccess(null)){
            throw new IllegalArgumentException("@RESTHandler method '" + method + "' must be accessible");
        }
    }
}
