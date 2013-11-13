package com.wordnik.swagger.jaxrs;

import com.wordnik.swagger.interfaces.IJaxrsResourceFactory;

public class DefaultJaxrsResourceFactory implements IJaxrsResourceFactory
{
    @Override
    public <T> T acquireResource(Class<T> clazz) throws InstantiationException, IllegalAccessException
    {
        return clazz.newInstance();
    }
}
