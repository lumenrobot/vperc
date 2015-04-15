package id.ac.itb.lumen.vperc;

import com.google.common.base.Preconditions;
import com.opencsv.CSVReader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.FileReader;

@SpringBootApplication
@Profile("vperc-image")
public class VpercImageApplication implements CommandLineRunner {

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

    @Override
    public void run(String... args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Please input image file name");
        final File imageFile = new File(args[0]);
        log.info("Processing image file '{}' ...", imageFile);
        final Mat imgMat = Highgui.imread(imageFile.getPath());
        log.info("Image mat: rows={} cols={}", imgMat.rows(), imgMat.cols());
    }
}
