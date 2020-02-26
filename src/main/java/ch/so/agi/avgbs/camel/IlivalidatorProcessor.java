package ch.so.agi.avgbs.camel;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import ch.ehi.basics.settings.Settings;

//import org.apache.commons.io.FilenameUtils;

import org.interlis2.validator.Validator;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class IlivalidatorProcessor implements Processor {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String MODELNAME = "GB2AV";
    
    /**
     * 
     */
	@Override
	public void process(Exchange exchange) throws Exception {
        String fileName = (String) exchange.getIn().getHeaders().get("CamelFileName");        
        File dataFile = exchange.getIn().getBody(File.class);
       
        File destDir = new File(dataFile.getParentFile().getAbsolutePath());
        String logFileName = Paths.get(destDir.getAbsolutePath(), fileName + ".log").toFile().getAbsolutePath();

        Settings settings = new Settings();
        settings.setValue(Validator.SETTING_ILIDIRS, Validator.SETTING_DEFAULT_ILIDIRS);
        settings.setValue(Validator.SETTING_LOGFILE, logFileName);
        
        // "Freeze" to AVGBS INTERLIS model. Ilivalidator will return
        // false if we want to validate another model than "GB2AV".
        settings.setValue(Validator.SETTING_MODELNAMES, MODELNAME);
        
        String xmlFile = Paths.get(destDir.getAbsolutePath(), fileName).toFile().getAbsolutePath();
        boolean valid = Validator.runValidation(xmlFile, settings);
        
        if (!valid) {
            String logFileContent = new String(Files.readAllBytes(Paths.get(logFileName)));
            throw new IlivalidatorProcessorException(logFileContent);
        }

	}
		
	private Optional<String> getExtensionFromFileName(String fileName) {
			return Optional.ofNullable(fileName)
					.filter(f -> f.contains("."))
					.map(f -> f.substring(fileName.lastIndexOf(".") + 1));
	}
}
