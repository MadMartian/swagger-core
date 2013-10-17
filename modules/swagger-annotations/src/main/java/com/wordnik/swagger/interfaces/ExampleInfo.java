package com.wordnik.swagger.interfaces;

public class ExampleInfo
{
    public final String mediatype;
    public final String example;

    public ExampleInfo(String sMediaType, String sExample)
    {
        this.mediatype = sMediaType;
        this.example = sExample;
    }
}
