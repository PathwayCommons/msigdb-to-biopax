package com.google.gsoc14.msigdb2biopax;

import com.google.gsoc14.msigdb2biopax.converter.MSigDB2BioPAXConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;

import java.io.FileOutputStream;

public class MSigDB2BioPAXConverterMain {
    private static Log log = LogFactory.getLog(MSigDB2BioPAXConverterMain.class);

    public static void main(String[] args) throws Exception {
        if(args.length < 2) {
            System.err.println(
                    "Missing options.\n"
                    + "Usage: MSigDB2BioPAXConverterMain /path/to/msigdb_v4.0.xml /path/to/output.owl"
            );
            System.exit(-1);
        }

        String msigdbFile = args[0].trim();
        log.info("MSigDB File: " + msigdbFile);
        MSigDB2BioPAXConverter converter = new MSigDB2BioPAXConverter();
        Model model = converter.convert(msigdbFile);

        SimpleIOHandler simpleIOHandler = new SimpleIOHandler();
        String outputFile = args[1].trim();
        log.info("Conversion done. Now exporting the BioPAX model as a file: " + outputFile);
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        simpleIOHandler.convertToOWL(model, outputStream);
        outputStream.close();

        log.info("All done.");
    }
}
