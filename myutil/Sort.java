package myutil;

import myutil.Comparable;

public class Sort
{
    /*  *** qksort: sort v[left]...v[right] into increasing order ***
	    reference: The C Programming Language,
		       Kernighan and Ritchie
		       2nd. edition, page 87.  */

    public static void quick(Comparable[] v)
    {
	qk(v, 0, v.length-1);
    }

    public static void quick(Comparable[] v, int n)
    {
	qk(v, 0, n-1);
    }

    static void qk(Comparable[] v, int left, int right)
    {
	int i, last;

	if (left >= right) return;   /* do nothing if array contains
                                        fewer than two elements */

	swap(v, left, (left + right)/2);	/* move partition elem */
	last = left;				/* to v[left] */
	for (i = left+1; i <= right; i++) {	/* partition */
            if (v[i].compare(v[left]) < 0) {
		swap(v, ++last, i);
	    }
	}
	swap(v, left, last);			/* restore partition elem */
	qk(v, left, last-1);
	qk(v, last+1, right);
    }

    /*  *** swap: interchange v[i] and v[j] */

    static void swap(Comparable[] v, int i, int j)
    {
	Comparable temp;

	temp = v[i];
	v[i] = v[j];
	v[j] = temp;
    }
}
