/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.jcajce.util;

import java.security.PrivateKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for a private key that carries annotations that can be used
 * for tracking or debugging.
 * @hide This class is not part of the Android public SDK API
 */
public class AnnotatedPrivateKey
    implements PrivateKey
{
    public static final String LABEL = "label";

    private final PrivateKey key;
    private final Map<String, Object> annotations;

    AnnotatedPrivateKey(PrivateKey key, String label)
    {
        this.key = key;
        this.annotations = Collections.singletonMap(LABEL, (Object)label);
    }

    AnnotatedPrivateKey(PrivateKey key, Map<String, Object> annotations)
    {
       this.key = key;
       this.annotations = annotations;
    }

    public PrivateKey getKey()
    {
        return key;
    }

    public Map<String, Object> getAnnotations()
    {
        return annotations;
    }
    
    public String getAlgorithm()
    {
        return key.getAlgorithm();
    }

    public Object getAnnotation(String key)
    {
        return annotations.get(key);
    }

    /**
     * Return a new annotated key with an additional annotation added to it.
     *
     * @param name the name of the annotation to add.
     * @param annotation the object providing the annotation details.
     * @return a new annotated key with the extra annotation.
     */
    public AnnotatedPrivateKey addAnnotation(String name, Object annotation)
    {
        Map<String, Object> newAnnotations = new HashMap<String, Object>(annotations);

        newAnnotations.put(name, annotation);

        return new AnnotatedPrivateKey(this.key, Collections.unmodifiableMap(newAnnotations));
    }

    /**
     * Return a new annotated key with the named annotation removed.
     *
     * @param name the name of the annotation to remove.
     * @return a new annotated key with the named annotation removed.
     */
    public AnnotatedPrivateKey removeAnnotation(String name)
    {
        Map<String, Object> newAnnotations = new HashMap<String, Object>(annotations);

        newAnnotations.remove(name);

        return new AnnotatedPrivateKey(this.key, Collections.unmodifiableMap(newAnnotations));
    }

    public String getFormat()
    {
        return key.getFormat();
    }

    public byte[] getEncoded()
    {
        return key.getEncoded();
    }

    public int hashCode()
    {
        return this.key.hashCode();
    }

    public boolean equals(Object o)
    {
        if (o instanceof AnnotatedPrivateKey)
        {
            return this.key.equals(((AnnotatedPrivateKey)o).key);
        }
        return this.key.equals(o);
    }

    public String toString()
    {
        if (annotations.containsKey(LABEL))
        {
            return annotations.get(LABEL).toString();
        }

        return key.toString();
    }
}
