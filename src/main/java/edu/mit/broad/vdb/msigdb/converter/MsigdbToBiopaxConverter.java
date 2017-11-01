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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MsigdbToBiopaxConverter {
    private static Logger log = LoggerFactory.getLogger(MsigdbToBiopaxConverter.class);

    final static String WMAF = "which matches annotation for ";
    final static Pattern symbolPattern = Pattern.compile("(\\w+):");

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
        model.setXmlBase(getXMLBase());
        MSigDB mSigDB = ParserFactory.readMSigDB(fileName, msigdbInputStream, true, true);
        log.info("Read the msigdb file: " + mSigDB.getNumGeneSets() + " gene sets in the file.");

        int cnt=0;
        final int numAnnotations = mSigDB.getNumGeneSets();
        for (int i=0; i < numAnnotations; i++) {
            GeneSetAnnotation annotation = mSigDB.getGeneSetAnnotation(i);
            GeneSetCategory category = annotation.getCategory();
            // We are going to get only c3 human TFT (motif) gene sets
            if(category.getCode().equalsIgnoreCase("c3")
                 && category.getName().equalsIgnoreCase("motif")
                 && annotation.getOrganism().getName().equalsIgnoreCase("homo sapiens"))
            {
                String briefDesc = annotation.getDescription().getBrief();
                int pos = briefDesc.indexOf(WMAF); //TODO: does NOT work for MSigDB v6.x
                if(pos > 0) {
                    //extract TF names after the sub-string:
                    Matcher matcher = symbolPattern.matcher(briefDesc.substring(pos + WMAF.length()));
                    Set<String> symbols = new HashSet<String>();
                    while(matcher.find()) symbols.add(matcher.group(1));
                    convertGeneSet(model, symbols, annotation);
                    cnt++;
                }
            }
        }

        log.info("Converted " + cnt + " gene sets to a BioPAX sub-network.");
        return model;
    }

    protected <T extends BioPAXElement> T create(Class<T> aClass, String uri) {
        return getBioPAXFactory().create(aClass, uri);
    }

    protected String completeId(String partialId) {
        return getXMLBase() + partialId;
    }

    private void convertGeneSet(Model model, Set<String> tfSymbols, GeneSetAnnotation annotation)
    {
        // Map TF names to Proteins - to be controllers of the TemplateReactionRegulations
        final Set<Protein> controllers = new HashSet<Protein>();
        for(String symbol: tfSymbols) {
            Set<Gene> tfGenes = hgncUtil.getGenes(symbol);
            if (tfGenes.isEmpty()) {
                log.warn("Couldn't find transcription factor: " + symbol);
            } else if (tfGenes.size() > 1) {
                // If more than one matches, then create a generic entity
                Protein tfel = getGenericProtein(model, symbol);
                for (Gene tfGene : tfGenes) {
                    Protein member = getProtein(model, tfGene);
                    tfel.getEntityReference().addMemberEntityReference(member.getEntityReference());
                }
                controllers.add(tfel);
            } else {
                Protein tfel = getProtein(model, tfGenes.iterator().next());
                controllers.add(tfel);
            }
        }

//TODO: use annotation.getExternalLinks(), e.g. getPMID(), create/add PublicationXrefs
//      addXrefs(regulation, annotation..getExternalLinks());
        for (Object o : annotation.getGeneSet(true).getMembers())
        {
            String tSymbol = o.toString();
            Set<Gene> genes = hgncUtil.getGenes(tSymbol);
            if(!genes.isEmpty()) {
                for (Gene gene : genes) {
                    TemplateReactionRegulation regulation = model.addNew(
                            TemplateReactionRegulation.class, completeId("control_" + UUID.randomUUID()));
                    for(Protein tfel: controllers)
                        regulation.addController(tfel);
//        regulation.setControlType(ControlType.ACTIVATION); //don't set - it's in fact unknown
                    regulation.setDisplayName(annotation.getStandardName().replaceFirst("V\\$",""));
                    regulation.setStandardName(annotation.getLSIDName());
                    regulation.addComment(annotation.getDescription().getBrief());
//        if(!annotation.getDescription().getFull().isEmpty())
//            regulation.addComment(annotation.getDescription().getFull());
                    regulation.addComment(annotation.getCategory().getCode());
                    regulation.addComment(annotation.getCategory().getName());
                    TemplateReaction transcription = getTranscriptionOf(model, gene);
                    regulation.addControlled(transcription);//max. one value is allowed (according to the BioPAX spec.)
                }
            }
        }
    }

    private Protein getGenericProtein(Model model, String name) {
        String nid = name;
        Protein prot = create(Protein.class, completeId("generic_protein_" + nid));
        model.add(prot);
        prot.setDisplayName(name);
        prot.addName(name);
        prot.setStandardName(name);

        ProteinReference protReference = create(ProteinReference.class, completeId("generic_proteinref" + nid));
        model.add(protReference);
        protReference.setStandardName(name);
        protReference.setDisplayName(name);
        protReference.addName(name);

        prot.setEntityReference(protReference);

        return prot;
    }

    private TemplateReaction getTranscriptionOf(Model model, Gene gene) {
        Rna target = getRna(model, gene);
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

    private Rna getRna(Model model, Gene gene) {
        String uri = completeId("rna_" + gene.toString());
        Rna rna = (Rna) model.getByID(uri);
        if(rna == null) {
            rna = create(Rna.class, uri);
            model.add(rna);
            setNames(gene, rna);
            RnaReference rnaReference = create(RnaReference.class, completeId("rnaref_" + gene.toString()));
            model.add(rnaReference);
            setNames(gene, rnaReference);
            assignXrefs(model, gene, rnaReference);
            rna.setEntityReference(rnaReference);
        }
        return rna;
    }

    private Protein getProtein(Model model, Gene gene)
    {
        String uri = completeId("protein_" + gene.toString());
        Protein protein = (Protein) model.getByID(uri);
        if(protein == null) {
            protein = create(Protein.class, uri);
            model.add(protein);
            setNames(gene, protein);
            ProteinReference proteinReference = create(ProteinReference.class,
                    completeId("proteinref_" + gene.toString()));
            model.add(proteinReference);
            setNames(gene, proteinReference);
            assignXrefs(model, gene, proteinReference);
            protein.setEntityReference(proteinReference);
        }

        return protein;
    }

    private void assignXrefs(Model model, Gene gene, EntityReference entityReference) {
        String uri = completeId("rxref_geneid_" + gene.getEntrezId());
        RelationshipXref relationshipXref = (RelationshipXref)model.getByID(uri);
        if (relationshipXref == null) {
            relationshipXref = model.addNew(RelationshipXref.class, uri);
            relationshipXref.setDb("NCBI Gene");
            relationshipXref.setId(gene.getEntrezId());
        }
        entityReference.addXref(relationshipXref);

        uri =  completeId("rxref_hgnc_" + gene.getHgncId());
        relationshipXref = (RelationshipXref)model.getByID(uri);
        if (relationshipXref == null) {
            relationshipXref = model.addNew(RelationshipXref.class, uri);
            relationshipXref.setDb("HGNC");
            relationshipXref.setId(gene.getHgncId());
        }
        entityReference.addXref(relationshipXref);

        uri =  completeId("rxref_symbol_" + gene.getSymbol());
        relationshipXref = (RelationshipXref)model.getByID(uri);
        if (relationshipXref == null) {
            relationshipXref = model.addNew(RelationshipXref.class, uri);
            relationshipXref.setDb("HGNC Symbol");
            relationshipXref.setId(gene.getSymbol());
        }
        entityReference.addXref(relationshipXref);
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
