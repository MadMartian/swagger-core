package com.wordnik.swagger.interfaces;

public interface IJaxrsResourceFactory
{
    public <T> T acquireResource(Class<T> clazz) throws InstantiationException, IllegalAccessException;
}
