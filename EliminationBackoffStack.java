import java.util.EmptyStackException;
import java.util.concurrent.TimeoutException;

class EliminationBackoffStack<T> {
  EliminationArray<T> eliminationArray;
  static final int CAPACITY = 1;

  public void push(T x) {
    RangePolicy p = policy.get();
    Node<T> n = new Node<>(x);
    while (true) {
      if (tryPush(n)) return;
      try {
        T y = eliminationArray.visit(x, p.getRange());
        if (y == null) {
          p.recordEliminationSuccess();
          return;
        }
      }
      catch (TimeoutException e) {
        p.recordEliminationTimeout();
      }
    }
  }

  public T pop() throws EmptyStackException {
    RangePolicy p = policy.get();
    while (true) {
      Node<T> n = tryPop();
      if (n != null) return n.value;
      try {
        T y = eliminationArray.visit(null, p.getRange());
        if (y != null) {
          p.recordEliminationSuccess();
          return y;
        }
      }
      catch (TimeoutException e) {
        p.recordEliminationTimeout();
      }
    }
  }
}
