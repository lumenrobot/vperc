package id.ac.itb.lumen.vperc;

import com.google.common.collect.ImmutableMap;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import javax.inject.Inject;
import java.util.Map;

@SpringBootApplication
@Profile("vperc")
public class VpercApplication implements CommandLineRunner {

    private static Logger log = LoggerFactory.getLogger(VpercApplication.class);

    static {
        log.info("Loading OpenCV: {}", Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(VpercApplication.class)
                .profiles("vperc")
                .run(args);
    }

//    @Inject
//    private LumenCamelConfiguration lumenCamelConfiguration;

    @Override
    public void run(String... args) throws Exception {
        log.info("java.library.path={}", System.getProperty("java.library.path"));
//        final Map<String, Double> personName = ImmutableMap.of("Kaoak", 124.3, "Zao", 1243.2);
//        log.info("Hello: {}", personName);
    }
}
