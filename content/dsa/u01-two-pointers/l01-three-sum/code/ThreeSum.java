import java.util.*;

/**
 * DSA 1.1: 3Sum. Sort, fix one number (the captain), then run two pointers on the rest.
 * Every line this prints is quoted in the chapter, and the playground's step-by-step trace is tested against it.
 * Run it: java ThreeSum.java
 */
public class ThreeSum {

    /** O(n^2): sort, then Two Sum II on the rest for every captain, skipping duplicates. */
    static List<List<Integer>> threeSum(int[] nums, boolean trace) {
        int[] a = nums.clone();                 // keep the caller's array as it was
        Arrays.sort(a);
        if (trace) System.out.println("sorted " + Arrays.toString(a));
        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < a.length - 2; i++) {
            if (a[i] > 0) {
                if (trace) System.out.println("captain i=" + i + " (" + a[i] + ") is positive -> stop");
                break;
            }
            if (i > 0 && a[i] == a[i - 1]) {
                if (trace) System.out.println("captain i=" + i + " (" + a[i] + ") same as previous captain -> skip");
                continue;
            }
            if (trace) System.out.println("captain i=" + i + " (" + a[i] + ")");
            int left = i + 1, right = a.length - 1;
            while (left < right) {
                int sum = a[i] + a[left] + a[right];
                String at = "  L=" + left + "(" + a[left] + ") R=" + right + "(" + a[right] + ") sum=" + sum;
                if (sum == 0) {
                    result.add(List.of(a[i], a[left], a[right]));
                    if (trace) System.out.println(at + " -> found " + List.of(a[i], a[left], a[right]));
                    left++;
                    right--;
                    while (left < right && a[left] == a[left - 1]) left++;
                    while (left < right && a[right] == a[right + 1]) right--;
                } else if (sum < 0) {
                    if (trace) System.out.println(at + " -> too small, left++");
                    left++;
                } else {
                    if (trace) System.out.println(at + " -> too big, right--");
                    right--;
                }
            }
        }
        return result;
    }

    /** O(n^3) referee: obviously correct, so it can check the fast version. */
    static Set<List<Integer>> bruteForce(int[] nums) {
        Set<List<Integer>> out = new HashSet<>();
        for (int i = 0; i < nums.length; i++)
            for (int j = i + 1; j < nums.length; j++)
                for (int k = j + 1; k < nums.length; k++)
                    if (nums[i] + nums[j] + nums[k] == 0) {
                        int[] t = {nums[i], nums[j], nums[k]};
                        Arrays.sort(t);
                        out.add(List.of(t[0], t[1], t[2]));
                    }
        return out;
    }

    public static void main(String[] args) {
        System.out.println("== trace for [-1, 0, 1, 2, -1, -4]");
        System.out.println("result " + threeSum(new int[]{-1, 0, 1, 2, -1, -4}, true));

        System.out.println("== more inputs");
        int[][] cases = {{0, 0, 0}, {0, 0, 0, 0}, {0, 1, 1}, {}, {-2, 0, 0, 2, 2}, {3, -2, 1, 0}};
        for (int[] c : cases) System.out.println(Arrays.toString(c) + " -> " + threeSum(c, false));

        int[] original = {-1, 0, 1, 2, -1, -4};
        threeSum(original, false);
        System.out.println("caller's array untouched: " + Arrays.toString(original));

        // Fixed seed: the same 2000 random arrays on every run, so this line is stable for verify-content.
        Random rnd = new Random(42);
        int agree = 0;
        for (int t = 0; t < 2000; t++) {
            int[] arr = new int[rnd.nextInt(9)];
            for (int k = 0; k < arr.length; k++) arr[k] = rnd.nextInt(11) - 5;
            if (new HashSet<>(threeSum(arr, false)).equals(bruteForce(arr))
                    && threeSum(arr, false).size() == bruteForce(arr).size()) agree++;
        }
        System.out.println("random check: " + agree + " / 2000 match brute force, no duplicate triplets");
    }
}
