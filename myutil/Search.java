package myutil;

import myutil.Comparable;

public class Search
{
    public static Comparable binary(Comparable key, Comparable[] base)
    {
	int n = base.length;
	return binary(key, base, n);
    }

    public static Comparable binary(Comparable key, Comparable[] base, int n)
    {

       int low, high, mid, cmp;

       low = 0; high = n-1;
       while ( low <= high ) {
	  mid = (low+high)/2;
	  cmp = key.compare(base[mid]);
	  if ( cmp < 0 ) {
	     high = mid - 1;          /* in lower half */
	  } else if ( cmp > 0 ) {
	     low  = mid + 1;          /* in upper half */
	  } else {
	     return base[mid];    /* found it */
	  }
       }
       if (high >= 0) {
	   return null;         /* not found (minus indicates failure) */
       } else {
	   return base[0];
       }
    }
}
