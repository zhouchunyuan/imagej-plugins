import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.io.*;

import ij.measure.ResultsTable;
import java.util.*;
import java.util.List;
import org.apache.commons.math3.ml.clustering.*;

public class DBSCAN_Cluster implements PlugIn {

	public void run(String arg) {

            OpenDialog openDlg = new OpenDialog("Choose a STORM particle tables","","*.txt");

            ResultsTable table = ResultsTable.open2(openDlg.getPath());
            table.show(openDlg.getFileName());

            double[] x = table.getColumnAsDoubles(table.getColumnIndex("Xc"));
            double[] y = table.getColumnAsDoubles(table.getColumnIndex("Yc"));

            List<DoublePoint> points = new ArrayList<DoublePoint>();
            for(int i=0;i<table.getCounter();i++){
                try {
                    double[] d = new double[2];
                    d[0] = x[i];
                    d[1] = y[i];
                    points.add(new DoublePoint(d));
                } catch (ArrayIndexOutOfBoundsException e) {
                } 
            }
            IJ.log("clustering...");
		DBSCANClusterer dbscan = new DBSCANClusterer(200, 40);
            List<Cluster<DoublePoint>> cluster = dbscan.cluster(points);

            ImagePlus imp = IJ.createImage("Untitled", "8-bit black", 2730, 2730, 1);
            ImageProcessor ip = imp.getProcessor();
            for(Cluster<DoublePoint> c: cluster){
                //IJ.log("...");
                for( DoublePoint dp:c.getPoints()){
                    //IJ.log(""+dp);
                    int p0 = (int)(dp.getPoint()[0]/30);
                    int p1 = (int)(dp.getPoint()[1]/30);
                    ip.putPixel(p0,p1, 255);                    
                }
    
                //IJ.log(""+c.getPoints().get(0));
            }
            imp.show(); 
	}

}
