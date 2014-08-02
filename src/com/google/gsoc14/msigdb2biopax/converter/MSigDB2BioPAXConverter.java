package com.google.gsoc14.msigdb2biopax.converter;

import com.google.gsoc14.msigdb2biopax.util.Gene;
import com.google.gsoc14.msigdb2biopax.util.HGNCUtil;
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
import org.biopax.paxtools.model.level3.*;

import java.io.File;
import java.util.Set;
import java.util.UUID;

public class MSigDB2BioPAXConverter {
    private static Log log = LogFactory.getLog(MSigDB2BioPAXConverter.class);
    private final String symbolPattern = ".* (\\w+): .*";

    private HGNCUtil hgncUtil = new HGNCUtil();

    public HGNCUtil getHgncUtil() {
        return hgncUtil;
    }

    public void setHgncUtil(HGNCUtil hgncUtil) {
        this.hgncUtil = hgncUtil;
    }

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
        log.info("Read the msigdb file: " + mSigDB.getNumGeneSets() + " gene sets in the file.");

        int cnt=0;
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
                        cnt++;
                    }
                }
            }

        }

        log.info("Converted " + cnt + " gene sets into BioPAX pathway.");
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

        Set<Gene> tfGenes = hgncUtil.getGenes(symbol);
        if(tfGenes == null) {
            log.warn("Couldn't find transcription factor: " + symbol);
            return null;
        }


        for (Gene tf : tfGenes) {
            Rna tfel = getGene(model, tf);
            TemplateReactionRegulation regulation = create(TemplateReactionRegulation.class, "control_" + UUID.randomUUID());
            model.add(regulation);
            regulation.addController(tfel);
            String rname = annotation.getStandardName();
            regulation.setControlType(ControlType.ACTIVATION);
            regulation.setStandardName(rname);
            regulation.setDisplayName(rname);
            regulation.addName(rname);

            pathway.addPathwayComponent(regulation);

            for (Object o : annotation.getGeneSet(true).getMembers()) {
                String tSymbol = o.toString();

                Set<Gene> genes = hgncUtil.getGenes(tSymbol);
                if(genes == null) { continue; }

                for (Gene gene : genes) {
                    Rna target = getGene(model, gene);
                    TemplateReaction transcription = getTranscriptionOf(model, target);
                    regulation.addControlled(transcription);
                    pathway.addPathwayComponent(transcription);
                }
            }

        }

        return pathway;
    }

    private TemplateReaction getTranscriptionOf(Model model, Rna target) {
        // Make these transcription events unique
        String id = "transcription_" + target.getDisplayName() + "_" + UUID.randomUUID();
        TemplateReaction templateReaction = (TemplateReaction) model.getByID(id);
        if(templateReaction == null) {
            templateReaction = create(TemplateReaction.class, id);
            model.add(templateReaction);
            String tname = "Transcription of " + target.getDisplayName();
            templateReaction.setDisplayName(tname);
            templateReaction.setStandardName(tname);
            templateReaction.addName(tname);
            templateReaction.addProduct(target);
            templateReaction.setTemplateDirection(TemplateDirectionType.FORWARD);
        }

        return templateReaction;
    }

    private Rna getGene(Model model, Gene gene) {
        String id = gene.toString();
        Rna rna = (Rna) model.getByID(id);
        if(rna == null) {
            rna = createGene(model, gene);
        }
        return rna;
    }

    private Rna createGene(Model model, Gene gene) {
        Rna rna = create(Rna.class, gene.toString());
        model.add(rna);
        setNames(gene, rna);

        RnaReference rnaReference = create(RnaReference.class, "ref" + gene.toString());
        model.add(rnaReference);
        setNames(gene, rnaReference);
        assignXrefs(model, gene, rnaReference);

        rna.setEntityReference(rnaReference);

        return rna;
    }

    private void assignXrefs(Model model, Gene gene, RnaReference rnaReference) {
        String geneStr = gene.toString() + "_" + UUID.randomUUID();
        UnificationXref unificationXref = create(UnificationXref.class, "uxref_" + geneStr);
        model.add(unificationXref);
        unificationXref.setDb("NCBI Gene");
        unificationXref.setId(gene.getEntrezId());
        rnaReference.addXref(unificationXref);

        RelationshipXref relationshipXref = create(RelationshipXref.class, "rxref_" + geneStr);
        model.add(relationshipXref);
        relationshipXref.setDb("HGNC");
        relationshipXref.setId(gene.getHgncId());
        rnaReference.addXref(relationshipXref);

        RelationshipXref symbolXref = create(RelationshipXref.class, "rxref_symbol_" + geneStr);
        model.add(symbolXref);
        symbolXref.setDb("HGNC Symbol");
        symbolXref.setId(gene.getSymbol());
        rnaReference.addXref(symbolXref);
    }

    private void setNames(Gene gene, Named named) {
        String name = gene.getSymbol();
        named.setStandardName(name);
        named.setDisplayName(name);
        named.addName(name);

        for (String s : gene.getSynonyms()) {
            named.addName(s);
        }
    }
}
