package net.pearlchain;

import org.apache.catalina.loader.WebappLoader;

public class CustomWebappLoader extends WebappLoader {
    public CustomWebappLoader() {
        super();
        super.setLoaderClass("net.pearlchain.CustomWebappClassLoader");
    }

    public CustomWebappLoader(ClassLoader parent) {
        super(parent);
        super.setLoaderClass("net.pearlchain.CustomWebappClassLoader");
    }
}
