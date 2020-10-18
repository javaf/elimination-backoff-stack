import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

// Elimination-backoff stack is an unbounded lock-free LIFO
// linked list, that eliminates concurrent pairs of pushes
// and pops with exchanges.  It uses compare-and-set (CAS)
// atomic operation to provide concurrent access with
// obstruction freedom. In order to support even greater
// concurrency, in case a push/pop fails, it tries to
// pair it with another pop/push to eliminate the operation
// through exchange of values.

class EliminationBackoffStack<T> {
  AtomicReference<Node<T>> top;
  EliminationArray<T> eliminationArray;
  static final int CAPACITY = 100;
  static final long TIMEOUT = 10;
  static final TimeUnit UNIT = TimeUnit.MILLISECONDS;
  // top: top of stack (null if empty)
  // eliminationArray: for exchanging values between push, pop
  // CAPACITY: capacity of elimination array
  // TIMEOUT: exchange timeout for elimination array
  // UNIT: exchange timeout unit for elimination array

  public EliminationBackoffStack() {
    top = new AtomicReference<>(null);
    eliminationArray = new EliminationArray<>(
      CAPACITY, TIMEOUT, UNIT
    );
  }

  // 1. Create a new node with given value.
  // 2. Try pushing it to stack.
  // 3a. If successful, return.
  // 3b. Otherwise, try exchanging on elimination array.
  // 4a. If found a matching pop, return.
  // 4b. Otherwise, retry 2.
  public void push(T x) {
    Node<T> n = new Node<>(x); // 1
    while (true) {
      if (tryPush(n)) return;  // 2, 3a
      try {
      T y = eliminationArray.visit(x); // 3b
      if (y == null) return;           // 4a
      }
      catch (TimeoutException e) {}   
    } // 4b
  }

  // 1. Try popping a node from stack.
  // 2a. If successful, return node's value
  // 2b. Otherwise, try exchanging on elimination array.
  // 3a. If found a matching push, return its value.
  // 3b. Otherwise, retry 1.
  public T pop() throws EmptyStackException {
    while (true) {
      Node<T> n = tryPop();          // 1
      if (n != null) return n.value; // 2a
      try {
      T y = eliminationArray.visit(null); // 2b
      if (y != null) return y;            // 3a
      }
      catch (TimeoutException e) {} // 3b
    }
  }

  // 1. Get stack top.
  // 2. Set node's next to top.
  // 3. Try push node at top (CAS).
  protected boolean tryPush(Node<T> n) {
    Node<T> m = top.get(); // 1
    n.next = m;                     // 2
    return top.compareAndSet(m, n); // 3
  }

  // 1. Get stack top, and ensure stack not empty.
  // 2. Try pop node at top, and set top to next (CAS).
  protected Node<T> tryPop() throws EmptyStackException {
    Node<T> m = top.get();                          // 1
    if (m == null) throw new EmptyStackException(); // 1
    Node<T> n = m.next;                       // 2
    return top.compareAndSet(m, n)? m : null; // 2
  }
}
