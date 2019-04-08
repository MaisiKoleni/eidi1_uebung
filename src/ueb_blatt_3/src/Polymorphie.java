package ueb_blatt_3.src;

public class Polymorphie {

	public static void main(String[] args) {
		// @formatter:off
		int intOne = 1;
		int intTwo = 3;
		double doubleOne = 3;
		double doubleTwo = 7.0;
		System.out.println(foo(intOne));			// Ausgabe 1
//		System.out.println(foo(doubleTwo));			// Ausgabe 2
//		System.out.println(foo(intTwo, intOne));	// Ausgabe 3
		System.out.println(foo(doubleOne, intTwo));	// Ausgabe 4
		System.out.println(foo(intOne, doubleOne));	// Ausgabe 5
		A a = new A();								// Obj.Erzeugung 1
		B b1 = new B();								// Obj.Erzeugung 2
		B b2 = new B(b1);							// Obj.Erzeugung 3
//		C c1 = new C();								// Obj.Erzeugung 4
		C c2 = new C(b2);							// Obj.Erzeugung 5
		// Sehr viele nicht unbedingt alle für die Übung,
		// manche auch für Zuhause, wenn man nochmal üben will
		System.out.println(a.goo(a));				// Ausgabe 6
		System.out.println(a.goo(b2));				// Ausgabe 7
//		System.out.println(a.goo(c2));				// Ausgabe 8
		System.out.println(a.goo(new E()));			// Ausgabe 9
		System.out.println(b1.goo(b1));				// Ausgabe 10
		System.out.println(b2.goo(a));				// Ausgabe 11
		System.out.println(b1.equals(b2.goo(b2)));	// Ausgabe 12
		System.out.println(c2.goo(a, b2));			// Ausgabe 13
		System.out.println(c2.goo(b1, a));			// Ausgabe 14
		System.out.println(c2.hoo());				// Ausgabe 15
		b1 = new C(b2);								// Obj.Erzeugung 6
		System.out.println(b1.goo(b1));				// Ausgabe 16
		System.out.println(b1.goo(b2));				// Ausgabe 17
		// @formatter:on
	}

	private static double foo(double a, double b) {
		return a > b ? a : b;
	}

	private static int foo(double a, int b) {
		return foo((int) a);
	}

	private static int foo(int a, double b) {
		return (int) foo((double) a, b);
	}

	private static int foo(int a) {
		return 4 * a + 2;
	}

	public static class A {
		public String toString() {
			return "A";
		}

		public A goo(A a) {
			return new A();
		}

		public C goo(D d) {
			return (C) d;
		}
	}

	public static class B extends A {
		public B b;

		public B(B b) {
			this.b = b;
		}

		public B() {
			b = null;
		}

		public String toString() {
			return "B";
		}

		public A goo(B b) {
			return this.b;
		}

		public B goo(A a, B b) {
			return (B) b.goo(a);
		}
	}

	public static class C extends B implements D {
		public C(B b) {
			super(b);
		}

		public String toString() {
			return "C";
		}

		public A goo(A a) {
			return a.goo(a);
		}

		public A goo(B b) {
			return new B(null);
		}

		public A goo(B b, A a) {
			return a.goo(b);
		}

		public B hoo() {
			return new B(this.b);
		}
	}

	public interface D {
		public B hoo();
	}

	public static class E implements D {
		public B hoo() {
			return new C(null);
		}
	}
}
