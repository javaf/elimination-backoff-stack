import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

// Exchanger is a lock-free object that permits two threads
// to exchange values, within a time limit.

class Exchanger<T> {
  AtomicStampedReference<T> slot;
  static final int EMPTY = 0;
  static final int WAITING = 1;
  static final int BUSY = 2;
  // slot: stores value and stamp
  // EMPTY: slot has no value.
  // WAITING: slot has 1st value, waiting for 2nd.
  // BUSY: slot has 2nd value, waiting to be empty.

  public Exchanger() {
    slot = new AtomicStampedReference<>(null, 0);
  }

  // 1. Calculate last wait time.
  // 2. If wait time exceeded, then throw expection.
  // 3. Get slot value and stamp.
  // 4a. If slot is EMPTY (no value):
  // 4b. Try adding 1st value to slot, else retry 2.
  // 4c. Try getting 2nd value from slot, within time limit.
  // 5a. If slot is WAITING (has 1st value):
  // 5b. Try adding 2nd value to slot, else retry 2.
  // 5c. Return 1st value.
  // 6a. If slot is BUSY (has 2nd value):
  // 6b. Retry 2.
  public T exchange(T y, long timeout, TimeUnit unit)
    throws TimeoutException {
    long w = unit.toNanos(timeout); // 1
    long W = System.nanoTime() + w; // 1
    int[] stamp = {EMPTY};
    while (System.nanoTime() < W) { // 2
      T x = slot.get(stamp); // 3
      switch (stamp[0]) {    // 3
        case EMPTY:    // 4
        if (addA(y)) { // 4
          while (System.nanoTime() < W)            // 4
            if ((x = removeB()) != null) return x; // 4
          throw new TimeoutException(); // 5
        }
        break;
        case WAITING:   // 7
        if (addB(x, y)) // 7
          return x;     // 7
        break;
        case BUSY: // 8
        break;     // 8
        default:
      }
    }
    throw new TimeoutException(); // 2
  }

  // 1. Add 1st value to slot.
  // 2. Set its stamp as WAITING (for 2nd).
  private boolean addA(T y) { // 1, 2
    return slot.compareAndSet(null, y, EMPTY, WAITING);
  }

  // 1. Add 2nd value to slot.
  // 2. Set its stamp as BUSY (for 1st to remove).
  private boolean addB(T x, T y) { // 1, 2
    return slot.compareAndSet(x, y, WAITING, BUSY);
  }

  // 1. If stamp is not BUSY (no 2nd value in slot), exit.
  // 2. Set slot as EMPTY, and get 2nd value from slot.
  private T removeB() {
    int[] stamp = {EMPTY};
    T x = slot.get(stamp);             // 1
    if (stamp[0] != BUSY) return null; // 1
    slot.set(null, EMPTY); // 2
    return x;              // 2
  }
}
