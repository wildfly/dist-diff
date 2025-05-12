@XmlJavaTypeAdapters({
        @XmlJavaTypeAdapter(value = CtFieldAdapter.class, type = CtField.class),
        @XmlJavaTypeAdapter(value = CtMethodAdapter.class, type = CtMethod.class),
})
package org.wildfly.qa.distdiff2.jardiff.jaxb;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import javassist.CtField;
import javassist.CtMethod;
