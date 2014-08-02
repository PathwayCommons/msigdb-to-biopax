package com.google.gsoc14.msigdb2biopax.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class HGNCUtil {
    private HashMap<String, HashSet<Gene>> symbol2gene = new HashMap<String, HashSet<Gene>>();
    
    public HGNCUtil() {
        InputStream inputStream = this.getClass().getResourceAsStream("hgnc_20140801.tsv");
        Scanner scanner = new Scanner(inputStream);
        scanner.nextLine(); // skip the header

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
            gene.setRefseqId(tokens[4]);
            gene.setEntrezId(tokens[5]);

            addGeneToTheMap(gene);
        }

        scanner.close();
    }

    private void addGeneToTheMap(Gene gene) {
        addToGeneMap(gene.getSymbol(), gene);
        for (String s : gene.getSynonyms()) {
            addToGeneMap(s, gene);
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

    public Set<Gene> getGenes(String symbol) {
        return symbol2gene.get(symbol);
    }
}