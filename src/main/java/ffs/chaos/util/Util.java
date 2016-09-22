package ffs.chaos.util;

import java.util.Iterator;
import java.util.Map;

public class Util {

  public static <S, T> void printMap(Map<S, T> map) {
    Iterator<Map.Entry<S, T>> it = map.entrySet().iterator();
    Map.Entry<S, T> entry;
    System.out.printf("{ ");
    if (it.hasNext()) {
      entry = it.next();
      System.out.printf("%s: \"%s\"", entry.getKey(), entry.getValue());
    }
    while (it.hasNext()) {
      entry = it.next();
      System.out.printf(", %s: \"%s\"", entry.getKey(), entry.getValue());
    }
    System.out.printf(" }\n");
  }
}
