# msigdb-to-biopax
Originated from https://bitbucket.org/armish/gsoc14 and will continue here (ToDo).

## MSigDB (TRANSFAC) to BioPAX Level3 data converter
This type of data sets are available from different resources, such as 
[TRANSFAC](http://www.biobase-international.com/gene-regulation), [JASPAR](http://jaspar.genereg.net/) and [ENCODE](http://www.genome.gov/Encode/) projects; 
but converting the data provided by these databases will require additional 
processing—e.g. mapping ChIP-Seq peaks to near-by genes or finding genes 
by binding motifs.

The easiest way, however, to import this data type is to use [MSigDB](http://www.broadinstitute.org/gsea/msigdb/collections.jsp), 
where transcription factors and their predicted targets are present in 
GSEA format provided by TRANSFAC. These associations can be converted to 
BioPAX by transcription events regulated by corresponding transcription factors.

### Data source
- **Home page**: [http://www.broadinstitute.org/gsea/msigdb/collections.jsp](http://www.broadinstitute.org/gsea/msigdb/collections.jsp)
- **Type**: Transcription factor - target
- **Format**: XML (MSigDB format)
- **License**: Free for academic use (also see [MSigDB license](http://www.broadinstitute.org/cancer/software/gsea/wiki/index.php/MSigDB_License))

### Implementation details
MSigDB provides data in various formats, the most commonly used being a 
tab-delimited format, GMT or GSEA, to describe a gene set.
It also provides downloadable files in XML format where full description 
of gene sets can be obtained in a single file.
This converter, _with the help of the Java-based GSEA software as a library_ (MIT proprietary), 
parses the XML to obtain information about gene sets that describe 
transcription factors and their targets as gene sets. For this, we are 
specifically extracting information from the `C3` category, specifically 
the `TFT` subcategory of it.

The genes in the gene sets are identified either via their symbols or 
Entrez Gene IDs. Although unstructured, the name of the transcription 
factor associated with each gene set is present in the description of 
the gene set. This converter has a built-in HGNC utility that helps 
better map the gene identifiers to full BioPAX `RnaReference`s.
For each TFT gene set, it creates a separate pathway identified by the 
unique name, and within this pathway, the transcription factor positively 
regulates the transcription of targets listed in the corresponding gene set.
This information is captured via `TemplateReaction`s where target `Rna`s 
are being produced, and the reaction itself is being regulated by the 
transcription factor via `TemplateReactionRegulation`.

### Usage
This project uses the MIT GSEA java library (gsea2-2*.jar), which is not open source (as well as 
[MSigDB](http://software.broadinstitute.org/cancer/software/gsea/wiki/index.php/MSigDB_Acknowledgements) data); see 
[GSEA/MSigDB licence](http://software.broadinstitute.org/gsea/msigdb/download_file.jsp?filePath=/resources/licenses/gsea_msigdb_license.txt)
and [this wiki page](http://software.broadinstitute.org/cancer/software/gsea/wiki/index.php/MSigDB_License) .
So, we downloaded the software at http://software.broadinstitute.org/gsea/downloads.jsp and added to the `lib` folder 
(we also manually removed the junit package from that jar).

Check out (git clone) the repository, change to:

	$ cd msigdb-to-biopax

To compile the code and create an executable JAR file, run:

	$ mvn clean package

You can then run the converter as follows:

	$ java -jar target/msigdb-to-biopax.jar 
	Usage: MsigdbToBiopax /path/to/msigdb_v5.2.xml /path/to/output.owl

For the conversion, you need to download the MSigDB database as an XML file: [msigdb_v5.2.xml](http://www.broadinstitute.org/gsea/msigdb/download_file.jsp?filePath=/resources/msigdb/5.2/msigdb_v5.2.xml).
Once downloaded, you can convert this into BioPAX as follows:

	$ java -jar msigdb-to-biopax.jar msigdb_v5.2.xml msigdb_v5.2.owl

