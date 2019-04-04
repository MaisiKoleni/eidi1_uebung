package ueb_blatt_1.tests;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import ueb_blatt_1.src.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("static-method")
public class ConcurrentListTest {

    private static final String SRC_LIST_ELEMENT = "ueb_blatt_1.src.ListElement";
    private static final String SRC_CONCURRENT_LIST = "ueb_blatt_1.src.ConcurrentList";

    @Test
    public void testClassStructure() {
        Class<?> list = null;
        try {
            list = Class.forName(SRC_CONCURRENT_LIST);
        } catch (@SuppressWarnings("unused") ClassNotFoundException e) {
            fail("Keine Klasse \"ueb_blatt_1.src.ConcurrentList\" gefunden. Ist die Klasse falsch benannt oder nicht im vorgesehenen Package?");
            return;
        }
        assertFalse("ConcurrentList darf kein Enum sein", list.isEnum());
        assertFalse("ConcurrentList darf kein Interface sein", list.isInterface());
        assertFalse("ConcurrentList darf keine abstrakte Klasse sein", Modifier.isAbstract(list.getModifiers()));
        assertFalse("ConcurrentList darf keine Annotation sein", list.isAnnotation());
        assertEquals("ConcurrentList sollte von keiner Klasse (außer Object) erben", Object.class,
                list.getSuperclass());
        if (list.getInterfaces().length != 1) {
            fail("ConcurrentList sollte einzig das Interface ueb_blatt_1.src.List implementieren, gefunden: "
                    + Arrays.toString(list.getInterfaces()));
        }
        if (!list.getInterfaces()[0].equals(List.class)) {
            fail("ConcurrentList sollte das Interface ueb_blatt_1.src.List implementieren, gefunden: " + list.getInterfaces()[0]);
        }
        int numT = 0;
        for (TypeVariable<?> typeParam : list.getTypeParameters()) {
            for (Type bound : typeParam.getBounds()) {
                assertEquals("ConcurrentList Typparameter " + (numT + 1) + " ist bounded", Object.class, bound);
            }
            numT++;
        }
        assertEquals("ConcurrentList sollte exakt 2 Typparameter haben", 2, numT);
        if (list.getDeclaredClasses().length != 0) {
            fail("ConcurrentList sollte keine geschachtelten Klassen besitzen.\n"
                    + "(Das ist hier overkill; wenn ihr meint, dass das hier Sinn ergibt, schreibt mir, warum.)");
        }
    }

    @Test(timeout = 5000)
    public void testConstructor() throws ReflectiveOperationException {
        makeInstance();
    }

    @SuppressWarnings("unchecked")
    private static List<Object, Object> makeInstance() throws ReflectiveOperationException {
        return (List<Object, Object>) Class.forName(SRC_CONCURRENT_LIST).getConstructor().newInstance();
    }

    @Test
    public void testFields() throws ReflectiveOperationException {
        Class<?> list = Class.forName(SRC_CONCURRENT_LIST);
        Class<?> listElement = Class.forName(SRC_LIST_ELEMENT);
        Field[] fs = list.getDeclaredFields();
        HashSet<Class<?>> fields = new HashSet<>(java.util.List.of(ReentrantReadWriteLock.class, listElement));
        for (Field f : fs) {
            f.setAccessible(true);
            assertTrue("Attribute sollten private sein", Modifier.isPrivate(f.getModifiers()));
            assertFalse("Statische Variablen sind hier nicht vorteilhaft/nötig", Modifier.isStatic(f.getModifiers()));
            if (!fields.remove(f.getType())) {
                fail("Attribut zuviel (nicht erlaubt): " + f);
            }
        }
        if (!fields.isEmpty())
            fail("Folgende Attribute fehlen: " + fields);
    }

    @Test
    public void testMethodContracts() throws ReflectiveOperationException {
        Class<?> list = Class.forName(SRC_CONCURRENT_LIST);
        java.util.List<Method> ms = new ArrayList<>(java.util.List.of(list.getDeclaredMethods()));
        ms.removeIf(m -> Modifier.isPrivate(m.getModifiers()));
        for (Method om : Object.class.getMethods()) {
            ms.removeIf(m -> signaturesEqual(m, om));
        }
        for (Method lm : List.class.getDeclaredMethods()) {
            ms.removeIf(m -> signaturesEqual(m, lm));
        }
        assertTrue("Folgende Methoden sind nicht spezifiziert worden / sollte dann private sein: " + ms, ms.isEmpty());
    }

    @Test(timeout = 5000)
    public void testMethod_hashCode() throws ReflectiveOperationException {
        try {
            Class.forName(SRC_CONCURRENT_LIST).getDeclaredMethod("hashCode");
        } catch (@SuppressWarnings("unused") NoSuchMethodException e) {
            return; // War nicht gefordert
        }
        List<Object, Object> l1 = makeInstance();
        List<Object, Object> l2 = makeInstance();
        assertEquals(l1.hashCode(), l2.hashCode());
        l1.add(null, null);
        l2.add(new Object(), "abc");
        l2.add("!", null);
        assertEquals(l1.hashCode(), l2.hashCode());
        assertLockFree(l1, l2);

        testParallel(50, 500, 1000, () -> assertEquals(l1.hashCode(), l2.hashCode()));
        assertLockFree(l1, l2);
    }

    @Test(timeout = 5000)
    public void testMethod_add() throws ReflectiveOperationException {
        List<Object, Object> l = makeInstance();
        l.add(null, null);
        l.add("abc", null);
        l.add(null, "xyz");
        l.add(new Object(), new Object());
        for (int i = 0; i < 5_000; i++) {
            l.add(Integer.valueOf(i), Integer.valueOf(i));
        }
        assertLockFree(l);

        testParallel(50, 500, 1000, () -> l.add(null, null));
        assertEquals(5504, l.size());
        assertLockFree(l);
    }

    @Test(timeout = 5000)
    public void testMethod_get() throws ReflectiveOperationException {
        List<Object, Object> l = makeInstance();
        assertThrows(() -> l.get(-1), RuntimeException.class);
        assertThrows(() -> l.get(0), RuntimeException.class);
        assertThrows(() -> l.get(1), RuntimeException.class);
        for (int i = 0; i < 5000; i++) {
            l.add(Integer.valueOf(i), Integer.valueOf(i));
        }
        assertThrows(() -> l.get(-1), RuntimeException.class);
        assertThrows(() -> l.get(5000), RuntimeException.class);
        Object le = l.get(0);
        assertNotNull(le);
        assertEquals(Integer.valueOf(0), getVal(le));
        le = l.get(4999);
        assertNotNull(le);
        assertEquals(Integer.valueOf(4999), getVal(le));
        assertLockFree(l);

        testParallel(50, 500, 1000, () -> l.get(4242));
        assertLockFree(l);
    }

    @Test(timeout = 5000)
    public void testMethod_remove() throws ReflectiveOperationException {
        List<Object, Object> l = makeInstance();
        assertThrows(() -> l.remove(-1), RuntimeException.class);
        assertThrows(() -> l.remove(0), RuntimeException.class);
        assertThrows(() -> l.remove(1), RuntimeException.class);
        l.add(null, null);
        Object le = l.remove(0);
        assertNotNull(le);
        assertNull(getVal(le));
        assertThrows(() -> l.remove(0), RuntimeException.class);
        l.add("abc", null);
        l.add(null, "xyz");
        l.add(null, null);
        le = l.remove(0);
        assertNotNull(le);
        assertTrue(getVal(le) == null || getVal(le) == "abc");
        assertLockFree(l);

        for (int i = 0; i < 500; i++) {
            l.add(Integer.valueOf(i), Integer.valueOf(i));
        }
        testParallel(50, 500, 1000, () -> l.remove(0));
        assertEquals(2, l.size());
        assertLockFree(l);
    }

    @Test(timeout = 5000)
    public void testMethod_size() throws ReflectiveOperationException {
        List<Object, Object> l = makeInstance();
        assertEquals(0, l.size());
        l.add(null, null);
        assertEquals(1, l.size());
        l.add("abc", null);
        l.add(null, "xyz");
        l.add(null, null);
        assertEquals(4, l.size());
        l.remove(0);
        assertEquals(3, l.size());
        l.remove(2);
        assertEquals(2, l.size());
        for (int i = 0; i < 1000; i++) {
            l.add(Integer.valueOf(i), Integer.valueOf(i));
        }
        assertEquals(1002, l.size());
        assertLockFree(l);

        testParallel(50, 500, 1000, () -> assertEquals(1002, l.size()));
        assertLockFree(l);
    }

    @Test(timeout = 5000)
    public void testMethod_indexOf() throws ReflectiveOperationException {
        List<Object, Object> l1 = makeInstance();
        List<Object, Object> l2 = makeInstance();
        l2.add("x", "y");
        l2.add(null, null);
        assertEquals(-1, l1.indexOf(null));
        assertEquals(-1, l1.indexOf(l2.get(0)));
        assertEquals(-1, l1.indexOf(l2.get(1)));
        l1.add(null, null);
        l1.add("x", "y");
        assertEquals(1, l1.indexOf(l2.get(0)));
        assertEquals(0, l1.indexOf(l2.get(1)));
        l1.remove(0);
        l1.remove(0);
        for (int i = 0; i < 100; i++)
            l1.add(i, i);
        for (int i = 0; i < 100; i++)
            assertEquals(i, l1.indexOf(l1.get(i)));
        assertLockFree(l1, l2);

        testParallel(50, 500, 1000, () -> assertEquals(50, l1.indexOf(l1.get(50))));
        assertLockFree(l1, l2);
    }

    @Test
    public void testMethod_reverse() throws ReflectiveOperationException {
        List<Object, Object> l1 = makeInstance();
        l1.reverse();
        l1.add("a", null);
        l1.reverse();
        l1.add("b", null);
        l1.add("c", null);
        l1.add("f", null);
        l1.add("0", null);
        List<Object, Object> l2 = makeInstance();
        l2.add("0", null);
        l2.add("f", null);
        l2.add("c", null);
        l2.add("b", null);
        l2.add("a", null);
        l1.reverse();
        assertEquals(l1.get(0), l2.get(0));
        assertEquals(l1.get(4), l2.get(4));
        assertEquals(l1, l2);
        l2.reverse();
        assertNotEquals(l1, l2);
        l1.reverse();
        assertEquals(l1, l2);
        for (int i = 0; i < l2.size(); i++) {
            assertEquals(i, l2.indexOf(l1.remove(0)));
        }
        l1.reverse();
        assertLockFree(l1, l2);

        l1.add("a", null);
        assertEquals(l1.get(0), l2.get(0));
        testParallel(50, 500, 1000, l2::reverse);
        assertEquals(5, l2.size());
        assertEquals(l1.get(0), l2.get(0));
        assertLockFree(l1, l2);
    }

    @Test(timeout = 30_000)
    public void testMethod_doSelectionSort() throws ReflectiveOperationException {
        List<Object, Object> l1 = makeInstance();
        List<Object, Object> l2 = makeInstance();
        l2.add(-12, -12);
        l2.add(-8, -8);
        l2.add(-4, -4);
        l2.add(-2, -2);
        l2.add(-1, -1);
        l2.add(0, 0);
        Comparator<Object> comp = Comparator.comparingInt(i -> {
            try {
                return (Integer) getVal(i);
            } catch (@SuppressWarnings("unused") ReflectiveOperationException e) {
                return 0;
            }
        });
        l1.doSelectionSort((a, b) -> comp.compare(a, b));
        l1.add(0, 0);
        l1.doSelectionSort((a, b) -> comp.compare(a, b));
        l1.add(-8, -8);
        l1.add(-2, -2);
        l1.add(-12, -12);
        l1.add(-4, -4);
        l1.add(-1, -1);
        assertNotEquals(l1, l2);
        l1.doSelectionSort((a, b) -> comp.compare(a, b));
        assertEquals(l1, l2);
        l2.doSelectionSort((a, b) -> comp.compare(a, b));
        assertEquals(l1, l2);
        assertLockFree(l1, l2);

        Random r = new Random(42);
        Integer[] nums = Stream.generate(() -> r.nextInt(500)).limit(2000).toArray(Integer[]::new);
        Arrays.stream(nums).parallel().forEach(i -> l1.add(i, i));
        Arrays.stream(nums).sorted().forEachOrdered(i -> l2.add(i, i));
        int size = l1.size();
        l1.doSelectionSort((a, b) -> comp.compare(a, b));
        assertEquals(l1, l2);
        assertLockFree(l1, l2);
        Object le = l1.get(1000);
        setVal(le, null);
        assertThrows(() -> l1.doSelectionSort((a, b) -> comp.compare(a, b)), RuntimeException.class);
        assertLockFree(l1, l2);
        setVal(le, getVal(l2.get(1000)));
        l1.doSelectionSort((a, b) -> comp.compare(a, b));
        assertEquals(l1, l2);
        assertLockFree(l1, l2);

        testParallel(10, 10, 20_000, () -> l1.doSelectionSort((a, b) -> comp.compare(a, b)));
        assertEquals(size, l1.size());
        assertEquals(l1, l2);
        assertLockFree(l1, l2);

        for (int j = 0; j < l1.size(); j++) {
            setVal(l1.get(j), null);
        }
        assertLockFree(l1, l2);
    }

    @Test(timeout = 8000)
    public void testMethod_forEach() throws InterruptedException, ReflectiveOperationException {
        List<Object, Object> l = makeInstance();
        l.add("a", "b");
        List<Object, Object> other = makeInstance();
        other.add("a", "b");
        int[] count = { 0 };
        l.forEach(le -> {
            count[0]++;
            assertEquals(other.get(0), le);
        });

        CountDownLatch cdl1 = new CountDownLatch(11);
        CountDownLatch cdl2 = new CountDownLatch(11);
        Consumer<Object> wait = le -> {
            try {
                cdl1.countDown();
                cdl1.await();
                cdl2.countDown();
                cdl2.await();
            } catch (@SuppressWarnings("unused") InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        ExecutorService blocker = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            blocker.submit(() -> {
                Class.forName(SRC_CONCURRENT_LIST).getMethod("forEach", Consumer.class).invoke(l, wait);
                return null;
            });
        }
        cdl1.countDown();
        cdl1.await(); // WAIT
        ExecutorService working = Executors.newFixedThreadPool(4);
        working.submit(() -> assertEquals(0, l.indexOf(other.get(0))));
        working.submit(() -> assertEquals(0, other.indexOf(l.get(0))));
        working.submit(() -> assertTrue(l.equals(other)));
        working.submit(() -> assertEquals(1, l.size()));
        working.shutdown();
        Thread.sleep(100); // Safety because auf ReentrantReadWriteLock behaviour
        ExecutorService blocked = Executors.newFixedThreadPool(4);
        blocked.submit(() -> l.add(null, null));
        blocked.submit(() -> l.reverse());
        blocked.submit(() -> l.remove(0));
        working.awaitTermination(500, TimeUnit.MILLISECONDS);
        assertTrue("Lesende Methoden werden blockiert", working.awaitTermination(500, TimeUnit.MILLISECONDS));
        blocked.shutdown();
        assertFalse("reverse und remove werden nicht blockiert", blocked.awaitTermination(500, TimeUnit.MILLISECONDS));
        cdl2.countDown();
        boolean finished = cdl2.await(500, TimeUnit.MILLISECONDS);
        assertTrue(finished);
        assertTrue("reverse und remove werden noch immer blockiert",
                blocked.awaitTermination(500, TimeUnit.MILLISECONDS));
        blocker.shutdown();
        blocker.awaitTermination(500, TimeUnit.MILLISECONDS);
        assertEquals(1, l.size());
        assertLockFree(l);

        assertThrows(() -> l.forEach(le -> {
            throw new RuntimeException();
        }), RuntimeException.class);
        assertLockFree(l);
    }

    @Test(timeout = 5000)
    public void testMethod_equals() throws ReflectiveOperationException {
        List<Object, Object> l1 = makeInstance();
        List<Object, Object> l2 = makeInstance();
        // null Sicherheit
        assertFalse(l1.equals(null));
        assertFalse(l2.equals(null));
        // andere Typen
        assertFalse(l1.equals(new Object()));
        assertFalse(l1.equals("xyz"));
        // Reflexivität 1
        assertTrue(l1.equals(l1));
        assertTrue(l2.equals(l2));
        // Gleichheit & Symmetrie
        assertEquals(l1, l2);
        l1.add(null, null);
        assertNotEquals(l1, l2);
        l2.add("abc", "abc");
        assertNotEquals(l1, l2);
        l2.add(null, null);
        assertNotEquals(l1, l2);
        l2.remove(0);
        assertEquals(l1, l2);
        l1.add("a", "b");
        l2.add("a", "b");
        assertEquals(l1, l2);
        setVal(l1.get(0), "z");
        assertNotEquals(l1, l2);
        // ... Reflexivität 2
        assertEquals(l1, l1);
        assertEquals(l2, l2);
        assertLockFree(l1, l2);

        testParallel(50, 500, 1000, () -> l1.equals(l2), () -> l2.equals(l1));
        assertLockFree(l1, l2);
    }

    private void testParallel(int threads, int tasks, int msecTimeOut, Runnable... taskTypes) {
        CyclicBarrier cb = new CyclicBarrier(threads + 1);
        ExecutorService es = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < tasks; i++) {
            final int index = i;
            es.submit(() -> {
                try {
                    if (index < threads)
                        cb.await();
                    taskTypes[index % taskTypes.length].run();
                } catch (InterruptedException | BrokenBarrierException e) {
                    fail(e.toString());
                    Thread.currentThread().interrupt();
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

    private static Method getVal = null;
    private static Method setVal1 = null;
    private static Method setVal2 = null;
    private static Field lock = null;

    private static Object getVal(Object le) throws ReflectiveOperationException {
        if (getVal == null) {
            Class<?> listElement = Class.forName(SRC_LIST_ELEMENT);
            for (Method m : listElement.getMethods()) {
                if (m.getParameters().length == 0 && m.getReturnType().equals(Object.class)) {
                    getVal = m;
                    break;
                }
            }
            if (getVal == null)
                fail("ListLement get Object 1/2 nicht gefunden");
        }
        return getVal.invoke(le);
    }

    private static void setVal(Object le, Object val) throws ReflectiveOperationException {
        if (setVal1 == null || setVal2 == null) {
            Class<?> listElement = Class.forName(SRC_LIST_ELEMENT);
            setVal1 = null;
            for (Method m : listElement.getMethods()) {
                if (m.getParameters().length == 1 && m.getReturnType().equals(Void.TYPE)
                        && m.getParameterTypes()[0].equals(Object.class)) {
                    if (setVal1 == null)
                        setVal1 = m;
                    else {
                        setVal2 = m;
                        break;
                    }
                }
            }
            if (setVal1 == null || setVal2 == null)
                fail("ListLement set Object 1/2 nicht gefunden");
        }
        setVal1.invoke(le, val);
        setVal2.invoke(le, val);
    }

    private static ReentrantReadWriteLock getLock(Object le) throws ReflectiveOperationException {
        if (lock == null) {
            Class<?> listElement = Class.forName(SRC_CONCURRENT_LIST);
            for (Field f : listElement.getDeclaredFields()) {
                if (f.getType().equals(ReentrantReadWriteLock.class)) {
                    lock = f;
                    lock.setAccessible(true);
                    break;
                }
            }
            if (lock == null)
                fail("Lock der Liste nicht gefunden");
        }
        return (ReentrantReadWriteLock) lock.get(le);
    }

    private static void assertLockFree(List<?, ?>... lists) throws ReflectiveOperationException {
        for (List<?, ?> list : lists) {
            assertLockFree(getLock(list));
        }
    }

    static void assertLockFree(ReentrantReadWriteLock lock) {
        assertFalse(lock.hasQueuedThreads());
        assertFalse(lock.isWriteLocked());
        assertEquals(0, lock.getReadLockCount());
    }

    public static boolean signaturesEqual(Method m1, Method m2) {
        if (m1 == m2)
            return true;
        if (m1 == null || m2 == null)
            return false;
        if (!Objects.equals(m1.getName(), m2.getName()))
            return false;
        return Objects.deepEquals(m1.getParameterTypes(), m2.getParameterTypes());
    }

    public static <T extends Exception> void assertThrows(Runnable r, Class<T> expected) {
        try {
            r.run();
            fail("expected " + expected.getName() + " but nothing was thrown");
        } catch (Exception e) {
            if (!expected.isInstance(e))
                fail("expected " + expected.getName() + " but was " + e.getClass().getName());
        }
    }
}
