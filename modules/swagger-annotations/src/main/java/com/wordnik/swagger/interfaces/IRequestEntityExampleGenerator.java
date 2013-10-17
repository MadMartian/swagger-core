package com.wordnik.swagger.interfaces;

public interface IRequestEntityExampleGenerator
{
    ExampleInfo[] entity(Class<?> cResourceClass, Class<?> cDataType);
    String[][] parameters(Class<?> cResourceClass);
}
