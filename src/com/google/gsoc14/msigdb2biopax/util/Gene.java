package com.google.gsoc14.msigdb2biopax.util;

import java.util.HashSet;
import java.util.Set;

public class Gene {
    private String symbol;
    private String hgncId;
    private Set<String> synonyms = new HashSet<String>();
    private String entrezId;
    private String refseqId;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getHgncId() {
        return hgncId;
    }

    public void setHgncId(String hgncId) {
        this.hgncId = hgncId;
    }

    public Set<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(Set<String> synonyms) {
        this.synonyms = synonyms;
    }

    public String getEntrezId() {
        return entrezId;
    }

    public void setEntrezId(String entrezId) {
        this.entrezId = entrezId;
    }

    public String getRefseqId() {
        return refseqId;
    }

    public void setRefseqId(String refseqId) {
        this.refseqId = refseqId;
    }

    @Override
    public String toString()  {
        return "gene_" + entrezId;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
