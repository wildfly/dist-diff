package org.wildfly.qa.distdiff2.jardiff.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.wildfly.qa.distdiff2.jardiff.ClassDiff;

import javassist.CtField;

/**
 * @author Jan Martiska
 */
public class CtFieldAdapter extends XmlAdapter<String, CtField> {

    @Override
    public CtField unmarshal(String v) {
        return null;
    }

    @Override
    public String marshal(CtField v) {
        return ClassDiff.convertToString(v, false);
    }

}
