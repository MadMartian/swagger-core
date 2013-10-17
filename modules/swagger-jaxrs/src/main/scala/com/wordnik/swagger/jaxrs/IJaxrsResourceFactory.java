package com.wordnik.swagger.jaxrs;

public interface IJaxrsResourceFactory
{
    public <T> T acquireResource(Class<T> clazz) throws InstantiationException, IllegalAccessException;
}
