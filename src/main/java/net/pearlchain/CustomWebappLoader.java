package net.pearlchain;

import org.apache.catalina.loader.WebappLoader;

public class CustomWebappLoader extends WebappLoader {
    static
    {
        System.out.println("CustomWebappLoader static init");
        ClassLoader classLoader = new CustomWebappClassLoader();
    }


    public CustomWebappLoader() {
        super();
        super.setLoaderClass("CustomWebappClassLoader");
        System.err.println("CustomWebappLoader constructor" + this);
    }

    public CustomWebappLoader(ClassLoader parent) {
        super(parent);
        super.setLoaderClass("CustomWebappClassLoader");
        System.err.println("CustomWebappLoader constructor + " + this + " " + parent);
    }
}
