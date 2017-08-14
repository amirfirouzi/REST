import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by amir on 1/16/17.
 */
public class storm {
  public static <K, V> HashMap<V, List<K>> reverseMap(Map<K, V> map) {
    HashMap<V, List<K>> rtn = new HashMap<V, List<K>>();
    if (map == null) {
      return rtn;
    }
    for (Map.Entry<K, V> entry : map.entrySet()) {
      K key = entry.getKey();
      V val = entry.getValue();
      List<K> list = rtn.get(val);
      if (list == null) {
        list = new ArrayList<K>();
        rtn.put(entry.getValue(), list);
      }
      list.add(key);
    }
    return rtn;
  }

  public static void main(String[] args) {
    Map<Integer, String> tasks = new HashMap<Integer, String>();
    tasks.put(1, "a");
    tasks.put(1, "b");
    tasks.put(3, "c");

    Map<String, List<Integer>> componentTasks = reverseMap(tasks);
    System.out.println(componentTasks);
  }
}
