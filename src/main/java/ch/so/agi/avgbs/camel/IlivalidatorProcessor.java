package ch.so.agi.avgbs.camel;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.settings.Settings;
import ch.interlis.iom_j.itf.ItfReader;
import ch.interlis.iom_j.xtf.XtfReader;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox_j.EndTransferEvent;
import ch.interlis.iox_j.StartBasketEvent;

//import org.apache.commons.io.FilenameUtils;

import org.interlis2.validator.Validator;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class IlivalidatorProcessor implements Processor {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final String MODELNAME = "GB2AV";
    
    /**
     * 
     */
	@Override
	public void process(Exchange exchange) throws Exception {
        String fileName = (String) exchange.getIn().getHeaders().get("CamelFileName");
                
        System.out.println(exchange.getIn().getHeaders().toString());
        
        
        File dataFile = exchange.getIn().getBody(File.class);
       
        File destDir = new File(dataFile.getParentFile().getAbsolutePath());
        String logFileName = Paths.get(destDir.getAbsolutePath(), fileName + ".log").toFile().getAbsolutePath();

        Settings settings = new Settings();
        settings.setValue(Validator.SETTING_ILIDIRS, Validator.SETTING_DEFAULT_ILIDIRS);
        settings.setValue(Validator.SETTING_LOGFILE, logFileName);
        
        // "Freeze" to AVGBS INTERLIS model. Ilivalidator will return
        // failed validation if we want to validate another model.
        settings.setValue(Validator.SETTING_MODELNAMES, MODELNAME);
        
        String xmlFile = Paths.get(destDir.getAbsolutePath(), fileName).toFile().getAbsolutePath();
        boolean valid = Validator.runValidation(xmlFile, settings);
        
        System.out.println("valid: " + valid);

	}
	
    /**
     * Figure out INTERLIS model name from INTERLIS transfer file. Works with ili1
     * and ili2.
     */
    private String getModelNameFromTransferFile(String transferFileName) throws IoxException, IllegalArgumentException {
        String model = null;
        String ext = this.getExtensionFromFileName(transferFileName).orElseThrow(IllegalArgumentException::new);
        IoxReader ioxReader = null;

        try {
            File transferFile = new File(transferFileName);

            if (ext.equalsIgnoreCase("itf")) {
                ioxReader = new ItfReader(transferFile);
            } else {
                ioxReader = new XtfReader(transferFile);
            }

            IoxEvent event;
            StartBasketEvent be = null;
            do {
                event = ioxReader.read();
                if (event instanceof StartBasketEvent) {
                    be = (StartBasketEvent) event;
                    break;
                }
            } while (!(event instanceof EndTransferEvent));

            ioxReader.close();
            ioxReader = null;

            if (be == null) {
                throw new IllegalArgumentException("no baskets in transfer-file");
            }

            String namev[] = be.getType().split("\\.");
            model = namev[0];

        } catch (IoxException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new IoxException("could not parse file: " + new File(transferFileName).getName());
        } finally {
            if (ioxReader != null) {
                try {
                    ioxReader.close();
                } catch (IoxException e) {
                    log.error(e.getMessage());
                    e.printStackTrace();
                    throw new IoxException(
                            "could not close interlise transfer file: " + new File(transferFileName).getName());
                }
                ioxReader = null;
            }
        }
        return model;
    }
	
	private Optional<String> getExtensionFromFileName(String fileName) {
			return Optional.ofNullable(fileName)
					.filter(f -> f.contains("."))
					.map(f -> f.substring(fileName.lastIndexOf(".") + 1));
	}
}
