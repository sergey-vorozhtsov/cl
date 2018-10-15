package com.example;

import org.apache.catalina.loader.WebappLoader;

public class CustomWebappLoader extends WebappLoader {
    static
    {
        System.out.println("CustomWebappLoader static init");
        ClassLoader classLoader = new CustomWebappClassLoader();
    }


    public CustomWebappLoader() {
        super();
        super.setLoaderClass("com.example.CustomWebappClassLoader");
        System.err.println("CustomWebappLoader constructor" + this);
    }

    public CustomWebappLoader(ClassLoader parent) {
        super(parent);
        super.setLoaderClass("com.example.CustomWebappClassLoader");
        System.err.println("CustomWebappLoader constructor + " + this + " " + parent);
    }
}
