@XmlJavaTypeAdapters({
        @XmlJavaTypeAdapter(value = org.wildfly.qa.distdiff2.jardiff.jaxb.CtFieldAdapter.class, type = CtField.class),
        @XmlJavaTypeAdapter(value = org.wildfly.qa.distdiff2.jardiff.jaxb.CtMethodAdapter.class, type = CtMethod.class),
})
package org.wildfly.qa.distdiff2.jardiff;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import javassist.CtField;
import javassist.CtMethod;
