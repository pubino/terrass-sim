package myutil;

import java.awt.*;
import java.applet.*;
import java.awt.image.*;
import java.util.*;
import java.net.*;

import myutil.*;

public class AppletFrame extends Frame
implements AppletStub, AppletContext
{
    public AppletFrame(Applet a, int x, int y)
    {
	setTitle("Territory Assignment Optimizer");
	resize(x,y);
	add("Center", a);
	a.setStub(this);
	a.init();
	addWindowListener(new java.awt.event.WindowAdapter() {
	    public void windowClosing(java.awt.event.WindowEvent e) {
		System.exit(0);
	    }
	});
	setVisible(true);
	a.start();
    }

    // AppletStub methods
    public boolean isActive()    {return true;}
    public URL getDocumentBase() {return null;}
    public URL getCodeBase()     {return null;}
    public String getParameter() {return "";}
    public String getParameter(String s) {return "";}
    public AppletContext getAppletContext() {return this;}
    public void appletResize(int width, int height) {}

    // AppletContext methods
    public AudioClip getAudioClip(URL url) {return null;}
    public Image getImage(URL url) {return null;}
    public Applet getApplet(String name) {return null;}
    public Enumeration getApplets() {return null;}
    public void showDocument(URL url, String target) {}
    public void showDocument(URL url) {}
    public void showStatus(String status) {}
    public java.io.InputStream getStream(String key) {return null;}
    public java.util.Iterator<String> getStreamKeys() {return null;}
    public void setStream(String key, java.io.InputStream stream) {}
}
