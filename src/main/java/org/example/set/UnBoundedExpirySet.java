package org.example.set;

import org.example.model.ExiryPair;
import org.example.service.PurneService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

public class UnboundedExpirySet<T> implements ExpirySet<T> {
  private final ConcurrentMap<T, Long> map;
  private final PurneService purgeService;
  // Optionally store the ExecutorService if needed for future extensions.
  private final ExecutorService executorService;

  /**
   * Default constructor.
   * Uses the default PurgeService singleton.
   */
  public UnboundedExpirySet() {
    this.map = new ConcurrentHashMap<>();
    this.purgeService = PurneService.getInstance();
    this.executorService = null; // Not provided
  }

  /**
   * Constructor that accepts an ExecutorService.
   * The provided executorService may be used by PurgeService if needed.
   *
   * @param executorService an executor service to be used for scheduling purge tasks.
   */
  public UnboundedExpirySet(ExecutorService executorService) {
    this.map = new ConcurrentHashMap<>();
    this.executorService = executorService;
    // If PurgeService supports injection, you could pass the executorService here.
    this.purgeService = PurneService.getInstance();
  }

  /**
   * Adds an item with a time-to-live (in milliseconds).
   * The given ttl is added to the current time to compute the expiration timestamp.
   *
   * @param t          the item to add
   * @param ttlMillis  time-to-live in milliseconds
   * @return the added item
   */
  @Override
  public T add(T t, long ttlMillis) {
    long expirationTime = System.currentTimeMillis() + ttlMillis;
    map.put(t, expirationTime);
    // Register the expiry event with the PurgeService, passing the absolute expiration time.
    purgeService.addItem(new ExiryPair(expirationTime, map, t));
    return t;
  }

  /**
   * Checks if the set contains the item.
   *
   * @param t the item to check
   * @return true if present, false otherwise
   */
  @Override
  public boolean contains(T t) {
    Long expireTime = map.get(t);
    if (expireTime == null) {
      return false;
    }
    // Optionally, you can remove the key lazily if it's expired.
    if (System.currentTimeMillis() >= expireTime) {
      map.remove(t, expireTime);
      return false;
    }
    return true;
  }

  /**
   * Removes the item from the set.
   *
   * @param t the item to remove
   * @return the removed item
   */
  @Override
  public T remove(T t) {
    map.remove(t);
    return t;
  }
}
