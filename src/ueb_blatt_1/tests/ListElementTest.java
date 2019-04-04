package ueb_blatt_1.tests;

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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("static-method")
public class ListElementTest {

    private static final String SRC_LIST_ELEMENT = "ueb_blatt_1.src.ListElement";

    private static TypeVariable<?>[] types = new TypeVariable<?>[2];
    private static Map<String, Field> fields = new HashMap<>();
    private static Map<String, Method> methods = new HashMap<>();

    @Test
    public void testClassStructure() {
        Class<?> listElement = null;
        try {
            listElement = Class.forName(SRC_LIST_ELEMENT);
        } catch (@SuppressWarnings("unused") ClassNotFoundException e) {
            fail("Keine Klasse \"ueb_blatt_1.src.ListElement\" gefunden. Ist die Klasse falsch benannt oder nicht im vorgesehenen Package?");
            return;
        }
        assertFalse("ListElement darf kein Enum sein", listElement.isEnum());
        assertFalse("ListElement darf kein Interface sein", listElement.isInterface());
        assertFalse("ListElement darf keine abstrakte Klasse sein", Modifier.isAbstract(listElement.getModifiers()));
        assertFalse("ListElement darf keine Annotation sein", listElement.isAnnotation());
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
        assertEquals("ListElement sollte exakt 2 Typparameter haben", 2, numT);
        if (listElement.getDeclaredClasses().length != 0) {
            fail("ListElement sollte keine geschachtelten Klassen besitzen.\n"
                    + "(Das ist hier overkill; wenn ihr meint, dass das hier Sinn ergibt, schreibt mir, warum.)");
        }
        types[0] = listElement.getTypeParameters()[0];
        types[1] = listElement.getTypeParameters()[1];
    }

    @Test(timeout = 5000)
    public void testConstructor() throws ReflectiveOperationException {
        makeInstance();
    }

    static Object makeInstance() throws ReflectiveOperationException {
        Class<?> listElement = Class.forName(SRC_LIST_ELEMENT);
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
                instance = c.newInstance(null, null);
            } else if (pars.length == 3) {
                instance = c.newInstance(null, null, null);
            }
        }
        return Objects.requireNonNull(instance, "makeInstance() konnte kein Objekt erzeugen");
    }

    @Test
    public void testFields() throws ReflectiveOperationException {
        Class<?> listElement = Class.forName(SRC_LIST_ELEMENT);
        Field[] fs = listElement.getDeclaredFields();
        for (Field f : fs) {
            f.setAccessible(true);
            assertTrue("Attribute sollten private sein", Modifier.isPrivate(f.getModifiers()));
            assertFalse("Statische Variablen sind hier nicht vorteilhaft/nötig", Modifier.isStatic(f.getModifiers()));
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
        Class<?> listElement = Class.forName(SRC_LIST_ELEMENT);
        Method[] ms = listElement.getDeclaredMethods();
        List<Method> objectMethods = List.of(Object.class.getMethods());
        for (Method m : ms) {
            m.setAccessible(true);
            if (Modifier.isPrivate(m.getModifiers()))
                continue;
            assertTrue("Methodennamen beginnen in Java per Konvention mit einem Kleinbuchstaben:\n" + m,
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
                assertTrue("updateVal2 operator Typparameter ist nicht gleich Nr.1 der Klasse",
                        paramTypeEquals(params[0].getParameterizedType(), UnaryOperator.class, types[1]));
                if (!methods.containsKey("updateVal2"))
                    methods.put("updateVal2", m);
                else
                    fail("updateVal2 sollte es nur einmal geben");
            } else if (params.length == 1 && Object.class.equals(params[0].getType()) && m.getName().equals("equals")
                    && Boolean.TYPE.equals(ret)) {
                // EQUALS
                assertEquals("equals darf keine Typparameter haben", 0, m.getTypeParameters().length);
                assertTrue("equals muss öffentlich sein", Modifier.isPublic(m.getModifiers()));
                if (!methods.containsKey("equals"))
                    methods.put("equals", m);
                else
                    fail("equals sollte es nur einmal geben");
            } else if (params.length == 0 && m.getName().equals("hashCode") && Integer.TYPE.equals(ret)) {
                // HASHCODE
                assertEquals("hashCode darf keine Typparameter haben", 0, m.getTypeParameters().length);
                assertTrue("hashCode muss öffentlich sein", Modifier.isPublic(m.getModifiers()));
                if (!methods.containsKey("hashCode"))
                    methods.put("hashCode", m);
                else
                    fail("hashCode sollte es nur einmal geben");
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
        if (expected.stream().filter(x -> !methods.containsKey(x)).count() > 0) {
            fail("Die folgenden elementaren Bestandteile von ListElement fehlen: "
                    + expected.stream().filter(x -> !methods.containsKey(x)).collect(Collectors.toList()));
        }
    }

    @Test(timeout = 5000)
    public void testMethod_equals() throws ReflectiveOperationException {
        Method equals = methods.get("equals");
        Field next = fields.get("next");
        Field val1 = fields.get("val1");
        Field val2 = fields.get("val2");
        assertNotNull("equals(Object) nicht gefunden", equals);
        assertNotNull("Attribut vom ersten Typ nicht gefunden", val1);
        assertNotNull("Attribut vom zweiten Typ nicht gefunden", val2);
        assertNotNull("Attribut vom nächsten Element nicht gefunden", next);
        Object le1 = makeInstance();
        Object le2 = makeInstance();

        // null Sicherheit
        assertFalse((Boolean) equals.invoke(le1, new Object[] { null }));
        // andere Typen
        assertFalse((Boolean) equals.invoke(le1, new Object()));
        assertFalse((Boolean) equals.invoke(le1, new String("")));
        // Reflexivität 1
        assertTrue((Boolean) equals.invoke(le1, le1));
        assertTrue((Boolean) equals.invoke(le2, le2));
        // Gleichheit & Symmetrie
        assertTrue((Boolean) equals.invoke(le1, le2));
        assertTrue((Boolean) equals.invoke(le2, le1));
        val1.set(le1, new String("a"));
        assertFalse((Boolean) equals.invoke(le1, le2));
        assertFalse((Boolean) equals.invoke(le2, le1));
        // ... Reflexivität 2
        assertTrue((Boolean) equals.invoke(le1, le1));
        assertTrue((Boolean) equals.invoke(le2, le2));
        val1.set(le2, new String("a"));
        assertTrue((Boolean) equals.invoke(le1, le2));
        assertTrue((Boolean) equals.invoke(le2, le1));
        // ... with val2
        val2.set(le1, Integer.valueOf(10_000));
        assertFalse((Boolean) equals.invoke(le1, le2));
        assertFalse((Boolean) equals.invoke(le2, le1));
        val2.set(le2, Integer.valueOf(10_000));
        assertTrue((Boolean) equals.invoke(le1, le2));
        assertTrue((Boolean) equals.invoke(le2, le1));
        // null Sicherheit mit non-null Werten
        assertFalse((Boolean) equals.invoke(le1, new Object[] { null }));
        // next hat keinen Einfluss
        class NoEquals {
            @Override
            public boolean equals(Object obj) {
                fail("Equals sollte hier nicht aufgerufen werden");
                throw new RuntimeException();
            }

            @Override
            public int hashCode() {
                return 0;
            }
        }
        Object le3 = makeInstance();
        Object le4 = makeInstance();
        val1.set(le3, new NoEquals());
        val1.set(le4, new NoEquals());
        val2.set(le3, new NoEquals());
        val2.set(le4, new NoEquals());
        next.set(le1, le3);
        next.set(le2, le4);
        assertTrue((Boolean) equals.invoke(le1, le2));
        assertTrue((Boolean) equals.invoke(le2, le1));
        // equals Reflexivität über Referenz-Gleichheit
        val1.set(le1, new NoEquals());
        val2.set(le1, new NoEquals());
        assertTrue((Boolean) equals.invoke(le1, le1));
        assertLockFree(le1, le2);

        testParallel(50, 500, 500, () -> equals.invoke(le1, le2), () -> equals.invoke(le2, le1));
        assertLockFree(le1, le2);
    }

    @Test(timeout = 5000)
    public void testMethod_getSetVal1() throws ReflectiveOperationException {
        Method get = methods.get("getVal1");
        Method set = methods.get("setVal1");
        Field val1 = fields.get("val1");
        assertNotNull("Getter für den ersten Typ nicht gefunden", get);
        assertNotNull("Setter für den ersten Typ nicht gefunden", set);
        assertNotNull("Attribut vom ersten Typ nicht gefunden", val1);

        testGetSetVal(val1, get, set);
    }

    @Test(timeout = 5000)
    public void testMethod_getSetVal2() throws ReflectiveOperationException {
        Method get = methods.get("getVal2");
        Method set = methods.get("setVal2");
        Field val2 = fields.get("val2");
        assertNotNull("Getter für den zweiten Typ nicht gefunden", get);
        assertNotNull("Setter für den zweiten Typ nicht gefunden", set);
        assertNotNull("Attribut vom zweiten Typ nicht gefunden", val2);

        testGetSetVal(val2, get, set);
    }

    private void testGetSetVal(Field val, Method get, Method set) throws ReflectiveOperationException {
        Object le = makeInstance();
        Object o = new Object();
        // GETTER
        val.set(le, null);
        assertNull(get.invoke(le));
        val.set(le, "test");
        assertSame("test", get.invoke(le));
        val.set(le, o);
        assertSame(o, get.invoke(le));
        // SETTER
        set.invoke(le, new Object[] { null });
        assertNull(val.get(le));
        set.invoke(le, "test");
        assertSame("test", val.get(le));
        set.invoke(le, o);
        assertSame(o, val.get(le));
        assertLockFree(le);

        testParallel(50, 500, 200, () -> val.get(le), () -> set.invoke(le, o));
        assertSame(o, val.get(le));
        assertLockFree(le);
    }

    @Test(timeout = 5000)
    public void testMethod_updateVal1() throws ReflectiveOperationException, InterruptedException {
        Method update = methods.get("updateVal1");
        Field val1 = fields.get("val1");
        assertNotNull("updateVal1 nicht gefunden", update);
        assertNotNull("Attribut vom ersten Typ nicht gefunden", val1);

        testUpdateMethod(val1, update);
    }

    @Test(timeout = 5000)
    public void testMethod_updateVal2() throws ReflectiveOperationException, InterruptedException {
        Method update = methods.get("updateVal2");
        Field val2 = fields.get("val2");
        assertNotNull("updateVal2 nicht gefunden", update);
        assertNotNull("Attribut vom zweiten Typ nicht gefunden", val2);

        testUpdateMethod(val2, update);
    }

    private void testUpdateMethod(Field val, Method update) throws ReflectiveOperationException, InterruptedException {
        Object le = makeInstance();

        val.set(le, Integer.valueOf(0));
        update.invoke(le, (UnaryOperator<Object>) i -> Integer.valueOf((Integer) i + 1));
        assertEquals(Integer.valueOf(1), val.get(le));

        ExecutorService pool = Executors.newFixedThreadPool(10);
        UnaryOperator<Object> incAndSleep = i -> {
            Integer res = Integer.valueOf((Integer) i + 1);
            try {
                Thread.sleep(10);
            } catch (@SuppressWarnings("unused") InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return res;
        };
        val.set(le, Integer.valueOf(0));
        for (int j = 0; j < 50; j++) {
            pool.submit(() -> {
                update.invoke(le, incAndSleep);
                return null;
            });
        }

        pool.shutdown();
        boolean finished = pool.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(finished);
        assertEquals(Integer.valueOf(50), val.get(le));
        assertLockFree(le);

        testParallel(50, 500, 500, () -> update.invoke(le, (UnaryOperator<Object>) x -> ((Integer) x) + 1));
        assertEquals(Integer.valueOf(550), val.get(le));
        assertLockFree(le);
    }

    @Test(timeout = 5000)
    public void testMethod_getValLock() throws ReflectiveOperationException {
        Method gl1 = methods.get("getVal1Lock");
        Method gl2 = methods.get("getVal2Lock");
        Object le = makeInstance();

        Object l1 = gl1.invoke(le);
        Object l2 = gl2.invoke(le);
        assertNotNull(l1);
        assertNotNull(l2);
        assertNotSame(l1, l2);
        assertLockFree(le);
    }

    @Test(timeout = 5000)
    public void testMethodsCombined1()
            throws ReflectiveOperationException, InterruptedException, BrokenBarrierException {
        Method getThis = methods.get("getVal1");
        Method setThis = methods.get("setVal1");
        Method updateThis = methods.get("updateVal1");
        Method getOther = methods.get("getVal2");
        Method setOther = methods.get("setVal2");
        Method updateOther = methods.get("updateVal2");
        testMethodsCombined(getThis, getOther, setThis, setOther, updateThis, updateOther);
    }

    @Test(timeout = 5000)
    public void testMethodsCombined2()
            throws ReflectiveOperationException, InterruptedException, BrokenBarrierException {
        Method getThis = methods.get("getVal2");
        Method setThis = methods.get("setVal2");
        Method updateThis = methods.get("updateVal2");
        Method getOther = methods.get("getVal1");
        Method setOther = methods.get("setVal1");
        Method updateOther = methods.get("updateVal1");
        testMethodsCombined(getThis, getOther, setThis, setOther, updateThis, updateOther);
    }

    private void testMethodsCombined(Method get1, Method get2, Method set1, Method set2, Method update1, Method update2)
            throws ReflectiveOperationException, InterruptedException, BrokenBarrierException {
        Object le = makeInstance();

        CyclicBarrier cb = new CyclicBarrier(2);
        UnaryOperator<Object> wait = i -> {
            try {
                cb.await();
                cb.await();
            } catch (@SuppressWarnings("unused") InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
            }
            return i;
        };
        ExecutorService blocker = Executors.newSingleThreadExecutor();
        blocker.submit(() -> {
            update1.invoke(le, wait);
            return null;
        });
        cb.await(); // WAIT
        ExecutorService working = Executors.newFixedThreadPool(4);
        working.submit(() -> {
            update2.invoke(le, (UnaryOperator<Object>) i -> i);
            return null;
        });
        working.submit(() -> {
            get2.invoke(le, new Object());
            return null;
        });
        working.submit(() -> {
            set2.invoke(le, new Object());
            return null;
        });
        working.shutdown();
        Thread.sleep(100); // Safety because auf ReentrantReadWriteLock behaviour
        ExecutorService blocked = Executors.newFixedThreadPool(4);
        blocked.submit(() -> {
            get1.invoke(le, new Object());
            return null;
        });
        blocked.submit(() -> {
            set1.invoke(le, new Object());
            return null;
        });
        assertTrue("Methoden vom andere Wert werden blockiert", working.awaitTermination(500, TimeUnit.MILLISECONDS));
        blocked.shutdown();
        assertFalse("get und set werden nicht blockiert", blocked.awaitTermination(500, TimeUnit.MILLISECONDS));
        try {
            cb.await(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            fail(e.toString());
        }
        assertTrue("get und set noch immer blockiert", blocked.awaitTermination(500, TimeUnit.MILLISECONDS));
        // Funktioniert alles noch?
        set1.invoke(le, "abc");
        set2.invoke(le, "abc");
        get1.invoke(le);
        get2.invoke(le);
        update1.invoke(le, (UnaryOperator<Object>) i -> {
            if (i != "abc")
                fail();
            return i;
        });
        update2.invoke(le, (UnaryOperator<Object>) i -> {
            if (i != "abc")
                fail();
            return i;
        });
        blocker.shutdown();
        blocker.awaitTermination(1, TimeUnit.SECONDS);
        assertLockFree(le);

        testParallel(50, 500, 500, () -> set1.invoke(le, "x"), () -> set2.invoke(le, "y"), () -> get1.invoke(le),
                () -> get2.invoke(le), () -> methods.get("equals").invoke(le, le),
                () -> update1.invoke(le, (UnaryOperator<Object>) x -> x),
                () -> update2.invoke(le, (UnaryOperator<Object>) x -> x));
        assertEquals("x", get1.invoke(le));
        assertEquals("y", get2.invoke(le));
        assertLockFree(le);
    }

    @Test(timeout = 5000)
    public void testMethod_hashCode() throws ReflectiveOperationException {
        if (!methods.containsKey("hashCode"))
            return; // War nicht Teil der Aufgabenstellung
        Method hc = methods.get("hashCode");
        Object le1 = makeInstance();
        Object le2 = makeInstance();
        assertEquals(hc.invoke(le1), hc.invoke(le2));

        Field val1 = fields.get("val1");
        Field val2 = fields.get("val2");
        val1.set(le1, "xyz");
        val2.set(le1, new Object());
        val1.set(le2, null);
        val2.set(le2, Integer.valueOf(128));
        assertEquals(hc.invoke(le1), hc.invoke(le2));
        assertLockFree(le1, le2);

        testParallel(50, 500, 500, () -> hc.invoke(le1));
        assertLockFree(le1, le2);
    }

    private void testParallel(int threads, int tasks, int msecTimeOut, Callable<?>... taskTypes) {
        CyclicBarrier cb = new CyclicBarrier(threads + 1);
        ExecutorService es = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < tasks; i++) {
            final int index = i;
            es.submit(() -> {
                try {
                    if (index < threads)
                        cb.await();
                    taskTypes[index % taskTypes.length].call();
                } catch (InterruptedException | BrokenBarrierException e) {
                    fail(e.toString());
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    fail(e.toString());
                }
            });
        }
        try {
            cb.await();
            es.shutdown();
            assertTrue(es.awaitTermination(msecTimeOut, TimeUnit.MILLISECONDS));
        } catch (InterruptedException | BrokenBarrierException e) {
            fail(e.toString());
            Thread.currentThread().interrupt();
        }
    }

    static void assertLockFree(Object... les) throws IllegalAccessException {
        for (Object le : les) {
            ConcurrentListTest.assertLockFree((ReentrantReadWriteLock) fields.get("lock1").get(le));
            ConcurrentListTest.assertLockFree((ReentrantReadWriteLock) fields.get("lock2").get(le));
        }
    }

    public static boolean signaturesEqual(Method m1, Method m2) {
        if (m1 == m2)
            return true;
        if (m1 == null || m2 == null)
            return false;
        if (!Objects.equals(m1.getName(), m2.getName()))
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
