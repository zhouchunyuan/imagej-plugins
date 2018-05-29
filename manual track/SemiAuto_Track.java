import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;


/**
    Adds a panel containing "start" button
    yellow to define target to track, red to define search area
    Use wheel to change search area, use ctrl-wheel to change target size
    .
*/
public class SemiAuto_Track implements PlugIn,KeyListener {

        /*************** global var **************/
        ImagePlus imp;
        CompositeImage cimp;
        int pos[][];

        int currentMouseX;
        int currentMouseY;
        int roiSize = 40;
        int searchSize = 80;
        int currentRoiX;
        int currentRoiY;
        /**************** functions **************/
        double correlation(byte[] a,byte[] b) {

            double sumAB=0;
            double sumAsqr=0,sumBsqr=0;
            double mA=0,mB=0;
            int n = a.length;
            /**
            for (int i=0;i<n;i++) {
                mA += a[i];
                mB += b[i];
            }
            mA /= n;
            mB /= n;
            for (int i=0;i<n;i++) {
                double A = a[i]-mA;
                double B = b[i]-mB;
                sumAB+=A*B;
                sumAsqr+=A*A;
                sumBsqr+=B*B;
            }
            */
            for (int i=0;i<n;i++) {
                double A = a[i];
                double B = b[i];
                sumAB+=A*B;
                sumAsqr+=A*A;
                sumBsqr+=B*B;
            }
            return sumAB/(Math.sqrt(sumAsqr)*Math.sqrt(sumBsqr));
        }
        double correlation(byte[] a,byte[] b, byte meanb) {

            double sumAB=0;
            double sumAsqr=0,sumBsqr=0;
            double mA=0,mB=0;
            int n = a.length;

            for (int i=0;i<n;i++) {
                double A = a[i];
                double B = b[i];
                if (B > meanb) {
                    sumAB+=A*B;
                    sumAsqr+=A*A;
                    sumBsqr+=B*B;
                }
            }
            return sumAB/(Math.sqrt(sumAsqr)*Math.sqrt(sumBsqr));
        }
        int[] dosearch(ByteProcessor ip0, ByteProcessor ip1) {
            int w0 = ip0.getWidth();
            int h0 = ip0.getHeight();
            int w1 = ip1.getWidth();
            int h1 = ip1.getHeight();

            double roimean = ip0.getStats().mean;
            int x_max=0,y_max=0;
            double corrMax = 0;
            for (int y=h0;y<h1-h0;y++) {
                for (int x=w0;x<w1-w0;x++) {
                    double R = Math.sqrt((x-currentRoiX)*(x-currentRoiX)+(y-currentRoiY)*(y-currentRoiY));
                    if (R < searchSize/2 ) {
                        ip1.setRoi(x - w0/2, y - h0/2, w0, h0);
                        ByteProcessor tmp = (ByteProcessor)ip1.crop();


                        double mean2 = tmp.getStats().mean;
                        if (Math.abs(mean2-roimean)/roimean < 0.2) {
                            byte[] array1 = (byte[])tmp.getPixels();
                            byte[] array0 = (byte[])ip0.getPixels();
                            double corr = correlation(array0,array1,(byte)mean2);
                            if (corrMax < corr) {
                                corrMax=corr;
                                int sumI = 0;
                                int xC=0,yC=0;
                                for (int j=0;j<h0;j++) {
                                    for (int i=0;i<w0;i++) {
                                        int v = tmp.getPixel(i,j);
                                        sumI += v;
                                        xC += v*i;
                                        yC += v*j;
                                    }
                                }
                                xC = xC/sumI;
                                yC = yC/sumI;
                                x_max= x+(xC-w0/2);
                                y_max= y+(yC-h0/2);
                                //x_max = x;
                                //y_max = y;
                                //monitor(ip0,tmp);
                            }
                        }
                    }
                }
            }
            //IJ.log("dosearch:"+x_max+","+y_max);
            return new int[] {x_max,y_max};
        }

        int[] searchByStatistics(ByteProcessor ip0, ByteProcessor ip1) {
            int w0 = ip0.getWidth();
            int h0 = ip0.getHeight();
            int w1 = ip1.getWidth();
            int h1 = ip1.getHeight();

            int distanceSq = (int)Double.POSITIVE_INFINITY;;

            int[][] pixArry = ip0.getIntArray();
            int row = pixArry.length;
            int col = pixArry[0].length;
            //IJ.log(""+d1+","+d2);
            Point cp = new Point(0,0);
            int sumI = 0;
            for (int j=0;j<row;j++) {
                for (int i=0;i<col;i++) {
                    int v = ip0.getPixel(i,j);
                    sumI += v;
                    cp.x += v*i;
                    cp.y += v*j;
                }
            }

            cp.x = cp.x/sumI;
            cp.y = cp.y/sumI;
            //IJ.log("search:  "+cp.x+","+cp.y);
            int mean = sumI/(col*row);
            /***************get cx cy sumI *************/

            Point p = new Point(0,0);

            for (int y=h0;y<h1-h0;y++) {
                for (int x=w0;x<w1-w0;x++) {
                    double R = Math.sqrt((x-currentRoiX)*(x-currentRoiX)+(y-currentRoiY)*(y-currentRoiY));
                    if (ip1.getPixel(x,y)> mean/2 && R < searchSize/2 ) {
                        ip1.setRoi(x - w0/2, y - h0/2, w0, h0);
                        ByteProcessor tmp = (ByteProcessor)ip1.crop();
                        int[][] pixtmp = tmp.getIntArray();

                        Point cptmp = new Point(0,0);;
                        int sumItmp = 0;
                        for (int j=0;j<row;j++) {
                            for (int i=0;i<col;i++) {
                                int v = tmp.getPixel(i,j);
                                sumItmp += v;
                                cptmp.x += v*i;
                                cptmp.y += v*j;
                            }
                        }

                        cptmp.x = cptmp.x/sumItmp;
                        cptmp.y = cptmp.y/sumItmp;

                        double intensityChange =Math.abs((double)(sumI-sumItmp)/sumI);
                        if (intensityChange < 0.1) {
                            int dx = cp.x-cptmp.x;
                            int dy = cp.y-cptmp.y;
                            if (dx*dx+dy*dy<distanceSq) {
                                distanceSq = dx*dx+dy*dy;
                                p.x= x+(cptmp.x-w0/2);
                                p.y= y+(cptmp.y-h0/2);
                                //IJ.log("searchByStatistics sumIntensity A ="+sumI + ", sumIntensity B ="+sumItmp+" Change:"+intensityChange);
                                monitor(ip0,cp,tmp,cptmp);
                            }

                        }
                    }
                }
            }
            //IJ.log("return from searchByStatistics:"+p.x+","+p.y);
            return new int[] {p.x,p.y};
        }

        void monitor(ByteProcessor a,Point centera, ByteProcessor b, Point centerb) {
            /**
              does not work if I clone the processor and setProcessor
            */
            Roi roia = new Roi(centera.x,centera.y,1,1);
            Roi roib = new Roi(centerb.x,centerb.y,1,1);
            roia.setStrokeColor(Color.yellow);
            roib.setStrokeColor(Color.blue);
            Overlay o_mark = new Overlay(roia);
            o_mark.add(roib);
            cimp.setOverlay(o_mark);

            int w = a.getWidth();
            int h = a.getHeight();
            cimp.setSlice(1);
            ByteProcessor bp = (ByteProcessor)cimp.getChannelProcessor();

            //IJ.log("in monitor, center A ="+centera.x+","+centera.y+" center B ="+centerb.x+","+centerb.y);
            for (int j = 0; j<h;j++) {
                for (int i=0;i<w;i++) {
                    bp.putPixel(i,j,a.getPixel(i,j));
                }
            }

            cimp.setSlice(2);
            bp = (ByteProcessor)cimp.getChannelProcessor();

            for (int j = 0; j<h;j++) {
                for (int i=0;i<w;i++) {
                    bp.putPixel(i,j,b.getPixel(i,j));
                }
            }
            cimp.updateAllChannelsAndDraw();
            //cimp.setDisplayMode( IJ.COMPOSITE);

        }

        void markNextFrame() {
            ByteProcessor target_ip = (ByteProcessor)imp.getProcessor().crop();
            imp.setSlice(imp.getCurrentSlice()+1);
            //int[] xy = searchByStatistics(target_ip,(ByteProcessor)imp.getProcessor());
            int[] xy = dosearch(target_ip,(ByteProcessor)imp.getProcessor());
            currentRoiX = xy[0];
            currentRoiY = xy[1];
            //IJ.log("button1:"+currentRoiX+","+currentRoiY);
            imp.setRoi( currentRoiX-roiSize/2,currentRoiY-roiSize/2,roiSize,roiSize);
            pos[imp.getCurrentSlice()-1][0]=currentRoiX;
            pos[imp.getCurrentSlice()-1][1]=currentRoiY;
        }
        public class MarkLoopThread extends Thread {

                public void run() {
                    do {
                        markNextFrame();
                                    Roi roi = new Roi(currentRoiX,currentRoiY,1,1);
                                        roi.setStrokeColor(Color.yellow);
                                Overlay o_mark = new Overlay(roi);
            o_mark.add(roi);
            imp.setOverlay(o_mark);
                        if (IJ.escapePressed()) {
                            IJ.resetEscape();
                            break;
                        }
                    } while (currentRoiX!=0 && imp.getCurrentSlice()<imp.getStackSize());
                }
        }

        void autoMoveMouse(int x,int y) {
            Point p = imp.getCanvas().getLocationOnScreen();
            try {
                Robot robot = new Robot();
                robot.mouseMove(imp.getCanvas().screenX(p.x+x), imp.getCanvas().screenY(p.y+y));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        /** Handle the key typed event . */
        public void keyTyped(KeyEvent e) {
        }

        /** Handle the key-pressed event */
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (key == KeyEvent.VK_SPACE) {
                (new MarkLoopThread()).start();
            }
        }

        /** Handle the key-released event  */
        public void keyReleased(KeyEvent e) {

        }
        /****************  run *******************/
        public void run(String arg) {
            imp = WindowManager.getCurrentImage();
            ImageProcessor ip;
            if (imp==null) {
                IJ.error("Side Panel ", "No file is open.");
                return;
            }
            if (imp.isHyperStack()) {
                IJ.error("Side Panel ", "This plugin does not work with hyperstacks.");
                return;
            }
            CustomCanvas cc = new CustomCanvas(imp);
            if (imp.getStackSize()>1) {
                new CustomStackWindow(imp, cc);
                pos = new int[imp.getStackSize()][2];
            } else {
                IJ.error("Side Panel ", "This plugin works only with stack file.");
                return;
            }
            cc.requestFocus();

            //stk.setPixel((java.lang.Object)(new int[]{0}),1);
            //stk.setPixel((java.lang.Object)(new int[]{0}),1);
            cimp = new CompositeImage( IJ.createImage("Untitled", "8-bit black", 100, 100, 2));
            cimp.setMode(IJ.COMPOSITE);
            cimp.show();
            cimp.getCanvas().zoomIn(50, 50);
            cimp.getCanvas().zoomIn(50, 50);
            cimp.getCanvas().zoomIn(50, 50);
            cc.addKeyListener(this);
        }

        /***************** classes ***************/
        class CustomCanvas extends ImageCanvas {

                CustomCanvas(ImagePlus imp) {
                    super(imp);
                    addMouseWheelListener(new MouseWheelListener() {
                        public void mouseWheelMoved(MouseWheelEvent e) {
                            int modEx = e.getModifiersEx() & MouseWheelEvent.CTRL_DOWN_MASK;
                            int steps = e.getWheelRotation();
                            if (modEx == 0) {
                                searchSize +=2*steps;
                                if (searchSize <roiSize*2)searchSize = roiSize*2;
                                if (searchSize > roiSize*50)searchSize = roiSize*50;
                            } else {
                                roiSize += steps;
                                if (roiSize > 100)roiSize = 100;
                                if (roiSize < 10) roiSize = 10;
                            }
                            repaint();
                        }
                    });
                }

                public void paint(Graphics g) {
                    super.paint(g);

                    int screenSize = (int)(searchSize*getMagnification());
                    int x = screenX(currentMouseX - searchSize/2);
                    int y = screenY(currentMouseY - searchSize/2);
                    g.setColor(Color.red);
                    g.drawOval(x, y, screenSize, screenSize);

                    screenSize = (int)(roiSize*getMagnification());
                    x = screenX(currentMouseX - roiSize/2);
                    y = screenY(currentMouseY - roiSize/2);
                    g.setColor(Color.yellow);
                    g.drawOval(x, y, screenSize, screenSize);

                    imp.setRoi( pos[imp.getCurrentSlice()-1][0]-roiSize/2,pos[imp.getCurrentSlice()-1][1]-roiSize/2,roiSize,roiSize);
                }

                public void mouseMoved(MouseEvent e) {
                    super.mouseMoved(e);
                    currentMouseX = offScreenX(e.getX());
                    currentMouseY = offScreenY(e.getY());
                    repaint();
                }
                public void mousePressed(MouseEvent e) {
                    super.mousePressed(e);
                    int modEx = e.getModifiersEx() & MouseWheelEvent.CTRL_DOWN_MASK;
                    if (e.getButton() == MouseEvent.BUTTON1 && modEx != 0) {
                        currentMouseX = offScreenX(e.getX());
                        currentMouseY = offScreenY(e.getY());
                        imp.setRoi( currentMouseX-roiSize/2,currentMouseY-roiSize/2,roiSize,roiSize);
                        currentRoiX = currentMouseX;
                        currentRoiY = currentMouseY;
                        pos[imp.getCurrentSlice()-1][0]=currentRoiX;
                        pos[imp.getCurrentSlice()-1][1]=currentRoiY;
                        /*
                                                ByteProcessor target_ip = (ByteProcessor)imp.getProcessor().crop();
                                                //byte[] ay = (byte[])imp.getProcessor().crop().getPixels();
                                                imp.setSlice(imp.getCurrentSlice()+1);
                                                //int[] xy = dosearch(target_ip,(ByteProcessor)imp.getProcessor());
                                                int[] xy = searchByStatistics(target_ip,(ByteProcessor)imp.getProcessor());
                                                currentRoiX = xy[0];
                                                currentRoiY = xy[1];

                                                //IJ.log(xy[0]+","+xy[0]+":"+currentMouseX+","+currentMouseY);
                                                imp.setRoi( currentRoiX-roiSize/2,currentRoiY-roiSize/2,roiSize,roiSize);
                                                //monitor(target_ip,(ByteProcessor)imp.getProcessor().crop());
                                                autoMoveMouse(screenX(currentRoiX),screenY(currentRoiY));
                        */
                    }
                    if (e.getButton() == MouseEvent.BUTTON2) {
                        IJ.log("Middle Click!");
                    }
                    if (e.getButton() == MouseEvent.BUTTON3) {

                    }
                }


                /** Overrides handlePopupMenu() in ImageCanvas to suppress the right-click popup menu. */
                protected void handlePopupMenu(MouseEvent e) {

                }

        } // CustomCanvas inner class


        class CustomStackWindow extends StackWindow implements ActionListener  {

                private Button button1, button2;

                CustomStackWindow(ImagePlus imp, ImageCanvas ic) {
                    super(imp, ic);
                    addPanel();
                }

                void addPanel() {
                    Panel panel = new Panel();
                    panel.setLayout(new GridLayout(1, 4));
                    button1 = new Button("Start[space]");
                    button1.addActionListener(this);
                    panel.add(button1);
                    button2 = new Button("Step");
                    button2.addActionListener(this);
                    panel.add(button2);
                    panel.add(new Label(""));
                    add(panel);
                    pack();
                }
                public void mouseWheelMoved(MouseWheelEvent e) {
                    super.mouseWheelMoved(e);
                }

                public void actionPerformed(ActionEvent e) {
                    Object b = e.getSource();
                    if (b==button1) {
                        (new MarkLoopThread()).start();
                    } else if (b== button2) {
                        markNextFrame();
                    }
                    ImageCanvas ic = imp.getCanvas();
                    if (ic!=null)
                        ic.requestFocus();
                }


        } // CustomStackWindow inner class

} // Side_Panel class
