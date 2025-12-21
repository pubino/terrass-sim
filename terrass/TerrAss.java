package terrass;

import java.awt.*;
import java.applet.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.lang.Math.*;
import java.lang.reflect.*;

import myutil.*;
import myutil.Format;
import myutil.Sort;
import myutil.Search;
import myutil.Memory;

final public class TerrAss extends Applet
{
    TextField M;
    TextField BIAS_START;
    TextField BIAS_END;
    TextField FRAC1;
    TextField NCHANGES;
    TextField BIAS_REDUCTION;
    TextField REG_FORMAT;
    TextField DIST_FORMAT;
    TextField TERR_FORMAT;
    TextField LATMIN, LATMAX, LONMIN, LONMAX;

    URL codebase;

    TerrAssCanvas tac;

    public static void main(String[] args) {
	    new AppletFrame(new TerrAss(), 620, 800);
    }

    public void init()
    {
	codebase = getCodeBase();
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBackground(Color.gray);

        M              = new TextField("500", 5);
	BIAS_START     = new TextField("500", 5);
	BIAS_END       = new TextField("100", 5);
	FRAC1          = new TextField("0.0", 5);
	NCHANGES       = new TextField("0", 5);
	BIAS_REDUCTION = new TextField("2.0", 5);
	REG_FORMAT     = new TextField("%2s", 5);
	DIST_FORMAT    = new TextField("%3s", 5);
	TERR_FORMAT    = new TextField("%3s", 5);


        GridPanel gp = new GridPanel();

        gp.add(new Label("Number of Terrs:"), 1,1); gp.add(M, 2,1);
        gp.add(new Label("Bias Start:"), 1,2); gp.add(BIAS_START, 2,2);
        gp.add(new Label("End:"), 3,2); gp.add(BIAS_END, 4,2);
        gp.add(new Label("Reduction:"), 5,2); gp.add(BIAS_REDUCTION, 6,2);
	/*
        gp.add(new Label("Formats: Region:"), 1,3); gp.add(REG_FORMAT, 2,3);
        gp.add(new Label("District:"), 3,3); gp.add(DIST_FORMAT, 4,3);
        gp.add(new Label("Terrs:"), 5,3); gp.add(TERR_FORMAT, 6,3);
	*/
        gp.add(new Label("Frac:"), 1,4); gp.add(FRAC1, 2,4);
        gp.add(new Label("Num Changes:"), 3,4); gp.add(NCHANGES, 4,4);

        gp.add(new Label("min lat"),1,5);
        gp.add(LATMIN = new TextField("20"),2,5);
        gp.add(new Label("max lat"),3,5);
        gp.add(LATMAX = new TextField("60"),4,5);
        gp.add(new Label("min lon"),5,5);
        gp.add(LONMIN = new TextField("-130"),6,5);
        gp.add(new Label("max lon"),7,5);
        gp.add(LONMAX = new TextField("-60"),8,5);
        gp.add(new Button("Start"),1,6);
        gp.add(new Button("Stop"), 2,6);
        add(gp);

        tac = new TerrAssCanvas(this);
        add(tac);
        tac.setSize(600,600);
        validate();
        setVisible(true);
    }

    public void stop()
    {
	tac.stop();
    }

    public boolean action(Event evt, Object arg)
    {
	double steplen, strength;
	String filename;
	if (arg.equals("Start")) {
	    tac.stop();
	    tac.start();
	} else if (arg.equals("Stop")) {
	    tac.stop();
	} else {
	    return super.action(evt, arg);
	}
	return true;
    }

    public boolean handleEvent(Event evt)
    {
	if (evt.id == Event.WINDOW_DESTROY) System.exit(0);
	return super.handleEvent(evt);
    }
}

final class TerrAssCanvas extends Canvas
	implements Runnable
{
    int m, n;

    ZIPDATA[] z;
    TERRDATA[] t;
    ARCS a;

    double latmin, latmax, lonmin, lonmax;

    double[] target;

    int[] linkr, linkl;

    String format;
    String format2;

    PARAMS params;

    TerrAss parent;
    int paint_flag=0;
    Graphics g;

    URL codebase;

    public TerrAssCanvas(TerrAss parent)
    {
	this.parent = parent;
	paint_flag=0;
    }

    private Thread animate;
    private boolean ginited=false;

    public void start()
    {
	params = new PARAMS();

	params.m = SL.atoi(parent.M.getText());
	params.bias_start = SL.atof(parent.BIAS_START.getText());
	params.bias_end   = SL.atof(parent.BIAS_END.getText());
	params.bias_reduction = SL.atof(parent.BIAS_REDUCTION.getText());
	params.reg_format = parent.REG_FORMAT.getText();
	params.dist_format = parent.DIST_FORMAT.getText();
	params.terr_format = parent.TERR_FORMAT.getText();
	params.frac1 = SL.atof(parent.FRAC1.getText());
	params.nchanges = SL.atoi(parent.NCHANGES.getText());
        latmin = SL.atof(parent.LATMIN.getText());
        latmax = SL.atof(parent.LATMAX.getText());
        lonmin = SL.atof(parent.LONMIN.getText());
        lonmax = SL.atof(parent.LONMAX.getText());

	codebase = parent.codebase;

	read_data( params );

        genprob();

	animate = new Thread( this );
	animate.start();
    }

    public void stop()
    {
	    System.out.println("stopping");
            if (animate != null) {
                animate.stop();
                animate = null;
            }
    }

    public void run()
    {
       int   i, j, k;    /* loop indices                          */
       int   i1, i2, jj;

	try { Thread.sleep( 1000 ); }
	catch ( InterruptedException e ){};

       /*******************************************************************
       *   Solve the problem here
       *******************************************************************/

	int ii;
	ZIPDATA[] z0;
	TERRDATA[] t0;

	// ******************************************************
	// Compute x and y coords in azimuthal Projection
	// ******************************************************

	azimuthal(n, z);

	// ******************************************************
	// Compute ``centers'' for each territory
	// ******************************************************

	for (i=0; i<m; i++) {
	    computemedioid( i );
	}

	// ******************************************************
	// Do different things depending on if it is a national
	// or district optimization
	// ******************************************************

	if (params.probtype == _NATIONAL) {
	    m = params.m;
	    extract_groups(n, m, params);
	} else {
	    System.err.println("TROUBLE in main");
	}
    }

    public void paint (Graphics g)
    {
      int j;

      if (paint_flag == 1) {

	g = getGraphics();
	GL.ginit(g, size().width, size().height, this, false);

	GL.color(Color.black);  GL.clear();
	GL.color(Color.red);
	for (j=0; j<n; j++) {
	    GL.pnt2(z[j].x, z[j].y);
	}
      } else
      if (paint_flag == 2) {
	GL.color(Color.black);  GL.clear();
	for (int i=0; i<m; i++) {
	    GL.color(color[i%7]);
	    for (int k=0; k<t[i].nzips; k++) {
		GL.pnt2(t[i].ziplist[k].x, t[i].ziplist[k].y);
	    }
	}
      }
    }

    static final double EPSILON = 1.0e-7;
    static final double EPSSOL = 1.0e-5;
    static final int NOREORD = 0;
    static final int MD  = 1;
    static final int MLF = 2;
    static final int NZ = 80;     /* threshold for printing details        */
    static final double MARGIN = 0.25;/* % tolerance in window coordinates */
    static final double BIGM = 999999.99;     /* a large value             */
    static final Color[] color = {Color.red, Color.blue, Color.green,
				  Color.cyan, Color.yellow, Color.magenta,
				  Color.white};


    void genprob()
    {
       paint_flag = 1;
       paint(g);
       paint_flag = 2;
    }

    void open_graphics_window( int n )
    {
	int j;

	mxy = 0;
	mny = 1;
	for (j=0; j<n; j++) {
	    if (z[j].y > mxy) mxy = z[j].y;
	    if (z[j].y < mny) mny = z[j].y;
	}
	mxx = 0;
	mnx = 1;
	for (j=0; j<n; j++) {
	    if (z[j].x > mxx) mxx = z[j].x;
	    if (z[j].x < mnx) mnx = z[j].x;
	}

	double mxdiff = MAX(mxx-mnx, mxy-mny)/2;
	double midx = (mxx+mnx)/2;
	double midy = (mxy+mny)/2;

	mnx = midx - 1.05*mxdiff;
	mxx = midx + 1.05*mxdiff;
	mny = midy - 1.05*mxdiff;
	mxy = midy + 1.05*mxdiff;
	GL.ortho2(mnx, mxx, mny, mxy);
    }

    void azimuthal(
	int       n,
	ZIPDATA[] z
    )
    {
	int j;
	double xbar, ybar, zbar;
	double r, rho, theta, phi;

	// **********************************************************
	// Compute barycenter (in Euclidean coordinates).
	// **********************************************************

	xbar = 0;
	ybar = 0;
	zbar = 0;
	for (j=0; j<n; j++) {
	    phi   = 2*Math.PI*z[j].lat/360;
	    theta = 2*Math.PI*z[j].lon/360;

	    xbar += Math.cos(phi)*Math.cos(theta);
	    ybar += Math.cos(phi)*Math.sin(theta);
	    zbar += Math.sin(phi);
	}
	xbar /= n;
	ybar /= n;
	zbar /= n;

	// **********************************************************
	// Compute spherical coordinates of barycenter.
	// **********************************************************

	r = Math.sqrt(xbar*xbar + ybar*ybar);
	theta = Math.atan2(ybar, xbar);

	rho = Math.sqrt(xbar*xbar + ybar*ybar + zbar*zbar);
	phi = Math.atan2(r, zbar);

	// **********************************************************
	// Compute unit tangent vector parallel to lattitude line.
	// **********************************************************

	xx1 = -Math.sin(theta);
	yy1 =  Math.cos(theta);
	zz1 =  0;

	// **********************************************************
	// Compute unit tangent vector parallel to longitude line.
	// **********************************************************

	xx2 = -Math.cos(phi)*Math.cos(theta);
	yy2 = -Math.cos(phi)*Math.sin(theta);
	zz2 =  Math.sin(phi);

	// **********************************************************
	// The azimuthal coordinates are simply the inner products
	// of the cartesian coordinates with these unit vectors.
	// **********************************************************

	for (j=0; j<n; j++) {
	    phi   = 2*Math.PI*z[j].lat/360;
	    theta = 2*Math.PI*z[j].lon/360;

	    xbar = Math.cos(phi)*Math.cos(theta);
	    ybar = Math.cos(phi)*Math.sin(theta);
	    zbar = Math.sin(phi);

	    z[j].x = xbar*xx1 + ybar*yy1 + zbar*zz1;
	    z[j].y = xbar*xx2 + ybar*yy2 + zbar*zz2;
	}

	// **********************************************************
	// Scale the azimuthal coordinates so that they lie between
	// 0 and 1.  Save the scale factor in a global variable for
	// use by azimuth().
	// **********************************************************

	maxy = -HUGE_VAL;
	miny =  HUGE_VAL;
	for (j=0; j<n; j++) {
	    if (z[j].y > maxy) maxy = z[j].y;
	    if (z[j].y < miny) miny = z[j].y;
	}
	maxx = -HUGE_VAL;
	minx =  HUGE_VAL;
	for (j=0; j<n; j++) {
	    if (z[j].x > maxx) maxx = z[j].x;
	    if (z[j].x < minx) minx = z[j].x;
	}
	maxx += 0.11*(maxx-minx);
	minx -= 0.11*(maxx-minx);
	maxy += 0.11*(maxy-miny);
	miny -= 0.11*(maxy-miny);
	ascale = MAX( maxx-minx, maxy-miny );
	for (j=0; j<n; j++) {
	    z[j].x = (z[j].x - minx)/ascale;
	    z[j].y = (z[j].y - miny)/ascale;
	}
    }

		/* do we need this?
    void azimuth( double lat, double lon, double &x, double &y )
    {
	double phi, theta, xbar, ybar, zbar;

	phi   = 2*Math.PI*(90-lat)/360;
	theta = 2*Math.PI*lon/360;

	xbar = Math.sin(phi)*Math.cos(theta);
	ybar = Math.sin(phi)*Math.sin(theta);
	zbar = Math.cos(phi);

	x = xbar*xx1 + ybar*yy1 + zbar*zz1;
	y = xbar*xx2 + ybar*yy2 + zbar*zz2;
	x = (x - minx)/ascale;
	y = (y - miny)/ascale;
    }
    */

    double gc_dist( double lat1, double lon1, double lat2, double lon2 )
    {
	double x1, y1, z1, x2, y2, z2;
	double rho = 24000/(2*Math.PI), radperdeg = Math.PI/180;
	double rho1;

	rho1 = rho + 0.001;  // makes distance from a point to itself
			     // into a small positive number (about 4 miles)

	x1 = rho * Math.cos( lat1*radperdeg ) * Math.cos( lon1*radperdeg );
	x2 = rho * Math.cos( lat2*radperdeg ) * Math.cos( lon2*radperdeg );
	y1 = rho * Math.cos( lat1*radperdeg ) * Math.sin( lon1*radperdeg );
	y2 = rho * Math.cos( lat2*radperdeg ) * Math.sin( lon2*radperdeg );
	z1 = rho * Math.sin( lat1*radperdeg );
	z2 = rho * Math.sin( lat2*radperdeg );

	return rho * Math.acos( (x1*x2 + y1*y2 + z1*z2)/(rho1*rho1) );
    }

    void cartesian(
	double   lat,
	double   lon,
	double[] x
    )
    {
	double radperdeg = Math.PI/180;

	x[0] = Math.cos( lat*radperdeg ) * Math.cos( lon*radperdeg );
	x[1] = Math.cos( lat*radperdeg ) * Math.sin( lon*radperdeg );
	x[2] = Math.sin( lat*radperdeg );
    }

    void crossprod(
	double[] x,  // input
	double[] y,  // input
	double[] z   // output
    )
    {
	z[0] = x[1]*y[2] - y[1]*x[2];
	z[1] = x[2]*y[0] - y[2]*x[0];
	z[2] = x[0]*y[1] - y[0]*x[1];
    }

    double dotprod(
	double[] x,
	double[] y
    )
    {
	int j;
	double dp;

	dp = 0;
	for (j=0; j<3; j++) {
	    dp += x[j]*y[j];
	}
	return dp;
    }

    // *********************************************************
    // Read data from zlls.dat and zta.dat into ZIPDATA array z
    // and TERRDATA array t.
    // *********************************************************

    void read_data( PARAMS params)
    {
	int i, j, k;
	String terr;
	double att, scale;
	String zip, str;
	ZIPDATA z0;
	DataInputStream in;

	// *********************************************************
	// Read data from zlls.dat into ZIPDATA array z.
	// *********************************************************

	try { in = new DataInputStream(
		   new BufferedInputStream(
		   new FileInputStream("zlls.dat"))); }
	catch (IOException fe){System.err.println("File not found"); return;}

	n = FileIO.readInt(in);
	z = new ZIPDATA[n];
	k = 0;
	for (j=0; j<n; j++) {
	    if (j%1000 == 0) {System.out.println(j);}

	    z[k] = new ZIPDATA();
	    z[k].zip = FileIO.readString(in);
	    z[k].lat = FileIO.readDouble(in);
	    z[k].lon = FileIO.readDouble(in);
	    z[k].st  = FileIO.readString(in);

	    z[k].att = EPS;
	    z[k].terr = -1;  // This is what it is but not what it should be
	    z[k].idx  = k;

	    if (z[k].lat > latmin && z[k].lat < latmax
		    && z[k].lon > lonmin && z[k].lon < lonmax)
	    {k++;}
	}
	n = k;
	FileIO.close(in);

	// *********************************************************
	// Sort ZIPDATA array on zips so that bsearch will work correctly
	// *********************************************************

	Sort.quick( (myutil.Comparable []) z, n );

	// *********************************************************
	// Read data from zta.dat and set up TERRDATA array t.
	// *********************************************************

	try { in = new DataInputStream(
		   new BufferedInputStream(
		   new FileInputStream("zta.dat"))); }
	catch (IOException fe){System.err.println("File not found"); return;}

	int n2 = FileIO.readInt(in);

	for (j=0; j<n2; j++) {
	    zip = FileIO.readString(in);
	    terr = FileIO.readString(in);
	    att = FileIO.readDouble(in);

	    z0 = (ZIPDATA) Search.binary( new ZIPDATA(zip), z, n );
	    if (att == 0) {att = EPS;}
	    if (z0 != null) {
		z0.att = att;
	    } else {
		//System.err.println(zip+" in zta.dat not found in zlls.dat");
	    }
	}
    }

    void computecentroid( int i )
    {
	int k;
	double lat, lon, xcoord=0.0, ycoord=0.0, zcoord=0.0, r, att,
	      rho=24000.0/(2*Math.PI), radperdeg=Math.PI/180;

	if (t[i].nzips == 0) return;

	for (k=0; k<t[i].nzips; k++) {
	    lat = t[i].ziplist[k].lat;
	    lon = t[i].ziplist[k].lon;
	    att = t[i].ziplist[k].att;
	    xcoord += att*rho*Math.cos(lat*radperdeg)*Math.cos(lon*radperdeg);
	    ycoord += att*rho*Math.cos(lat*radperdeg)*Math.sin(lon*radperdeg);
	    zcoord += att*rho*Math.sin(lat*radperdeg);
	}
	xcoord /= t[i].att;
	ycoord /= t[i].att;
	zcoord /= t[i].att;
	r = Math.sqrt( xcoord*xcoord + ycoord*ycoord );
	lat = Math.atan2(zcoord, r     )/radperdeg;
	lon = Math.atan2(ycoord, xcoord)/radperdeg;
	t[i].lat = lat;
	t[i].lon = lon;
    }

    void recomputemedioid( int i )
    {
	int k, l;
	double zatt, zlat, zlon, totdist;
	double numx, numy, numz, denx, deny, denz;
	double x, y, z, xdiff, ydiff, zdiff;
	double dist;
	double lat, lon, xcoord=0.0, ycoord=0.0, zcoord=0.0, r, att,
	      rho=24000.0/(2*Math.PI), radperdeg=Math.PI/180;

	if (t[i].nrecomp != 0) {
	  t[i].nrecomp--;
	} else if (t[i].nzips > 0) {
	  t[i].nrecomp = (int) (0.02*t[i].nzips);

	  lat = t[i].lat;
	  lon = t[i].lon;
	  xcoord = rho * Math.cos(lat*radperdeg) * Math.cos(lon*radperdeg);
	  ycoord = rho * Math.cos(lat*radperdeg) * Math.sin(lon*radperdeg);
	  zcoord = rho * Math.sin(lat*radperdeg);

	  numx=0.0;
	  numy=0.0;
	  numz=0.0;
	  denx=0.0;
	  deny=0.0;
	  denz=0.0;
	  for (k=0; k<t[i].nzips; k++) {

	      lat = t[i].ziplist[k].lat;
	      lon = t[i].ziplist[k].lon;
	      att = t[i].ziplist[k].att;
	      x = rho * Math.cos( lat*radperdeg ) * Math.cos( lon*radperdeg );
	      y = rho * Math.cos( lat*radperdeg ) * Math.sin( lon*radperdeg );
	      z = rho * Math.sin( lat*radperdeg );

	      xdiff = x - xcoord;
	      ydiff = y - ycoord;
	      zdiff = z - zcoord;

	      dist = Math.sqrt(xdiff*xdiff + ydiff*ydiff + zdiff*zdiff) + EPS;

	      numx += att * x/dist;
	      numy += att * y/dist;
	      numz += att * z/dist;

	      denx += att/dist;
	      deny += att/dist;
	      denz += att/dist;
	  }
	  xcoord = numx/denx;
	  ycoord = numy/deny;
	  zcoord = numz/denz;

	  r = Math.sqrt( xcoord*xcoord + ycoord*ycoord );
	  lat = Math.atan2(zcoord, r     )/radperdeg;
	  lon = Math.atan2(ycoord, xcoord)/radperdeg;
	  t[i].lat = lat;
	  t[i].lon = lon;

	  totdist = 0.0;
	  for (l=0; l<t[i].nzips; l++) {
	    zatt = t[i].ziplist[l].att;
	    zlat = t[i].ziplist[l].lat;
	    zlon = t[i].ziplist[l].lon;
	    totdist += zatt * gc_dist( lat, lon, zlat, zlon );
	  }
	  t[i].totdist = totdist;
	}
    }

    void computemedioid( int i )
    {
	int k, l;
	double zatt, zlat, zlon, totdist;
	double numx, numy, numz, denx, deny, denz;
	double x, y, z, xdiff, ydiff, zdiff;
	double dist;
	double lat=0, lon=0, xcoord=0.0, ycoord=0.0, zcoord=0.0, r, att,
	      rho=24000.0/(2*Math.PI), radperdeg=Math.PI/180;

	if (t[i].nrecomp != 0) {
	    t[i].nrecomp--;
	} else if (t[i].nzips > 0) {
	    t[i].nrecomp = (int) (0.10*t[i].nzips);

	    computecentroid( i );

	    if (t[i].nzips > 1) {

	      lat = t[i].lat;
	      lon = t[i].lon;
	      xcoord = rho*Math.cos(lat*radperdeg)*Math.cos(lon*radperdeg);
	      ycoord = rho*Math.cos(lat*radperdeg)*Math.sin(lon*radperdeg);
	      zcoord = rho*Math.sin(lat*radperdeg);

	      for (l=0; l<5; l++) {
		  numx=0.0;
		  numy=0.0;
		  numz=0.0;
		  denx=0.0;
		  deny=0.0;
		  denz=0.0;
		  for (k=0; k<t[i].nzips; k++) {

		      lat = t[i].ziplist[k].lat;
		      lon = t[i].ziplist[k].lon;
		      att = t[i].ziplist[k].att;
		      x = rho * Math.cos( lat*radperdeg )
			      * Math.cos( lon*radperdeg );
		      y = rho * Math.cos( lat*radperdeg )
			      * Math.sin( lon*radperdeg );
		      z = rho * Math.sin( lat*radperdeg );

		      xdiff = x - xcoord;
		      ydiff = y - ycoord;
		      zdiff = z - zcoord;

		      dist = Math.sqrt(xdiff*xdiff+ydiff*ydiff+zdiff*zdiff)
			     + EPS;

		      numx += att * x/dist;
		      numy += att * y/dist;
		      numz += att * z/dist;

		      denx += att/dist;
		      deny += att/dist;
		      denz += att/dist;
		  }
		  xcoord = numx/denx;
		  ycoord = numy/deny;
		  zcoord = numz/denz;
	      }

	      r = Math.sqrt( xcoord*xcoord + ycoord*ycoord );
	      lat = Math.atan2(zcoord, r     )/radperdeg;
	      lon = Math.atan2(ycoord, xcoord)/radperdeg;
	      t[i].lat = lat;
	      t[i].lon = lon;
	    }
	    totdist = 0.0;
	    for (l=0; l<t[i].nzips; l++) {
		zatt = t[i].ziplist[l].att;
		zlat = t[i].ziplist[l].lat;
		zlon = t[i].ziplist[l].lon;
		totdist += zatt * gc_dist( lat, lon, zlat, zlon );
	    }
	    t[i].totdist = totdist;
	}
    }

    // *********************************************************
    // Reassign zip j0 from territory i0 to territory i1
    // *********************************************************

    void reassign(
	int j0,
	int i0,
	int i1
	)
    {
	int l;
	ZIPDATA z0=z[j0];

	z0.terr = i1;
	GL.color( color[i1%7] );
	GL.pnt2( z0.x, z0.y );
	t[i1].nzips++;

	// REALLOC( t[i1].ziplist, t[i1].nzips, ZIPDATA * );
	t[i1].ziplist = (ZIPDATA[]) Memory.alloc(t[i1].ziplist, t[i1].nzips);
	t[i1].ziplist[t[i1].nzips-1] = z0;
	for (l=0; l<t[i0].nzips; l++) {
	    if (t[i0].ziplist[l] == z0) {
		t[i0].nzips--;
		t[i0].ziplist[l] = t[i0].ziplist[t[i0].nzips];
		break;
	    }
	}
	t[i1].att += z0.att;
	t[i0].att -= z0.att;

	t[i1].totdist += z0.att * gc_dist(z0.lat,z0.lon,t[i1].lat,t[i1].lon);
	t[i0].totdist -= z0.att * gc_dist(z0.lat,z0.lon,t[i0].lat,t[i0].lon);

	recomputemedioid(i1);
	recomputemedioid(i0);
    }

    // *********************************************************
    // Set up and sweep through linked list of border zips
    // and reassign across border if said reassignment improves
    // objective function.
    // *********************************************************

    int rebalance(
	int n,
	int m,
	double wt,
	double[] target,
	PARAMS params
    )
    {
	int cnt1, cnt2;
	int pk, k, kk;
	int i, i0, i1, j, j0, j1;
	int changes=0;
	ZIPDATA z0, z1;
	double ototdist, ounbal, ntotdistr, nunbalr;
	double obj, oobj, bobj, dobj;
	int nz = a.k[n];  // number of adjacencies

	// **********************************************************
	// initialize linkr and linkl
	// **********************************************************

	pk = -1;
	linkl[i(-1)] = -1;
	for (k=0; k<nz; k++) {
	    i = a.i[k];
	    j = a.j[k];
	    if ( i > j  &&  z[j].terr != z[i].terr ) {
		linkr[i(pk)] = k;  linkl[i(k)] = pk;
		pk = k;
	    } else {
		linkl[i(k)] = -2;  // linkl is used to mark unlinked indices
	    }
	}
	linkr[i(pk)] = nz; linkl[i(nz)] = pk;
	linkr[i(nz)] = nz;

	// **********************************************************
	// make improvements to the objective function
	// **********************************************************

	obj = HUGE_VAL;
	do {
	    oobj = obj;
	    dobj = 0;
	    bobj = 0;
	    for (i=0; i<m; i++) {
		dobj += t[i].totdist;
		bobj += sq(target[i] - t[i].att);
	    }
	    obj = bobj + wt*dobj;
	    if (obj > oobj) break;

	    // ******************************************************
	    // loop through linked list.  for each pair of adjacent
	    // nodes j0/j1 belonging to different territories i0/i1,
	    // respectively, consider whether reassigning j0 from
	    // i0 to i1 would improve objective function.  Also,
	    // consider the reverse: whether reassigning j1 from
	    // i1 to i0 would improve things.
	    // ******************************************************

	    cnt1=0; cnt2=0;
	    for (k=linkr[i(-1)]; k<nz; k=linkr[i(k)]) {

		j0 = a.j[k];
		z0 = z[j0];
		i0 = z0.terr;

		j1 = a.i[k];
		z1 = z[j1];
		i1 = z1.terr;

		// **************************************************
		// fix up linked lists, if needed
		// **************************************************

		if (i0 == i1) {
		    linkr[i(linkl[i(k)])] = linkr[i(k)];
		    linkl[i(linkr[i(k)])] = linkl[i(k)];
		    linkl[i(k)] = -2;
		    k = linkr[i(k)];
		    continue;
		}

		cnt2++;

		// ******************************************************
		// check if j1 should be reassigned to i0
		// ******************************************************

		ototdist  = z1.att*gc_dist(z1.lat,z1.lon,t[i1].lat,t[i1].lon);
		ntotdistr = z1.att*gc_dist(z1.lat,z1.lon,t[i0].lat,t[i0].lon);

		ounbal  = sq(target[i1] - t[i1].att)
			+ sq(target[i0] - t[i0].att);
		nunbalr = sq(target[i1] - t[i1].att + z1.att)
			+ sq(target[i0] - t[i0].att - z1.att);

		if (nunbalr + wt*ntotdistr < ounbal + wt*ototdist) {
		    changes++;
		    cnt1++;

		    for (kk=a.k[j1]; kk<a.k[j1+1]; kk++) {
			if (linkl[i(kk)] == -2 && z[a.i[kk]].terr != i0) {
			    linkr[i(kk)] = linkr[i(-1)];
			    linkr[i(-1)] = kk;
			    linkl[i(linkr[i(kk)])] = kk;
			    linkl[i(kk)] = -1;
			}
		    }

		    linkr[i(linkl[i(k)])] = linkr[i(k)];  // del current link
		    linkl[i(linkr[i(k)])] = linkl[i(k)];  // del current link
		    linkl[i(k)] = -2;
		    k = linkr[i(k)];

		    reassign( j1, i1, i0 );
		    continue;
		}

		// ******************************************************
		// check if j0 should be reassigned to i1
		// ******************************************************

		ototdist  = z0.att*gc_dist(z0.lat,z0.lon,t[i0].lat,t[i0].lon);
		ntotdistr = z0.att*gc_dist(z0.lat,z0.lon,t[i1].lat,t[i1].lon);

		ounbal  = sq(target[i0] - t[i0].att)
			+ sq(target[i1] - t[i1].att);
		nunbalr = sq(target[i0] - t[i0].att + z0.att)
			+ sq(target[i1] - t[i1].att - z0.att);

		if (nunbalr + wt*ntotdistr < ounbal + wt*ototdist) {
		    changes++;
		    cnt1++;

		    for (kk=a.k[j0]; kk<a.k[j0+1]; kk++) {
			if (linkl[i(kk)] == -2 && z[a.i[kk]].terr != i1) {
			    linkr[i(kk)] = linkr[i(-1)];
			    linkr[i(-1)] = kk;
			    linkl[i(linkr[i(kk)])] = kk;
			    linkl[i(kk)] = -1;
			}
		    }

		    linkr[i(linkl[i(k)])] = linkr[i(k)];  // del current link
		    linkl[i(linkr[i(k)])] = linkl[i(k)];  // del current link
		    linkl[i(k)] = -2;
		    k = linkr[i(k)];

		    reassign( j0, i0, i1 );
		    continue;
		}
	    }
	    System.out.print("1opt: cnt1 = " + cnt1);
	    System.out.println(" cnt2 = " + cnt2);
	} while (cnt1 > params.frac1*cnt2);
	return changes;
    }

    int i(int i) { return i+1; }

    void extract_groups(
	int n,
	int m,
	PARAMS params
    )
    {
	int i, j;
	double totatt;

	// ******************************************************
	// Initialize totatt
	// ******************************************************

	totatt = 0.0;
	for (j=0; j<n; j++) {
	    totatt += z[j].att;
	}

	open_graphics_window(n);

	GL.color(Color.black);
	GL.clear();

	// ******************************************************
	// Set up territories in ZIPDATA array z starting with a
	// quick pass through a Spacefilling curve.  Also
	// initialize TERRDATA array t.
	// ******************************************************

	cold_start( n, m, totatt, params);

	// ******************************************************
	// Read in or compute an adjacency network
	// ******************************************************

	makenetwork( n );

	// ******************************************************
	// Now optimize using local improvement heuristic
	// ******************************************************

	optimize(n, m, totatt, params);

		/* do we need this?
	FILE *tfp = fopen("terrdata", "w");
	for (i=0; i<m; i++) {
	    fprintf(tfp, "%5d %9.3f %9.3f \n", i, t[i].lat, t[i].lon);
	}
	fclose(tfp);

	FILE *zfp = fopen("zipdata", "w");
	for (j=0; j<n; j++) {
	    double tlat, tlon, zlat, zlon;
	    tlat = t[z0[j].terr].lat;
	    tlon = t[z0[j].terr].lon;
	    zlat = z[j].lat;
	    zlon = z[j].lon;
	    fprintf(zfp, "%5d %9.3f %9.3f %5.0f %9.3f\n",
		j, z[j].lat, z[j].lon, z[j].att,
		gc_dist(tlat,tlon,zlat,zlon));
	}
	fclose(zfp);
	*/
	compute_stats(m, target);
	System.out.println("end of extract groups");
    }

    // ******************************************************
    // Set up territories in ZIPDATA array z starting with a
    // quick pass through a Spacefilling curve.  Also
    // initialize TERRDATA array t.
    // ******************************************************

    void cold_start(
	int n,
	int m,
	double totatt,
	PARAMS params
    )
    {

	int i, j;
	POINT[] p;		// POINT is defined in sfc.h
	POINT[] road;
	double att = 0.0;

	double target = totatt/m;

	// ******************************************************
	// Set up array of zipcode POINTs for sfc code
	// ******************************************************

	p = new POINT[n];

	for (j=0; j<n; j++) {
	    p[j] = new POINT();
	    p[j].x = z[j].x;
	    p[j].y = z[j].y;
	    p[j].handle = (Object) z[j];
	}

	Sfc.init( p, n );
	road = Sfc.getroad();

	for ( i=0, j=0; i<m && j<n; j++ ) {

	    ZIPDATA z = (ZIPDATA) road[j].handle;

	    if (att + z.att/2 <= target) {
		z.terr = i;
		att += z.att;
	    } else {
		totatt -= att;
		att = 0.0;
		j--;
		i++;
		target = totatt/(m-i);
	    }
	    GL.color( color[i%7] );
	    GL.pnt2(road[j].x, road[j].y);
	}

	if (i!=m-1) System.err.print("trouble in river city \n");
	System.err.println("m = "+m);
	if (j!=n) System.err.print("more trouble in river city \n");

	// ******************************************************
	// Set up TERRDATA array t based on initial zip assignment
	// ******************************************************

	t = new TERRDATA[m];

	for (i=0; i<m; i++) {
	    t[i] = new TERRDATA();
	    t[i].nzips = 0;
	    t[i].att   = EPS;
	}

	for (j=0; j<n; j++) {
	    i = z[j].terr;
	    t[i].nzips ++;
	    t[i].att += z[j].att;
	}
	for (i=0; i<m; i++) {
	    t[i].ziplist = new ZIPDATA[1+t[i].nzips];
	}
	for (i=0; i<m; i++) {
	    t[i].nzips = 0;
	    t[i].nrecomp = 0;
	    t[i].scale = 1.0;
	    t[i].terr = ""+i;
	}
	for (j=0; j<n; j++) {
		i = z[j].terr;
		t[i].ziplist[t[i].nzips] = z[j];
		t[i].nzips++;
	}

	for (i=0; i<m; i++) {
	    computemedioid( i );
	}
    }

    double compute_obj(int m, double[] target, double wt)
    {
	int i;
	double obj, dobj, bobj, cnt, maxunbal, geoavunbal, ratio,
	       totwtdist, totwt, wtdist;

	// ******************************************************
	// Compute and print statistics
	// ******************************************************

	dobj = 0;
	bobj = 0;
	for (i=0; i<m; i++) {
	    dobj += t[i].totdist;
	    bobj += sq(target[i] - t[i].att);
	}
	obj = bobj + wt*dobj;
	// System.out.println("obj = " + obj);

	return obj;
    }

    void compute_stats(int m, double[] target)
    {
	int i;
	double cnt, maxunbal, geoavunbal, ratio, totwtdist, totwt, wtdist;

	// ******************************************************
	// Compute and print statistics
	// ******************************************************

	cnt=0;
	for (i=0; i<m; i++) {
	    if ( RATIO(t[i].att, target[i]) > 1.1 ) cnt++;
	}
	GL.drawString("pct of territories more than 10% from target " +
		Format.floating(5,1,100*cnt/m), mnx, mny, "LEFT", "BOTTOM");

	maxunbal = 0;
	geoavunbal = 0;
	for (i=0; i<m; i++) {
	    ratio = RATIO( t[i].att, target[i] );
	    maxunbal = MAX( maxunbal, ratio );
	    geoavunbal += Math.log( ratio );
	}
	maxunbal -= 1.0;
	geoavunbal = Math.exp( geoavunbal/m );

	totwtdist = 0.0;
	totwt     = 0.0;
	for (i=0; i<m; i++) {
	    totwtdist += t[i].totdist;
	    totwt     += t[i].att;
	}
	wtdist = totwtdist/totwt;

	GL.drawString("maximum unbalance (%):  "
	    + Format.floating(5,1,maxunbal*100),
	      mnx, mny+0.05, "LEFT", "BOTTOM");
	GL.drawString("geom avg of unbal (%):  "
	    + Format.floating(5,1,(geoavunbal-1)*100),
	      mnx, mny+0.1, "LEFT", "BOTTOM");
	GL.drawString("wt'ed avg dist in miles: "
	    + Format.floating(5,1,wtdist),
	      mnx, mny+0.15, "LEFT", "BOTTOM");
	// System.out.println("");
    }

    void makenetwork( int n )
    {
	DataInputStream in;
	try { in = new DataInputStream(
		   new BufferedInputStream(
		   new FileInputStream("mknet/arcs.dat"))); }
	catch (IOException fe){
	    System.out.println("File mknet/arcs.dat not found");
	    createnetwork(n);
	    return;
	}

	int nz, j, j1, j2, k;
	String zip1, zip2;
	ZIPDATA z1, z2;

	nz = FileIO.readInt(in);
	a   = new ARCS();
	a.i = new int[nz];
	a.j = new int[nz];
	a.k = new int[n+1];
	int[] offset = new int[n];
	for (j=0; j<n; j++) {offset[j] = 0;}
	for (k=0; k<nz; k++) {
	    zip1 = FileIO.readString(in);
	    zip2 = FileIO.readString(in);
	    z1 = (ZIPDATA) Search.binary( new ZIPDATA(zip1), z, n );
	    z2 = (ZIPDATA) Search.binary( new ZIPDATA(zip2), z, n );
	    if (z1 != null & z2 != null) {
		j1 = z1.idx;
		j2 = z2.idx;
		offset[j1]++;
	    }
	}
	a.k[0] = 0;
	for (j=0; j<n; j++) {
	    a.k[j+1] = a.k[j] + offset[j];
	    offset[j] = 0;
	}

	try { in = new DataInputStream(
		   new BufferedInputStream(
		   new FileInputStream("mknet/arcs.dat"))); }
	catch (IOException fe){
	    System.out.println("File mknet/arcs.dat not found");
	    createnetwork(n);
	    return;
	}

	nz = FileIO.readInt(in);
	for (int kk=0; kk<nz; kk++) {
	    zip1 = FileIO.readString(in);
	    zip2 = FileIO.readString(in);
	    z1 = (ZIPDATA) Search.binary( new ZIPDATA(zip1), z, n );
	    z2 = (ZIPDATA) Search.binary( new ZIPDATA(zip2), z, n );
	    if (z1 != null && z2 != null) {
		j1 = z1.idx;
		j2 = z2.idx;
		k = a.k[j1] + offset[j1];
		offset[j1]++;
		a.i[k] = j2;
		a.j[k] = j1;
	    }
	}

	for (j=0; j<n; j++) {
	    if (a.k[j+1] == a.k[j]) {
		System.out.print("neighborless zip = "+z[j].zip);
		System.out.print(": lat = "+z[j].lat);
		System.out.println(", lon = "+z[j].lon);
	    }
	}

	GL.color(Color.black);
	GL.clear();

	for (j=0; j<n; j++) {
	    GL.color( color[(j+1)%7] );
	    for (k=a.k[j]; k<a.k[j+1]; k++) {
		GL.move2( z[j].x, z[j].y );
		GL.draw2( z[a.i[k]].x, z[a.i[k]].y );
	    }
	}

	paint(g);

	// **********************************************************
	// linkr/linkl is a bidirectional linked list of arcs
	// (i.e. indices into a.i/a.j) marking territory-transition arcs.
	// **********************************************************

	nz = a.k[n];  // number of adjacencies

	linkr = new int[nz+2];
	linkl = new int[nz+2];
    }

    void createnetwork( int n )
    {
	    int pass, npass=4, i, j, j1, j2, k, new_k, cnt;
	    POINT[] p;
	    POINT[] road;
	    POINT r1, r2;
	    ZIPDATA z1, z2;
	    int[] offset;

	    p = new POINT[n];

	    for (j=0; j<n; j++) {
		p[j] = new POINT();
		p[j].x = z[j].x;
		p[j].y = z[j].y;
		p[j].handle = (Object) z[j];
	    }

	    a   = new ARCS();
	    a.k = new int[n+1];
	    a.i = new int[2*npass*n];

	    offset = new int[n];
	    for (j=0; j<n; j++) {
		offset[j] = 0;
		a.k[j] = 2*npass*j;
	    }
	    a.k[n] = 2*npass*n;

	    //--------------------------------------------------------
	    // Set up adjacency matrix
	    //--------------------------------------------------------

	    for (pass=0; pass<npass; pass++) {
		Sfc.init( p, n );
		road = Sfc.getroad();

		for (k=0; k<n-1; k++) {
		    r1 = road[k];
		    z1 = (ZIPDATA)r1.handle;
		    j1 = z1.idx;

		    r2 = road[k+1];
		    z2 = (ZIPDATA)r2.handle;
		    j2 = z2.idx;

		    for (i=0; i<offset[j1]; i++) {
			if (j2 == a.i[a.k[j1]+i]) break;
		    }

		    if (i==offset[j1]) {
			a.i[a.k[j1]+offset[j1]] = j2;
			offset[j1]++;

			a.i[a.k[j2]+offset[j2]] = j1;
			offset[j2]++;
		    }
		}
	    }

	    //--------------------------------------------------------
	    // Flag dead space for removal
	    //--------------------------------------------------------

	    cnt = 0;
	    for (j=0; j<n; j++) {
		for (k=a.k[j]+offset[j]; k<a.k[j+1]; k++) {
		    a.i[k] = -1;
		    cnt++;
		}
	    }
	    // System.out.println("number of repeats " + cnt);

	    //--------------------------------------------------------
	    // Remove flagged entries
	    //--------------------------------------------------------

	    for (j=0; j<n; j++) { offset[j] = 0; }
	    new_k = 0;
	    for (j=0; j<n; j++) {
		for (k=a.k[j]; k<a.k[j+1]; k++) {
		    if (a.i[k] != -1) {
			a.i[new_k] = a.i[k];
			new_k++;
			offset[j]++;
		    }
		}
	    }
	    for (j=0; j<n; j++) {
		a.k[j+1] = a.k[j] + offset[j];
	    }

	    //--------------------------------------------------------
	    // Flag outliers for removal
	    //--------------------------------------------------------

	    cnt = 0;
	    for (j=0; j<n; j++) {
		double dist = 0;
		for (k=a.k[j]; k<a.k[j+1]; k++) {
		    j2 = a.i[k];
		    dist += gc_dist(z[j].lat, z[j].lon, z[j2].lat, z[j2].lon);
		}
		dist /= a.k[j+1] - a.k[j];

		for (k=a.k[j]; k<a.k[j+1]; k++) {
		    j2 = a.i[k];
		    if ( gc_dist( z[j].lat, z[j].lon, z[j2].lat, z[j2].lon )
			> (npass*dist)/3 ) {
			a.i[k] = -1;
			cnt++;
		    }
		}
	    }
	    // System.out.println("number of outliers " + cnt);

	    //--------------------------------------------------------
	    // Remove flagged entries
	    //--------------------------------------------------------

	    for (j=0; j<n; j++) { offset[j] = 0; }
	    new_k = 0;
	    for (j=0; j<n; j++) {
		for (k=a.k[j]; k<a.k[j+1]; k++) {
		    if (a.i[k] != -1) {
			a.i[new_k] = a.i[k];
			new_k++;
			offset[j]++;
		    }
		}
	    }
	    for (j=0; j<n; j++) {
		a.k[j+1] = a.k[j] + offset[j];
	    }

	    /*
	    REALLOC( a.i, a.k[n], int );
	    */
	    a.i = (int[]) Memory.alloc(a.i, a.k[n]);

	    a.j = new int[a.k[n]];
	    for (j=0; j<n; j++) {
		for (k=a.k[j]; k<a.k[j+1]; k++) {
		    a.j[k] = j;
		}
	    }

	    GL.color(Color.black);
	    GL.clear();

	    for (j=0; j<n; j++) {
		GL.color( color[(j+1)%7] );
		for (k=a.k[j]; k<a.k[j+1]; k++) {
		    GL.move2( z[j].x, z[j].y );
		    GL.draw2( z[a.i[k]].x, z[a.i[k]].y );
		}
	    }

	    paint(g);

	    // **********************************************************
	    // linkr/linkl is a bidirectional linked list of arcs
	    // (i.e. indices into a.i/a.j) marking territory-transition arcs.
	    // **********************************************************

	    int nz = a.k[n];  // number of adjacencies

	    linkr = new int[nz+2];
	    linkl = new int[nz+2];
    }

    void optimize(
	int n,
	int m,
	double totatt,
	PARAMS params
    )
    {
	int i, j, changes;
	double totscale, obj, oobj, bias, wt, dobj, bobj;
	/*
	char fname[30];
	FILE *fp;
	*/

	if (n==0) return;

	// ******************************************************
	// Initialize target
	// ******************************************************

	target = new double[m];

	totscale = 0.0;
	for (i=0; i<m; i++) {
	    totscale += t[i].scale;
	}
	for (i=0; i<m; i++) {
	    target[i] = totatt * t[i].scale / totscale;
	}

	// ******************************************************
	// Compute two main terms of objective function and initialize wt:
	//     bobj = sum_i (total_attribute_of_terr_i - target_i)^2
	//            is the balance term, and
	//     dobj = sum_j great-circle_distance_from_zip_j_to_territory_center
	//            is the distance term (for compactness)
	// Objective function is:
	//     sum_i (total_attribute_of_terr_i - target_i)^2
	//     +
	//     wt *
	//     sum_j great-circle_distance_from_zip_j_to_territory_center
	// ******************************************************

	dobj = 0;
	bobj = 0;
	for (i=0; i<m; i++) {
	    dobj += t[i].totdist;
	    bobj += sq(target[i] - t[i].att);
	}

	// ******************************************************
	// Here's the outer loop of the main optimization
	// ******************************************************

	  bias = params.bias_start;
	  wt = bias * bobj/dobj;

	  while( bias >= params.bias_end ) {
	    System.out.println("bias = " + bias);
	    obj = HUGE_VAL;
	    do {
		changes=0;
		oobj = obj;
		obj = compute_obj(m, target, wt);
		if (obj > oobj) break;

		changes  = rebalance(n, m, wt, target, params);

		System.out.println("changes = " + changes);

	    } while (changes>params.nchanges);

	    /*
	    show_data( z, t, n, m );

	    sprintf(fname, "ass_%0.0f", bias);
	    fp = fopen( fname, "a" );
	    for (j=0; j<n; j++) {
		i = z[j].terr;
		strcpy(format, "\"%5s\",\"");
		strcat(format, params->reg_format);
		strcat(format, params->dist_format);
		strcat(format, params->terr_format);
		strcat(format, "\",\"%7.0lf\"\n");
		fprintf(fp, format,
			    z[j].zip, t[i].reg, t[i].dist, t[i].terr, z[j].att);
	    }
	    fflush(fp);
	    fclose(fp);

	    sprintf(fname, "terr_zlls_%0.0f", bias);
	    fp = fopen( fname, "w" );
	    for (i=0; i<m; i++) {
		strcpy(format, "\"%05d\",\"%8.4lf\",\"%8.4lf\",\"AA\"\n");
		fprintf(fp, format, atoi(t[i].terr), t[i].lat, t[i].lon);
	    }
	    fflush(fp);
	    fclose(fp);

	    sprintf(fname, "terr_zta_%0.0f", bias);
	    fp = fopen( fname, "w" );
	    for (i=0; i<m; i++) {
		strcpy(format, "\"%05d\",\"00000000\",\"%8.0lf\"\n");
		fprintf(fp, format, atoi(t[i].terr), t[i].att);
	    }
	    fflush(fp);
	    fclose(fp);
	    */

	    wt   /= params.bias_reduction;	// reduce wt
	    bias /= params.bias_reduction;	// and bias accordingly
	  }

    }

    double sq( double x) { return x*x; }

    double MAX(double x,double y) {return x > y ? x : y;}
    double MIN(double x,double y) {return x > y ? y : x;}
    double ABS(double x)          {return x > 0 ? x : -x;}
    double RATIO(double x, double y) {return x > y ? x/y : y/x ;}

    double xx1, yy1, zz1, xx2, yy2, zz2;
    double minx, maxx, miny, maxy, ascale;
    double mnx, mxx, mny, mxy;

    static final double HUGE_VAL = 1.0e+300;
    static final double EPS = 0.001;
    static final int _NATIONAL = 0;
}

final class ZIPDATA implements myutil.Comparable
{
    String zip;         // zip code
    double lat;         // latitude
    double lon;         // longitude
    String st;          // state (or other regional entity)
    double att;         // amount of 'attribute' in this zip code
    int terr;           // territory to which zip code is assigned
    double x;           // azimuthal coordinate (for graphics)
    double y;           // the other azimuthal coordinate
    int idx;

    public ZIPDATA() { }

    public ZIPDATA(String zip) {
	this.zip = zip;
    }

    public int compare(myutil.Comparable b) {
	ZIPDATA zb = (ZIPDATA) b;
	return zip.compareTo(zb.zip);
    }
}

final class TERRDATA
{
    double  lat;        // latitude of ``median''
    double  lon;        // longitude of ``median''
    double  att;        // total attribute
    int     nzips;      // number of zips in this territory
    ZIPDATA[] ziplist;  // array of pointers to ZIPDATAs
    double  totdist;    // total distance to ``median''
    int     nrecomp;    // number of changes remaining till next
                        // recomputation of ``median''
    String   terr;      // territory number
    double  scale;      // scale factor to adjust target by
}

// *********************************************************
// Data structure for zip code adjacency information.
// Suppose a is an ARCS structure.  Then, a.k and a.i are
// arrays of integers constituting the usual sparse matrix
// representation of the adjacency matrix.  That is,
//      a.i[] is an array of neighbors of the zipcodes starting
//              with the 0th zipcode and going up to the n-1st one, and
//      a.k[j] is the index in a.i at which the neighbors of zipcode
//              j start.
// Array a.j[] is an array for which a.j[k] = j if and only if
//      a.k[j] <= k < a.k[j+1].
// For example, suppose that there are 4 zipcodes, 0,1,2,3, and that
// they have the following adjacencies:
//      0 -> 1, 2, 3
//      1 -> 0, 3
//      2 -> 0
//      3 -> 0, 1.
// Then,
//      a.i[] = 1 2 3 0 3 0 0 1
//      a.j[] = 0 0 0 1 1 2 3 3
//      a.k[] = 0     3   5 6   8
// *********************************************************

final class ARCS
{
    int[] i;
    int[] j;
    int[] k;
}

// *********************************************************
// A data structure containing all of the adjustable parameters
// *********************************************************

final class PARAMS
{
    int    m;
    int    nchanges;
    int    probtype;
    double wt;
    double bias_start;
    double bias_end;
    double frac1;
    double bias_reduction;
    String   reg_format;
    String   dist_format;
    String   terr_format;
}

// *********************************************************
// A data structure for the barriers.  It consists of a pair
// of lat/lons and an associated length penalty to add to each
// length that crosses the short segment of the great circle determined
// by these two points.
// *********************************************************

final class BARRIERS
{
    double lat0;
    double lon0;
    double lat1;
    double lon1;
    double len;
}
