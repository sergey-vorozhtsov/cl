package net.pearlchain;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.TrackedWebResource;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomWebappClassLoader extends WebappClassLoaderBase {
    private static final Log log = LogFactory.getLog(CustomWebappClassLoader.class);
    private static String CLASSPATH_FILENAME;
    private static String PATH_SEPARATOR = "/";
    private static String PROPERTIES_FILENAME = "classpath.properties";

    public CustomWebappClassLoader() {
        super();
        readProperties();
    }

    public CustomWebappClassLoader(ClassLoader parent) {
        super(parent);
        readProperties();
    }

    private void readProperties() {
        String filepath = Objects.requireNonNull(this.getClass().getClassLoader().getResource(".")).getFile();
        File file = new File(filepath);
        String tomcatHome = file.getParent();
        StringJoiner sj = new StringJoiner(PATH_SEPARATOR);
        sj.add(tomcatHome);
        sj.add("conf");
        sj.add(PROPERTIES_FILENAME);

        Properties properties = new Properties();
        try (FileReader fileReader = new FileReader(sj.toString())) {
            properties.load(fileReader);
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while reading classpath dependency properties file", e);
        }
        CLASSPATH_FILENAME = properties.getProperty("classpath.dependencies.filename");
    }

    @Override
    public ClassLoader copyWithoutTransformers() {
        return null;
    }

    @Override
    public void setResources(WebResourceRoot resources) {
        WebResourceRoot customResources = new WebResourceRoot() {

            private Path findClasspathFilePath() {
                String filepath = Objects.requireNonNull(this.getClass().getClassLoader().getResource(".")).getFile();
                String parent = new File(filepath).getParent();

                try (Stream<Path> stream = Files.find(Paths.get(parent), 5,
                        (path, attr) -> path.getFileName().toString().equals(CLASSPATH_FILENAME))) {
                    return stream.findAny().get();
                } catch (IOException e) {
                    throw new RuntimeException("Error occurred while reading classpath file", e);
                }
            }

            private String readClasspathFile(Path filepath) {
                String fileAsString = null;
                if (filepath == null) {
                    return null;
                }
                try (Stream<String> lines = Files.lines(filepath, Charset.defaultCharset())) {
                    fileAsString = lines.collect(Collectors.joining(" "));
                } catch (IOException e) {
                    throw new RuntimeException("Error occurred while read classpath dependencies file: ", e);
                }
                return fileAsString;
            }

            private WebResource[] sortResources(WebResource[] resources, String[] jarNamesOrder) {
                if (resources == null || resources.length <= 0) {
                    log.warn("Can't sort resources - null or zero length of resources");
                    return null;
                } else if (jarNamesOrder == null || jarNamesOrder.length <= 0) {
                    log.warn("Can't sort resources - null or zero length of classpath dependencies");
                    return null;
                }

                WebResource[] result = new WebResource[resources.length];
                int k = 0;
                for (int i = 0; i < jarNamesOrder.length; i++) {
                    String jarName = jarNamesOrder[i];
                    for (int j = 0; j < resources.length; j++) {
                        WebResource resource = resources[j];
                        if (resource != null && jarName != null) {
                            if (jarName.equalsIgnoreCase(resource.getName())) {
                                result[k] = resource;
                                jarNamesOrder[i] = null;
                                resources[j] = null;
                                k++;
                            }
                        }
                    }
                }

                if (resources.length >= jarNamesOrder.length) {
                    for (int i = 0; i < resources.length; i++) {
                            if (resources[i] != null) {
                                result[i] = resources[i];
                            }
                    }
                } else {
                    log.warn("Count of jars less than expected");
                }
                return result;
            }

            @Override
            public WebResource[] listResources(String path) {
                WebResource[] webResourcesOriginal = resources.listResources(path);
                WebResource[] result = null;

                if (webResourcesOriginal != null && webResourcesOriginal.length > 0 && path.equalsIgnoreCase("/WEB-INF/lib")) {
                    Path classpathFilePath = findClasspathFilePath();
                    String fileAsString = readClasspathFile(classpathFilePath);

                    String[] jars = null;
                    if (fileAsString != null) {
                        jars = fileAsString.split(File.pathSeparator);
                        for (int i = 0, jarsLength = jars.length; i < jarsLength; i++) {
                            File file = new File(jars[i]);
                            jars[i] = file.getName();
                        }
                    }
                    result = sortResources(webResourcesOriginal, jars);
                }

                if (result != null) {
                    return result;
                } else {
                    return webResourcesOriginal;
                }
            }

            @Override
            public WebResource getResource(String path) {
                return resources.getResource(path);
            }

            @Override
            public WebResource[] getResources(String path) {
                return resources.getResources(path);
            }

            @Override
            public WebResource getClassLoaderResource(String path) {
                return resources.getClassLoaderResource(path);
            }

            @Override
            public WebResource[] getClassLoaderResources(String path) {
                return resources.getClassLoaderResources(path);
            }

            @Override
            public String[] list(String path) {
                return resources.list(path);
            }

            @Override
            public Set<String> listWebAppPaths(String path) {
                return resources.listWebAppPaths(path);
            }

            @Override
            public boolean mkdir(String path) {
                return resources.mkdir(path);
            }

            @Override
            public boolean write(String path, InputStream is, boolean overwrite) {
                return resources.write(path, is, overwrite);
            }

            @Override
            public void createWebResourceSet(ResourceSetType type, String webAppMount, URL url, String internalPath) {
                resources.createWebResourceSet(type, webAppMount, url, internalPath);
            }

            @Override
            public void createWebResourceSet(ResourceSetType type, String webAppMount, String base, String archivePath, String internalPath) {
                resources.createWebResourceSet(type, webAppMount, base, archivePath, internalPath);
            }

            @Override
            public void addPreResources(WebResourceSet webResourceSet) {
                resources.addPreResources(webResourceSet);
            }

            @Override
            public WebResourceSet[] getPreResources() {
                return resources.getPreResources();
            }

            @Override
            public void addJarResources(WebResourceSet webResourceSet) {
                resources.addJarResources(webResourceSet);
            }

            @Override
            public WebResourceSet[] getJarResources() {
                return resources.getJarResources();
            }

            @Override
            public void addPostResources(WebResourceSet webResourceSet) {
                resources.addPostResources(webResourceSet);
            }

            @Override
            public WebResourceSet[] getPostResources() {
                return resources.getPostResources();
            }

            @Override
            public Context getContext() {
                return resources.getContext();
            }

            @Override
            public void setContext(Context context) {
                resources.setContext(context);
            }

            @Override
            public void setAllowLinking(boolean allowLinking) {
                resources.setAllowLinking(allowLinking);
            }

            @Override
            public boolean getAllowLinking() {
                return resources.getAllowLinking();
            }

            @Override
            public void setCachingAllowed(boolean cachingAllowed) {
                resources.setCachingAllowed(cachingAllowed);
            }

            @Override
            public boolean isCachingAllowed() {
                return resources.isCachingAllowed();
            }

            @Override
            public void setCacheTtl(long ttl) {
                resources.setCacheTtl(ttl);
            }

            @Override
            public long getCacheTtl() {
                return resources.getCacheTtl();
            }

            @Override
            public void setCacheMaxSize(long cacheMaxSize) {
                resources.setCacheMaxSize(cacheMaxSize);
            }

            @Override
            public long getCacheMaxSize() {
                return resources.getCacheMaxSize();
            }

            @Override
            public void setCacheObjectMaxSize(int cacheObjectMaxSize) {
                resources.setCacheObjectMaxSize(cacheObjectMaxSize);
            }

            @Override
            public int getCacheObjectMaxSize() {
                return resources.getCacheObjectMaxSize();
            }

            @Override
            public void setTrackLockedFiles(boolean trackLockedFiles) {
                resources.setTrackLockedFiles(trackLockedFiles);
            }

            @Override
            public boolean getTrackLockedFiles() {
                return resources.getTrackLockedFiles();
            }

            @Override
            public void backgroundProcess() {
                resources.backgroundProcess();
            }

            @Override
            public void registerTrackedResource(TrackedWebResource trackedResource) {
                resources.registerTrackedResource(trackedResource);
            }

            @Override
            public void deregisterTrackedResource(TrackedWebResource trackedResource) {
                resources.deregisterTrackedResource(trackedResource);
            }

            @Override
            public List<URL> getBaseUrls() {
                return resources.getBaseUrls();
            }

            @Override
            public void gc() {
                resources.gc();
            }

            @Override
            public void addLifecycleListener(LifecycleListener listener) {
                resources.addLifecycleListener(listener);
            }

            @Override
            public LifecycleListener[] findLifecycleListeners() {
                return resources.findLifecycleListeners();
            }

            @Override
            public void removeLifecycleListener(LifecycleListener listener) {
                resources.removeLifecycleListener(listener);
            }

            @Override
            public void init() throws LifecycleException {
                resources.init();
            }

            @Override
            public void start() throws LifecycleException {
                resources.start();
            }

            @Override
            public void stop() throws LifecycleException {
                resources.stop();
            }

            @Override
            public void destroy() throws LifecycleException {
                resources.destroy();
            }

            @Override
            public LifecycleState getState() {
                return resources.getState();
            }

            @Override
            public String getStateName() {
                return resources.getStateName();
            }
        };
        super.setResources(customResources);
    }
}
