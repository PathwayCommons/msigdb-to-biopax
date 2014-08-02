package com.google.gsoc14.msigdb2biopax.converter;

import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.vdb.msigdb.GeneSetAnnotation;
import edu.mit.broad.vdb.msigdb.GeneSetCategory;
import edu.mit.broad.vdb.msigdb.MSigDB;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;

import java.io.File;

public class MSigDB2BioPAXConverter {
    private static Log log = LogFactory.getLog(MSigDB2BioPAXConverter.class);
    private final String symbolPattern = ".* (\\w+): .*";

    private BioPAXFactory bioPAXFactory = BioPAXLevel.L3.getDefaultFactory();

    public BioPAXFactory getBioPAXFactory() {
        return bioPAXFactory;
    }

    public void setBioPAXFactory(BioPAXFactory bioPAXFactory) {
        this.bioPAXFactory = bioPAXFactory;
    }

    public Model convert(String msigdbFile) throws Exception {
        Model model = getBioPAXFactory().createModel();
        MSigDB mSigDB = ParserFactory.readMSigDB(new File(msigdbFile), true);
        log.debug("Read the msigdb file: " + mSigDB.getNumGeneSets() + " gene sets in the file.");

        for (GeneSetAnnotation annotation : mSigDB.getGeneSetAnnotations()) {
            GeneSetCategory category = annotation.getCategory();

            // We are going to get only c3 human motif gene sets
            if(category.getCode().equalsIgnoreCase("c3")
                    && category.getName().equalsIgnoreCase("Motif")
                    && annotation.getOrganism().getName().equalsIgnoreCase("Homo sapiens"))
            {
                String briefDesc = annotation.getDescription().getBrief();
                if(briefDesc.contains("which matches annotation for")) {
                    if(briefDesc.matches(symbolPattern)) {
                        String symbol = briefDesc.replaceAll(symbolPattern, "$1");

                        createPathway(model, symbol, annotation);
                    }
                }
            }

        }
        return model;
    }

    protected <T extends BioPAXElement> T create(Class<T> aClass, String uri) {
        return getBioPAXFactory().create(aClass, uri);
    }

    private Pathway createPathway(Model model, String symbol, GeneSetAnnotation annotation) {
        Pathway pathway = create(Pathway.class, annotation.getLSIDName());
        model.add(pathway);

        String name = annotation.getStandardName();
        pathway.setStandardName(name);
        pathway.setDisplayName(name);
        pathway.addName(name);

        pathway.addComment(annotation.getDescription().getBrief());
        pathway.addComment(annotation.getDescription().getFull());
        pathway.addComment(annotation.getCategory().getDesc());

        return null;
    }
}
