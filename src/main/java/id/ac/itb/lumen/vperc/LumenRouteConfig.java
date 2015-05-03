package id.ac.itb.lumen.vperc;

import com.github.ooxi.jdatauri.DataUri;
import com.rabbitmq.client.ConnectionFactory;
import id.ac.itb.lumen.core.*;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.objdetect.HOGDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by ceefour on 1/19/15.
 */
@Configuration
@Profile("vperc")
class LumenRouteConfig {

    private static final Logger log = LoggerFactory.getLogger(LumenRouteConfig.class);

//    @Inject
//    protected AgentRepository agentRepo
//    @Inject
//    protected ToJson toJson

    //variable
    double imageWidth = 538;
    double imageHeight = 303;
    double F = 4.3; //focal length pada kamera (F) 35mm
    //sensor :  Dan sensor 36mm x 24mm (Full-frame)
    double sensorWidth = 6.64;//lebar sensor
    double sensorHeight = 4.98;//tinggi sensor
    double sx = imageWidth / sensorWidth; //sekala x
    double sy = imageHeight / sensorHeight;//sekala y
    double Yobject=0;
    double Ycamera=1;

    @Inject
    private Environment env;
    @Inject
    private ToJson toJson;
    @Inject
    private ProducerTemplate producerTemplate;

    private Map<String, Human> humans = new LinkedHashMap<>();

    @Bean
    public ConnectionFactory amqpConnFactory() {
        final ConnectionFactory connFactory = new ConnectionFactory();
        connFactory.setHost(env.getProperty("amqp.host", "localhost"));
        connFactory.setUsername(env.getProperty("amqp.username", "guest"));
        connFactory.setPassword(env.getProperty("amqp.password", "guest"));
        log.info("AMQP configuration: host={} username={}", connFactory.getHost(), connFactory.getUsername());
        return connFactory;
    }

    @Bean
    public RouteBuilder cameraProcessorRouteBuilder() {
        log.info("Initializing camera processor RouteBuilder");
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final String humanDetectionUri = "rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=lumen.arkan.human.detection";
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.image")
                    .to("log:IN.avatar.NAO.data.image?showHeaders=true&showAll=true&multiline=true")
                    .process(exchange -> {

                        final ImageObject imageObject = toJson.getMapper().readValue(
                                (byte[]) exchange.getIn().getBody(), ImageObject.class);
                        log.info("Object yang kita dapatkan: {}", imageObject);
                        final DataUri dataUri = DataUri.parse(imageObject.getContentUrl(), StandardCharsets.UTF_8);
                        final Mat ocvImg = Highgui.imdecode(new MatOfByte(dataUri.getData()), Highgui.IMREAD_UNCHANGED);
                        log.info("OpenCV Mat: rows={} cols={}", ocvImg.rows(), ocvImg.cols());

                        final HumanChanges humanChanges = PeopledetectMultiScale(ocvImg);
                        final String humanDetectedsJson = toJson.getMapper().writeValueAsString(humanChanges);
                        exchange.getOut().setBody(humanDetectedsJson);
                    })
                        .to(humanDetectionUri)
                        .to("log:OUT.lumen.arkan.human.detection");
            }
        };
    }


    private HumanChanges PeopledetectMultiScale(Mat imgMat)
    {
        //CM
        Mat CM=CameraMatrix();
        //RxT
        Mat RxT=RotationXTranpus(0);
        //CmRxt
        Mat CmRxT=GetCmRT(CM, RxT);

        final HOGDescriptor hog = new HOGDescriptor();
        final MatOfFloat descriptors = HOGDescriptor.getDefaultPeopleDetector();
        hog.setSVMDetector(descriptors);

        final MatOfRect foundLocations = new MatOfRect();
        final MatOfDouble foundWeights = new MatOfDouble();

        final Size winStride = new Size(8, 8);
        final Size padding = new Size(32, 32);

        hog.detectMultiScale(imgMat, foundLocations, foundWeights, 0.0,
                winStride, padding, 1.05, 2.0, false);

        ImageHumanDetection(imgMat,foundLocations,foundWeights);

        final HumanChanges humanChanges = new HumanChanges();
        if (foundLocations.rows() > 0) {
            List<Double> weightList = foundWeights.toList();
            List<Rect> rectList = foundLocations.toList();
            int i = 0;
            for (Rect rect : rectList) {
                float x=(float)rect.x + (rect.width/2);
                float y=(float)rect.y + (rect.height*7/8);

                Mat SUV=SetSUV(x, y);
                //Get XYZ dari sUV yg telah diketahui (tapi in reality, s belum diketahui)
                Mat inverse = new Mat(4, 3, CvType.CV_32F);
                Core.invert(CmRxT, inverse, Core.DECOMP_SVD);
                Mat XYZ2 = new Mat(4, 1, CvType.CV_32F);
                Core.gemm(inverse, SUV, 1, new Mat(), 0, XYZ2, 0);

                //Nilai S
                double S=(Yobject-Ycamera)/XYZ2.get(1, 0)[0];

                //Kalikan Dengan S
                final Vector3 humanPos = new Vector3(
                        XYZ2.get(0, 0)[0]*S,//XYZ2.get(0, 0)[0])
                        XYZ2.get(1, 0)[0]*S,
                        XYZ2.get(2, 0)[0]*S);

                // check if there's already human nearby
                Double nearestDist = null;
                Human nearestHuman = null;
                for (Human it : humans.values()) {
                    final double distance = Math.sqrt(
                            Math.pow((double) (humanPos.getX() - it.getPosition().getX()), 2.0) +
                                    Math.pow((double) (humanPos.getY() - it.getPosition().getY()), 2.0) +
                                    Math.pow((double) (humanPos.getZ() - it.getPosition().getZ()), 2.0));
                    if (distance <= 0.3 && (nearestDist == null || distance < nearestDist)) {
                        nearestDist = distance;
                        nearestHuman = it;
                    }
                }

                if (nearestHuman != null) {
                    // human udah ada, let's move it to new position
                    log.info("Found human {} distance {} from old {} to new {}",
                            nearestHuman.getHumanId(), nearestDist, nearestHuman.getPosition(), humanPos);
                    nearestHuman.setPosition(humanPos);
                    final HumanMoving humanMoving = new HumanMoving();
                    humanMoving.setHumanId(nearestHuman.getHumanId());
                    humanMoving.setPosition(humanPos);
                    humanChanges.getHumanMovings().add(humanMoving);
                } else {
                    final HumanDetected humanDetected = new HumanDetected();
                    humanDetected.setHumanId(UUID.randomUUID().toString());
                    humanDetected.setPosition(humanPos);
                    humanChanges.getHumanDetecteds().add(humanDetected);

                    // add to database
                    final Human human = new Human();
                    human.setHumanId(humanDetected.getHumanId());
                    human.setPosition(humanDetected.getPosition());
                    humans.put(human.getHumanId(), human);
                }
            }
        }

        log.info("HumanChanges contains {} detected and {} moving", humanChanges.getHumanDetecteds().size(),
                humanChanges.getHumanMovings().size());
        return humanChanges;
    }

//    @Bean
//    public RouteBuilder periodicSent() {
//        return new RouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                final String endpointUri = "rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=lumen.arkan.human.detection";
//                from("timer:detikan?period=5000")
//                    .process(exchange -> {
//                        final HumanDetected humanDetected = new HumanDetected();
//                        humanDetected.setHumanId(UUID.randomUUID().toString());
//                        humanDetected.setPosition(new Vector3(RandomUtils.nextDouble(0.0, 20.0) - 10.0, 0.0, RandomUtils.nextDouble(0.0, 20.0) - 10.0));
//                        final String json = toJson.getMapper().writeValueAsString(humanDetected);
//                        log.info("Sending: {}", json);
//                        //producerTemplate.sendBody(endpointUri, json);
//                        exchange.getIn().setBody(json);
//                    })
//                    .to(endpointUri)
//                    .to("log:lumen.arkan.human.recognition?multiline=true&showAll=true");
//            }
//        };
//    }

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

    private  Mat SetSUV(float x,float y)
    {
        Mat SUV = new Mat(3, 1, CvType.CV_32F);
        SUV.put(0, 0,x);
        SUV.put(1, 0,y);
        SUV.put(2, 0, 1);
        return SUV;
    }

    private void  ImageHumanDetection(Mat imgMat,MatOfRect foundLocations,MatOfDouble foundWeights)
    {
        final Point rectPoint1 = new Point();
        final Point rectPoint2 = new Point();
        final Scalar rectColor = new Scalar(0, 255, 0);

        if (foundLocations.rows() > 0) {
            List<Double> weightList = foundWeights.toList();
            List<Rect> rectList = foundLocations.toList();
            int i = 0;
            for (Rect rect : rectList) {
                rectPoint1.x = rect.x+(rect.width/2);
                rectPoint1.y = rect.y+ (rect.height*1/8);
                rectPoint2.x = rect.x + (rect.width/2);
                rectPoint2.y = rect.y + (rect.height*7/8);

                Core.rectangle(imgMat, rectPoint1, rectPoint2, rectColor, 2);
                Highgui.imwrite("d:\\Capture3.PNG", imgMat);
            }
        }

    }

    //model 3D
    public  class Coordinat3D
    {
        private float X;
        private float Y;
        private float Z;

        public void setX(float x) {
            this.X = x;
        }

        public float getX() {
            return X;
        }

        public void setY(float y) {
            this.Y = y;
        }

        public float getY() {
            return Y;
        }

        public void setZ(float z) {
            this.Z = z;
        }

        public float getZ() {
            return Z;
        }
    }

}
