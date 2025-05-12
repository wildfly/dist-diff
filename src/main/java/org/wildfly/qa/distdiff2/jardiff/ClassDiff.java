package org.wildfly.qa.distdiff2.jardiff;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.wildfly.qa.distdiff2.helpers.AdaptorCDATA;
import org.wildfly.qa.distdiff2.jardiff.jaxb.FieldModifiersChange;
import org.wildfly.qa.distdiff2.jardiff.jaxb.MethodModifiersChange;

/**
 * @author Jan Martiska
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ClassDiff {

    // method changes
    @XmlElementWrapper(name = "addedMethods")
    @XmlElement(name = "addedMethod")
    private final Set<CtMethod> addedMethods;

    @XmlElementWrapper(name = "removedMethods")
    @XmlElement(name = "removedMethod")
    private final Set<CtMethod> removedMethods;

    @XmlElementWrapper(name = "methodModifiersChanges")
    @XmlElement(name = "methodModifiersChange")
    private final Set<MethodModifiersChange> methodModifiersChanges;

    // field changes
    @XmlElementWrapper(name = "addedFields")
    @XmlElement(name = "addedField")
    private final Set<CtField> addedFields;

    @XmlElementWrapper(name = "removedFields")
    @XmlElement(name = "removedField")
    private final Set<CtField> removedFields;

    @XmlElementWrapper(name = "fieldModifiersChanges")
    @XmlElement(name = "fieldModifiersChange")
    private final Set<FieldModifiersChange> fieldModifiersChanges;

    @XmlElement(name = "originalClassFormatVersion")
    private Integer originalClassFormatVersion = 0;

    @XmlElement(name = "newClassFormatVersion")
    private Integer newClassFormatVersion = 0;

    // class-level modifiers changes
    @XmlElement(name = "originalModifier")
    private Integer originalModifier;

    @XmlElement(name = "newModifier")
    private Integer newModifier;

    @XmlElement(name = "html_sourceCodeDiff")
    @XmlJavaTypeAdapter(AdaptorCDATA.class)
    private String html_sourceCodeDiff;


    public ClassDiff() {
        addedMethods = new HashSet<>();
        removedMethods = new HashSet<>();
        methodModifiersChanges = new HashSet<>();
        removedFields = new HashSet<>();
        fieldModifiersChanges = new HashSet<>();
        addedFields = new HashSet<>();
    }

    public void addedField(CtField field) {
        addedFields.add(field);
    }

    public void removedField(CtField field) {
        removedFields.add(field);
    }

    public void fieldModifierChanged(CtField field, int originalModifier, int newModifier) {
        FieldModifiersChange change = new FieldModifiersChange(field, originalModifier, newModifier);
        fieldModifiersChanges.add(change);
    }

    public void classModifiersChanged(int originalModifier, int newModifier) {
        this.originalModifier = originalModifier;
        this.newModifier = newModifier;
    }

    public void methodModifiersChanged(CtMethod method, int originalModifier, int newModifier) {
        MethodModifiersChange change = new MethodModifiersChange(method, originalModifier, newModifier);
        methodModifiersChanges.add(change);
    }

    public boolean isEmpty() {
        return addedFields.size() +
                removedFields.size() +
                addedMethods.size() +
                fieldModifiersChanges.size() +
                removedMethods.size() +
                methodModifiersChanges.size() == 0
                &&
                originalModifier == null
                &&
                (html_sourceCodeDiff == null || html_sourceCodeDiff.isEmpty())
                &&
                (originalClassFormatVersion.equals(newClassFormatVersion));
    }

    public void addedMethod(CtMethod method) {
        addedMethods.add(method);
    }

    public void removedMethod(CtMethod method) {
        removedMethods.add(method);
    }

    /**
     * doesn't include modifiers!
     */
    public static String convertToString(CtMethod method) {
        String returnType;
        try {
            returnType = method.getReturnType().getName();
        } catch (Exception e) {
            // the return type is a class which is neither in the same JAR nor in standard library, so we cannot
            // obtain its CtClass instance
            // => we must parse the class name from the signature
            // eg. (Ljava/lang/String;)Lorg/jboss/msc/service/ServiceName; becomes org.jboss.msc.service.ServiceName
            String signature = method.getSignature();
            returnType = signature.substring(signature.lastIndexOf(')') + 1, signature.length() - 1)
                    .replace('/', '.');
            // handle array types
            while (returnType.startsWith("[")) {
                returnType = returnType.substring(1) + "[]";
            }
            if (!returnType.startsWith("L")) {
                throw new Error("Cannot parse signature: " + method.getSignature());
            }
            returnType = returnType.substring(1);
        }
        // cut out the declaring class' name from the longName, eg.
        // org.jboss.modules.ModuleXmlParser.createMavenArtifactLoader(java.lang.String) becomes
        // createMavenArtifactLoader(java.lang.String)
        // ~ must cut out everything up to the last dot before the first left bracket
        String longName = method.getLongName();
        String myLongName = longName
                .substring(longName.substring(0, longName.indexOf("(")).lastIndexOf(".") + 1);
        return returnType + " " + myLongName;
    }


    public static String convertToString(CtField field, boolean includeModifiers) {
        StringBuilder builder = new StringBuilder();
        if (includeModifiers) {
            builder.append(Modifier.toString(field.getModifiers()));
            builder.append(" ");
        }
        String fieldType;
        try {
            fieldType = field.getType().getName();
        } catch (NotFoundException e) {
            String signature = field.getSignature();
            signature = signature.replace("/", ".");
            while (signature.startsWith("[")) {
                signature = signature.substring(1) + "[]";
            }
            fieldType = signature.substring(1, signature.length() - 1);

        }
        builder.append(fieldType);
        String name = field.getName(); // strip everything before $ if it is in the field name
        if (name.contains("$")) {
            name = name.substring(name.indexOf("$") + 1);
        }
        builder.append(" ").append(name);
        return builder.toString();
    }

    public String getHtml_sourceCodeDiff() {
        return html_sourceCodeDiff;
    }

    public void setHtml_sourceCodeDiff(String html_sourceCodeDiff) {
        this.html_sourceCodeDiff = html_sourceCodeDiff;
    }

    public Integer getNewClassFormatVersion() {
        return newClassFormatVersion;
    }

    public void setNewClassFormatVersion(Integer newClassFormatVersion) {
        this.newClassFormatVersion = newClassFormatVersion;
    }

    public Integer getOriginalClassFormatVersion() {
        return originalClassFormatVersion;
    }

    public void setOriginalClassFormatVersion(Integer originalClassFormatVersion) {
        this.originalClassFormatVersion = originalClassFormatVersion;
    }

    public Set<CtMethod> getAddedMethods() {
        return addedMethods;
    }

    public Set<CtMethod> getRemovedMethods() {
        return removedMethods;
    }

    public Set<MethodModifiersChange> getMethodModifiersChanges() {
        return methodModifiersChanges;
    }

    public Set<CtField> getAddedFields() {
        return addedFields;
    }

    public Set<CtField> getRemovedFields() {
        return removedFields;
    }

    public Set<FieldModifiersChange> getFieldModifiersChanges() {
        return fieldModifiersChanges;
    }

    public Integer getOriginalModifier() {
        return originalModifier;
    }

    public Integer getNewModifier() {
        return newModifier;
    }
}
