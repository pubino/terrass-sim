package myutil;

import java.awt.*;
import java.awt.image.*;
import java.awt.FontMetrics.*;
import java.awt.FontMetrics;
import java.awt.Font.*;

public class GL
{
	static Graphics g;

        static double x, y;

	static double xmin, xmax, ymin, ymax;
	static int width, height;

	static Polygon fp;

	static Font fnt;
	static int strXpos=0, strYpos=0;

	static Image bi;
	static Graphics bg;
	static Component c;

	static public synchronized
	void ginit(Graphics gg, int w, int h, Component cc)
	{
		ginit(gg, w, h, cc, true);
	}

	static public synchronized
	void ginit(Graphics gg, int w, int h, Component cc, boolean dblBuffer)
	{
		g = gg;
		c = cc;

		if (dblBuffer) {
		    bi = c.createImage(w,h);
		    bg = bi.getGraphics();
		} else {
		    bg = g;
		}

		width = w;
		height = h;
		xmin = 0;
		xmax = width;
		ymin = 0;
		ymax = height;

		fnt = new Font("Courier",Font.BOLD,12);
		bg.setFont(fnt);
	}

	static public synchronized void swapbuffers()
	{
		g.drawImage(bi,0,0,c);
	}

	static public synchronized void ortho2(double xmin1, double xmax1,
			   double ymin1, double ymax1)
	{
		xmin = xmin1; xmax = xmax1;
		ymin = ymin1; ymax = ymax1;
	}

        static public synchronized double xi( int x)
        {
		return ( ((xmax-xmin)*x)/width + xmin);
        }

        static public synchronized double yi( int y)
        {
		return ( ymax - ((ymax-ymin)*y)/height);
        }

        static public synchronized int x( double x)
        {
                return (int)(( x - xmin )*(width/(xmax-xmin)));
        }

        static public synchronized int y( double y)
        {
                return (int)(( ymax - y )*(height/(ymax-ymin)));
        }

        static public synchronized void pnt2(double x, double y)
        {
                bg.drawRect(x(x),y(y),1,1);
                // bg.drawLine(x(x),y(y),x(x),y(y));
        }

        static public synchronized void drawRect(double x1, double y1, double x2, double y2)
        {
                bg.drawRect(x(x1),y(y1),
		Math.abs(x(x2)-x(x1)),Math.abs(y(y2)-y(y1)));
        }

	static public synchronized void rectf(double x0, double y0, double x1, double y1)
	{
                bg.fillRect(Math.min(x(x0),x(x1)),Math.min(y(y0),y(y1)),
		Math.abs(x(x1)-x(x0)),Math.abs(y(y0)-y(y1)));
	}

        static public synchronized void circf(double x, double y, double r)
        {
                bg.fillArc(x(x-r),y(y+r),x(x+r)-x(x-r),y(y-r)-y(y+r),0,360);
        }

	static public synchronized void color(Color c)
	{
		bg.setColor(c);
	}

	static public synchronized void clear()
	{
		fp = new Polygon();
		fp.addPoint(x(xmin), y(ymin));
		fp.addPoint(x(xmax), y(ymin));
		fp.addPoint(x(xmax), y(ymax));
		fp.addPoint(x(xmin), y(ymax));
		bg.fillPolygon(fp);
	}

	static public synchronized void move2(double xx, double yy)
	{
		x = xx; y = yy;
	}

	static public synchronized void draw2(double xx, double yy)
	{
		bg.drawLine(x(x),y(y),x(xx),y(yy));
		x = xx; y = yy;
	}

	static public synchronized void pmv2(double xx, double yy)
	{
		fp = new Polygon();
		fp.addPoint(x(xx), y(yy));
	}

	static public synchronized void pdr2(double xx, double yy)
	{
		fp.addPoint(x(xx), y(yy));
	}

	static public synchronized void pclos()
	{
		bg.fillPolygon(fp);
	}

	static public synchronized void
	drawString(String str, double xx, double yy, String xpos, String ypos)
	{
		int h, w;
		int dx=0, dy=0;

		h = bg.getFontMetrics().getHeight();
		w = bg.getFontMetrics().stringWidth(str);

		if (xpos.equals( "LEFT" )) { dx = 0;   } else
		if (xpos.equals("CENTER")) { dx = w/2; } else
		if (xpos.equals( "RIGHT")) { dx = w;   }

		if (ypos.equals( "TOP"  )) { dy = -h/2;} else
		if (ypos.equals("CENTER")) { dy = 0;   } else
		if (ypos.equals("BOTTOM")) { dy = h/2; }

		// bg.drawString(str, x(xx)-20, y(yy)+15);
		strXpos = x(xx)-dx;
		strYpos = y(yy)-dy;
		if (strXpos + w > width) {
		    strXpos = 0; strYpos += h;
		}
		bg.drawString(str, strXpos, strYpos);
		strXpos += w;
	}

	static public synchronized void drawString(String str)
	{
		int h, w;

		h = bg.getFontMetrics().getHeight();
		w = bg.getFontMetrics().stringWidth(str);

		if (strXpos + w > width) {
		    strXpos = 0; strYpos += h;
		}
		bg.drawString(str, strXpos, strYpos);
		strXpos += w;
	}

	static public synchronized void drawImage(Image img,
		double x0, double y0, int w, int h, String xpos, String ypos,
		ImageObserver io)
	{
		int dx=0, dy=0;

		if (xpos.equals( "LEFT" )) { dx = 0;   } else
		if (xpos.equals("CENTER")) { dx = w/2; } else
		if (xpos.equals( "RIGHT")) { dx = w;   }

		if (ypos.equals( "TOP"  )) { dy = 0;   } else
		if (ypos.equals("CENTER")) { dy = h/2; } else
		if (ypos.equals("BOTTOM")) { dy = h;   }

		bg.drawImage(img, x(x0)-dx, y(y0)-dy, w, h, io);
	}

}
