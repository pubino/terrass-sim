    /*********************************************************
      Spacefilling Curve Stuff (sfc.c)
     ********************************************************/

package terrass;

import java.awt.*;
import myutil.Sort;
import myutil.GL;

final public class Sfc
{

    /*-----------------------------------------------------------------+
    | static global variables                                         */

    static POINT[] tasks_sort=null;
    static int npoints = 0;
    static int depth;
    static double xshift=0.0, yshift=0.0, oxshift=0.0, oyshift=0.0;
    static int xmajor;
    static boolean swapflag;

    /*-----------------------------------------------------------------+
    | library function definitions                                    */

    static double hash( POINT tasks )
    {
       int i, jx, jy, corner;
       double x, y, xx=tasks.x, yy=tasks.y, z;
       double hsh, scale;

       xx += xshift-oxshift;
       yy += yshift-oyshift;

       corner = 0;
       hsh = 0;
       scale = 1;
       if (xmajor == 0) {
	   x = xx;
	   y = yy;
       } else {
	   y = xx;
	   x = yy;
       }

       for (i=0; i<depth; i++) {
	   switch(corner) {
	      case 1:
		 x = 1-x;
		 if (swapflag) {z=x; x=y; y=z;} // swap x and y
		 break;
	      case 2:
		 x = 1-x;
		 y = 1-y;
		 break;
	      case 3:
		 y = 1-y;
		 if (swapflag) {z=x; x=y; y=z;} // swap x and y
		 break;
	   }
	   jx = (int) Math.floor(3*x);
	   jy = (int) Math.floor(3*y);
	   scale /= 9;
	   switch( 3*jx + jy ) {
	      case 0:
		 hsh += 0*scale;
		 corner = 0;
		 break;
	      case 1:
		 hsh += 1*scale;
		 corner = 1;
		 break;
	      case 2:
		 hsh += 2*scale;
		 corner = 0;
		 break;
	      case 3:
		 hsh += 5*scale;
		 corner = 3;
		 break;
	      case 4:
		 hsh += 4*scale;
		 corner = 2;
		 break;
	      case 5:
		 hsh += 3*scale;
		 corner = 3;
		 break;
	      case 6:
		 hsh += 6*scale;
		 corner = 0;
		 break;
	      case 7:
		 hsh += 7*scale;
		 corner = 1;
		 break;
	      case 8:
		 hsh += 8*scale;
		 corner = 0;
		 break;
	   }
	   x = 3*x - jx;
	   y = 3*y - jy;
       }

       return hsh;
    }

    public static void init( POINT[] tasks, int n )
    {
       int j;

       npoints = n;

       oxshift = xshift;
       oyshift = yshift;

       xshift = 0.20*(Math.random()-1.0);
       yshift = 0.20*(Math.random()-1.0);
       xmajor = (int) Math.floor( 2*Math.random() );
       swapflag = 2*Math.random() < 1 ? false : true ;

       /*-----------------------------------------------------------------+
       | assign space filling curve hash values                          */

       depth = (int)(Math.log(n+1)/Math.log(9.0) + 3);

       for (j=0; j<n; j++) {
	   tasks[j].t = hash(tasks[j]);
       }

       /*-----------------------------------------------------------------+
       | alloc space and setup tasks_sort                                */

       tasks_sort = new POINT[n];

       for (j=0; j<n; j++) {
	   tasks_sort[j] = tasks[j];
       }

       /*-----------------------------------------------------------------+
       | call the UNIX sort function to sort nodes into increasing order */

       Sort.quick( (myutil.Comparable []) tasks_sort, n );
    }

    public static POINT[] getroad()
    {
	return tasks_sort;
    }

    /**********************************************************************
    *                     Display the Curves
    **********************************************************************/

    public static void show()
    {
       int j, n=npoints;

       GL.color(Color.red);

       if (n>0) GL.move2( tasks_sort[0].x, tasks_sort[0].y );
       for (j=1; j<n; j++) {
	  GL.draw2( tasks_sort[j].x, tasks_sort[j].y );
       }
    }
}
