# BCVE: Billiards Computer Vision Engine - Prototype

This is a hobby project or prototype to billiard table and balls recognition system that any one can afford. Normal 
mobile phone is set up over the table and used to send video signal to computer that runs computer vision algorithms
to detect table and balls on table in real time. 

![BCVE Engine](engine_snapshot.png)


## Process of detecting table and balls

Key elements of used detecting decisions are described here. Details are left to read from code. The project
is prototype, much of work is done by experimenting and not knowing the outcome. Still the code is tried to keep 
readable and open for new ideas and development. Main recognition process is implemented in *BilliardVisionEngine*
class.


### Detecting table

Play area consists of flat surface that lays between corners. Camera stand should be placed so that it contains as
much of table surface as possible. Normally this is near roof and near some corner of table. Optimal place would be 
straight over table's middle point, but this is practically impossible because necessary light source for the game 
blocks view.

Corners of the table are detect using OpenCV's *floodFill* method. This is basically same method that Windows Paint
has the bucket icon and it will paint all same color surface with given color. It is recursive algorithm that
examines neighbor pixels and if they are same color, or close enough in this case, they get painted white. Starting 
point is chosen on center of the image with little experiments near by in case there would be ball or cue there. After
fill is executed we have all white playing surface and detecting corners is straight forward. We assume that table 
is not often moved so we don't have to check corners for every frame. If detecting corners is not as accurate as 
wanted or will completely fail, then mouse can be used to force corner positions. 



### Detecting balls

Playing area is transformed to rectangle shape with OpenCV's *warpPerspective* method. For ball recognition we are
interested of edges in image so OpenCV's *canny* operation is now executed to find edges. After that we get all
contours with *findContours* method. We examine these contours and if they are suitable sized and near each other
then they are determined as a ball. If they constitute too big (or small) object by radius, such as a player or cue, 
they are skipped. Balls are detected on every frame and if one is moving linearly it is determining as moving ball.
If object is not behaving with linear speed it is ditched as it can't be a ball. We have method to detect is ball
moving or not, based on previous frames so that it is possible to recognize start and end point of one turn.

These steps to detect table and balls are illustrated in above picture (engine_snapshot.png). Every screen illustrate
one step of the process. In the final image balls are pointed with yellow circle and not moving balls with extra
red circle. Automatically detected playing surface is illustrated with green circles and lines.


### Challenges of recognition and possible applications

As we only use one camera there are limitations to accuracy of system. Also players can temporarily block view.
Perspective gets skewed and especially far from camera detection accuracy suffers. This process dosen't
do well with balls too near each other. It will determine them as one ball or skip them entirely as they are too
big to be one ball.

That be said, this prototype of very simple detection process still works pretty well. It dosen't output 100% correct
output all of the time, but in many applications this is not even necessary. For example, with this setup it could be
possible to implement automatic score counter for straight pool. This could be achieved by examining balls movements
and number on table and by recognizing pocketing of moving ball.

One interesting application is training billiards as computer engine is able to keep track of shots and for example
average accuracy. If you are interested to apply this project but need some technical help, please do not hesitate to
contact me.


## Installation and running BCVE engine

BCVE engine uses OpenCV graphics library. Because of highly optimized low level implementation it is written in C++.
It supports Windows, Mac and Linux, but must be natively installed. To run it with Java as we do here you can check
instructions from OpenCV's documentation or from here:
https://medium.com/javarevisited/setting-up-opencv-for-java-44c6eb6ae7e1 . After installation of OpenCV you only
need JRE to run BCVE and Groovy if you want to build and develop it.

BCVE-engine can also be used in offline mode where online video streaming is replaced with pre recorded video file.
Or it can be used in real time by connecting computer's web cam to OpenCV input stream. This can be done using separate
mobile web camera or by using mobile phone as computer's web cam. I have used DroidCamX app for this as it is free
(or little cost pro version) and easy to use.

Full setup of BCVE engine requires:
	
	- Billiard table and balls
	- Mobile phone or external web cam that can record video and phone stand located high
	  on some corner of table
	- Computer to run this Billiards Computer Vision Engine. Computer requirements:
		- JRE installed
		- OpenCV framework installed 
		- Groovy installed if you want to develop and build new versions of BCVE
	- With online mode DroidCamX app or other web camera system to stream video source to BCVE engine
	
Offline mode is set as default to test and run engine first. After you get offline mode work you can setup online
mode, which of course is more interesting in practice. 


## Installation and running BCVE engine in offline-mode

Next I list dependencies needed to run BCVE engine in offline mode with pre recorded video file (included with project).
With all dependencies it is very possible that newer and in some cases also older versions works fine too. But if you
want to be sure I also list exact version numbers that are tested to work.

	1. I have used Windows with development but this project should be operating system independent
	2. JRE is needed to run BCVE-engine (Java 8)
	3. OpenCV (https://opencv.org/), choose OpenCV according to your operating system (version 3.4.2)
	4. Optionally Groovy is needed if you want to develop and build new versions of engine (Groovy 2.5)

I have used Eclipse IDE with development. I share this project as Eclipse project and give instructions to run
this through Eclipse. However, you don't need Eclipse to run this project and running it with your own choice of
IDE or command line is as straightforward as running java progam is. I have also shared binaries (./bin/ directory)
in Git repository so you don't necessarily need Groovy as JRE is sufficient. 


`Run VideoInputApplication class (fi.samzone.sportsai.billiards.ui.videosource.VideoInputApplication.class/groovy)`


If you run BCVE engine with Eclipse IDE make sure you have OpenCV and Groovy listed in your Run Configuration's Dependencies
tab. In Eclipse you can run it with "Run as Java application" (do not use Run as Groovy console, because it dosen't work). If
you don't use Eclipse just run VideoInputApplication class with OpenCV linked proper way depending on your OS and  by following
OpenCV installation documents. You can also check out
[my instructions to add OpenCV support to Eclipse project](LinkOpenCvToJavaProject.md).


## Installation and running BCVE engine in online-mode

Change **online_mode=true** in ./default.properties file. Try to run VideoInputApplication and if you are using laptop with web cam
open you should see your self and BCVE engine's interpretation of image. This of course is a mesh and next thing you need to do
is install DroidCamX or other mobile app that can be used as input of web cam of your computer. OpenCV gives identifying number
for every input video source. If you run BCVE with laptop computer and mobile phone right input ID is number 1 as number zero is
taken by laptop's default web cam. If you have different setup you can change OpenCV video input number from ./default.properties file.


## Running BCVE server

To scale up BCVE to multiple instances there is separated server and listening clients model implemented. These can be found under
package *fi.samzone.sportsai.billiards.engine.net*. This work is still under construction and best way to explore it is through
source code.


## Regards

If you try this project and have some fun or useful benefits with it I would love to hear. Maybe you have some application for it
or maybe you have done something similar? Or even if you want to develop it (it sure could use more work...) or have problems to
run it. Don't hesitate to contact me.


Sampo Yrjänäinen (Samzone),
sampo@samzone.fi,
www.samzone.fi
