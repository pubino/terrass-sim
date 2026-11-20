package terrass;

import java.awt.TextArea;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Random;
import myutil.GL;
import sun.misc.Unsafe;

// Run the unmodified desktop solver without constructing AWT text widgets.
public final class TerritoryReference {
    public static final class Trace extends TextArea {
        @Override public void append(String text) {
            System.out.print("progress\t" + text);
        }
    }

    public static void main(String[] args) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Trace trace = (Trace) ((Unsafe) unsafeField.get(null)).allocateInstance(Trace.class);
        Field rngField = Class.forName("java.lang.Math$RandomNumberGeneratorHolder")
            .getDeclaredField("randomNumberGenerator");
        rngField.setAccessible(true);
        ((Random) rngField.get(null)).setSeed(Long.parseLong(args[0]));

        TerrAss app = new TerrAss();
        app.outputArea = trace;
        TerrAssCanvas model = new TerrAssCanvas(app);
        PARAMS params = new PARAMS();
        params.m = Integer.parseInt(args[1]);
        params.bias_start = Double.parseDouble(args[2]);
        params.bias_end = Double.parseDouble(args[3]);
        params.bias_reduction = Double.parseDouble(args[4]);
        params.frac1 = Double.parseDouble(args[5]);
        params.nchanges = Integer.parseInt(args[6]);
        model.latmin = Double.parseDouble(args[7]);
        model.latmax = Double.parseDouble(args[8]);
        model.lonmin = Double.parseDouble(args[9]);
        model.lonmax = Double.parseDouble(args[10]);

        if (!model.read_data(params)) throw new IllegalStateException("Input load failed");
        model.m = params.m;
        GL.ginit(new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB).getGraphics(),
                 32, 32, model, false);
        model.azimuthal(model.n, model.z);
        model.extract_groups(model.n, model.m, params);

        for (int i = 0; i < model.n; i++) {
            ZIPDATA z = model.z[i];
            System.out.println("zip\t" + z.zip + "\t" + z.terr + "\t" + z.att + "\t" + z.x
                + "\t" + z.y + "\t" + z.toString());
            StringBuilder row = new StringBuilder("adj");
            for (int k = model.a.k[i]; k < model.a.k[i+1]; k++) row.append("\t").append(model.a.i[k]);
            System.out.println(row);
        }
        for (int i = 0; i < model.m; i++) {
            TERRDATA t = model.t[i];
            System.out.println("territory\t" + t.att + "\t" + t.lat + "\t" + t.lon + "\t"
                + t.totdist + "\t" + t.nzips + "\t" + model.target[i]);
        }
    }
}
