import java.util.*;

/**
 * DSA 1.3: Trapping Rain Water. Water above a bar = min(tallest bar to its left, tallest to its right) - its height.
 * Three versions: brute force O(n^2), prefix maxima O(n) time + O(n) space, two pointers O(n) time + O(1) space.
 * Every line this prints is quoted in the chapter; the playground's two-pointer trace is tested against it.
 * Run it: java TrappingRainWater.java
 */
public class TrappingRainWater {

    /** O(n^2): for every bar, scan both sides for the tallest. */
    static int bruteForce(int[] h) {
        int total = 0;
        for (int i = 0; i < h.length; i++) {
            int leftMax = 0, rightMax = 0;
            for (int j = 0; j <= i; j++) leftMax = Math.max(leftMax, h[j]);
            for (int j = i; j < h.length; j++) rightMax = Math.max(rightMax, h[j]);
            total += Math.min(leftMax, rightMax) - h[i];
        }
        return total;
    }

    /** O(n) time, O(n) space: precompute the tallest bar up to each index from both sides. */
    static int prefixMax(int[] h, boolean print) {
        int n = h.length;
        if (n == 0) return 0;
        int[] leftMax = new int[n], rightMax = new int[n], water = new int[n];
        leftMax[0] = h[0];
        for (int i = 1; i < n; i++) leftMax[i] = Math.max(leftMax[i - 1], h[i]);
        rightMax[n - 1] = h[n - 1];
        for (int i = n - 2; i >= 0; i--) rightMax[i] = Math.max(rightMax[i + 1], h[i]);
        int total = 0;
        for (int i = 0; i < n; i++) {
            water[i] = Math.min(leftMax[i], rightMax[i]) - h[i];
            total += water[i];
        }
        if (print) {
            System.out.println("height   " + Arrays.toString(h));
            System.out.println("leftMax  " + Arrays.toString(leftMax));
            System.out.println("rightMax " + Arrays.toString(rightMax));
            System.out.println("water    " + Arrays.toString(water));
        }
        return total;
    }

    /** O(n) time, O(1) space: the side with the smaller max is settled, so fill it and move inward. */
    static int twoPointers(int[] h, boolean trace) {
        int left = 0, right = h.length - 1;
        int leftMax = 0, rightMax = 0, total = 0;
        while (left < right) {
            if (h[left] < h[right]) {
                if (h[left] >= leftMax) {
                    leftMax = h[left];
                    if (trace) System.out.println("  L=" + left + "(" + h[left] + ") new leftMax=" + leftMax + " -> no water, left++");
                } else {
                    total += leftMax - h[left];
                    if (trace) System.out.println("  L=" + left + "(" + h[left] + ") leftMax=" + leftMax + " -> water "
                            + (leftMax - h[left]) + ", total=" + total + ", left++");
                }
                left++;
            } else {
                if (h[right] >= rightMax) {
                    rightMax = h[right];
                    if (trace) System.out.println("  R=" + right + "(" + h[right] + ") new rightMax=" + rightMax + " -> no water, right--");
                } else {
                    total += rightMax - h[right];
                    if (trace) System.out.println("  R=" + right + "(" + h[right] + ") rightMax=" + rightMax + " -> water "
                            + (rightMax - h[right]) + ", total=" + total + ", right--");
                }
                right--;
            }
        }
        return total;
    }

    public static void main(String[] args) {
        int[] example = {0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1};
        System.out.println("== prefix maxima for " + Arrays.toString(example));
        System.out.println("total " + prefixMax(example, true));

        System.out.println("== two-pointer trace for " + Arrays.toString(example));
        System.out.println("result " + twoPointers(example, true));

        System.out.println("== more inputs");
        int[][] cases = {{4, 2, 0, 3, 2, 5}, {3, 0, 2, 0, 4}, {1, 2, 3, 4}, {4, 3, 2, 1}, {5, 0, 5}, {2}, {}};
        for (int[] c : cases) System.out.println(Arrays.toString(c) + " -> " + twoPointers(c, false));

        Random rnd = new Random(11);             // fixed seed: the same arrays every run
        int agree = 0;
        for (int t = 0; t < 5000; t++) {
            int[] arr = new int[rnd.nextInt(14)];
            for (int k = 0; k < arr.length; k++) arr[k] = rnd.nextInt(8);
            int b = bruteForce(arr);
            if (b == prefixMax(arr, false) && b == twoPointers(arr, false)) agree++;
        }
        System.out.println("random check: " + agree + " / 5000, all three versions agree");
    }
}
