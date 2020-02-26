package ch.so.agi.avgbs.camel;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AvgbsRoute extends RouteBuilder {
    @Autowired
    AuthorisationProcessor authorisationProcessor;
	
	@Autowired
    IlivalidatorProcessor ilivalidatorProcessor;
    
	@Override
	public void configure() throws Exception {
		// Wenn die Exception hier abgefangen wird, failed die Route nicht,
		// wenn "handled(true)" verwendet wird.
		// Man könnte hier aber sämtliche Exceptions sammeln, 
		// gruppieren und dann sinnvoll weiterleiten, also
		// wieder eine Exception werfen.
        //onException(Exception.class)
        //.handled(true)
        //.log(LoggingLevel.ERROR, simple("${exception.stacktrace}").getText())

		// Reihenfolge sollte wohl Validierung -> Authorisierung sein.
		// Weil man für die Prüfung der Authorisierung das File parsen muss,
		// will man sicher sein, dass es sauberes INTERLIS ist.
		
        from("direct:avgbs-data-transfer")
        .log(LoggingLevel.INFO, "Ilivalidator started.")        
        //.process(ilivalidatorProcessor)
        .log(LoggingLevel.INFO, "Ilivalidator successfully passed.")
        .log(LoggingLevel.INFO, "Authorisation started.")
        .process(authorisationProcessor)
        .end();
        
        // TODO: Archivierung etc. etc.
        
//        .to("file:///Users/stefan/tmp/"); // FIXME
	}

}
