package org.example.set;

import java.util.concurrent.Future;

public interface ExpirySet<T> {

  public T add(T t, long timeMillis);

  public boolean contains(T t);

  public T remove(T t);
}
