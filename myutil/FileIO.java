package myutil;

import java.text.*;
import java.io.*;
import java.util.*;

public class FileIO
{
    static String s;
    static ParsePosition pp = new ParsePosition(0);
    static NumberFormat nf = NumberFormat.getInstance();
    static StringTokenizer st = null;

    public static String readString(BufferedReader in)
    {
	while (st == null || st.hasMoreTokens() == false) {
	    readLine(in);
            if (s==null) {
                // System.err.println("Cannot read");
                return null;
            }
	}
	return st.nextToken();
    }

    public static String readString(DataInputStream in)
    {
	while (st == null || st.hasMoreTokens() == false) {
	    readLine(in);
            if (s==null) {
                // System.err.println("Cannot read");
                return null;
            }
	}
	return st.nextToken();
    }

    public static int readInt(BufferedReader in)
    {
	while (st == null || st.hasMoreTokens() == false) {
	    readLine(in);
            if (s==null) {
                // System.err.println("Cannot read");
                return -1;
            }
	}
	return Integer.valueOf(st.nextToken()).intValue();
    }

    public static int readInt(DataInputStream in)
    {
	while (st == null || st.hasMoreTokens() == false) {
	    readLine(in);
            if (s==null) {
                // System.err.println("Cannot read");
                return -1;
            }
	}
	return Integer.valueOf(st.nextToken()).intValue();
    }

    public static double readDouble(BufferedReader in)
    {
	while (st == null || st.hasMoreTokens() == false) {
	    readLine(in);
            if (s==null) {
                // System.err.println("Cannot read");
                return -1;
            }
	}
	return Double.valueOf(st.nextToken()).doubleValue();
    }

    public static double readDouble(DataInputStream in)
    {
	while (st == null || st.hasMoreTokens() == false) {
	    readLine(in);
            if (s==null) {
                // System.err.println("Cannot read");
                return -1;
            }
	}
	return Double.valueOf(st.nextToken()).doubleValue();
    }

    public static void readLine(BufferedReader in)
    {
        try{
                s = in.readLine();
		if (s != null) st = new StringTokenizer(s);
        }
        catch (IOException e)
        {
                System.err.println("IO Error in reading");
        }
    }

    public static void readLine(DataInputStream in)
    {
        try{
                s = in.readLine();
		if (s != null) st = new StringTokenizer(s);
        }
        catch (IOException e)
        {
                System.err.println("IO Error in reading");
        }
    }

    public static void write(BufferedWriter out, String s)
    {
	String str = s;
	try{ out.write(str,0,str.length()); }
	catch (IOException e) {System.err.println("IO Error in writing");}
    }

    public static void writeln(BufferedWriter out, String s)
    {
	String str = s;
	try{ out.write(str,0,str.length()); out.newLine(); }
	catch (IOException e) {System.err.println("IO Error in writing");}
    }

    public static void write(BufferedWriter out, double x)
    {
	String str = ""+x;
	try{ out.write(str,0,str.length()); }
	catch (IOException e) {System.err.println("IO Error in writing");}
    }

    public static void writeln(BufferedWriter out, double x)
    {
	String str = ""+x;
	try{ out.write(str,0,str.length()); out.newLine(); }
	catch (IOException e) {System.err.println("IO Error in writing");}
    }

    public static void write(BufferedWriter out, int d)
    {
	String str = ""+d;
	try{ out.write(str,0,str.length()); }
	catch (IOException e) {System.err.println("IO Error in writing");}
    }

    public static void writeln(BufferedWriter out, int d)
    {
	String str = ""+d;
	try{ out.write(str,0,str.length()); out.newLine(); }
	catch (IOException e) {System.err.println("IO Error in writing");}
    }

    public static void flush(BufferedWriter out)
    {
	try{out.flush();}
	catch (IOException e) {System.err.println("IO Error on closing");}
    }

    public static void close(BufferedWriter out)
    {
	try{out.close();}
	catch (IOException e) {System.err.println("IO Error on closing");}
    }

    public static void close(BufferedReader in)
    {
	try{in.close();}
	catch (IOException e) {System.err.println("IO Error on closing");}
    }

    public static void close(DataInputStream in)
    {
	try{in.close();}
	catch (IOException e) {System.err.println("IO Error on closing");}
    }
}
