package org.webathome.wsrest.test;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class TestObject {
    public static final Type LIST_TYPE = new TypeToken<List<TestObject>>(){}.getType();

    private String a;
    private int b;

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }

    @SuppressWarnings("NonFinalFieldReferenceInEquals")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TestObject)) {
            return false;
        }

        TestObject other = (TestObject)obj;

        return b == other.b && !(a != null ? !a.equals(other.a) : other.a != null);

    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public int hashCode() {
        return 31 * (a != null ? a.hashCode() : 0) + b;
    }
}
