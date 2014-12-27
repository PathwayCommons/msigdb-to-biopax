package com.google.gsoc14.msigdb2biopax.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class HGNCUtil {
    private HashMap<String, HashSet<Gene>> symbol2gene = new HashMap<String, HashSet<Gene>>();
    private HashMap<String, HashSet<Gene>> synonym2gene = new HashMap<String, HashSet<Gene>>();

    public HGNCUtil() {
        InputStream inputStream = this.getClass().getResourceAsStream("hgnc_custom_download.tsv");
        Scanner scanner = new Scanner(inputStream);
        scanner.nextLine(); // skip the header
        // HGNC ID	Approved Symbol	Synonyms	Entrez Gene ID	RefSeq IDs	Gene Family Tag

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
        HashSet<Gene> genes = symbol2gene.get(s);
        if(genes == null) {
            genes = new HashSet<Gene>();
            symbol2gene.put(s, genes);
        }
        genes.add(gene);
    }

    private void addToSynonymGeneMap(String s, Gene gene) {
        HashSet<Gene> genes = synonym2gene.get(s);
        if(genes == null) {
            genes = new HashSet<Gene>();
            synonym2gene.put(s, genes);
        }
        genes.add(gene);
    }


    public Set<Gene> getGenes(String symbol) {
        // First look up the approved symbol and then go for the synonym
        HashSet<Gene> genes = symbol2gene.get(symbol);
        if(genes == null || genes.isEmpty()) {
            genes = synonym2gene.get(symbol);
        }
        return genes;
    }
}