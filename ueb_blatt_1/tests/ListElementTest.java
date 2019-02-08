package tests;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("static-method")
public class ListElementTest {

    private static TypeVariable<?>[] types = new TypeVariable<?>[2];
    private static Map<String, Field> fields = new HashMap<>();
    private static Map<String, Method> methods = new HashMap<>();

    @Test
    public void testClassStructure() {
        Class<?> listElement = null;
        try {
            listElement = Class.forName("src.ListElement");
        } catch (@SuppressWarnings("unused") ClassNotFoundException e) {
            fail("Keine Klasse \"src.ListElement\" gefunden. Ist die Klasse falsch benannt oder nicht im vorgesehenen Package?");
            return;
        }
        assertFalse("ListElement darf kein Enum sein", listElement.isEnum());
        assertFalse("ListElement darf kein Interface sein", listElement.isInterface());
        assertFalse("ListElement darf keine abstrakte Klasse sein", Modifier.isAbstract(listElement.getModifiers()));
        assertFalse("ListElement darf keine Annotaion sein", listElement.isAnnotation());
        assertEquals("ListElement sollte von keiner Klasse (außer Object) erben", Object.class,
                listElement.getSuperclass());
        if (listElement.getInterfaces().length != 0) {
            // Vielleicht sind manche Interfaces hier sinnvoll, aber die Aufgabenstellung
            // verlangt nicht danach.
            fail("ListElement sollte keine Interfaces implementieren: " + Arrays.toString(listElement.getInterfaces()));
        }
        int numT = 0;
        for (TypeVariable<?> typeParam : listElement.getTypeParameters()) {
            for (Type bound : typeParam.getBounds()) {
                assertEquals("ListElement Typparameter " + (numT + 1) + " ist bounded", Object.class, bound);
            }
            numT++;
        }
        if (listElement.getDeclaredClasses().length != 0) {
            fail("ListElement sollte keine geschachtelten Klassen besitzen.\n"
                    + "(Das ist hier overkill; wenn ihr meint, dass das hier Sinn ergibt, schreibt mir, warum.)");
        }
        assertEquals("ListElement sollte exakt 2 Typparameter haben", 2, numT);
        types[0] = listElement.getTypeParameters()[0];
        types[1] = listElement.getTypeParameters()[1];
    }

    @Test
    public void testConstructor() throws ReflectiveOperationException {
        makeInstance();
    }

    Object makeInstance() throws ReflectiveOperationException {
        Class<?> listElement = Class.forName("src.ListElement");
        Constructor<?>[] cs = listElement.getDeclaredConstructors();
        Object instance = null;
        for (Constructor<?> c : cs) {
            c.setAccessible(true);
            assertFalse("kein Konstruktor sollte public sein", Modifier.isPublic(c.getModifiers()));
            if (instance != null)
                continue;
            Class<?>[] pars = c.getParameterTypes();
            if (pars.length == 0) {
                instance = c.newInstance();
            } else if (pars.length == 1) {
                instance = c.newInstance(new Object[] { null });
            } else if (pars.length == 2) {
                instance = c.newInstance(new Object(), new Object());
            } else if (pars.length == 3) {
                Object[] args = new Object[3];
                for (int i = 0; i < cs.length; i++) {
                    if (pars[i].equals(listElement))
                        args[i] = null;
                    else
                        args[i] = new Object();
                }
                instance = c.newInstance(args);
            }
        }
        return Objects.requireNonNull(instance, "makeInstance() konnte kein Objekt erzeugen");
    }

    @Test
    public void testFields() throws ReflectiveOperationException {
        Class<?> listElement = Class.forName("src.ListElement");
        Field[] fs = listElement.getDeclaredFields();
        for (Field f : fs) {
            f.setAccessible(true);
            assertTrue("Attribute sollten private sein", Modifier.isPrivate(f.getModifiers()));
            assertFalse("Statische Variablen sind hier nicht vorteilhaft/ntöig", Modifier.isStatic(f.getModifiers()));
            if (ReentrantReadWriteLock.class.equals(f.getType())) {
                if (!fields.containsKey("lock1"))
                    fields.put("lock1", f);
                else if (!fields.containsKey("lock2"))
                    fields.put("lock2", f);
                else
                    fail("ListElement sollte nicht mehr als zwei locks haben");
            } else if (listElement.equals(f.getType())) {
                if (!fields.containsKey("next"))
                    fields.put("next", f);
                else
                    fail("ListElement soll für eine einfach verkettete Liste sein, also nur das nächste Element speichern");
            } else if (types[0].equals(f.getGenericType())) {
                if (!fields.containsKey("val1"))
                    fields.put("val1", f);
                else
                    fail("Warum hat ListElement mehr als einen Wert vom Typ " + types[0]);
            } else if (types[1].equals(f.getGenericType())) {
                if (!fields.containsKey("val2"))
                    fields.put("val2", f);
                else
                    fail("Warum hat ListElement mehr als einen Wert vom Typ " + types[1]);
            }
        }
        Set<String> expected = Set.of("val1", "val2", "lock1", "lock2", "next");
        if (!expected.equals(fields.keySet())) {
            fail("Die folgenden elementaren Bestandteile von ListElement fehlen: "
                    + expected.stream().filter(x -> !fields.containsKey(x)).collect(Collectors.toList()));
        }
    }

    @Test
    public void testMethodContracts() throws ReflectiveOperationException {
        Class<?> listElement = Class.forName("src.ListElement");
        Method[] ms = listElement.getDeclaredMethods();
        List<Method> objectMethods = List.of(Object.class.getMethods());
        for (Method m : ms) {
            m.setAccessible(true);
            if (Modifier.isPrivate(m.getModifiers()))
                continue;
            assertTrue("Methodennamen beginnen in Java per Konvention mit einem Kleinbuchstaben:\n"+m,
                    Character.isLowerCase(m.getName().charAt(0)));
            Type ret = m.getGenericReturnType();
            Parameter[] params = m.getParameters();
            if (ReentrantReadWriteLock.class.equals(ret) && params.length == 0) {
                // GETTER FOR LOCKS
                assertTrue("Getter " + m + "sollte mit get anfangen", m.getName().startsWith("get"));
                assertEquals("Getter " + m + " sollte keine Typparameter haben", 0, m.getTypeParameters().length);
                assertFalse("Getter " + m + " sollte nicht öffentlich sein", Modifier.isPublic(m.getModifiers()));
                if (!methods.containsKey("getVal1Lock"))
                    methods.put("getVal1Lock", m);
                else if (!methods.containsKey("getVal2Lock"))
                    methods.put("getVal2Lock", m);
                else
                    fail("ListElement sollte nicht mehr als zwei Methoden für die Locks haben");
            } else if (paramTypeEquals(ret, listElement, types) && params.length == 0) {
                // GET NEXT (prefix get not specified)
                assertEquals("Getter " + m + " sollte keine Typparameter haben", 0, m.getTypeParameters().length);
                assertFalse("Getter " + m + " sollte nicht öffentlich sein", Modifier.isPublic(m.getModifiers()));
                if (!methods.containsKey("getNext"))
                    methods.put("getNext", m);
                else
                    fail("ListElement sollte nur einen Getter für das nächste ListElement haben");
            } else if (params.length == 1 && listElement.equals(params[0].getType()) && Void.TYPE.equals(ret)) {
                // SET NEXT
                assertEquals("Setter " + m + " sollte keine Typparameter haben", 0, m.getTypeParameters().length);
                assertFalse("Setter " + m + " sollte nicht öffentlich sein", Modifier.isPublic(m.getModifiers()));
                assertTrue("Setter " + m + " Typparameter Nr.1 ist nicht gleich Nr.1 der Klasse",
                        params[0].getType().getTypeParameters()[0].equals(types[0]));
                assertTrue("Setter " + m + " Typparameter Nr.2 ist nicht gleich Nr.2 der Klasse",
                        params[0].getType().getTypeParameters()[1].equals(types[1]));
                if (!methods.containsKey("setNext"))
                    methods.put("setNext", m);
                else
                    fail("ListElement sollte nur einen Setter für das nächste ListElement haben");
            } else if (params.length == 1 && UnaryOperator.class.equals(params[0].getType())
                    && m.getName().equals("updateVal1") && Void.TYPE.equals(ret)) {
                // UPDATE 1
                assertEquals("updateVal1 sollte keine Typparameter haben", 0, m.getTypeParameters().length);
                assertTrue("updateVal1 muss öffentlich sein", Modifier.isPublic(m.getModifiers()));
                assertTrue("updateVal1 operator Typparameter ist nicht gleich Nr.1 der Klasse",
                        paramTypeEquals(params[0].getParameterizedType(), UnaryOperator.class, types[0]));
                if (!methods.containsKey("updateVal1"))
                    methods.put("updateVal1", m);
                else
                    fail("updateVal1 sollte es nur einmal geben");
            } else if (params.length == 1 && UnaryOperator.class.equals(params[0].getType())
                    && m.getName().equals("updateVal2") && Void.TYPE.equals(ret)) {
                // UPDATE 2
                assertEquals("updateVal2 sollte keine Typparameter haben", 0, m.getTypeParameters().length);
                assertTrue("updateVal2 muss öffentlich sein", Modifier.isPublic(m.getModifiers()));
                assertTrue("updateVal1 operator Typparameter ist nicht gleich Nr.1 der Klasse",
                        paramTypeEquals(params[0].getParameterizedType(), UnaryOperator.class, types[1]));
                if (!methods.containsKey("updateVal2"))
                    methods.put("updateVal2", m);
                else
                    fail("updateVal2 sollte es nur einmal geben");
            } else if (params.length == 1 && Object.class.equals(params[0].getType()) && m.getName().equals("equals")
                    && Boolean.TYPE.equals(ret)) {
                // EQUALS
                assertEquals("updateVal2 darf keine Typparameter haben", 0, m.getTypeParameters().length);
                assertTrue("equals muss öffentlich sein", Modifier.isPublic(m.getModifiers()));
                if (!methods.containsKey("equals"))
                    methods.put("equals", m);
                else
                    fail("equals sollte es nur einmal geben");
            } else if (ret.equals(types[0]) && params.length == 0) {
                // GETTER 1
                assertTrue("Getter " + m + " sollte mit get anfangen", m.getName().startsWith("get"));
                assertEquals("Getter " + m + " sollte keine Typparameter haben", 0, m.getTypeParameters().length);
                assertTrue("Getter " + m + " sollte nicht öffentlich sein", Modifier.isPublic(m.getModifiers()));
                if (!methods.containsKey("getVal1"))
                    methods.put("getVal1", m);
                else
                    fail("ListElement sollte nur einen Getter für Typ Nr.1 haben");
            } else if (ret.equals(types[1]) && params.length == 0) {
                // GETTER 2
                assertTrue("Getter " + m + " sollte mit get anfangen", m.getName().startsWith("get"));
                assertEquals("Getter " + m + " sollte keine Typparameter haben", 0, m.getTypeParameters().length);
                assertTrue("Getter " + m + " sollte nicht öffentlich sein", Modifier.isPublic(m.getModifiers()));
                if (!methods.containsKey("getVal2"))
                    methods.put("getVal2", m);
                else
                    fail("ListElement sollte nur einen Getter für Typ Nr.2 haben");
            } else if (params.length == 1 && types[0].equals(params[0].getParameterizedType())
                    && Void.TYPE.equals(ret)) {
                // SETTER 1
                assertTrue("Setter " + m + " sollte mit set anfangen", m.getName().startsWith("set"));
                assertEquals("Setter " + m + " sollte keine Typparameter haben", 0, m.getTypeParameters().length);
                assertTrue("Setter " + m + " sollte nicht öffentlich sein", Modifier.isPublic(m.getModifiers()));
                if (!methods.containsKey("setVal1"))
                    methods.put("setVal1", m);
                else
                    fail("ListElement sollte nur einen Setter für Typ Nr.1 haben");
            } else if (params.length == 1 && types[1].equals(params[0].getParameterizedType())
                    && Void.TYPE.equals(ret)) {
                // SETTER 2
                assertTrue("Setter " + m + " sollte mit set anfangen", m.getName().startsWith("set"));
                assertEquals("Setter " + m + " sollte keine Typparameter haben", 0, m.getTypeParameters().length);
                assertTrue("Setter " + m + " sollte nicht öffentlich sein", Modifier.isPublic(m.getModifiers()));
                if (!methods.containsKey("setVal2"))
                    methods.put("setVal2", m);
                else
                    fail("ListElement sollte nur einen Setter für Typ Nr.2 haben");
            } else if (objectMethods.stream().noneMatch(om -> signaturesEqual(om, m)))
                fail("Methode " + m + " ist nicht in der öffentlichen API der Angabe spezifiziert");
        }
        Set<String> expected = Set.of("getVal1Lock", "getVal2Lock", "getNext", "setNext", "updateVal1", "updateVal2",
                "getVal1", "getVal2", "setVal1", "setVal2", "equals");
        if (!expected.equals(methods.keySet())) {
            fail("Die folgenden elementaren Bestandteile von ListElement fehlen: "
                    + expected.stream().filter(x -> !methods.containsKey(x)).collect(Collectors.toList()));
        }
    }

    public static boolean signaturesEqual(Method m1, Method m2) {
        if (m1 == m2)
            return true;
        if (m1 == null || m2 == null)
            return false;
        if (!Objects.deepEquals(m1.getName(), m2.getName()))
            return false;
        return Objects.deepEquals(m1.getGenericParameterTypes(), m2.getGenericParameterTypes());
    }

    public static boolean paramTypeEquals(Type parameterizedType, Class<?> classOfType,
            TypeVariable<?>... typeVarsOfType) {
        StringBuilder typeName = new StringBuilder(classOfType.getName());
        if (typeVarsOfType != null && typeVarsOfType.length > 0)
            typeName.append(Arrays.stream(typeVarsOfType).map(TypeVariable::getName)
                    .collect(Collectors.joining(", ", "<", ">")));
        return parameterizedType.getTypeName().equals(typeName.toString());
    }
}
