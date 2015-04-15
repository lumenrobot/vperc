package id.ac.itb.lumen.vperc;

import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Created by ceefour on 1/19/15.
 */
@Configuration
@Profile("vperc")
class LumenCamelConfiguration extends CamelConfiguration {

}
