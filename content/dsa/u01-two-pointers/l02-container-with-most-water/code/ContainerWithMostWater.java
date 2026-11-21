import java.util.*;

/**
 * DSA 1.2: Container With Most Water. Start with the widest container, then always discard the shorter wall.
 * Every line this prints is quoted in the chapter, and the playground's trace is tested against it.
 * Run it: java ContainerWithMostWater.java
 */
public class ContainerWithMostWater {

    /** O(n) time, O(1) space. */
    static int maxArea(int[] height, boolean trace) {
        int left = 0;
        int right = height.length - 1;
        int best = 0;
        while (left < right) {
            int h = Math.min(height[left], height[right]);
            int area = h * (right - left);
            best = Math.max(best, area);           // record BEFORE moving, so this container is never lost
            String move = height[left] < height[right] ? "left++ (left is shorter)"
                    : height[left] > height[right] ? "right-- (right is shorter)" : "right-- (tie)";
            if (trace) {
                System.out.println("  L=" + left + "(" + height[left] + ") R=" + right + "(" + height[right] + ") level="
                        + h + " width=" + (right - left) + " area=" + area + " best=" + best + " -> " + move);
            }
            if (height[left] < height[right]) left++;
            else right--;
        }
        return best;
    }

    /** O(n^2) referee: every pair. */
    static int bruteForce(int[] height) {
        int best = 0;
        for (int i = 0; i < height.length; i++)
            for (int j = i + 1; j < height.length; j++)
                best = Math.max(best, Math.min(height[i], height[j]) * (j - i));
        return best;
    }

    public static void main(String[] args) {
        int[] example = {1, 8, 6, 2, 5, 4, 8, 3, 7};
        System.out.println("== trace for " + Arrays.toString(example));
        System.out.println("result " + maxArea(example, true));

        System.out.println("== more inputs");
        int[][] cases = {{1, 1}, {4, 3, 2, 1, 4}, {2, 2, 2}, {1, 2, 4, 3}, {7}, {}};
        for (int[] c : cases) System.out.println(Arrays.toString(c) + " -> " + maxArea(c, false));

        // Why "always move left" is wrong: the best pair is kept only by moving the shorter wall.
        int[] trap = {5, 1, 1, 1, 1, 9, 4};
        System.out.println("always moving left on " + Arrays.toString(trap) + " -> " + alwaysMoveLeft(trap)
                + " (correct " + maxArea(trap, false) + ")");

        Random rnd = new Random(7);              // fixed seed: the same arrays every run
        int agree = 0;
        for (int t = 0; t < 5000; t++) {
            int[] arr = new int[rnd.nextInt(12)];
            for (int k = 0; k < arr.length; k++) arr[k] = rnd.nextInt(10);
            if (maxArea(arr, false) == bruteForce(arr)) agree++;
        }
        System.out.println("random check: " + agree + " / 5000 match brute force");
    }

    /** A wrong strategy, kept to show why the shorter wall must be the one that moves. */
    static int alwaysMoveLeft(int[] height) {
        int left = 0, right = height.length - 1, best = 0;
        while (left < right) {
            best = Math.max(best, Math.min(height[left], height[right]) * (right - left));
            left++;
        }
        return best;
    }
}
