package edu.mit.broad.vdb.msigdb.converter;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.TemplateReactionRegulation;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import static org.junit.Assert.*;

public class MsigdbToBiopaxConverterTest {

    @Test
    public void testDescriptionParsing() {
        final String briefDescription = "Genes with promoter regions [-2kb,2kb] around transcription start site " +
                "containing the motif SGCGSSAAA which matches annotation for E2F1: E2F transcription " +
                "factor 1&lt;br&gt; TFDP1: transcription factor Dp-1&lt;br&gt; RB1: retinoblastoma 1 " +
                "(including osteosarcoma)";
        int i = briefDescription.lastIndexOf(MsigdbToBiopaxConverter.WMAF);
        assertTrue(i > 0);
        String subs = briefDescription.substring(i + MsigdbToBiopaxConverter.WMAF.length());
        Matcher matcher = MsigdbToBiopaxConverter.symbolPattern.matcher(subs);
        Set<String> factors = new HashSet<String>();
        while(matcher.find()) {
            String g = matcher.group(1);
            factors.add(g);
        }
        assertEquals(3, factors.size());
        assertTrue(factors.containsAll(Arrays.asList("E2F1","TFDP1","RB1")));
    }


    @Test
    public void testConvert() throws Exception {
        Model m = new MsigdbToBiopaxConverter()
            .convert("msigdb5-test.xml", getClass().getResourceAsStream("/msigdb5-test.xml"));
        assertNotNull(m);
        assertFalse(m.getObjects(Protein.class).isEmpty());
        assertEquals(4, m.getObjects(Protein.class).size());
        assertEquals(212, m.getObjects(TemplateReactionRegulation.class).size());
        TemplateReactionRegulation trr = m.getObjects(TemplateReactionRegulation.class).iterator().next();
        String dname = trr.getDisplayName();
        assertTrue("HOX13_01".equals(dname) || "SGCGSSAAA_E2F1DP2_01".equals(dname)); //'V$' - orig. prefix was removed
        assertEquals(1,trr.getControlled().size()); //only one process is allowed (functional property)!
        assertTrue(m.getObjects(Pathway.class).isEmpty()); //no pathways get generated anymore!
    }
}
