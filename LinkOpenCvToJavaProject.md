# How to build OpenCV

Look: https://opencv.org/releases/

# How to link installed OpenCV (jar and dll files) to Eclipse project in Windows

1. Open Configure Build Path
2. Add Library...
3. Select User Library
4. Select New...
5. Give name to User library, f.e. OpenCV
6. Select (External) Add JARs
7. Select opencv-xxx.jar under ...\opencv\build\java
8. Set Native library location by clicking Edit... and set directory as ...\opencv\build\java\x64
9. Select Finish