var x,y,speed;
width = 40;
height = 40;

dir = getDirectory("Choose a Directory ");
csvfiles= getFileList(dir);
for(n=0;n<lengthOf(csvfiles);n++){

	if( !endsWith(csvfiles[n],".csv") )continue;

	str = File.openAsString(dir+csvfiles[n]);
	lines = split(str,"\n");
	
	speed = newArray(lengthOf(lines)-1);

	setColor("cyan");

	for(i=1;i<lengthOf(lines);i++){
		xytxt = split(lines[i],",");
		x = parseInt(xytxt[0]);
		y = parseInt(xytxt[1]); 
		
		if(i==lengthOf(lines)-1){
		}else{
			xytxt = split(lines[i+1],",");
			x1 = parseInt(xytxt[0]);
			y1 = parseInt(xytxt[1]); 
			speed=sqrt((x1-x)*(x1-x)+(y1-y)*(y1-y));
			setResult(csvfiles[n], i-1, speed);
		}

		setSlice(i);
		Overlay.drawRect(x-width/2, y-height/2, width, height);
		Overlay.setPosition(i);
	}
}
Overlay.show;

