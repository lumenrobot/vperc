package id.ac.itb.lumen.vperc;

import com.github.ooxi.jdatauri.DataUri;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.rabbitmq.client.ConnectionFactory;
import id.ac.itb.lumen.core.HumanDetected;
import id.ac.itb.lumen.core.ImageObject;
import id.ac.itb.lumen.core.Vector3;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang3.RandomUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

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

    @Inject
    private Environment env;
    @Inject
    private ToJson toJson;
    @Inject
    private ProducerTemplate producerTemplate;

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
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.image")
                    .to("log:IN.avatar.NAO.data.image?showHeaders=true&showAll=true&multiline=true")
                    .process(exchange -> {
                        final ImageObject imageObject = toJson.getMapper().readValue(
                                (byte[]) exchange.getIn().getBody(), ImageObject.class);
                        log.info("Object yang kita dapatkan: {}", imageObject);
                        final DataUri dataUri = DataUri.parse(imageObject.getContentUrl(), StandardCharsets.UTF_8);
                        final Mat ocvImg = Highgui.imdecode(new MatOfByte(dataUri.getData()), Highgui.IMREAD_UNCHANGED);
                        log.info("OpenCV Mat: rows={} cols={}", ocvImg.rows(), ocvImg.cols());
                    });
            }
        };
    }

    @Bean
    public RouteBuilder periodicSent() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final String endpointUri = "rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=lumen.arkan.human.detection";
                from("timer:detikan?period=5000")
                    .process(exchange -> {
                        final HumanDetected humanDetected = new HumanDetected();
                        humanDetected.setHumanId(UUID.randomUUID().toString());
                        humanDetected.setPosition(new Vector3(RandomUtils.nextDouble(0.0, 20.0) - 10.0, 0.0, RandomUtils.nextDouble(0.0, 20.0) - 10.0));
                        final String json = toJson.getMapper().writeValueAsString(humanDetected);
                        log.info("Sending: {}", json);
                        //producerTemplate.sendBody(endpointUri, json);
                        exchange.getIn().setBody(json);
                    })
                    .to(endpointUri)
                    .to("log:lumen.arkan.human.recognition?multiline=true&showAll=true");
            }
        };
    }

}
