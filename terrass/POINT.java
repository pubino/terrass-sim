package terrass;

import java.lang.reflect.*;

final public class POINT implements myutil.Comparable
{
    public double x;
    public double y;
    public double t;
    public Object handle;

    public POINT() { }

    public int compare(myutil.Comparable b)
    {
	POINT pb = (POINT) b;
        int compflag;
        Object handle1, handle2;
        double val1=t, val2=pb.t;

        if ( val1 > val2 ) {
           compflag =  1;
        } else if (val1 == val2) {
           handle1 = handle;
           handle2 = pb.handle;
           if ( handle1.toString().compareTo(handle2.toString()) < 0 ) {
               compflag =  1;
           } else if (handle1 == handle2) {
               compflag =  0;
           } else {
               compflag =  -1;
           }
        } else {
           compflag = -1;
        }
        return compflag;
    }
}
