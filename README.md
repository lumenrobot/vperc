# vperc
Lumen Virtual Perception: Visualisasi lingkungan berdasarkan data dari seluruh informasi sensor yang ada

## OpenCV

OpenCV for Windows x64 DLL is needed, dan sudah dimasukkan ke git juga biar gampang.

Copy `opencv\win_x64\opencv_java*.dll` DLL tersebut ke `C:\ProgramData\Oracle\Java\javapath`

Hendy's internal note: The `org.opencv:opencv` artifact is published in `soluvas-public-thirdparty`. You can re-publish to soluvas-thirdparty using:

```
mvn deploy:deploy-file -DrepositoryId=soluvas-public-thirdparty -Durl=http://nexus.bippo.co.id/nexus/content/repositories/soluvas-public-thirdparty/ -Dfile=opencv/opencv-2411.jar -Dpackaging=jar -DgroupId=org.opencv -DartifactId=opencv -Dversion=2.4.11
```
