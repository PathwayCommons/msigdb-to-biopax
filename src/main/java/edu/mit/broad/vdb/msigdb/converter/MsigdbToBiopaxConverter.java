package edu.mit.broad.vdb.msigdb.converter;

import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.vdb.msigdb.GeneSetAnnotation;
import edu.mit.broad.vdb.msigdb.GeneSetCategory;
import edu.mit.broad.vdb.msigdb.MSigDB;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class MsigdbToBiopaxConverter {
    private static Logger log = LoggerFactory.getLogger(MsigdbToBiopaxConverter.class);
    private final String symbolPattern = ".* (\\w+): .*";

    private String XMLBase = "http://www.gene-regulation.com/#";

    public String getXMLBase() {
        return XMLBase;
    }

    public void setXMLBase(String XMLBase) {
        this.XMLBase = XMLBase;
    }

    private HgncUtil hgncUtil = new HgncUtil();

    public HgncUtil getHgncUtil() {
        return hgncUtil;
    }

    public void setHgncUtil(HgncUtil hgncUtil) {
        this.hgncUtil = hgncUtil;
    }

    private BioPAXFactory bioPAXFactory = BioPAXLevel.L3.getDefaultFactory();

    public BioPAXFactory getBioPAXFactory() {
        return bioPAXFactory;
    }

    public void setBioPAXFactory(BioPAXFactory bioPAXFactory) {
        this.bioPAXFactory = bioPAXFactory;
    }

    public Model convert(final String msigdbFile) throws Exception {
        return convert(msigdbFile, ParserFactory.createInputStream(msigdbFile));
    }

    public Model convert(final String fileName, final InputStream msigdbInputStream) throws Exception {
        Model model = getBioPAXFactory().createModel();
        MSigDB mSigDB = ParserFactory.readMSigDB(fileName, msigdbInputStream, true, true);
        log.info("Read the msigdb file: " + mSigDB.getNumGeneSets() + " gene sets in the file.");

        int cnt=0;
        final int numAnnotations = mSigDB.getNumGeneSets();
        for (int i=0; i < numAnnotations; i++) {

            final GeneSetAnnotation annotation = mSigDB.getGeneSetAnnotation(i);
            final GeneSetCategory category = annotation.getCategory();

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

        model.setXmlBase(getXMLBase());

        log.info("Converted " + cnt + " gene sets into BioPAX pathway.");
        return model;
    }

    protected <T extends BioPAXElement> T create(Class<T> aClass, String uri) {
        return getBioPAXFactory().create(aClass, uri);
    }

    protected String completeId(String partialId) {
        return getXMLBase() + partialId;
    }

    private Pathway createPathway(Model model, String symbol, GeneSetAnnotation annotation) {
        Pathway pathway = create(Pathway.class, completeId(annotation.getLSIDName()));
        model.add(pathway);

        String name = annotation.getStandardName();
        pathway.setStandardName(name);
        pathway.setDisplayName(name);
        pathway.addName(name);

        pathway.addComment(annotation.getDescription().getBrief());
        pathway.addComment(annotation.getDescription().getFull());
        pathway.addComment(annotation.getCategory().getCode());
        pathway.addComment(annotation.getCategory().getName());

        Set<Gene> tfGenes = hgncUtil.getGenes(symbol);
        if(tfGenes == null) {
            log.warn("Couldn't find transcription factor: " + symbol);
            return null;
        }

        Rna tfel;
        if(tfGenes.size() > 1) {
            // If more than one matches, then create a generic entity
            tfel = getGenericGene(model, symbol);
            for (Gene tfGene : tfGenes) {
                Rna memberGene = getGene(model, tfGene);
                tfel.getEntityReference().addMemberEntityReference(memberGene.getEntityReference());
            }
        } else {
            tfel = getGene(model, tfGenes.iterator().next());
        }
        assert tfel != null;

        for (Object o : annotation.getGeneSet(true).getMembers()) {
            String tSymbol = o.toString();

            Set<Gene> genes = hgncUtil.getGenes(tSymbol);
            if(genes == null) { continue; }

            for (Gene gene : genes) {
                TemplateReactionRegulation regulation
                        = create(TemplateReactionRegulation.class, completeId("control_" + UUID.randomUUID()));
                model.add(regulation);
                regulation.addController(tfel);
                String rname = annotation.getStandardName();
                regulation.setControlType(ControlType.ACTIVATION);
                regulation.setStandardName(rname);
                regulation.setDisplayName(rname);
                regulation.addName(rname);
                pathway.addPathwayComponent(regulation);

                Rna target = getGene(model, gene);
                TemplateReaction transcription = getTranscriptionOf(model, target);
                regulation.addControlled(transcription);
                pathway.addPathwayComponent(transcription);
            }
        }


        return pathway;
    }

    private Rna getGenericGene(Model model, String name) {
        String nid = name + "_" + UUID.randomUUID();
        Rna rna = create(Rna.class, completeId("generic_" + nid));
        model.add(rna);
        rna.setDisplayName(name);
        rna.addName(name);
        rna.setStandardName(name);

        RnaReference rnaReference = create(RnaReference.class, completeId("generic_ref" + nid));
        model.add(rnaReference);
        rnaReference.setStandardName(name);
        rnaReference.setDisplayName(name);
        rnaReference.addName(name);

        rna.setEntityReference(rnaReference);

        return rna;
    }

    private TemplateReaction getTranscriptionOf(Model model, Rna target) {
        // Make these transcription events unique
        String id = "transcription_" + target.getDisplayName() + "_" + UUID.randomUUID();
        TemplateReaction templateReaction = (TemplateReaction) model.getByID(completeId(id));
        if(templateReaction == null) {
            templateReaction = create(TemplateReaction.class, completeId(id));
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
        Rna rna = (Rna) model.getByID(completeId(id));
        if(rna == null) {
            rna = createGene(model, gene);
        }
        return rna;
    }

    private Rna createGene(Model model, Gene gene) {
        Rna rna = create(Rna.class, completeId(gene.toString()));
        model.add(rna);
        setNames(gene, rna);

        RnaReference rnaReference = create(RnaReference.class, completeId("ref" + gene.toString()));
        model.add(rnaReference);
        setNames(gene, rnaReference);
        assignXrefs(model, gene, rnaReference);

        rna.setEntityReference(rnaReference);

        return rna;
    }

    private void assignXrefs(Model model, Gene gene, RnaReference rnaReference) {
        String geneStr = gene.toString() + "_" + UUID.randomUUID();
        UnificationXref unificationXref = create(UnificationXref.class, completeId("uxref_" + geneStr));
        model.add(unificationXref);
        unificationXref.setDb("NCBI Gene");
        unificationXref.setId(gene.getEntrezId());
        rnaReference.addXref(unificationXref);

        RelationshipXref relationshipXref = create(RelationshipXref.class, completeId("rxref_" + geneStr));
        model.add(relationshipXref);
        relationshipXref.setDb("HGNC");
        relationshipXref.setId(gene.getHgncId());
        rnaReference.addXref(relationshipXref);

        RelationshipXref symbolXref = create(RelationshipXref.class, completeId("rxref_symbol_" + geneStr));
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