import java.util.*;

/**
 * Lesson 0.1: a guided tour of the Collections family. Every line of output is quoted in the chapter, so the
 * chapter's numbers are always what Java really prints. Run it: java CollectionsTour.java
 */
public class CollectionsTour {

    public static void main(String[] args) {
        // 1. List: ordered, allows duplicates, index access.
        List<String> list = new ArrayList<>();
        list.add("mango");
        list.add("kiwi");
        list.add("apple");
        list.add("banana");
        list.add("apple");
        System.out.println("1 ArrayList      " + list + " get(2)=" + list.get(2));

        // 2. Set: no duplicates. Three flavours, three orders.
        String[] fruits = {"mango", "kiwi", "apple", "banana", "apple"};
        Set<String> hash = new HashSet<>(Arrays.asList(fruits));
        Set<String> linked = new LinkedHashSet<>(Arrays.asList(fruits));
        Set<String> tree = new TreeSet<>(Arrays.asList(fruits));
        System.out.println("2 HashSet        " + hash + " size=" + hash.size());
        System.out.println("3 LinkedHashSet  " + linked);
        System.out.println("4 TreeSet        " + tree);

        // 3. add() on a Set tells you whether it was new.
        System.out.println("5 add(\"apple\") again -> " + hash.add("apple"));

        // 4. Map: key -> value. Not a Collection! put() returns the OLD value.
        Map<String, Integer> stock = new HashMap<>();
        System.out.println("6 put first      -> " + stock.put("apple", 10));
        System.out.println("7 put again      -> " + stock.put("apple", 25));
        System.out.println("8 get(\"apple\")   -> " + stock.get("apple") + ", get(\"kiwi\") -> " + stock.get("kiwi"));
        System.out.println("9 Map is a Collection? " + (stock instanceof Collection));

        // 5. Where does "apple" land in a 16-bucket HashMap? (hash spreading, then mask)
        int h = "apple".hashCode();
        int spread = h ^ (h >>> 16);
        System.out.println("10 \"apple\".hashCode()=" + h + " bucket=" + (spread & 15) + " of 16");

        // 6. Queue and Deque: FIFO vs LIFO with the same class.
        Deque<String> deque = new ArrayDeque<>();
        deque.offer("first");
        deque.offer("second");
        deque.offer("third");
        System.out.println("11 queue poll()  -> " + deque.poll());
        Deque<String> stack = new ArrayDeque<>();
        stack.push("bottom");
        stack.push("middle");
        stack.push("top");
        System.out.println("12 stack pop()   -> " + stack.pop());

        // 7. PriorityQueue: the head is always the smallest, but printing it is NOT sorted.
        PriorityQueue<Integer> pq = new PriorityQueue<>(List.of(5, 1, 4, 2, 3));
        System.out.println("13 PriorityQueue " + pq + " peek=" + pq.peek());

        // 8. Immutable factories throw on change.
        List<String> fixed = List.of("a", "b");
        try {
            fixed.add("c");
        } catch (UnsupportedOperationException e) {
            System.out.println("14 List.of(...).add -> " + e.getClass().getSimpleName());
        }

        // 9. Arrays.asList is a fixed-size VIEW: set works, add does not.
        List<String> view = Arrays.asList("x", "y");
        view.set(0, "z");
        try {
            view.add("w");
        } catch (UnsupportedOperationException e) {
            System.out.println("15 Arrays.asList " + view + " add -> " + e.getClass().getSimpleName());
        }
    }
}
