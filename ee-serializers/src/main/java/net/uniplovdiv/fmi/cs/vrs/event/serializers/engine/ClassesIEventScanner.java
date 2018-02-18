package net.uniplovdiv.fmi.cs.vrs.event.serializers.engine;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import net.uniplovdiv.fmi.cs.vrs.event.*;
import net.uniplovdiv.fmi.cs.vrs.event.IEvent;

import java.util.HashSet;

/**
 * Retriever of the classes implementing {@link IEvent} interface.
 */
public class ClassesIEventScanner {

    private final String[] packagesToScan;
    private final FastClasspathScanner fastClasspathScanner;
    private HashSet<Class<? extends IEvent>> foundEventClasses;

    /**
     * Constructor adding package candidate {@link net.uniplovdiv.fmi.cs.vrs.event} for scanning.
     */
    public ClassesIEventScanner() {
        foundEventClasses = new HashSet<>();
        String mainPackage = IEvent.class.getPackage().getName();
        packagesToScan = new String[] { mainPackage };
        fastClasspathScanner = new FastClasspathScanner(packagesToScan)
                .matchClassesImplementing(IEvent.class, matchedClass -> foundEventClasses.add(matchedClass));
    }

    /**
     * Constructor specifying the packages to be scanned for classes implementing {@link IEvent} interface plus
     * {@link net.uniplovdiv.fmi.cs.vrs.event} package.
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
        fastClasspathScanner = new FastClasspathScanner(packagesToScan)
                .matchClassesImplementing(IEvent.class, matchedClass -> foundEventClasses.add(matchedClass));
    }

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
    public HashSet<Class<? extends IEvent>> scan() {
        this.foundEventClasses.clear();
        fastClasspathScanner.scan();
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
