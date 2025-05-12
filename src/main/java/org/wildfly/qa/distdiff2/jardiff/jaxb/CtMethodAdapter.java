package org.wildfly.qa.distdiff2.jardiff.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.wildfly.qa.distdiff2.jardiff.ClassDiff;

import javassist.CtMethod;

/**
 * @author Jan Martiska
 */
public class CtMethodAdapter extends XmlAdapter<String, CtMethod> {

    @Override
    public CtMethod unmarshal(String v) {
        return null;
    }

    @Override
    public String marshal(CtMethod v) {
        return ClassDiff.convertToString(v);
    }

}
