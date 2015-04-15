package id.ac.itb.lumen.vperc;

import com.google.common.base.Preconditions;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.FileReader;

@SpringBootApplication
@Profile("vperc-csv")
public class VpercCsvApplication implements CommandLineRunner {

    private static Logger log = LoggerFactory.getLogger(VpercCsvApplication.class);
    
    public static void main(String[] args) {
        new SpringApplicationBuilder(VpercCsvApplication.class)
                .profiles("vperc-csv")
                .run(args);
    }

//    @Autowired
//    private SentimentAnalyzer sentimentAnalyzer;

    @Override
    public void run(String... args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Please input CSV file name");
        final File csvFile = new File(args[0]);
        log.info("Processing file '{}' ...", csvFile);
        try (final CSVReader csv = new CSVReader(new FileReader(csvFile), ',')) {
            final String[] headers = csv.readNext(); // headers
            log.info("CSV Headers: {}", (Object) headers);
            while (true) {
                final String[] row = csv.readNext();
                if (row == null) {
                    break;
                }
                log.info("Row: {}", (Object) row);
            }
        }
    }
}
