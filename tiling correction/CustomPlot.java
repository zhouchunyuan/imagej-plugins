import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.tool.PlugInTool;
import ij.plugin.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/*************************************/
/*
   This plot can adjust data curve
   by mouse drag
                                     */
/*************************************/
public class CustomPlot extends Plot {

        // private

        final int targetsNumber = 20;
        final int radius = 5;
        static TargetCircle currentMovingTarget = null;

        double [] axisData;
        double [] raw_data;
        double [] rol_data; //rolling average
        double [] fit_data; //fit data, can be adjusted by mouse



        TargetCircle [] targetCircle;

        class TargetCircle {
                int index = -1;
                
                boolean moving = false;
                public Roi roi;
                public double x,y;// data coordinate
                public Color color = Color.red;
                public boolean isMoving = false;
                
                public TargetCircle() {}

                public TargetCircle(double a,double b, int i) {
                        index = i;
                        moveTo(a,b);
                }

                public boolean onMe(int a, int b) {
                        
                        int screenX = (int)scaleXtoPxl(x);
                        int screenY = (int)scaleYtoPxl(y);

                    return (a-screenX)*(a-screenX)+(b-screenY)*(b-screenY) < radius*radius;
                }
                
                public void moveTo(double a, double b){
                    x=a;
                    y=b;
                        int screenX = (int)scaleXtoPxl(x);
                        int screenY = (int)scaleYtoPxl(y);
                        
                    roi = new OvalRoi(screenX-radius,screenY-radius,radius*2, radius*2);
                    roi.setStrokeColor(Color.red);
                    roi.setStrokeWidth(2);                        
                }

        }

        // update currentMovingTarget related nodes
        // does not update first nor last
        public void updateFitData(){ 
                int i = currentMovingTarget.index;
                    if(i>0 && i<targetsNumber-1){
                            double x0 = targetCircle[i-1].x;
                            double x1 = targetCircle[i].x;
                            double y0 = targetCircle[i-1].y;
                            double y1 = targetCircle[i].y;
                            for(int x=(int)x0;x<x1;x++){
                                    fit_data[x] = y0+(y1-y0)*(x-x0)/(x1-x0);
                            }
                            x0 = targetCircle[i].x;
                            x1 = targetCircle[i+1].x;
                            y0 = targetCircle[i].y;
                            y1 = targetCircle[i+1].y;
                            for(int x=(int)x0;x<x1;x++){
                                    fit_data[x] = y0+(y1-y0)*(x-x0)/(x1-x0);
                            }
                    }

           
        }
        public void drawOverlay() {

                Overlay overlay = new Overlay();

                for(int i=0;i<targetsNumber;i++){
                    overlay.add(targetCircle[i].roi);
                }
                            /** draw polyline of the fix curve **/
                int n = fit_data.length;
                int[] xPoints = new int[n];
                int[] yPoints = new int[n];
                for(int i=0;i<n;i++){
                        xPoints[i] = (int)scaleXtoPxl((double)i);
                        yPoints[i] = (int)scaleYtoPxl((double)fit_data[i]);
                }
                PolygonRoi polyline = new PolygonRoi(xPoints,yPoints, n,PolygonRoi.POLYLINE);
                polyline.setStrokeColor(Color.green);
                polyline.setStrokeWidth(1);
                overlay.add(polyline); 
            
            getImagePlus().setOverlay(overlay);
            updateImage();
        }

        MouseListener mlsnr = new MouseListener() {
            public void mouseClicked(MouseEvent e) {
            }
            public void mousePressed(MouseEvent e) {
                    int x = e.getX();
                    int y = e.getY();
                    for(TargetCircle tc : targetCircle){
                            if(tc.onMe(x,y)){
                                    tc.roi.setStrokeColor(Color.yellow);
                                    tc.moving = true;
                                    currentMovingTarget = tc;
                            }else{
                                    tc.roi.setStrokeColor(Color.red);
                                    tc.moving = false;
                            }
                    }
                    drawOverlay();
            }
            public void mouseReleased(MouseEvent e) {
                    for(TargetCircle tc : targetCircle){
                           tc.roi.setStrokeColor(Color.red);
                           tc.moving = false;
                    }
                    
                    updateFitData();// here will use currentMovingTarget
                    drawOverlay();
                    
                    currentMovingTarget = null;
            }
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}

        };
        MouseMotionListener mmlsnr = new MouseMotionListener() {

            public void mouseDragged(MouseEvent e) {
                    double xm = descaleX(e.getX());
                    double ym = descaleY(e.getY());
                    double x0 = targetCircle[0].x;
                    double x1 = targetCircle[targetsNumber-1].x;
                    
                    if (currentMovingTarget != null){
                        if(currentMovingTarget.index ==0){
                                if(scaleXtoPxl(x1) - scaleXtoPxl(xm) > (targetsNumber-1)*radius)
                                        spreadTargets(xm,x1);
                        }else if (currentMovingTarget.index == targetsNumber-1){
                                if(scaleXtoPxl(xm) - scaleXtoPxl(x0) > (targetsNumber-1)*radius)
                                        spreadTargets(x0,xm);
                        }else currentMovingTarget.moveTo(currentMovingTarget.x,ym);
                }
                drawOverlay();
            }
            public void mouseMoved(MouseEvent e) {}
        };


        // public
        
        // accept double value x0,x1
        // and re spread target circles
        public void spreadTargets(double x0, double x1){

                //IJ.log(""+x1+": "+vx1+" : "+(int)scaleXtoPxl(vx1));
                if(x0<0)x0=0;
                if(x1>fit_data.length-1)x1=fit_data.length-1;

                double step = (x1-x0)/(targetsNumber-1);
                
                targetCircle = new TargetCircle[targetsNumber];
//IJ.log(""+x0+"-"+x1+" step="+step);
                for (int i=1;i<targetsNumber-1;i++) {
                        double idx = i*step+x0;
                        if(idx > fit_data.length-1) idx = fit_data.length-1;
                        
                        targetCircle[i] = new TargetCircle(idx,
                                                   fit_data[(int)idx],
                                                   i);

                } 
                targetCircle[0] = new TargetCircle(x0,
                                                   fit_data[(int)x0],
                                                   0);
                targetCircle[targetsNumber-1] = new TargetCircle(x1,
                                                   fit_data[(int)x1],
                                                   targetsNumber-1);                
        }
        
        /** init data **/
        public void initRawData(double [] data) {

            int n = data.length;
            raw_data = new double [n];
            axisData = new double [n];
            fit_data = new double [n];
            rol_data = new double [n];
            
            for (int i=0;i<data.length;i++) {
                raw_data[i]=data[i];
                axisData[i]=(double)i;
            }
            
            /* calculate rolling average */
            int rollSize = n/20;
            int correction;
            for (int x=0; x<n; x++){
                double rollingAvg = 0.0;
                int start =x-rollSize/2;
                int end = x+rollSize/2;
                if(start<0)start = 0;
                if(end > n)end = n;
                int w = end-start;
                for(int i=start;i<end;i++){
                        rollingAvg += raw_data[i];
                }
                rol_data[x] = rollingAvg/w;
                fit_data[x] = rol_data[x];
            }
            
            addPoints(axisData,raw_data,Plot.LINE);
            addPoints(axisData,fit_data,Plot.LINE);
            show();
           
            /* prepare target circles */
            spreadTargets(0,(double)fit_data.length);


        }
        public double [] getRollAvg() {
            return rol_data;
        }
        public double [] getFitData() {
            return fit_data;
        }
        public CustomPlot(String title,double [] data) {
            super(title,"x","y");

            initRawData(data);

            drawOverlay();
            
            getImagePlus().getCanvas().addMouseListener(mlsnr);
            getImagePlus().getCanvas().addMouseMotionListener(mmlsnr);
        }




}