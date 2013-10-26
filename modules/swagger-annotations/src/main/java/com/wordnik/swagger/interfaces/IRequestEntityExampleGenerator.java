package com.wordnik.swagger.interfaces;

import java.lang.reflect.Method;

public interface IRequestEntityExampleGenerator
{
    ExampleInfo[] entity(Class<?> cResourceClass, Method mOperationMethod, Class<?> cDataType);
    String[][] parameters(Class<?> cResourceClass, Method mOperationMethod);
}
