package edu.mit.broad.vdb.msigdb;

import edu.mit.broad.vdb.msigdb.converter.MsigdbToBiopaxConverter;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;

import java.io.FileOutputStream;

public class MsigdbToBiopax {

    public static void main(String[] args) throws Exception {
        if(args.length < 2) {
            System.err.println(
                    "Missing options.\n"
                    + "Usage: MsigdbToBiopax /path/to/msigdb_v4.0.xml /path/to/output.owl"
            );
            System.exit(-1);
        }

        String msigdbFile = args[0].trim();
        MsigdbToBiopaxConverter converter = new MsigdbToBiopaxConverter();
        Model model = converter.convert(msigdbFile);
        SimpleIOHandler simpleIOHandler = new SimpleIOHandler();
        String outputFile = args[1].trim();
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        simpleIOHandler.convertToOWL(model, outputStream);
        outputStream.close();
        System.out.println("All done.");
    }

}
