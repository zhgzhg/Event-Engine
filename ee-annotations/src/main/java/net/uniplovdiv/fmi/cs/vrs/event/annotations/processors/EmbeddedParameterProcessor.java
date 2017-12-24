package net.uniplovdiv.fmi.cs.vrs.event.annotations.processors;

import net.uniplovdiv.fmi.cs.vrs.event.annotations.EmbeddedParameter;
import net.uniplovdiv.fmi.cs.vrs.event.annotations.wrappers.UnknownParameterWrapper;
import org.apache.commons.lang3.ClassUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
//import javax.lang.model.element.Modifier;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementKindVisitor7;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
//import java.lang.reflect.*;
import java.io.*;
//import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Processor for compilation time used to check which reference type fields have copy constructors - required by
 * EmbeddedParameter.
 */
public class EmbeddedParameterProcessor extends AbstractProcessor {

    private Types typeUtils;
    private Elements elementUtils;
    @SuppressWarnings("FieldCanBeLocal")
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment env){
        super.init(env);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Class<EmbeddedParameter> epc = EmbeddedParameter.class;
        String upwName = UnknownParameterWrapper.class.getName();
        final String ieventTypeRegex = "^net\\.uniplovdiv\\.fmi\\.cs\\.vrs\\.event\\.IEvent$";
        final String comparableTypeRegex = "^" + Pattern.quote(Comparable.class.getCanonicalName()) + "\\<.+\\>$";
        final String serializableTypeRegex = "^" + Pattern.quote(Serializable.class.getCanonicalName()) + "$";
        final String externalizableTypeRegex = "^" + Pattern.quote(Externalizable.class.getCanonicalName()) + "$";

        final Map<String, Boolean> checkedForSerialVersionUIDClasses = new HashMap<>();
        final Set<String> checkedForImplementingIEventClasses = new HashSet<>();
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(epc)) {
  		    if (annotatedElement.getKind() != ElementKind.FIELD) {
  		        error(annotatedElement, "@%s must be applied only to fields!", epc.getSimpleName());
  		        return true;
            }

            // <editor-fold defaultstate="collapsed" desc="Checks for IEvent implementation and serialVersionUID field presence">
            {
                // Check if the class containing the annotation implements IEvent

                String enclosingClassTypeName = annotatedElement.getEnclosingElement().asType().toString();

                boolean implementsIEvent = false;
                if (checkedForImplementingIEventClasses.contains(enclosingClassTypeName)) {
                    implementsIEvent = true;
                } else {
                    implementsIEvent = hasImplementedTypeHereOrAbove(
                            annotatedElement.getEnclosingElement().asType(), ieventTypeRegex);
                }

                if (!implementsIEvent) {
                    error(annotatedElement,
                            "Class %s has to implement IEvent in order to contain fields with annotation @%s - %s",
                            enclosingClassTypeName,
                            epc.getSimpleName(),
                            annotatedElement.asType().toString());
                    return true;
                } else {
                    checkedForImplementingIEventClasses.add(enclosingClassTypeName);
                }

                // Check if the class containing the annotation has serialVersionUID field.

                boolean hasSerialVersionUuid = false;

                if (checkedForSerialVersionUIDClasses.containsKey(enclosingClassTypeName)) {
                    hasSerialVersionUuid = checkedForSerialVersionUIDClasses.get(enclosingClassTypeName);
                } else {
                    hasSerialVersionUuid = annotatedElement.getEnclosingElement()
                            .accept(new SerialVersionUIDVisitor(), null);
                    checkedForSerialVersionUIDClasses.put(enclosingClassTypeName, hasSerialVersionUuid);
                }

                if (!hasSerialVersionUuid) {
                    warning(annotatedElement.getEnclosingElement(),
                            "Class %s does not have a proper, statically pregenerated serialVersionUID field. This can cause performance degradation.",
                            annotatedElement.getEnclosingElement().asType().toString()
                    );
                }
            }
            // </editor-fold>

            EmbeddedParameter ep = annotatedElement.getAnnotation(epc);
            if (!ep.value().isEmpty()) {
                TypeMirror annotatedElementType = annotatedElement.asType();

                // Check if the field implements serializable
                Set<Modifier> modifiers = annotatedElement.getModifiers();
                if (!modifiers.contains(Modifier.STATIC) && !modifiers.contains(Modifier.TRANSIENT)) {
                    if (!annotatedElementType.getKind().isPrimitive()) {
                        if (!hasImplementedTypeHereOrAbove(annotatedElement.asType(), serializableTypeRegex)
                                && !hasImplementedTypeHereOrAbove(annotatedElement.asType(), externalizableTypeRegex)) {
                            warning(annotatedElement,
                                    "Field of type %s annotated as @%s does not implement %s or %s which may cause run-time problems!",
                                    annotatedElement.asType().toString(),
                                    epc.getSimpleName(),
                                    Serializable.class.getCanonicalName(),
                                    Externalizable.class.getCanonicalName()
                            );
                        }
                    }
                }

                try {
                    if (!annotatedElementType.getKind().isPrimitive()) {
                        //this.elementUtils.getBinaryName(annotatedElement)...
                        //this.messager.printMessage(Diagnostic.Kind.WARNING, "---->>> " + annotatedElementType.toString());
                        //this.messager.printMessage(Diagnostic.Kind.WARNING, "----<<< " + typeUtils.directSupertypes(annotatedElementType).toString());

                        // check if the annotated element data type is interface
                        /*Boolean isInterface =
                            typeUtils.asElement(annotatedElementType).accept(new InterfaceOrAbstractClassCheckerVisitor(), null);
                        //annotatedElement.accept(new InterfaceOrAbstractClassCheckerVisitor(), isInterface);
                        if (isInterface) {
                            error(annotatedElement, "@%s cannot be used with interface %s",
                                    epc.getSimpleName(), annotatedElementType.toString());
                            return true;
                        }*/

                        // "static" attempt to check for class copy constructor.
                        boolean hasCopyCtor = false;
                        boolean isArray = false, isArrayOfPrimitives = false;
                        if (!annotatedElementType.getKind().equals(TypeKind.ARRAY)) {
                            hasCopyCtor = typeUtils.asElement(annotatedElementType)
                                    .accept(new ClassConstructorVisitor(), annotatedElementType.toString());
                        } else {
                            // We also accept that an array of primitives has copy ctor;
                            isArray = true;
                            if (getArrayElementRealType((ArrayType)annotatedElementType).getKind().isPrimitive()) {
                                isArrayOfPrimitives = true;
                                hasCopyCtor = true; // the boxing types have copy ctors and we are also able to copy arrays
                            }
                        }

                        // get the fully qualified class name set inside "wrapper" field
                        String wrapperClassName;
                        try {
                            wrapperClassName = ep.wrapper().getName(); // this should throw
                        } catch(MirroredTypeException mte) {
                            wrapperClassName = mte.getTypeMirror().toString();
                        }

                        if (wrapperClassName.equals(upwName)) {
                            if (!isArrayOfPrimitives) {
                                if (!isArray) {
                                    if (!hasImplementedTypeHereOrAbove(annotatedElementType, comparableTypeRegex)) {
                                        throw new ClassNotFoundException("Not implemented java.lang.Comparable interface");
                                    }
                                } else {
                                    TypeMirror componentType = getArrayElementRealType((ArrayType) annotatedElementType);
                                    if (!hasImplementedTypeHereOrAbove(componentType, comparableTypeRegex)) {
                                        throw new ClassNotFoundException("Not implemented java.lang.Comparable interface");
                                    } else {
                                        hasCopyCtor = true;
                                    }
                                }
                            }
                        } else {
                            if (!hasCopyCtor) {
                                // check if the class object from the "wrapper" field has the required copy ctor
                                TypeElement classRepresentative = elementUtils.getTypeElement(wrapperClassName);
                                if (classRepresentative != null) {
                                    hasCopyCtor = classRepresentative.accept(new ClassConstructorVisitor(), wrapperClassName);
                                } else {
                                    // last attempt to check if there is copy constructor for this class by trying to
                                    // instantiate it this is something that usually does not work
                                    try {
                                        Class<?> c = Class.forName(annotatedElementType.toString());
                                        if (!ClassUtils.isPrimitiveWrapper(c)) { // for some of the primitives wrapper it's very likely
                                            c.getConstructor(c); // to have constructors only for the primitive types which is OK, because they are immutable
                                        }
                                        hasCopyCtor = true;
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }

                        if (!hasCopyCtor) {
                            Element containerClass = annotatedElement.getEnclosingElement();
                            this.warning(annotatedElement,
                                    "Unable to determine if annotated field @%s \"%s %s\"\ndeclared in class"
                                            + " \"%s\"\nhas a copy constructor! This may result run-time inabilities to"
                                            + " provide appropriate values for some event parameters!",
                                            epc.getSimpleName(),
                                            annotatedElementType.toString(),
                                            annotatedElement.toString(),
                                            (containerClass != null ? containerClass.toString() : "")
                            );
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    error(annotatedElement, "%s - @%s : %s", ex.getMessage(), epc.getSimpleName(),
                            annotatedElementType.toString());
                    return true;
                }
            } else {
                error(annotatedElement, "Nonempty annotation value must be specified in @%s - %s !",
                        epc.getSimpleName(), annotatedElement.getSimpleName());
                return true;
            }
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> hs = new LinkedHashSet<>();
        hs.add(EmbeddedParameter.class.getCanonicalName());
        return hs;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * Adds a diagnostic error for specific element.
     * @param e The element for which the error has occurred.
     * @param msg The human-readable message describing the error.
     * @param args Additional parameters to the msg.
     */
    private void error(Element e, String msg, Object... args) {
        this.messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
    }

    /**
     * Adds a diagnostic warning for specific element.
     * @param e The element for which the error has occurred.
     * @param msg The human-readable message describing the error.
     * @param args Additional parameters to the msg.
     */
    private void warning(Element e, String msg, Object... args) {
        this.messager.printMessage(Diagnostic.Kind.WARNING, String.format(msg, args), e);
    }

    /**
     * Determines the type of a single or a multidimensional array.
     * @param arrayElement The array whose type to be determined.
     * @return TypeMirror instance of the determined type.
     */
    private TypeMirror getArrayElementRealType(ArrayType arrayElement) {
        TypeMirror tm = arrayElement.getComponentType();
        while (tm.getKind().equals(TypeKind.ARRAY)) {
            tm = ((ArrayType) tm).getComponentType();
        }
        return tm;
    }

    /**
     * Recursively checks whether the current @link{TypeMirror} has any subtypes that at least partially match
     * @param tm An initialized TypeMirror from which the search to begin.
     * @param typeRegex A regular expression describing the searched type for e.g ^java\.lang\.Comparable\&lt;\s+\&gt;
     * @return True if a match has been found, otherwise false.
     */
    private boolean hasImplementedTypeHereOrAbove(TypeMirror tm, String typeRegex) {
        if (tm == null) return false;
        List<? extends TypeMirror> typeMirrors = typeUtils.directSupertypes(tm);
        if (typeMirrors == null || typeMirrors.size() == 0) {
            return false;
        }
        for (TypeMirror t : typeMirrors) {
            if (t.toString().matches(typeRegex)) {
                return true;
            }
        }
        for (TypeMirror t : typeMirrors) {
            if (hasImplementedTypeHereOrAbove(t, typeRegex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Visitor to investigate if a specific class has a public copy constructor
     */
    class ClassConstructorVisitor extends ElementKindVisitor7<Boolean, String> {
        public ClassConstructorVisitor() {
            super(false);
        }

        @Override
        public Boolean visitExecutableAsConstructor(ExecutableElement e, String p) {
            List<? extends VariableElement> parameters = e.getParameters();

            if (parameters.size() == 1) {
                // messager.printMessage(Diagnostic.Kind.WARNING, "+++ " + e.toString() + " - " + parameters.get(0).asType().toString() + " %%%% " + p);
                String parameterTypeAsString = parameters.get(0).asType().toString();
                if (parameterTypeAsString.equals(p)) {
                    return true;
                } else { // no direct copy constructor. Check if there is one for the implemented/extended above things
                    TypeMirror ownerClass = e.getEnclosingElement().asType();
                    List<? extends TypeMirror> implementedExtendedThings = typeUtils.directSupertypes(ownerClass)
                            .stream().filter(type -> !typeUtils.asElement(type).getKind().isInterface()) // skip any interfaces
                            .collect(Collectors.toList());
                    for (TypeMirror ext_impl : implementedExtendedThings) {
                        if (parameterTypeAsString.equals(ext_impl.toString())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public Boolean visitTypeAsClass(TypeElement e, String p) {
            Boolean hasCtor = super.visitTypeAsClass(e, p);

            //messager.printMessage(Diagnostic.Kind.WARNING, e.toString() + " >>> " + p);

            if (e != null) {
                try { // handle wrapper classes - their copy ctors may not accept the same type, but the wrapped only which is fine
                    Class<?> c = ClassUtils.getClass(e.getQualifiedName().toString());
                    if (c != null) {
                        hasCtor |= ClassUtils.isPrimitiveWrapper(c);
                    }
                } catch (ClassNotFoundException ex) {
                }

                for (Element el : elementUtils.getAllMembers(e)) {
                    if (el.asType().getKind().equals(TypeKind.EXECUTABLE)) {
                        hasCtor |= el.accept(this, p);
                    }
                }

                /*for (Element el : e.getEnclosedElements()) { // returns variables on ordinary constructors only
                    messager.printMessage(Diagnostic.Kind.WARNING, el.toString());
                    if (el.asType().getKind().equals(TypeKind.EXECUTABLE)) {
                        hasCtor |= el.accept(this, null);
                        if (hasCtor) break;
                    }
                }*/
            }

            //messager.printMessage(Diagnostic.Kind.WARNING, "--------------------" + e.toString() + "--------------");
            return hasCtor;
        }
    }

    /**
     * Visitor to check if a concrete event class has a serialVersionUuid field.
     */
    class SerialVersionUIDVisitor extends ElementKindVisitor7<Boolean, Void> {
        public SerialVersionUIDVisitor() {
            super(true);
        }

        @Override
        public Boolean visitTypeAsClass(TypeElement e, Void aVoid) {
            if (e.getModifiers().contains(Modifier.ABSTRACT)) return true;
            for (Element el : e.getEnclosedElements()) {
                if (el.getKind().isField()) {
                    if (el.accept(this, null)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public Boolean visitVariableAsField(VariableElement e, Void aVoid) {
            if (e.asType().getKind().equals(TypeKind.LONG)) {
                Set<Modifier> modifiers = e.getModifiers();
                if (modifiers.contains(Modifier.STATIC) && modifiers.contains(Modifier.FINAL)) {
                    if (e.getSimpleName().toString().equals("serialVersionUID")) {
                        Object val = e.getConstantValue();
                        boolean good = false;
                        if (val != null) {
                            try {
                                good = ((Long)val).longValue() != 0;
                            } catch (Exception ex) {
                                good = false;
                            }
                        }
                        return good;
                    }
                }
            }
            return false;
        }
    }

    /*
     * Visitor to check if a field is an interface or an abstract class.
     *
     class InterfaceOrAbstractClassCheckerVisitor extends ElementKindVisitor6<Boolean, Void> {
         public InterfaceOrAbstractClassCheckerVisitor() {
         super(false);
         }

         @Override
         public Boolean visitTypeAsInterface(TypeElement e, Void p) {
         return true;
         }

         @Override
         public Boolean visitTypeAsClass(TypeElement e, Void p) {
         return e.getModifiers().contains(Modifier.ABSTRACT);
         }
     }*/
}
