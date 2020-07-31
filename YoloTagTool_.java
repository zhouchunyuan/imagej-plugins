import ij.*;
import ij.gui.*;
import java.awt.*;
import java.awt.event.*;
import ij.plugin.frame.*;
import ij.process.*;
import ij.plugin.*;
import ij.io.*;
import java.nio.file.*;
import java.io.*;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;


public class YoloTagTool_ extends PlugInFrame implements ActionListener, KeyListener {

	   public final Color[] colors ={Color.red,Color.yellow,Color.blue,Color.green,Color.pink,Color.gray,Color.orange};
       public final int WINDOW_WIDTH = 300;
       public final int WINDOW_HEIGHT = 275;
	   public final int XMARGIN = 20;
       public final int OP_CMD = 0;
       public final int OP_NEW = 1;
       public final int OP_LBL = 2;
	   public final int OP_SAV = 3;
	   public final int OP_LOA = 4;
	   
	   String path = "";
	   ImagePlus imp;
	   
	   ArrayList labelButtons = new ArrayList<Button>();
       public TextField text;
       int buttonWidth, buttonHeight;

       static PlugInFrame instance;

    public YoloTagTool_() {
        super("YoloTagTool Plugin");
        if (instance!=null) {
            instance.toFront();
            return;
        }
        WindowManager.addWindow(this);
        instance = this;
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        GUI.center(this);
        setVisible(true);
    }

    /** Overrides Component.addNotify(). Init() must be called after 
        addNotify() or getInsets() will not return the title bar height. */
    public void addNotify() {
        super.addNotify();
        init();
    }

    public void init() {
          int  titleBarHeight = getInsets().top;
          int rows = 5;
          int columns = 5;
          buttonWidth = 50;
          buttonHeight = 34;
          int xm = XMARGIN; // x margin
          int ym =  titleBarHeight+38; // y margin
          int xinc = (WINDOW_WIDTH-xm*2)/columns+1;
          int yinc = (WINDOW_HEIGHT-ym)/rows-2;

          Button btn1 = makeButton("Open", OP_CMD, xm, ym);
          Button btn2 = makeButton("New Label", OP_NEW, btn1.getX()+btn1.getWidth(), ym);  
          Button btn3 = makeButton("Save Tags", OP_SAV, btn2.getX()+btn2.getWidth(), ym); 
		  Button btn4 = makeButton("Load Tags", OP_LOA, btn3.getX()+btn3.getWidth(), ym); 
 

          setLayout(null);
          setForeground(java.awt.Color.darkGray);
          setResizable(true);
          setFont(new Font("Helvetica", Font.PLAIN, 14));

          text = new TextField("", 80);
          text.setEditable(true);
          text.addKeyListener(this);
          text.setBounds(xm, ym-30, WINDOW_WIDTH-xm*2, 25);
          add(text);

             // Handles language dependent decimal separator.
       }
	int getStringWidth(String text,Font font){
		AffineTransform affinetransform = new AffineTransform();     
		FontRenderContext frc = new FontRenderContext(affinetransform,true,true);     
		int textwidth = (int)(font.getStringBounds(text, frc).getWidth());
		int textheight = (int)(font.getStringBounds(text, frc).getHeight());
		return textwidth;
	}
	
    Button makeButton(String label, int op, int x, int y) {
        Button button = new Button(label);
        button.addActionListener(this);
        button.addKeyListener(this);
        button.setName(""+op);
        if (label.length()>2)
                button.setFont(new Font("Helvetica", Font.PLAIN, 14));
        else
                button.setFont(new Font("Monospaced", Font.PLAIN, 16));
		int width = getStringWidth(label,button.getFont());
        button.setBounds(x, y, width+10, buttonHeight);
        add(button);
        return button;
    }     

    public void actionPerformed(ActionEvent evt) {
        Button b = (Button)evt.getSource();
        int op = Integer.parseInt(b.getName());
        if (op==OP_LBL) {
            doOp(OP_LBL,b.getLabel());
        } else
            doOp(op,"");
	}
    public void doOp(int op, String lblText) {
	   /*************************************/
	   /**   open directory                **/
	   /*************************************/
		if(op==OP_CMD){
			path = (new DirectoryChooser("choose a folder")).getDirectory();
			imp = FolderOpener.open(path+"/images/", "");
			imp.show();

			RoiManager rm = RoiManager.getInstance();
			if (rm==null) rm = new RoiManager();
			rm.runCommand(imp,"Show All with labels");
			rm.runCommand("Associate", "true");
			rm.runCommand("Centered", "false");
			rm.runCommand("UseNames", "true");
			
		}
		
	   /*************************************/
	   /**   label target                  **/
	   /*************************************/
		if(op==OP_LBL){
			RoiManager rm = RoiManager.getInstance();
			if (rm==null) rm = new RoiManager();
			
			int[] selectedidx = rm.getSelectedIndexes();
			if(selectedidx.length>0){
				for(int i =0; i<selectedidx.length;i++)
					rm.rename(selectedidx[i], lblText);
			}else{
				rm.addRoi(imp.getRoi());
				rm.rename(rm.getCount()-1,lblText);
			}
			rm.runCommand(imp,"Show All with labels");
			rm.deselect();
		}
	   /*************************************/
	   /**   create New label button       **/
	   /*************************************/		
		if (op==OP_NEW) {

			String label = text.getText();
			addLabelButton(label);
       }
	   /*************************************/
	   /**   save tags                     **/
	   /*************************************/	   
	   if (op==OP_SAV) {
		    String parentDirName = Paths.get(path).getFileName().toString();
			RoiManager rm = RoiManager.getInstance();
			if (rm==null) rm = new RoiManager();
			Roi[] rois = rm.getRoisAsArray();
			double width = (double)imp.getWidth();
			double height = (double)imp.getHeight();
			if(labelButtons.size()==0)IJ.error("No label buttons");
			
			String[] labels = imp.getImageStack().getSliceLabels();
			
			ArrayList<String> trainfilelist = new ArrayList<String>();
			for(int i=0;i<rois.length;i++){
				double xc = (rois[i].getBounds().x + rois[i].getBounds().width/2)/width;
				double yc = (rois[i].getBounds().y + rois[i].getBounds().height/2)/height;
				double w = (rois[i].getBounds().width)/width;
				double h = (rois[i].getBounds().height)/height;
				int frameIdx = rois[i].getPosition();
				String roiname = rois[i].getName();
				String filename="";
				if(frameIdx>0)
					filename = labels[frameIdx-1];
				else
					filename = labels[0];

				if(!trainfilelist.contains(filename))trainfilelist.add(filename);
				int idx = -1;
				
				for (int j = 0; j < labelButtons.size(); j++) { 		      
					Button btn = (Button)labelButtons.get(j);
					
					if(roiname.equals(btn.getLabel())){
						idx = j;
						break;
					}
				}
				String context = String.format("%d", idx)+" "+
					   String.format("%.6f", xc)+" "+
				       String.format("%.6f", yc)+" "+
					   String.format("%.6f", w)+" "+
					   String.format("%.6f", h)+System.lineSeparator();
					   
				IJ.log(roiname+" , "+
					   String.format("frame:%d", frameIdx)+" , "+filename+" , "+
					   String.format("%d", idx)+" , "+
					   String.format("%.6f", xc)+" , "+
				       String.format("%.6f", yc)+" , "+
					   String.format("%.6f", w)+" , "+
					   String.format("%.6f", h));
				String fileNameWithOutExt = filename.substring(0, filename.lastIndexOf('.'));
				savetxt(path+"/labels/"+fileNameWithOutExt+".txt",context);
			}
			
			try {
				Files.deleteIfExists(Paths.get(path+"/list.txt"));
			}catch (IOException e) {
				IJ.log(e.toString());
			}
			String liststr = "";
			for(String str: trainfilelist){
				liststr += parentDirName+"/images/"+str + System.lineSeparator();
			}
			savetxt(path+"/list.txt",liststr);
			
			/** save names.names **/
			String names = "";
			for(int i=0;i<labelButtons.size();i++){
				Button btn = (Button)labelButtons.get(i);
				names += btn.getLabel()+System.lineSeparator();
			}
			
			try {
				Files.deleteIfExists(Paths.get(path+"/names.names"));
			}catch (IOException e) {
				IJ.log(e.toString());
			}
			savetxt(path+"/names.names",names);
			
			/** save data.data **/
			 
			String datastr = "classes="+String.format("%d", labelButtons.size()) + System.lineSeparator()+
							  "train=" + Paths.get(parentDirName +"/list.txt") + System.lineSeparator()+
							  "valid=" + Paths.get(parentDirName +"/list.txt") + System.lineSeparator()+
							  "names=" + Paths.get(parentDirName +"/names.names");
			
			try {
				Files.deleteIfExists(Paths.get(path+"/data.data"));
			}catch (IOException e) {
				IJ.log(e.toString());
			}
			savetxt(path+"/data.data",datastr);
	   }
	   
	   /*************************************/
	   /**   load tags                     **/
	   /*************************************/
	   if (op==OP_LOA) {
		   	RoiManager rm = RoiManager.getInstance();
			if (rm==null) rm = new RoiManager();
			rm.reset();
			//remove buttons from frame
			for(int i=0;i<labelButtons.size();i++){
				remove((Button)labelButtons.get(i));
			}
			labelButtons.clear();
			
           final java.io.File folder = new java.io.File(path+"/labels/");
		   String[] list = folder.list();
		   
		   String[] slicelabels = imp.getImageStack().getSliceLabels();
    	   
		   /********** restore label buttons ****************/
		   String[] names = loadtxt(path+"/names.names").split(System.lineSeparator());
		   for(String name : names)addLabelButton(name);       
		   
		   for (String filename : list) {

		       String context = loadtxt(path+"/labels/"+filename);
			   String[] lines = context.split(System.lineSeparator());
			   
			   int roiposition = -1;
			   String fileNameWithOutExt = filename.substring(0, filename.lastIndexOf('.'));
			   for(int i=0;i<slicelabels.length;i++){
				   String label = slicelabels[i].substring(0, slicelabels[i].lastIndexOf('.'));
				   if(fileNameWithOutExt.equals(label)){
					   roiposition = i+1;
					   break;
				   }
			   }
			   for(String line : lines){
				   String[] items = line.split(" ");
				   int classid = Integer.parseInt(items[0]);
				   double xc = Double.parseDouble(items[1]);
				   double yc = Double.parseDouble(items[2]);
				   double w = Double.parseDouble(items[3]);
				   double h = Double.parseDouble(items[4]);
				   int imgW = imp.getWidth();
				   int imgH = imp.getHeight();
				   double x = (xc-w/2)*imgW;
				   double y = (yc-h/2)*imgH;
				   Roi roi = new Roi(x,y, w*imgW, h*imgH);
				   roi.setPosition(roiposition);
				   roi.	setStrokeColor(colors[classid%colors.length]);

				   rm.addRoi(roi);
				   rm.rename(rm.getCount()-1,names[classid]);
				   
				   IJ.log(filename+line);
			   }
		    }
			
	    }
	}
	private void addLabelButton(String label){
		int x = XMARGIN;
		int y = 110;
		int w = 0;
		int h = 0;
		int count = labelButtons.size();
		boolean existlabel = false;

		for (int i = 0; i < count; i++) { 		      
			Button btn = (Button)labelButtons.get(i);
			if(label.equals(btn.getLabel())){
				existlabel = true;
				break;
			}
		} 
		
		if(!("".equals(label) || existlabel)){
			Rectangle bounds = new Rectangle(x,y,w,h);
			
			if(count>0){
				Button lastBtn = (Button)labelButtons.get(count-1);
				bounds = lastBtn.getBounds();
				x = (int)bounds.getX()+(int)bounds.getWidth();
				y = (int)bounds.getY();
			}
			Button btn = makeButton(label, OP_LBL, x, y); 
			labelButtons.add(btn);
		}
		
		int totalWidth = 0;
		for (int i = 0; i < labelButtons.size(); i++) { 		      
			Button btn = (Button)labelButtons.get(i);
			totalWidth += btn.getBounds().getWidth();
		} 
		Dimension size = getSize();
		if(totalWidth >size.width ){
			setSize(totalWidth+XMARGIN*2,size.height);
		}		
	}
	private void savetxt(String filename,String context){
		try {
			if(Files.exists(Paths.get(filename)))
				Files.write(Paths.get(filename), context.getBytes(), StandardOpenOption.APPEND);
			else
				Files.write(Paths.get(filename), context.getBytes(), StandardOpenOption.CREATE);
		}catch (IOException e) {
			IJ.log(e.toString());
		}
	}
	
	private static String loadtxt(String filename){
		String data = "";
		try {
			data = new String(Files.readAllBytes(Paths.get(filename))); 
		}catch (IOException e) {
			IJ.log(e.toString());
		}
		return data; 
			
	} 
	
    
    /** Overrides windowClosing in PluginFrame. */
    public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        instance = null;
    }

    public void keyTyped(KeyEvent ev) {
    }

    public void keyPressed(KeyEvent ev) {}
    public void keyReleased(KeyEvent ev) {} 
    
}
