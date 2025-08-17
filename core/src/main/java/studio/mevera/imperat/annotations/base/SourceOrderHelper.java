package studio.mevera.imperat.annotations.base;

import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

public class SourceOrderHelper {

    /**
     * Gets methods in their original source code declaration order
     */
    public static List<Method> getMethodsInSourceOrder(Class<?> clazz) throws Exception {
        String className = clazz.getName().replace('.', '/') + ".class";
        ClassLoader loader = clazz.getClassLoader();
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        
        try (InputStream stream = loader.getResourceAsStream(className)) {
            if (stream == null) {
                throw new IOException("Class resource not found: " + className);
            }

            // First pass: Get method order from ASM
            ClassReader reader = new ClassReader(stream);
            MethodOrderVisitor visitor = new MethodOrderVisitor();
            reader.accept(visitor, ClassReader.SKIP_DEBUG);

            // Create map of method signatures to reflection Method objects
            Map<String, Method> methodMap = new HashMap<>();
            for (Method method : clazz.getDeclaredMethods()) {
                String key = method.getName() + Type.getMethodDescriptor(method);
                methodMap.put(key, method);
            }

            // Build ordered list based on ASM visitation order
            List<Method> orderedMethods = new ArrayList<>();
            for (MethodSignature signature : visitor.getMethodSignatures()) {
                Method method = methodMap.get(signature.name + signature.descriptor);
                if (method != null && !method.isSynthetic()) {
                    orderedMethods.add(method);
                }
            }

            return orderedMethods;
        }
    }
    
    /**
     * Gets inner classes (both static and non-static) in their original source code declaration order
     * Debug version with detailed logging
     */
    public static List<Class<?>> getInnerClassesInSourceOrder(Class<?> outerClass) throws Exception {
        String className = outerClass.getName().replace('.', '/') + ".class";
        try (InputStream stream = outerClass.getClassLoader().getResourceAsStream(className)) {
            if (stream == null) {
                throw new IOException("Class resource not found: " + className);
            }
            
            // First pass: Get inner class order from ASM
            ClassReader reader = new ClassReader(stream);
            InnerClassOrderVisitor visitor = new InnerClassOrderVisitor(outerClass.getName());
            reader.accept(visitor, ClassReader.SKIP_DEBUG);
            
            // Debug: Print what ASM visitor found
            List<String> innerClassNames = visitor.getInnerClassNames();
            
            // Create map of inner class names to Class objects
            Map<String, Class<?>> classMap = new HashMap<>();
            
            // Use getDeclaredClasses() to get all inner classes (static and non-static)
            // and combine with getNestMembers() to ensure we don't miss any
            
            // getDeclaredClasses() returns all classes declared within this class
            Set<Class<?>> allInnerClasses = new HashSet<>(Arrays.asList(outerClass.getDeclaredClasses()));
            
            // getNestMembers() includes nested classes that might be missed by getDeclaredClasses()
            for (Class<?> nestMember : outerClass.getDeclaredClasses()) {
                if (!nestMember.equals(outerClass)) { // Exclude the outer class itself
                    allInnerClasses.add(nestMember);
                }
                
            }
            
            // Map all inner classes by name
            for (Class<?> innerClass : allInnerClasses) {
                classMap.put(innerClass.getName(), innerClass);
            }
            
            // Build ordered list based on ASM visitation order
            List<Class<?>> orderedClasses = new ArrayList<>();
            
            // Debug: Check mapping
            for (String innerClassName : innerClassNames) {
                Class<?> innerClass = classMap.get(innerClassName);
                if (innerClass != null) {
                    orderedClasses.add(innerClass);
                }
            }
            return orderedClasses;
        }
    }

    /**
     * Gets all static inner classes, including those defined in inner classes
     */
    public static List<Class<?>> getAllNestedStaticClassesInSourceOrder(Class<?> rootClass) throws Exception {
        List<Class<?>> result = new ArrayList<>();
        Queue<Class<?>> queue = new LinkedList<>();
        queue.add(rootClass);

        while (!queue.isEmpty()) {
            Class<?> currentClass = queue.poll();
            // Skip the root class itself, only add its inner classes
            if (currentClass != rootClass) {
                result.add(currentClass);
            }

            // Add all inner classes to the queue for further processing
            List<Class<?>> innerClasses = getInnerClassesInSourceOrder(currentClass);
            queue.addAll(innerClasses);
        }

        return result;
    }

    private static class MethodOrderVisitor extends ClassVisitor {
        private final List<MethodSignature> methodSignatures = new ArrayList<>();

        public MethodOrderVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            // Record method signature in visitation order
            methodSignatures.add(new MethodSignature(name, descriptor));
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        public List<MethodSignature> getMethodSignatures() {
            return methodSignatures;
        }
    }

    private static class InnerClassOrderVisitor extends ClassVisitor {
        private final List<String> innerClassNames = new ArrayList<>();
        private final String outerClassName;

        public InnerClassOrderVisitor(String outerClassName) {
            super(Opcodes.ASM9);
            this.outerClassName = outerClassName;
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            // Check if it's a static inner class
            //if ((access & Opcodes.ACC_STATIC) != 0) {
                // Convert binary name to fully qualified name
                String fullClassName = name.replace('/', '.');

                // Only add if it's an immediate inner class of the outer class we're examining
                String expectedOuterName = outerClassName.replace('.', '/');
                if (outerName != null && outerName.equals(expectedOuterName)) {
                    // Add to the beginning of our list to reverse the order ASM provides
                    // This fixes the reverse order issue
                    innerClassNames.add(0, fullClassName);
                }
            //}
            super.visitInnerClass(name, outerName, innerName, access);
        }

        public List<String> getInnerClassNames() {
            return innerClassNames;
        }
    }

    private static class MethodSignature {
        final String name;
        final String descriptor;

        MethodSignature(String name, String descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }
    }
}