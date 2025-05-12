package org.wildfly.qa.distdiff2.jardiff.jaxb;

import java.lang.reflect.Modifier;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import javassist.CtField;

/**
 * @author Jan Martiska
 */
@XmlRootElement(name = "fieldModifiersChange")
@XmlAccessorType(XmlAccessType.FIELD)
public class FieldModifiersChange {

    public FieldModifiersChange() {
    }

    public FieldModifiersChange(CtField field, Integer oldModifiers, Integer newModifiers) {
        this.field = field;
        this.oldModifiers = Modifier.toString(oldModifiers);
        this.newModifiers = Modifier.toString(newModifiers);
    }

    private CtField field;

    private String oldModifiers;

    private String newModifiers;

}
