package gov.nist.core;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface MultiValueMap<K,V> extends Map<K,List<V>>, Serializable {
    // remove(K, V) conflicts with a Map method added in 1.8. http://b/27426743
    /*public Object remove( K key, V item );*/
}
