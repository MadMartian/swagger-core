package com.wordnik.swagger.jaxrs;

public class DefaultJaxrsResourceFactory implements IJaxrsResourceFactory
{
    @Override
    public <T> T acquireResource(Class<T> clazz) throws InstantiationException, IllegalAccessException
    {
        return clazz.newInstance();
    }
}
