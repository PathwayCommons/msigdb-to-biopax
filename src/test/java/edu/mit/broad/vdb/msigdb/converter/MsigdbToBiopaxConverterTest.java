package edu.mit.broad.vdb.msigdb.converter;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.TemplateReactionRegulation;
import org.junit.Test;

import static org.junit.Assert.*;

public class MsigdbToBiopaxConverterTest {
    @Test
    public void testConvert() throws Exception {
        Model m = new MsigdbToBiopaxConverter()
                .convert("msigdb5-test.xml", getClass().getResourceAsStream("/msigdb5-test.xml"));
        assertNotNull(m);
        assertFalse(m.getObjects(Protein.class).isEmpty());
        assertEquals(2, m.getObjects(Protein.class).size());
        assertEquals(212, m.getObjects(TemplateReactionRegulation.class).size());
        TemplateReactionRegulation trr = m.getObjects(TemplateReactionRegulation.class).iterator().next();
        String dname = trr.getDisplayName();
        assertTrue("HOX13_01".equals(dname) || "SGCGSSAAA_E2F1DP2_01".equals(dname)); //'V$' - orig. prefix was removed
        assertEquals(1,trr.getControlled().size()); //only one process is allowed (functional property)!
        assertTrue(m.getObjects(Pathway.class).isEmpty()); //no pathways get generated anymore!
    }
}
