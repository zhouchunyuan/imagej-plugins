import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.tool.PlugInTool;
import ij.plugin.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.Math;


/** This plugin pops up a panel holding buttons to correct tiled (large) image **/
/*
    This plugin works better with more tiling numbers.
    User must input 2 of the first 3 lines, and click a button to deduce the last parameter.
    The slider is used to specifiy the overlap range whoes default is 50% of the small area size
    click the "show graph" to see line projection and fix curves.
*/
public class Tiling_Panel extends PlugInFrame implements ActionListener, IJEventListener,FocusListener {
	private static Frame instance;
	private boolean logging;

        public ImagePlus ip;
        
        public int largeX;
        public int largeY;
        public int smallX=512;
        public int smallY=512;
        public int NUM_X = 10;
        public int NUM_Y = 10;
        public double OVRLP = 0.1; // overlap
        
        public double fixRange = 0.5; // make smooth connection from "fixRange" of small size from merge center
                                      // max is 0.5, which means connect from centers of two neibour area
        
        public double [] projX;
        public double [] projY;
        
        public double [] fixCurvX;
        public double [] fixCurvY;
        
        public TextField TF_num_X, TF_num_Y, TF_size_X, TF_size_Y, TF_OVRLP;
        public Choice imgChooser;
        public Label sizeInfo;
        public Scrollbar sclRange;
        
	public Tiling_Panel() {
		super("Tiling Panel");
		if (instance!=null) {
			instance.toFront();
			return;
		}
		if (IJ.versionLessThan("1.47e"))
			return;
		instance = this;
                
                //ip = IJ.getImage();
                //largeX = ip.getWidth();
                //largeY = ip.getHeight();
                
		addKeyListener(IJ.getInstance());
		IJ.addEventListener(this);
		setLayout(new FlowLayout());
		Panel panel = new Panel();
		panel.setLayout(new GridLayout(8, 1, 10, 10));

                // GridLayout line 1
                imgChooser = new Choice();
                imgChooser.addFocusListener(this);
                imgChooser.addItemListener( 
                        new ItemListener() {
                                public void itemStateChanged(ItemEvent e) {
                                        ip = ij.WindowManager.getImage(imgChooser.getSelectedItem());
                                        largeX = ip.getWidth();
                                        largeY = ip.getHeight();
                                        sizeInfo.setText("large size : "+largeX+" x "+largeY);
                                }
                        });
                panel.add(imgChooser); 
                               
                // GridLayout line 2
                sizeInfo = new Label("large size : "+largeX+" x "+largeY);
                panel.add(sizeInfo);
                // GridLayout line 3
                Panel inputPanel = new Panel();
                inputPanel.setLayout(new GridLayout(1,5));
                    inputPanel.add(new Label("Overlap"));
                    TF_OVRLP = new TextField(""+OVRLP);
                    inputPanel.add(TF_OVRLP);
                    
                    inputPanel.add(new Label(""));
                    inputPanel.add(new Label(""));
                    
                    Button cb1 = new Button("calculate overlap");
		    cb1.addActionListener(this);
		    inputPanel.add(cb1);
                panel.add(inputPanel);              
                // GridLayout line 4
                inputPanel = new Panel();
                inputPanel.setLayout(new GridLayout(1,5));
                    inputPanel.add(new Label("X"));
                    TF_num_X = new TextField(""+NUM_X);
                    inputPanel.add(TF_num_X);
                    
                    inputPanel.add(new Label("Y"));
                    TF_num_Y = new TextField(""+NUM_Y);
                    inputPanel.add(TF_num_Y);
                    
                    Button cb2 = new Button("calculate numbers");
		    cb2.addActionListener(this);
		    inputPanel.add(cb2);
                panel.add(inputPanel);
                // GridLayout line 5
                inputPanel = new Panel();
                inputPanel.setLayout(new GridLayout(1,5));
                    inputPanel.add(new Label("small X"));
                    TF_size_X = new TextField(""+smallX);
                    inputPanel.add(TF_size_X);
                    
                    inputPanel.add(new Label("small Y"));
                    TF_size_Y = new TextField(""+smallY);
                    inputPanel.add(TF_size_Y);
                    
                    Button cb3 = new Button("calculate sizes");
		    cb3.addActionListener(this);
		    inputPanel.add(cb3);
                panel.add(inputPanel);
                // GridLayout line 6
                inputPanel = new Panel();
                inputPanel.setLayout(new GridLayout(1,5));
                    inputPanel.add(new Label("Overlap range (1%-50% of each area size):"));
                    sclRange = new Scrollbar(Scrollbar.HORIZONTAL,100,10,1,100);
                    sclRange.addAdjustmentListener( 
                        new AdjustmentListener() {
                                public void adjustmentValueChanged(AdjustmentEvent e) {
                                        fixRange = ((double)sclRange.getValue())/((double)sclRange.getMaximum())*0.5;
                                        return;
                                }
                        });
                    inputPanel.add(sclRange);
                panel.add(inputPanel);
                // GridLayout line 7
                inputPanel = new Panel();
                inputPanel.setLayout(new GridLayout(1,2));
		    Button b = new Button("show graph");
		    b.addActionListener(this);
		    inputPanel.add(b);

		    b = new Button("Do correction");
		    b.addActionListener(this);
		    inputPanel.add(b);
                panel.add(inputPanel);
                
		add(panel);
		pack();
		GUI.center(this);
		setVisible(true);
	}
        
        /***********************************************/
        // calculate parameters
        /***********************************************/
        public void calculate(String cmd ){
                smallX = Integer.parseInt(TF_size_X.getText());
                smallY = Integer.parseInt(TF_size_Y.getText());
                OVRLP = Double.parseDouble(TF_OVRLP.getText());
                NUM_X = Integer.parseInt(TF_num_X.getText());
                NUM_Y = Integer.parseInt(TF_num_Y.getText());
                
                if(cmd == "calculate sizes"){
                        smallX = (int)(largeX/((1-OVRLP)*(NUM_X-1)+1)+0.5) ;
                        smallY = (int)(largeY/((1-OVRLP)*(NUM_Y-1)+1)+0.5) ; 
                        TF_size_X.setText(""+smallX);
                        TF_size_Y.setText(""+smallY);
                }
                if(cmd == "calculate numbers"){
                        NUM_X = (int)(largeX / ((1-OVRLP)*smallX));
                        NUM_Y = (int)(largeY / ((1-OVRLP)*smallY));
                        TF_num_X.setText(""+NUM_X);
                        TF_num_Y.setText(""+NUM_Y);
                }
                if(cmd == "calculate overlap"){
                        double ovrlpx = (double)(smallX*NUM_X-largeX)/(smallX*NUM_X-smallX);
                        double ovrlpy = (double)(smallY*NUM_Y-largeY)/(smallY*NUM_Y-smallY);
                        if(Math.abs(ovrlpx-ovrlpy)>0.001)
                                IJ.log("error when calculate overlap");
                        else
                                OVRLP = (ovrlpx+ovrlpy)/2;
                        
                        TF_OVRLP.setText(""+OVRLP);
                }
                return;
        }
        /******************************************/
        // create line intensity projection on X/Y
        // method 2
        // the correction is done on "fixRange" of the small
        // size, which is adjustable
        /******************************************/
        public void intensityProjection2(){
                projX = new double[largeX];
                projY = new double[largeY];

                ImageProcessor ipr = ip.getProcessor();

                for(int j=0;j<largeY;j++){
                        for(int i=0;i<largeX;i++){
                                int v = ipr.get(i,j);
                                projX[i]+=v;
                                projY[j]+=v;
                        }
                        
                }
                
                fixCurvX = new double[largeX];
                fixCurvY = new double[largeY];
                
                
               
                int nonOverlap = (int)((1-OVRLP)*smallX+0.5);
                int overlap =(int)(OVRLP*smallX+0.5);
                double range = fixRange*smallX-overlap/2;
                
                for(int i=0;i<largeX;i++){
                        int n = (int)((double)i/nonOverlap+0.5);
                        if(n>0 && n<NUM_X){
                                
                                int x1 = (int)(n*nonOverlap + (double)overlap/2 - range+0.5);
                                int x2 = (int)(n*nonOverlap + (double)overlap/2 + range+0.5);
                                
                                if(i>x1 && i<x2){
                                int idx = i-x1;
                                double delta = projX[x2] - projX[x1];
                                double k = delta*idx/(x2-x1);
                                double absCurvValue = projX[x1]+k;
                                fixCurvX[i]= absCurvValue/projX[i];
                                }else{
                                        fixCurvX[i]=1;
                                }
                        }else{
                                fixCurvX[i] = 1;
                        }
                
                }

                nonOverlap = (int)((1-OVRLP)*smallY+0.5);
                overlap =(int)(OVRLP*smallY+0.5);
                range = fixRange*smallY-overlap/2;
                
                for(int i=0;i<largeY;i++){
                        int n = (int)((double)i/nonOverlap+0.5);
                        if(n>0 && n <NUM_Y ){
                                int x1 = (int)(n*nonOverlap +(double)overlap/2 - range+0.5);
                                int x2 = (int)(n*nonOverlap +(double)overlap/2 + range+0.5);
                                
                                if(i>x1 && i<x2){
                                int idx = i-x1;
                                double delta = projY[x2] - projY[x1];
                                double k = delta*idx/(x2-x1);
                                double absCurvValue = projY[x1]+k;
                                fixCurvY[i]= absCurvValue/projY[i];
                                }else{
                                        fixCurvY[i] = 1;
                                }
                        }else{
                                fixCurvY[i] = 1;
                        }
                
                }
                
                return;
        }

        public void showGraph(){
                double[] x = new double[largeX];
                double[] y = new double[largeX];
                for(int i=0;i<largeX;i++){
                        x[i]=(double)i;
                        y[i]=projX[i]*fixCurvX[i];
                }
                Plot plot = new Plot("projection and fix in X","x","y");
                plot.setColor("red");
                plot.addPoints(x,projX,Plot.LINE);
                plot.setColor("blue");
                plot.addPoints(x,y,Plot.LINE);
                plot.show();
                
                x = new double[largeY];
                y = new double[largeY];
                for(int i=0;i<largeY;i++){
                        x[i]=(double)i;
                        y[i]=projY[i]*fixCurvY[i];
                }                
                plot = new Plot("projection and fix in Y","x","y");
                plot.setColor("red");
                plot.addPoints(x,projY,Plot.LINE);
                plot.setColor("blue");
                plot.addPoints(x,y,Plot.LINE);
                plot.show();
                
                return;
        }        
        /******************************************/
        // create line intensity projection on X/Y
        // the correction is done by half of the small
        // size
        /******************************************/
        public void intensityProjection(){
                projX = new double[largeX];
                projY = new double[largeY];

                ImageProcessor ipr = ip.getProcessor();

                for(int j=0;j<largeY;j++){
                        for(int i=0;i<largeX;i++){
                                int v = ipr.get(i,j);
                                projX[i]+=v;
                                projY[j]+=v;
                        }
                        
                }
                
                fixCurvX = new double[largeX];
                fixCurvY = new double[largeY];
                
                int nonOverlap = (int)((1-OVRLP)*smallX+0.5);
                int overlap =(int)(OVRLP*smallX+0.5);
                for(int i=0;i<largeX;i++){
                        int n = (int)((double)i/nonOverlap+0.5);
                        if(n>0 && n<NUM_X){
                                int x1 = (int)(n*nonOverlap - (double)nonOverlap/2+0.5);
                                int x2 = (int)(n*nonOverlap + (double)nonOverlap/2+0.5);
                                int idx = i-x1;
                                double delta = projX[x2] - projX[x1];
                                double k = delta*idx/(x2-x1);
                                double absCurvValue = projX[x1]+k;
                                fixCurvX[i]= absCurvValue/projX[i];
                        }else{
                                fixCurvX[i] = 1;
                        }
                
                }

                nonOverlap = (int)((1-OVRLP)*smallY+0.5);
                overlap =(int)(OVRLP*smallY+0.5);
                for(int i=0;i<largeY;i++){
                        int n = (int)((double)i/nonOverlap+0.5);
                        if(n>0 && n <NUM_Y ){
                                int x1 = (int)(n*nonOverlap -(double)nonOverlap/2+0.5);
                                int x2 = (int)(n*nonOverlap +(double)nonOverlap/2+0.5);
                                int idx = i-x1;
                                double delta = projY[x2] - projY[x1];
                                double k = delta*idx/(x2-x1);
                                double absCurvValue = projY[x1]+k;
                                fixCurvY[i]= absCurvValue/projY[i];
                        }else{
                                fixCurvY[i] = 1;
                        }
                
                }

                double[] x = new double[largeX];
                double[] y = new double[largeX];
                for(int i=0;i<largeX;i++){
                        x[i]=(double)i;
                        y[i]=projX[i]*fixCurvX[i];
                }
                Plot plot = new Plot("projection and fix in X","x","y");
                plot.setColor("red");
                plot.addPoints(x,projX,Plot.LINE);
                plot.setColor("blue");
                plot.addPoints(x,y,Plot.LINE);
                plot.show();
                
                x = new double[largeY];
                y = new double[largeY];
                for(int i=0;i<largeY;i++){
                        x[i]=(double)i;
                        y[i]=projY[i]*fixCurvY[i];
                }                
                plot = new Plot("projection and fix in Y","x","y");
                plot.setColor("red");
                plot.addPoints(x,projY,Plot.LINE);
                plot.setColor("blue");
                plot.addPoints(x,y,Plot.LINE);
                plot.show();
                
                return;
        }
        /******************************************/
        // fix it on itself
        /******************************************/
        public void fixLargeImage(){
                ImageProcessor ipr = ip.getProcessor();
                for(int j=0;j<largeY;j++){
                        IJ.showProgress((double)(j+1)/largeY);
                        for(int i=0;i<largeX;i++){
                                int v = ipr.get(i,j);
                                ipr.set(i, j, (int)(v*fixCurvX[i]*fixCurvY[j]));
                        }
                }  
                ip.updateAndDraw();                
        } 
        /******************************************/
        // fix it 2
        // create new image
        /******************************************/
        public void fixLargeImage2(){
                ImagePlus imp = IJ.createImage("Untitled", "16-bit black", largeX, largeY, 1);
                ImageProcessor ipr2 = imp.getProcessor(); 
                
                ImageProcessor ipr = ip.getProcessor();
                for(int j=0;j<largeY;j++){
                        for(int i=0;i<largeX;i++){
                                int v = ipr.get(i,j);
                                ipr2.set(i, j, (int)(v*fixCurvX[i]*fixCurvY[j]));
                        }
                }  
                imp.show();
                IJ.run(imp, "Enhance Contrast", "saturated=0.35");
        }         
        
	public void actionPerformed(ActionEvent e) {

		if(e.getActionCommand() =="show graph"){
                        intensityProjection2();
                        showGraph();
                }else if(e.getActionCommand() =="Do correction"){
                        intensityProjection2();
                        fixLargeImage2();

		}else
			calculate(e.getActionCommand());
	}
	public void eventOccurred(int eventID) {
		if (!logging)
			return;
	}
	public void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID()==WindowEvent.WINDOW_CLOSING) {
			instance = null;	
			IJ.removeEventListener(this);
                        //IJ.log("close");
		}
	}

               
        public void focusGained(FocusEvent e){
                String[] s = ij.WindowManager.getImageTitles();
                imgChooser.removeAll();
                for(String item : s){
                        imgChooser.add(item);
                }
                if(imgChooser.getItemCount()>0){
                        ip = ij.WindowManager.getImage(imgChooser.getSelectedItem());
                        largeX = ip.getWidth();
                        largeY = ip.getHeight();
                        sizeInfo.setText("large size : "+largeX+" x "+largeY);
                }
                return;
        }
        


        
    ///////////////////////////////////////////////////////////////
    static class CustomCanvas extends ImageCanvas implements MouseWheelListener { 

        public CustomCanvas(ImagePlus image) {
            super(image); 
            addMouseWheelListener(this); 
        } 
        public void mouseWheelMoved(MouseWheelEvent event) { 
            synchronized(this) { 
                int wheel = event.getWheelRotation();
                int screenx =offScreenX(event.getX());
                int screeny =offScreenY(event.getY());

                if (wheel<0){ 
                    IJ.log("hello");//
                }else{IJ.log("down");}
                    //

            } 
        }
    }//Class CustomCanvas

}




