import java.util.*;
import java.util.concurrent.*;

// Elimination array provides a list of exchangers which
// are picked at random for a given value.

class EliminationArray<T> {
  Exchanger<T>[] exchangers;
  Random random;
  static final long TIMEOUT = 1;
  static final TimeUnit UNIT = TimeUnit.MILLISECONDS;
  // exchangers: array of exchangers
  // random: random number generator
  // TIMEOUT: exchange timeout number
  // UNIT: exchange timeout unit

  @SuppressWarnings("unchecked")
  public EliminationArray(int capacity) {
    exchangers = new Exchanger[capacity];
    for (int i=0; i<capacity; i++)
      exchangers[i] = new Exchanger<>();
    random = new Random();
  }

  // 1. Try exchanging value on a random exchanger.
  public T visit(T x, int range) throws TimeoutException {
    int i = random.nextInt(range);
    return exchangers[i].exchange(x, TIMEOUT, UNIT);
  }
}
