package org.wildfly.qa.distdiff2.jardiff.jaxb;

import java.lang.reflect.Modifier;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import javassist.CtMethod;

/**
 * @author Jan Martiska
 */
@XmlRootElement(name = "methodModifiersChange")
@XmlAccessorType(XmlAccessType.FIELD)
public class MethodModifiersChange {

    public MethodModifiersChange() {
    }

    public MethodModifiersChange(CtMethod method, Integer oldModifiers, Integer newModifiers) {
        this.method = method;
        this.oldModifiers = Modifier.toString(oldModifiers);
        this.newModifiers = Modifier.toString(newModifiers);
    }

    private CtMethod method;

    private String oldModifiers;

    private String newModifiers;

}
