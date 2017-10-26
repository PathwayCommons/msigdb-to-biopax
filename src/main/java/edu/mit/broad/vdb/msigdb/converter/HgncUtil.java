package edu.mit.broad.vdb.msigdb.converter;

import java.io.InputStream;
import java.util.*;

public class HgncUtil {
    private Map<String, Set<Gene>> symbol2gene = new HashMap<String, Set<Gene>>();
    private Map<String, Set<Gene>> synonym2gene = new HashMap<String, Set<Gene>>();

    public HgncUtil() {
        InputStream inputStream = this.getClass().getResourceAsStream("/hgnc_custom_download.tsv");
        Scanner scanner = new Scanner(inputStream);
        scanner.nextLine(); // skip the header
//HGNC ID<tab>Approved Symbol<tab>Previous Symbols<tab>Synonyms<tab>Entrez Gene ID<tab>RefSeq IDs<tab>Gene Family Name
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String tokens[] = line.split("\t", -1);

            Gene gene = new Gene();
            gene.setHgncId(tokens[0]);
            gene.setSymbol(tokens[1]);

            for (String synonym : tokens[2].split(", ")) {
                gene.getSynonyms().add(synonym);
            }
            for (String synonym : tokens[3].split(", ")) {
                gene.getSynonyms().add(synonym);
            }

            gene.setEntrezId(tokens[4]);
            gene.setRefseqId(tokens[5]);

            for (String synonym : tokens[6].split(", ")) {
                gene.getSynonyms().add(synonym);
            }

            addGeneToTheMap(gene);
        }

        scanner.close();
    }

    private void addGeneToTheMap(Gene gene) {
        addToGeneMap(gene.getSymbol(), gene);
        for (String s : gene.getSynonyms()) {
            addToSynonymGeneMap(s, gene);
        }
    }

    private void addToGeneMap(String s, Gene gene) {
        Set<Gene> genes = symbol2gene.get(s);
        if(genes == null) {
            genes = new HashSet<Gene>();
            symbol2gene.put(s, genes);
        }
        genes.add(gene);
    }

    private void addToSynonymGeneMap(String s, Gene gene) {
        Set<Gene> genes = synonym2gene.get(s);
        if(genes == null) {
            genes = new HashSet<Gene>();
            synonym2gene.put(s, genes);
        }
        genes.add(gene);
    }


    /**
     * Get Gene(s) by name.
     *
     * @param symbol
     * @return corresponding gene objects or empty set (never null).
     */
    public Set<Gene> getGenes(String symbol) {
        // First look up the approved symbol and then go for the synonym
        Set<Gene> genes = symbol2gene.get(symbol);
        if(genes == null) {
            genes = synonym2gene.get(symbol);
        }
        return (genes != null) ? genes : Collections.emptySet();
    }
}