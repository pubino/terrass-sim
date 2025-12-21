package myutil;

import java.lang.reflect.*;

public class Memory
{
	public static void main(String[] args)
	{
		int[] a = {1,2,3,4};
		a = (int[])alloc(a,6);
		arrayPrint(a);
		a = (int[])alloc(a,3);
		arrayPrint(a);
	}

	public static Object alloc(Object x, int n)
	{
	        Object y=x;
		Class cl = x.getClass();
		if (!cl.isArray()) return null;
		Class componentType = x.getClass().getComponentType();
		int length = Array.getLength(x);
		if (n > length) {
		    int newlen = length == 0 ? 1 : 2*length;
		    newlen = newlen < n ? n : newlen;
		    y = Array.newInstance(componentType, newlen);
		    System.arraycopy(x, 0, y, 0, length);
		} else if (2*n <= length) {
		    n = (n == 0 ? 1 : n);
		    y = Array.newInstance(componentType, n);
		    System.arraycopy(x, 0, y, 0, n);
		}
		return y;
	}

	static void arrayPrint(Object a)
	{
		Class cl = a.getClass();
		if (!cl.isArray()) return;
		Class componentType = a.getClass().getComponentType();
		int length = Array.getLength(a);
		System.out.println(componentType.getName() + "["
				+ length + "]");
		for (int i=0; i<Array.getLength(a); i++) {
			System.out.println(Array.get(a,i));
		}
	}
}
