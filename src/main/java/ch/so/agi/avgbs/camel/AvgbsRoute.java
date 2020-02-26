package ch.so.agi.avgbs.camel;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AvgbsRoute extends RouteBuilder {
    @Autowired
    IlivalidatorProcessor ilivalidatorProcessor;

	@Override
	public void configure() throws Exception {
        from("direct:avgbs-data-transfer")
        .log(LoggingLevel.INFO, "Ilivalidator started.")        
        .process(ilivalidatorProcessor)
        .log(LoggingLevel.INFO, "Ilivalidator successfully passed.")
        .to("file:///Users/stefan/tmp/");

	}

}
