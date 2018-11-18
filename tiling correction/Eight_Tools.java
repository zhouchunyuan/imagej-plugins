import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.tool.PlugInTool;
import ij.plugin.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;


/** This plugin demonstrates how to add multiple tools to the ImageJ toolbar.
	It requires ImageJ 1.47e or later. */
public class Eight_Tools extends PlugInFrame implements ActionListener, IJEventListener {
	private static Frame instance;
	private boolean logging;
        
        private CustomPlot cpx,cpy;

        double[] fitXLine,fitYLine; // mouse ajustable line
        double[] projXLine,projYLine; // projection line
        double[] rolXavgLine,rolYavgLine; // roll average line
        
        public double x0,x1,xc;
        public boolean moveX0 = false;
        public boolean moveX1 = false;
        public boolean moveXc = false;
        int radius = 5;
        public Rectangle range;

        public ImagePlus imp;
        
        public ImagePlus ip;
	public Eight_Tools() {
		super("Eight_Tools");
		if (instance!=null) {
			instance.toFront();
			return;
		}
		if (IJ.versionLessThan("1.47e"))
			return;
		instance = this;
		addKeyListener(IJ.getInstance());
		IJ.addEventListener(this);
		setLayout(new FlowLayout());
		Panel panel = new Panel();
		panel.setLayout(new GridLayout(2, 1, 10, 10));
		Button b = new Button("Add Custom Tools");
		b.addActionListener(this);
		panel.add(b);
		b = new Button("Restore Tools");
		b.addActionListener(this);
		panel.add(b);
		add(panel);
		pack();
		GUI.center(this);
		setVisible(true);

	}

	private void addTools() {
		logging = false;
		Toolbar.removeMacroTools();
		for (int n=1; n<=7; n++)
			new Tool(n);
		logging = true;
	}

	private void restoreTools() {
		logging = false;
                //ImageCanvas ic = new ImageCanvas(ip);
                //ip.setWindow(new ImageWindow(ip,ic));
		Toolbar.restoreTools();
	}

	public Insets getInsets() {
    		Insets i= super.getInsets();
    		return new Insets(i.top+10, i.left+10, i.bottom+10, i.right+10);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().startsWith("Add"))
			addTools();
		else
			restoreTools();
	}

	public void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID()==WindowEvent.WINDOW_CLOSING) {
			instance = null;	
			IJ.removeEventListener(this);
			restoreTools();
		}
	}

	public void eventOccurred(int eventID) {
		if (!logging)
			return;
		switch (eventID) {
			case IJEventListener.FOREGROUND_COLOR_CHANGED:
				String c = Integer.toHexString(Toolbar.getForegroundColor().getRGB());
				c = "#"+c.substring(2);
				IJ.log("The user changed the foreground color to "+c);
				break;
			case IJEventListener.BACKGROUND_COLOR_CHANGED:
				c = Integer.toHexString(Toolbar.getBackgroundColor().getRGB());
				c = "#"+c.substring(2);
				IJ.log("The user changed the background color to "+c);
				break;
			case IJEventListener.TOOL_CHANGED:
				String name = IJ.getToolName();
				//IJ.log("The user switched to the "+name+(name.endsWith("Tool")?"":" tool"));
                                if(name.startsWith("Custom Tool 1")){
                                        
                                        adjustProfile();
                                        
                                }
                                if(name.startsWith("Custom Tool 2")){
                                        
                                        showFixedImage(1);
                                        
                                }
                                if(name.startsWith("Custom Tool 3")){
                                        
                                        showFixedImage(2);
                                        
                                }
                                if(name.startsWith("Custom Tool 4")){
                                        
                                        showFixedImageOnlyX();
                                        
                                }
                                if(name.startsWith("Custom Tool 5")){
                                        
                                        showFixedImageOnlyY();
                                        
                                }
                                if(name.startsWith("Custom Tool 7")){
                                        
                                        restoreTools();
                                        
                                }
				break;
		}
	}

	class Tool extends PlugInTool {
		int toolNumber;
	
		Tool(int toolNumber) {
			this.toolNumber = toolNumber;
			Toolbar.addPlugInTool(this);
		}
	
		public void mousePressed(ImagePlus imp, MouseEvent e) {
		}
	
		public void mouseDragged(ImagePlus imp, MouseEvent e) {
			//show(imp, e, "dragged");
		}
	
		public void showOptionsDialog() {
			IJ.log("User double clicked on the tool icon");
		}
	
		void show(ImagePlus imp, MouseEvent e, String msg) {
			ImageCanvas ic = imp.getCanvas();
			int x = ic.offScreenX(e.getX());
			int y = ic.offScreenY(e.getY());
			IJ.log("Tool "+toolNumber+" "+msg+" at ("+x+","+y+") on "+imp.getTitle());
		}
	
		public String getToolName() {
			return "Custom Tool "+toolNumber;
		}
	
		public String getToolIcon() {
                        String strIcon = "";
                        switch (toolNumber){
                        case 1:
                        strIcon += "C00aP111eee0" // axes
                                +"Ca00P082a4687a5c8e60" // red curve line
                                +"C0a0P0a2e4a69a8ceef0" // green curve line
                                +"C00aTd710P"; // the letter "P"
                        break;
                        case 2:
                        strIcon += "C00aT0e10F" // the letter "F"
                                 + "C00aT7e10i"  // the letter "i"
                                 + "C00aTae10x" // the letter "x"
                                 + "C00aTde101";// the number "1"
                        break;
                        case 3:
                        strIcon += "C00aT0e10F" // the letter "F"
                                 + "C00aT7e10i"  // the letter "i"
                                 + "C00aTae10x" // the letter "x"
                                 + "C00aTde102";// the number "2"
                        break;
                        case 4:
                        strIcon += "C00aT0e10D" // the letter "D"
                                 + "C00aT7e10o"  // the letter "o"
                                 + "C00aTae10_" // the letter "_"
                                 + "C00aTde10X"; //the letter "X"

                        break;
                        case 5:
                        strIcon += "C00aT0e10D" // the letter "D"
                                 + "C00aT7e10o"  // the letter "o"
                                 + "C00aTae10_" // the letter "_"
                                 + "C00aTde10Y"; // the number "Y"
                        break;
                        case 6:
                        break;
                        case 7:
                        strIcon += "C00aO02ee" // circle
                                +"C00aF702c"; // filled rectangle
                        break;
                        };

			return strIcon;
		}
                
	
	}
        
    /*******************************************/
    public void showFixedImage(int type){
            ImagePlus fiximage = IJ.createImage("Untitled", "16-bit black", imp.getWidth(),imp.getHeight(), 1);
            ImageProcessor ipr = fiximage.getProcessor();
            //IJ.log(""+range.x+"|"+range.y+"|");
            if(cpx!=null && cpy!=null){
                    fitXLine = cpx.getFitData();
                    rolXavgLine = cpx.getRollAvg();
                    fitYLine = cpy.getFitData();
                    rolYavgLine = cpy.getRollAvg();
                    
                    double [] baseXData = rolXavgLine;
                    double [] baseYData = rolYavgLine;
                    //type 1 is ratio to rolling average
                    //type 2 is ratio to max intensity projection average line
                    if(type == 1){baseXData = rolXavgLine;baseYData = rolYavgLine;}
                    if(type == 2){baseXData = projXLine;baseYData = projYLine;}
                    
                    for(int j=0;j<range.height;j++)
                            for(int i=0;i<range.width;i++){
                                    int v = imp.getProcessor().get(range.x+i,range.y+j);
                                    ipr.putPixel(range.x+i,range.y+j,
                                    (int)(v*fitXLine[i]/baseXData[i]*fitYLine[j]/baseYData[j]));
                            }
            }
            fiximage.show();
    }

    /*********** showFixedImage Merge in/out area ******************************/
    public void showFixedImageInArea(int type){
            ImagePlus fiximage = IJ.createImage("Untitled", "16-bit black", imp.getWidth(),imp.getHeight(), 1);
            ImageProcessor ipr = fiximage.getProcessor();
            
            for(int j=0;j<imp.getHeight();j++)
                    for(int i=0;i<imp.getWidth();i++){
                            int v = imp.getProcessor().get(i,j);
                            ipr.putPixel(i,j,v);
                    }
            if(cpx!=null && cpy!=null){
                    fitXLine = cpx.getFitData();
                    rolXavgLine = cpx.getRollAvg();
                    fitYLine = cpy.getFitData();
                    rolYavgLine = cpy.getRollAvg();
                    
                    double [] baseXData = rolXavgLine;
                    double [] baseYData = rolYavgLine;
                    //type 1 is ratio to rolling average
                    //type 2 is ratio to max intensity projection average line
                    if(type == 1){baseXData = rolXavgLine;baseYData = rolYavgLine;}
                    if(type == 2){baseXData = projXLine;baseYData = projYLine;}
                    
                    for(int j=0;j<range.height;j++)
                            for(int i=0;i<range.width;i++){
                                    int v = imp.getProcessor().get(range.x+i,range.y+j);
                                    ipr.putPixel(range.x+i,range.y+j,
                                    (int)(v*fitXLine[i]/baseXData[i]*fitYLine[j]/baseYData[j]));
                            }
            }
            fiximage.show();
    }

    /*********** fix only X direction  ******************************/
    public void showFixedImageOnlyX(){
            ImagePlus fiximage = IJ.createImage("Untitled", "16-bit black", imp.getWidth(),imp.getHeight(), 1);
            ImageProcessor ipr = fiximage.getProcessor();
            for(int j=0;j<imp.getHeight();j++)
                    for(int i=0;i<imp.getWidth();i++){
                            int v = imp.getProcessor().get(i,j);
                            ipr.putPixel(i,j,v);
                    }            
            if(cpx!=null ){
                    fitXLine = cpx.getFitData();
                  
                    for(int j=0;j<imp.getHeight();j++)
                            for(int i=0;i<range.width;i++){
                                    int v = imp.getProcessor().get(range.x+i,j);
                                    ipr.putPixel(range.x+i,j,
                                    (int)(v*fitXLine[i]/projXLine[i]));
                            }
            }
            fiximage.show();
    }    
    /*********** fix only Y direction  ******************************/
    public void showFixedImageOnlyY(){
            ImagePlus fiximage = IJ.createImage("Untitled", "16-bit black", imp.getWidth(),imp.getHeight(), 1);
            ImageProcessor ipr = fiximage.getProcessor();
            for(int j=0;j<imp.getHeight();j++)
                    for(int i=0;i<imp.getWidth();i++){
                            int v = imp.getProcessor().get(i,j);
                            ipr.putPixel(i,j,v);
                    }            
            if(cpy!=null ){
                    fitYLine = cpy.getFitData();
                  
                    for(int j=0;j<range.height;j++)
                            for(int i=0;i<imp.getWidth();i++){
                                    int v = imp.getProcessor().get(i,range.y+j);
                                    ipr.putPixel(i,range.y+j,
                                    (int)(v*fitYLine[j]/projYLine[j]));
                            }
            }
            fiximage.show();
    } 
    /*******************************************/
    public void adjustProfile(){
        imp = IJ.getImage();
        Roi roi = imp.getRoi();
        if (roi!=null && !roi.isArea()) roi = null;
                ImageProcessor ipr = imp.getProcessor();
		ImageProcessor mask = roi!=null?roi.getMask():null;
		range = roi!=null?roi.getBounds():new Rectangle(0,0,ipr.getWidth(),ipr.getHeight());
                double[] sumXLine = new double[range.width];
                double[] sumYLine = new double[range.height];
                double[] cntXLine = new double[range.width];
                double[] cntYLine = new double[range.height];
                projXLine = new double[range.width];
		projYLine = new double[range.height];

		for (int y=0; y<range.height; y++) {
                        for (int x=0; x<range.width; x++) {
				if (mask==null||mask.getPixel(x,y)!=0) {
					cntXLine[x]++;
                                        cntYLine[y]++;
                                        double v = ipr.getPixelValue(x+range.x, y+range.y);
					sumXLine[x] += v;
                                        sumYLine[y] += v;
				}
			}
		}
                double [] pixelIdx = new double[range.width];
                for (int x=0; x<range.width; x++){
                        pixelIdx[x]=(double)x;
                        projXLine[x] = sumXLine[x]/cntXLine[x]; 
                }
                cpx = new CustomPlot("project x",projXLine);
                
                pixelIdx = new double[range.height];
                for (int y=0; y<range.height; y++){
                        pixelIdx[y]=(double)y;
                        projYLine[y] = sumYLine[y]/cntYLine[y]; 
                }
                cpy = new CustomPlot("project y",projYLine);
              


                
    }//my function

}




