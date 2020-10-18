import java.util.*;

class Main {
  static Deque<Integer> stack;
  static EliminationBackoffStack<Integer> concurrentStack;
  static List<Integer>[] poppedValues;
  static int TH = 10, NUM = 1000;

  // Each unsafe thread pushes N numbers and pops N, adding
  // them to its own poppedValues for checking; using Java's
  // sequential stack implementation, ArrayDeque.
  static Thread unsafe(int id, int x, int N) {
    return new Thread(() -> {
      String action = "push";
      try {
      for (int i=0, y=x; i<N; i++)
        stack.push(y++);
      Thread.sleep(1000);
      action = "pop";
      for (int i=0; i<N; i++)
        poppedValues[id].add(stack.pop());
      }
      catch (Exception e) { log(id+": failed "+action); }
    });
  }

  // Each safe thread pushes N numbers and pops N, adding
  // them to its own poppedValues for checking; using
  // BackoffStack.
  static Thread safe(int id, int x, int N) {
    return new Thread(() -> {
      String action = "push";
      try {
      for (int i=0, y=x; i<N; i++)
        concurrentStack.push(y++);
      Thread.sleep(1000);
      action = "pop";
      for (int i=0; i<N; i++)
        poppedValues[id].add(concurrentStack.pop());
      }
      catch (Exception e) { log(id+": failed "+action); }
    });
  }

  // Checks if each thread popped N values, and they are
  // globally unique.
  static boolean wasLIFO(int N) {
    Set<Integer> set = new HashSet<>();
    boolean passed = true;
    for (int i=0; i<TH; i++) {
      int n = poppedValues[i].size();
      if (n != N) {
        log(i+": popped "+n+"/"+N+" values");
        passed = false;
      }
      for (Integer x : poppedValues[i])
        if (set.contains(x)) {
          log(i+": has duplicate value "+x);
          passed = false;
        }
      set.addAll(poppedValues[i]);
    }
    return passed;
  }

  @SuppressWarnings("unchecked")
  static void testThreads(boolean backoff) {
    stack = new ArrayDeque<>();
    concurrentStack = new EliminationBackoffStack<>();
    poppedValues = new List[TH];
    for (int i=0; i<TH; i++)
      poppedValues[i] = new ArrayList<>();
    Thread[] threads = new Thread[TH];
    for (int i=0; i<TH; i++) {
      threads[i] = backoff?
        safe(i, i*NUM, NUM) :
        unsafe(i, i*NUM, NUM);
      threads[i].start();      
    }
    try {
    for (int i=0; i<TH; i++)
      threads[i].join();
    }
    catch (Exception e) {}
  }

  public static void main(String[] args) {
    log("Starting "+TH+" threads with sequential stack");
    testThreads(false);
    log("Was LIFO? "+wasLIFO(NUM));
    log("");
    String name = "elimination backoff stack";
    log("Starting "+TH+" threads with "+name);
    testThreads(true);
    log("Was LIFO? "+wasLIFO(NUM));
    log("");
  }

  static void log(String x) {
    System.out.println(x);
  }
}
