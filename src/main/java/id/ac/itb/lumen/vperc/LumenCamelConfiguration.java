package id.ac.itb.lumen.vperc;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.inject.Inject;

/**
 * Created by ceefour on 1/19/15.
 */
@Configuration
@Profile("vperc")
class LumenCamelConfiguration extends CamelConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public ProducerTemplate producerTemplate() throws Exception {
        return camelContext().createProducerTemplate();
    }

}
