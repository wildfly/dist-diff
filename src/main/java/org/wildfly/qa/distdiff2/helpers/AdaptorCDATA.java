package org.wildfly.qa.distdiff2.helpers;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * AdaptorCDATA
 * <p>
 * Adaptor class for marshall, adds CDATA element into XML elements
 */
public class AdaptorCDATA extends XmlAdapter<String, String> {

    @Override
    public String marshal(String arg0) {
        return "<![CDATA[" + arg0 + "]]>";
    }

    @Override
    public String unmarshal(String arg0) {
        return arg0;
    }
}
