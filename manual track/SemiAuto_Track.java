import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;


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
        boolean isTracking=false;

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
        double correlation(int[] a,int[] b, int meanb) {

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
        int[] dosearch(ImageProcessor ip0, ImageProcessor ip1) {
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
                        ImageProcessor tmp = ip1.crop();

                        double mean2 = tmp.getStats().mean;
                        if (Math.abs(mean2-roimean)/roimean < 0.2) {

                            int[] array1 = new int[w0*h0];
                            int[] array0 = new int[w0*h0];
                            for (int j=0;j<h0;j++) {
                                for (int i=0;i<w0;i++) {
                                    array1[w0*j+i]=tmp.getPixel(i,j);
                                    array0[w0*j+i]=ip0.getPixel(i,j);
                                }
                            }


                            double corr = correlation(array0,array1,(int)mean2);
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
        /**
          用于第一次单击找到第一个细胞
          控制外周尺寸，使中间1/4的区域占据90%的总亮度
          (x,y) : pixel position = offScreenXY
        */
        void findFirstCell(int x,int y) {
            int centerSize = 0;
            double centerOverOut = 0;
            ImageProcessor ip = imp.getProcessor();
            do {
                centerSize++;
                int outerSize = 2*centerSize;
                int sumAll = 0, sumIn = 0;
                for (int j=0;j<outerSize;j++) {
                    for (int i=0;i<outerSize;i++) {
                        // 中心点是 x,y 宽outerSize
                        int in_x = i- outerSize/2;
                        int in_y = j- outerSize/2;
                        int v = ip.getPixel(x + in_x , y + in_y);
                        sumAll+=v;
                    }
                }
                //先算出平均值，下面算中心比例时要减去mean/2
                //这样可以避免由于offset引起的对比度不高问题
                int mean = sumAll/(outerSize*outerSize);

                sumAll = 0;
                for (int j=0;j<outerSize;j++) {
                    for (int i=0;i<outerSize;i++) {
                        // 中心点是 x,y 宽outerSize
                        int in_x = i- outerSize/2;
                        int in_y = j- outerSize/2;
                        int v = ip.getPixel(x + in_x , y + in_y)-mean/2;
                        sumAll+=v;
                        if (Math.abs(in_x)<=centerSize/2 && Math.abs(in_y)<=centerSize/2)sumIn+=v;
                    }
                }

                centerOverOut = (double)sumIn/sumAll;
                //IJ.log("centerOverOut:"+centerOverOut);
                if (outerSize >= searchSize) {
                    IJ.error("can not find good size for this cell");
                    break;
                }

            } while (centerOverOut < 0.9);
            roiSize = centerSize*2;
        }

        void markNextFrame() {
            ImageProcessor target_ip = imp.getProcessor().crop();
            imp.setSlice(imp.getCurrentSlice()+1);
            //int[] xy = searchByStatistics(target_ip,(ByteProcessor)imp.getProcessor());
            int[] xy = dosearch(target_ip,imp.getProcessor());
            currentRoiX = xy[0];
            currentRoiY = xy[1];
            //IJ.log("btnAuto:"+currentRoiX+","+currentRoiY);
            imp.setRoi( currentRoiX-roiSize/2,currentRoiY-roiSize/2,roiSize,roiSize);
            pos[imp.getCurrentSlice()-1][0]=currentRoiX;
            pos[imp.getCurrentSlice()-1][1]=currentRoiY;
        }
        public class MarkLoopThread extends Thread {

                public void run() {
                    do {
                        markNextFrame();
                        int x = imp.getCanvas().screenX(currentRoiX);
                        int y = imp.getCanvas().screenY(currentRoiY);
                        Roi roi = new Roi(currentRoiX,currentRoiY,1,1);
                        roi.setStrokeColor(Color.yellow);
                        Overlay o_mark = new Overlay(roi);
                        o_mark.add(roi);
                        imp.setOverlay(o_mark);
                        if (!isTracking)break;
                    } while (currentRoiX!=0 && imp.getCurrentSlice()<imp.getStackSize());
                }
        }
        /**
          保存坐标到CSV文件
        */
        void saveToFile() {
                
             StringBuilder sb = new StringBuilder();
                sb.append("X");
                sb.append(',');
                sb.append("Y");
                sb.append('\n');
                for(int i=0;i<pos.length;i++){
                sb.append(""+pos[i][0]);
                sb.append(',');
                sb.append(""+pos[i][1]);
                sb.append('\n');
                }
                ij.io.SaveDialog sd = new ij.io.SaveDialog("Save pos[][]","trackedPos_"+pos[0][0]+"_"+pos[0][1],".csv");
                String filepath = sd.getDirectory()+sd.getFileName();
                IJ.log(filepath);
                if(filepath=="nullnull")return;
            try {
                PrintWriter pw = new PrintWriter(new File(filepath));
                pw.write(sb.toString());
                pw.close();
            }
            catch (FileNotFoundException e) {

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
                isTracking = !isTracking;
                if (isTracking)
                    (new MarkLoopThread()).start();
            } else if (key == KeyEvent.VK_S) {
                markNextFrame();
            } else if (key == KeyEvent.VK_RIGHT) {
                imp.setSlice(imp.getCurrentSlice()+1);
            } else if (key == KeyEvent.VK_LEFT) {
                imp.setSlice(imp.getCurrentSlice()-1);
            } else if (key == KeyEvent.VK_ENTER) {
                try {
                    Robot robot = new Robot();
                    int mask = InputEvent.BUTTON1_DOWN_MASK;
                    robot.mousePress(mask);
                    robot.mouseRelease(mask);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            } else {}
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
                    removeKeyListener(IJ.getInstance());
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
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        if ("zoom"!=IJ.getToolName()) {
                            currentMouseX = offScreenX(e.getX());
                            currentMouseY = offScreenY(e.getY());
                            imp.setRoi( currentMouseX-roiSize/2,currentMouseY-roiSize/2,roiSize,roiSize);
                            currentRoiX = currentMouseX;
                            currentRoiY = currentMouseY;
                            pos[imp.getCurrentSlice()-1][0]=currentRoiX;
                            pos[imp.getCurrentSlice()-1][1]=currentRoiY;
                            //Ctrl+Click means mark this frame
                            if (modEx==0)imp.setSlice(imp.getCurrentSlice()+1);
                        }
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

                private Button btnAuto, btnStep, btnSave,btnClear;

                CustomStackWindow(ImagePlus imp, ImageCanvas ic) {
                    super(imp, ic);
                    addPanel();
                    //removeKeyListener(IJ.getInstance());
                }

                void addPanel() {
                    Panel panel = new Panel();
                    panel.setLayout(new GridLayout(1, 4));
                    btnAuto = new Button("Start[space]");
                    btnAuto.addActionListener(this);
                    panel.add(btnAuto);
                    btnStep = new Button("Step");
                    btnStep.addActionListener(this);
                    panel.add(btnStep);
                    btnSave = new Button("Save");
                    btnSave.addActionListener(this);
                    panel.add(btnSave);
                    btnClear = new Button("Clear");
                    btnClear.addActionListener(this);
                    panel.add(btnClear);
                    panel.add(new Label(""));
                    add(panel);
                    pack();
                }
                public void mouseWheelMoved(MouseWheelEvent e) {
                    //super.mouseWheelMoved(e);
                }

                public void actionPerformed(ActionEvent e) {
                    Object b = e.getSource();
                    if (b==btnAuto) {
                        isTracking = !isTracking;
                        if (isTracking) {
                            btnAuto.setLabel("Stop[space]");
                            (new MarkLoopThread()).start();
                        } else {
                            btnAuto.setLabel("Start[space]");
                        }
                    } else if (b== btnStep) {
                        markNextFrame();
                    } else if (b== btnSave) {
                        saveToFile();
                    } else if (b== btnClear) {
                        for(int i=0;i<pos.length;i++){pos[i][0]=0;pos[i][1]=0;}
                    }
                    ImageCanvas ic = imp.getCanvas();
                    if (ic!=null)
                        ic.requestFocus();
                }


        } // CustomStackWindow inner class

} // Side_Panel class
