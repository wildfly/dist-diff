package org.wildfly.qa.distdiff2.jardiff;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * @author Jan Martiska
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Tuple<X, Y> {

    private X x;
    private Y y;

    public Tuple(Y y, X x) {
        this.y = y;
        this.x = x;
    }

    public Y getY() {
        return y;
    }

    public void setY(Y y) {
        this.y = y;
    }

    public X getX() {
        return x;
    }

    public void setX(X x) {
        this.x = x;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tuple)) {
            return false;
        }

        Tuple tuple = (Tuple) o;

        if (x != null ? !x.equals(tuple.x) : tuple.x != null) {
            return false;
        }
        if (y != null ? !y.equals(tuple.y) : tuple.y != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = x != null ? x.hashCode() : 0;
        result = 31 * result + (y != null ? y.hashCode() : 0);
        return result;
    }
}
