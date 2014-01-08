//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.12.18 at 02:24:41 PM CET 
//


package playground.gregor.sim2d_v4.io.jaxb.gmlfeature;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 *         An abstract feature provides a set of common properties. A concrete 
 *         feature type must derive from this type and specify additional 
 *         properties in an application schema. A feature may optionally 
 *         possess an identifying attribute ('fid').
 *       
 * 
 * <p>Java class for AbstractFeatureType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AbstractFeatureType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.opengis.net/gml}description" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/gml}name" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/gml}boundedBy" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="fid" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbstractFeatureType", propOrder = {
    "description",
    "name",
    "boundedBy"
})
@XmlSeeAlso({
//    XMLSim2DEnvironmentSectionType.class,
    XMLAbstractFeatureCollectionBaseType.class
})
public abstract class XMLAbstractFeatureType {

    protected String description;
    protected String name;
    protected XMLBoundingShapeType boundedBy;
    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String fid;

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the boundedBy property.
     * 
     * @return
     *     possible object is
     *     {@link XMLBoundingShapeType }
     *     
     */
    public XMLBoundingShapeType getBoundedBy() {
        return this.boundedBy;
    }

    /**
     * Sets the value of the boundedBy property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLBoundingShapeType }
     *     
     */
    public void setBoundedBy(XMLBoundingShapeType value) {
        this.boundedBy = value;
    }

    /**
     * Gets the value of the fid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFid() {
        return this.fid;
    }

    /**
     * Sets the value of the fid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFid(String value) {
        this.fid = value;
    }

}
