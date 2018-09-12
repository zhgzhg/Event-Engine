package net.uniplovdiv.fmi.cs.vrs.event.serializers.engine;

//import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.classgraph.ClassGraph;
import net.uniplovdiv.fmi.cs.vrs.event.*;
import net.uniplovdiv.fmi.cs.vrs.event.IEvent;

import java.util.*;

/**
 * Retriever of the classes implementing {@link IEvent} interface.
 */
public class ClassesIEventScanner {

    private final String[] packagesToScan;
    private final ClassGraph classGraphScanner;
    private HashSet<Class<? extends IEvent>> foundEventClasses;

    /**
     * Constructor adding package candidate {@link net.uniplovdiv.fmi.cs.vrs.event} for scanning.
     */
    public ClassesIEventScanner() {
        foundEventClasses = new HashSet<>();
        String mainPackage = IEvent.class.getPackage().getName();
        packagesToScan = new String[] { mainPackage };
        classGraphScanner = new ClassGraph()
                .whitelistPackages(mainPackage)
                .enableExternalClasses()
                .enableAllInfo();
                //.whitelistPackages()
                //.matchClassesImplementing(IEvent.class, matchedClass -> foundEventClasses.add(matchedClass));
    }

    /**
     * Constructor specifying the packages to be scanned for classes implementing {@link IEvent} interface to which
     * {@link net.uniplovdiv.fmi.cs.vrs.event} package gets appended too.
     * @param packages The Java packages to be scanned. If is set to null or empty array only the default package will
     *                 be scanned.
     */
    public ClassesIEventScanner(String... packages) {
        foundEventClasses = new HashSet<>();
        int packagesLength = 1 + (packages != null ? packages.length : 0);
        packagesToScan = new String[packagesLength];
        packagesToScan[0] = IEvent.class.getPackage().getName();
        if (packages != null) {
            for (int i = 0, j = 1; i < packages.length; ++i, ++j) {
                packagesToScan[j] = packages[i];
            }
        }
        classGraphScanner = new ClassGraph()
                .whitelistPackages(packagesToScan)
                .enableExternalClasses()
                .enableAllInfo();
                //.matchClassesImplementing(IEvent.class, matchedClass -> foundEventClasses.add(matchedClass));
    }

    /**
     * Constructor specifying classes to be included as a mandatory part of the scanning, as well as the packages to be
     * scanned for classes implementing {@link IEvent} interface to which {@link net.uniplovdiv.fmi.cs.vrs.event}
     * package gets appended too.
     * @param classes The list manually specified of classes implementing {@link IEvent} interface. If is set null or is
     *                empty the parameter gets ignored.
     * @param packages The Java packages to be scanned. If is set to null or empty array only the default package will
     *                 be scanned.
     */
    public ClassesIEventScanner(Collection<Class<? extends IEvent>> classes, String... packages) {
        foundEventClasses = new HashSet<>();
        int packagesLength = 1 + (packages != null ? packages.length : 0);
        packagesToScan = new String[packagesLength];
        packagesToScan[0] = IEvent.class.getPackage().getName();
        if (packages != null) {
            for (int i = 0, j = 1; i < packages.length; ++i, ++j) {
                packagesToScan[j] = packages[i];
            }
        }
        classGraphScanner = new ClassGraph()
                .whitelistPackages(packages)
                .enableExternalClasses()
                .enableAllInfo();
                //.matchClassesImplementing(IEvent.class, matchedClass -> foundEventClasses.add(matchedClass));
        if (classes != null && !classes.isEmpty()) {
            classes.forEach(c -> { if (c != null) this.foundEventClasses.add(c); } );
        }
    }

    /* Not tested well enough
     * Constructor specifying the class paths from which the classes to be scanned and the packages to be scanned for
     * classes implementing {@link IEvent} interface plus {@link net.uniplovdiv.fmi.cs.vrs.event} package.
     * @param classPathElements The class paths to be scanned. If null the default classpath will be used. For every of
     *                          the provided elements method toString() will be used to determine the actual path.
     * @param packages The Java packages to be scanned. If is set to null or empty array only the default package will
     *                 be scanned.
     *
    public ClassesIEventScanner(Iterable<?> classPathElements, String... packages) {
        foundEventClasses = new HashSet<>();
        int packagesLength = 1 + (packages != null ? packages.length : 0);
        packagesToScan = new String[packagesLength];
        packagesToScan[0] = IEvent.class.getPackage().getName();
        if (packages != null) {
            for (int i = 0, j = 1; i < packages.length; ++i, ++j) {
                packagesToScan[j] = packages[i];
            }
        }

        FastClasspathScanner _fastClasspathScanner = new FastClasspathScanner(packagesToScan).verbose(true);
        if (classPathElements != null) {
            String classpath = "";
            try {
                classpath = System.getProperty("java.class.path");
                if (classpath == null) classpath = "";
            } catch (Exception ex) {
                classpath = "";
                ex.printStackTrace(System.err);
            }
            String[] classpathEntries = classpath.split(File.pathSeparator);
            final HashSet<String> elements = new HashSet<>(Arrays.asList(classpathEntries));
            classPathElements.forEach(c -> elements.add(c.toString()));

            _fastClasspathScanner = _fastClasspathScanner.overrideClasspath(elements);
        }
        classGraphScanner = _fastClasspathScanner
                .matchClassesImplementing(IEvent.class, matchedClass -> foundEventClasses.add(matchedClass));
    }*/

    /**
     * Returns an array of the packages that will be scanned for classes implementing {@link IEvent} interface.
     * @return Array of the packages that will be scanned.
     */
    public final String[] getPackagesToScan() {
        return packagesToScan;
    }

    /**
     * Scans the packages and returns the classes that implement {@link IEvent} interface.
     * @return On success nonempty set of data otherwise an empty one.
     */
    @SuppressWarnings("unchecked")
    public HashSet<Class<? extends IEvent>> scan() {
        this.foundEventClasses.clear();
        this.classGraphScanner.scan()
                .getClassesImplementing(IEvent.class.getCanonicalName())
                .loadClasses(true)
                .forEach(clazz -> foundEventClasses.add((Class<? extends IEvent>) clazz));

        return this.foundEventClasses;
    }

    /**
     * Returns the found set of classes implementing {@link IEvent} interface.
     * @return On success nonempty set otherwise an empty one.
     */
    public HashSet<Class<? extends IEvent>> getFoundEventClasses() {
        return foundEventClasses;
    }
}
