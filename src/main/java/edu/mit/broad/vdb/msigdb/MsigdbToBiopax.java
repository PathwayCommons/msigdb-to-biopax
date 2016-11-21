package edu.mit.broad.vdb.msigdb;

import edu.mit.broad.vdb.msigdb.converter.MsigdbToBiopaxConverter;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;

public class MsigdbToBiopax {
    private static Logger log = LoggerFactory.getLogger(MsigdbToBiopax.class);

    public static void main(String[] args) throws Exception {
        if(args.length < 2) {
            System.err.println(
                    "Missing options.\n"
                    + "Usage: MsigdbToBiopax /path/to/msigdb_v4.0.xml /path/to/output.owl"
            );
            System.exit(-1);
        }

        String msigdbFile = args[0].trim();
        log.info("MSigDB File: " + msigdbFile);
        MsigdbToBiopaxConverter converter = new MsigdbToBiopaxConverter();
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
