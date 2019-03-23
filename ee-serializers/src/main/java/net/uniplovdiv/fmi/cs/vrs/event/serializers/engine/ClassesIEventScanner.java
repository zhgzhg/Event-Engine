package net.uniplovdiv.fmi.cs.vrs.event.serializers.engine;

//import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
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
        if (classes != null && !classes.isEmpty()) {
            classes.forEach(c -> { if (c != null) this.foundEventClasses.add(c); } );
        }
    }

    /**
     * Returns an array of the packages that will be scanned for classes implementing {@link IEvent} interface.
     * @return Array of the packages that will be scanned.
     */
    public final String[] getPackagesToScan() {
        return packagesToScan;
    }

    /**
     * Returns the found set of classes implementing {@link IEvent} interface.
     * @return On success nonempty set otherwise an empty one.
     */
    public Set<Class<? extends IEvent>> getFoundEventClasses() {
        return foundEventClasses;
    }

    /**
     * Scans the packages and returns the classes that implement {@link IEvent} interface.
     * @return On success nonempty set of data otherwise an empty one.
     */
    @SuppressWarnings("unchecked")
    public Set<Class<? extends IEvent>> scan() {
        this.foundEventClasses.clear();
        ClassLoader currentClassLoader = null;
        try {
            currentClassLoader = this.getClass().getClassLoader();
        } catch (SecurityException e) {
            e.printStackTrace(System.err);
        }

        try (ScanResult results = this.classGraphScanner.scan()) {
            final ClassLoader localClassLoader = currentClassLoader;

            results.getClassesImplementing(IEvent.class.getCanonicalName())
                    .loadClasses(true)
                        // loaded by ClassGraph's class loader, so we must load them in our Applications's class loader
                    .forEach(clazz -> {
                        if (localClassLoader != null && localClassLoader != clazz.getClassLoader()) {
                            try {
                                clazz = localClassLoader.loadClass(clazz.getCanonicalName());
                            } catch (Exception ex) {
                                ex.printStackTrace(System.err);
                            }
                            foundEventClasses.add((Class<? extends IEvent>) clazz);
                        }
                    });
        }

        return this.foundEventClasses;
    }
}
