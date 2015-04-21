package id.ac.itb.lumen.vperc;
import com.google.common.base.Preconditions;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;

import java.io.File;

@SpringBootApplication
@Profile("vperc-image")
public class VpercImageApplication implements CommandLineRunner {

    //variable
    double imageWidth = 320;
    double imageHeight = 240;
    double F = 4.3; //focal length pada kamera (F) 35mm
                    //sensor :  Dan sensor 36mm x 24mm (Full-frame)
    double sensorWidth = 6.64;//lebar sensor
    double sensorHeight = 4.98;//tinggi sensor
    double sx = imageWidth / sensorWidth; //sekala x
    double sy = imageHeight / sensorHeight;//sekala y


    private static Logger log = LoggerFactory.getLogger(VpercImageApplication.class);

    static {
        log.info("Loading OpenCV: {}", Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(VpercImageApplication.class)
                .profiles("vperc-image")
                .run(args);
    }

//    @Autowired
//    private SentimentAnalyzer sentimentAnalyzer;

    public static String toString(Mat mat) {
        String s = "";
        for (int y=0; y< mat.rows(); y++)
        {
            for (int x=0; x < mat.cols(); x++) {
                s += String.format(" %3.2f", mat.get(y, x)[0]);
            }
            s += "\n";
        }
        return s;
    }

    @Override
    public void run(String... args) throws Exception {
//        Preconditions.checkArgument(args.length >= 1, "Please input image file name");
//        final File imageFile = new File(args[0]);
//        log.info("Processing image file '{}' ...", imageFile);
//        final Mat imgMat = Highgui.imread(imageFile.getPath());
//        log.info("Image mat: rows={} cols={}", imgMat.rows(), imgMat.cols());


        //CM
        Mat CM=CameraMatrix();
        log.info("CM =\n{}", toString(CM));
        //RxT
        Mat RxT=RotationXTranpus(0);
        log.info("RxT =\n{}", toString(RxT));
        //CmRxt
        Mat CmRxT=GetCmRT(CM, RxT);
        log.info("CmRxT =\n{}", toString(CmRxT));

        //input XYZ objec 3D
        Mat XYZ = Mat.zeros( 4, 1, CvType.CV_32F );
        XYZ.put(0, 0, 0);//X
        XYZ.put(1, 0, 0);//Y
        XYZ.put(2, 0, 1);//Z
        XYZ.put(3, 0, 1);
        log.info("XYZ =\n", toString(XYZ));

        //get SUV
        Mat SUV=GetSUV(CmRxT,XYZ);
        log.info("SUV =\n{}", toString(SUV));

        //Get XYZ dari sUV yg telah diketahui (tapi in reality, s belum diketahui)
        Mat inverse = new Mat(4, 3, CvType.CV_32F);
        Core.invert(CmRxT, inverse, Core.DECOMP_SVD);
        Mat XYZ2 = new Mat(4, 1, CvType.CV_32F);
        Core.gemm(inverse, SUV, 1, new Mat(), 0, XYZ2, 0);
        log.info("XYZ2 =\n{}", toString(XYZ2));

        //atau
//        Mat XYZ3 = new Mat(4, 1, CvType.CV_32F);
//        Core.solve(CmRxT,SUV, XYZ3, Core.DECOMP_SVD);
//        for(int i=0; i<4;i++) {
//            log.info("XYZ3[{},{}] {}",i,0, XYZ3.get(i, 0));
//        }
    }


    private Mat CameraMatrix()
    {
        Mat cameraMatrix = Mat.zeros( 3, 3, CvType.CV_32F );
        double fx = F * sx;
        double fy = F * sy;
        cameraMatrix.put(0,0,fx);
        cameraMatrix.put(1, 1, fy*-1);
        cameraMatrix.put(0, 2, imageWidth/2);//cx
        cameraMatrix.put(1, 2, imageHeight/2);//cy
        cameraMatrix.put(2, 2, 1);
        return cameraMatrix;
    }

    private Mat RotationXTranpus(double angle)
    {
        Mat RxT = Mat.zeros( 3, 4, CvType.CV_32F );
        //Rx
        RxT.put(0, 0, Math.cos(Math.toRadians(angle)));//fx
        RxT.put(0, 1, Math.sin(Math.toRadians(angle)));
        RxT.put(1, 0, Math.sin(Math.toRadians(angle)));//fy
        RxT.put(1, 1, Math.cos(Math.toRadians(angle)));
        RxT.put(2, 2, 1);
        //T
        RxT.put(0, 3,0);//TX
        RxT.put(1, 3,0);//TY
        RxT.put(2, 3,0);//TZ
        return RxT;
    }

    //CameraMatrix * RotationXTranpus
    private Mat GetCmRT(Mat cm,Mat rxt)
    {
        Mat CmRxT = new Mat(3, 4, CvType.CV_32F);
        Core.gemm(cm, rxt, 1, Mat.zeros(1, 1, CvType.CV_32F), 0, CmRxT, 0);
        return CmRxT;
    }

    private  Mat GetSUV(Mat cmrt,Mat xyz)
    {
        Mat SUV = new Mat(3, 1, CvType.CV_32F);
        Core.gemm(cmrt, xyz, 1, Mat.zeros(1, 1, CvType.CV_32F), 0, SUV, 0);
        return SUV;
    }
}
