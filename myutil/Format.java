package myutil;

public class Format
{
	private final int dec   = 0;
	private final int frac  = 1;

        private int width=9;
        private int precision=4;
	private int fraction=dec;

        public Format(int w, int p)
        {
                width = w;
                precision = p;
        }

	public void set(String fmt)
	{
	    if (fmt.equals("Fraction")) {
		fraction = frac;
	    } else {
		fraction = dec;
	    }
	}

	public void setWidth(int w)
	{
	    this.width = w;
	}

	public void setPrecision(int p)
	{
	    this.precision = p;
	}

        public String form(double t)
        {
		if (fraction == frac) {
		    return contFrac(width,precision,t);
		} else {
		    return floating(width,precision,t);
		}
        }

        static public String floating(int width, int precision, double t)
        {
                int s, len, j, k;
                double factor;
                String str;

		if (t<0) { s = -1; t *= -1; }
		else     { s =  1; }

		factor = 1.0;
		for (j=0; j<precision; j++) {
		    factor *= 10;
		}

		t = s * ( Math.rint(t*factor) ) / factor;
		str = Double.toString(t);
		len = str.length();

		for (j=0; j<len; j++) {
		    if (str.charAt(j) == '.') break;
		}

		for (k=j; k<width-precision-1; k++) {
		    str = " " + str;
		}

		for (k=str.length(); k<width; k++) {
		    str = str + " ";
		}

		return str;
        }

        static public String contFrac(int width, int precision, double t)
        {
	    int bj=0, Aj=0, Aj1, Aj2, Bj=1, Bj1, Bj2, num, den;
	    boolean pos;
	    double tj, maxDen = Math.pow(10, precision);
	    String numstr0, numstr;

	    if (t >= 0) { pos = true;   tj =  t; }
	    else        { pos = false;  tj = -t; }

	    bj  = (int) (tj+1.0e-12);
	    tj = 1/(tj-bj);
	    Aj  = bj; Aj1 = 1;
	    Bj  = 1;  Bj1 = 0;
	    num = Aj;
	    den = Bj;
	    if (!pos) {num = -num;}
	    numstr0 = "";
	    numstr = fracString(num, den, width, precision);
	    while (Math.abs(t - num/(double)den) > 1.0e-12 ) {
	        Aj2 = Aj1; Aj1 = Aj;
	        Bj2 = Bj1; Bj1 = Bj;

	        bj = (int) (tj+1.0e-12);
	        tj = 1/(tj-bj);
	        Aj = bj*Aj1 + Aj2;
	        Bj = bj*Bj1 + Bj2;

	        num = Aj;
	        den = Bj;
	        if (!pos) {num = -num;}
		numstr0 = numstr;
	        numstr = fracString(num, den, width, precision);
		if (numstr.length() >= width || Bj >= maxDen) {
		    return numstr0;
		}
	    }

	    return numstr;
        }

	static String fracString(int num, int den, int width, int precision)
	{
	    String numstr = "" + num;
	    int len = numstr.length();

	    for (int k=len; k<width-precision-1; k++) {
		numstr = " " + numstr;
	    }

	    if (den == 1) {
		return(numstr+"");
	    } else {
		return(numstr+"/"+den);
	    }
	}
}
