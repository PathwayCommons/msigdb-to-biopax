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
                    + "Usage: MsigdbToBiopax /path/to/msigdb_v5.2.xml /path/to/output.owl\n" +
                      "(does not work for msigdb_v6.x, which does not have TF gene symbols in the description)"
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
        System.out.println("MsigdbToBiopax: all done.");
    }

}
