package org.example.model;

import java.util.concurrent.ConcurrentMap;

public class ExiryPair<T> implements Comparable<ExiryPair<T>> {
  private long timeMilli;
  private ConcurrentMap<T,Long> map;

  private T t;

  public ExiryPair(long timeMilli, ConcurrentMap<T, Long> map, T t) {
    this.timeMilli = timeMilli;
    this.map = map;
    this.t = t;
  }

  public ConcurrentMap<T, Long> getMap() {
    return map;
  }

  @Override
  public int compareTo(ExiryPair<T> other) {
    return Long.compare(this.timeMilli, other.timeMilli);
  }

  public long getTimeMilli() {
    return timeMilli;
  }

  public T getT() {
    return t;
  }

  @Override
  public String toString() {
    return "ExiryPair{" +
            "timeMilli=" + timeMilli +
            ", map=" + map +
            ", t=" + t +
            '}';
  }
}
