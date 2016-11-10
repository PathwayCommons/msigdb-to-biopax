# msigdb-to-biopax
Originated from https://bitbucket.org/armish/gsoc14 and will continue here (ToDo).

## MSigDB (TRANSFAC) to BioPAX Level3 data converter
This type of data sets are available from different resources, such as [TRANSFAC](http://www.biobase-international.com/gene-regulation), [JASPAR](http://jaspar.genereg.net/) and [ENCODE](http://www.genome.gov/Encode/) projects; but converting the data provided by these databases will require additional processing—e.g. mapping ChIP-Seq peaks to near-by genes or finding genes by binding motifs.

The easiest way, however, to import this data type is to use [MSigDB](http://www.broadinstitute.org/gsea/msigdb/collections.jsp), where transcription factors and their predicted targets are present in GSEA format provided by TRANSFAC. These associations can be converted to BioPAX by transcription events regulated by corresponding transcription factors.

### Data source
- **Home page**: [http://www.broadinstitute.org/gsea/msigdb/collections.jsp](http://www.broadinstitute.org/gsea/msigdb/collections.jsp)
- **Type**: Transcription factor - target
- **Format**: XML (MSigDB format)
- **License**: Free for academic use (also see [MSigDB license](http://www.broadinstitute.org/cancer/software/gsea/wiki/index.php/MSigDB_License))

### Implementation details
MSigDB provides data in various formats, the most commonly used being a tab-delimited format, GMT or GSEA, to describe a gene set.
It also provides downloadable files in XML format where full description of gene sets can be obtained in a single file.
This converter, with the help of the Java-based GSEA software as a library, parses the XML to obtain information about gene sets that describe transcription factors and their targets as gene sets.
For this, we are specifically extracting information from the `C3` category, specifically the `TFT` subcategory of it.

The genes in the gene sets are identified either via their symbols or Entrez Gene IDs.
Although unstructured, the name of the transcription factor associated with each gene set is present in the description of the gene set.
This converter has a built-in HGNC utility that helps better map the gene identifiers to full BioPAX `RnaReference`s.
For each TFT gene set, it creates a separate pathway identified by the unique name,
and within this pathway, the transcription factor positively regulates the transcription of targets listed in the corresponding gene set.
This information is captured via `TemplateReaction`s where target `Rna`s are being produced
and the reaction itself is being regulated by the transcription factor via `TemplateReactionRegulation`.

Below is a screenshot that shows a sample gene set converted into a `Pathway`:

![EVI1 targets as a BioPAX pathway](https://bitbucket.org/armish/gsoc14/downloads/goal5_screenshot_singlegeneset.png)

### Usage
Check out (git clone) the repository, change to:

	$ cd msigdb-to-biopax

To compile the code and create an executable JAR file, run ant:

	$ ant

You can then run the converter as follows:

	$ java -jar out/jar/msigdb2biopax/msigdb2biopax.jar 
	Usage: MSigDB2BioPAXConverterMain /path/to/msigdb_v4.0.xml /path/to/output.owl

or directly download the executable JAR: [goal5_msigdb2biopax-20140802.jar](https://bitbucket.org/armish/gsoc14/downloads/goal5_msigdb2biopax-20140802.jar).

For the conversion, you need to download the MSigDB database as an XML file: [msigdb_v4.0.xml](http://www.broadinstitute.org/gsea/msigdb/download_file.jsp?filePath=/resources/msigdb/4.0/msigdb_v4.0.xml).
Once downloaded, you can convert this into BioPAX as follows:

	$ java -jar out/jar/msigdb2biopax/msigdb2biopax.jar msigdb_v4.0.xml msigdb_v4.0.owl

You can download the converted model from the Downloads: [goal5_msigdb_c3_tft-20140802.owl.gz](https://bitbucket.org/armish/gsoc14/downloads/goal5_msigdb_c3_tft-20140802.owl.gz).

### Validation results
The validation report for the fully converted model is available here: [goal5_validationResults-20130802.zip](https://bitbucket.org/armish/gsoc14/downloads/goal5_validationResults-20130802.zip).
The BioPAX model does not have any major validation warnings or errors, hence is pretty clean.
