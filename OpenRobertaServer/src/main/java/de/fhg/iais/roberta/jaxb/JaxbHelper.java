package de.fhg.iais.roberta.jaxb;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import de.fhg.iais.roberta.blockly.generated.BlockSet;

public class JaxbHelper {
    private static final Logger LOG = LoggerFactory.getLogger(JaxbHelper.class);

    private static final Schema blockSetSchema;
    private static final JAXBContext jaxbContext;
    static {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        InputStream xsdStream = JaxbHelper.class.getClassLoader().getResourceAsStream("blockly.xsd");
        StreamSource xsdSource = new StreamSource(xsdStream);
        try {
            blockSetSchema = sf.newSchema(xsdSource);
            jaxbContext = JAXBContext.newInstance(BlockSet.class);
        } catch ( Exception e ) {
            LOG.error("1. from blockly.xsd no schema could be generated or 2. JAXBContext could not be created", e);
            throw new RuntimeException("from blockly.xsd no schema could be generated");
        }
    }

    private JaxbHelper() {
        // no objects
    }

    /**
     * return the BlockSet generated by a jaxb unmarshaller for a given blockly XML string.
     *
     * @param blocklyXml the blockly XML as String
     * @return the BlockSet instance corresponding to the XML
     * @throws Exception
     */
    public static BlockSet xml2BlockSet(String blocklyXml) throws Exception {
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        jaxbUnmarshaller.setSchema(blockSetSchema);

        InputStream stream = new ByteArrayInputStream(blocklyXml.getBytes(StandardCharsets.UTF_8));
        InputSource src = new InputSource(stream);
        return (BlockSet) jaxbUnmarshaller.unmarshal(src);
    }

    /**
     * return the BlockSet generated by a jaxb unmarshaller for a file-path to blockly XML.
     *
     * @param pathToblocklyXml the file path to a blockly XML
     * @return the BlockSet instance corresponding to the XML
     * @throws Exception
     */
    public static BlockSet path2BlockSet(String pathToblocklyXml) throws Exception {
        return xml2BlockSet(IOUtils.toString(JaxbHelper.class.getResourceAsStream(pathToblocklyXml), "UTF-8"));
    }
}
