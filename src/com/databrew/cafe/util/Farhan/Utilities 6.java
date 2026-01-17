import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utilities.java — A comprehensive Java utility library.
 * Contains helper methods for arrays, strings, math, collections,
 * sorting algorithms, data structures, and more.
 *
 * @author Claude
 * @version 1.0
 */
public class Utilities {

    private static final Random RANDOM = new Random();

    // ========================================================================
    // Array Utilities
    // ========================================================================

    /**
     * Reverses an integer array in place.
     */
    public static void reverseArray(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        int left = 0;
        int right = arr.length - 1;
        while (left < right) {
            int temp = arr[left];
            arr[left] = arr[right];
            arr[right] = temp;
            left++;
            right--;
        }
    }

    /**
     * Returns the sum of all elements in an integer array.
     */
    public static long sumArray(int[] arr) {
        if (arr == null) {
            return 0;
        }
        long sum = 0;
        for (int value : arr) {
            sum += value;
        }
        return sum;
    }

    /**
     * Returns the maximum value in an integer array.
     */
    public static int maxArray(int[] arr) {
        if (arr == null || arr.length == 0) {
            throw new IllegalArgumentException("Array must not be null or empty");
        }
        int max = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
            }
        }
        return max;
    }

    /**
     * Returns the minimum value in an integer array.
     */
    public static int minArray(int[] arr) {
        if (arr == null || arr.length == 0) {
            throw new IllegalArgumentException("Array must not be null or empty");
        }
        int min = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
            }
        }
        return min;
    }

    /**
     * Returns the average of an integer array as a double.
     */
    public static double averageArray(int[] arr) {
        if (arr == null || arr.length == 0) {
            throw new IllegalArgumentException("Array must not be null or empty");
        }
        return (double) sumArray(arr) / arr.length;
    }

    /**
     * Removes duplicate values from an integer array.
     */
    public static int[] removeDuplicates(int[] arr) {
        if (arr == null) {
            return new int[0];
        }
        Set<Integer> seen = new HashSet<>();
        List<Integer> result = new ArrayList<>();
        for (int value : arr) {
            if (seen.add(value)) {
                result.add(value);
            }
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Flattens a 2D integer array into a 1D array.
     */
    public static int[] flatten2D(int[][] matrix) {
        if (matrix == null) {
            return new int[0];
        }
        List<Integer> flat = new ArrayList<>();
        for (int[] row : matrix) {
            if (row != null) {
                for (int val : row) {
                    flat.add(val);
                }
            }
        }
        return flat.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Rotates an array to the right by k positions.
     */
    public static void rotateRight(int[] arr, int k) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        k = k % arr.length;
        if (k < 0) {
            k += arr.length;
        }
        reverse(arr, 0, arr.length - 1);
        reverse(arr, 0, k - 1);
        reverse(arr, k, arr.length - 1);
    }

    private static void reverse(int[] arr, int start, int end) {
        while (start < end) {
            int temp = arr[start];
            arr[start] = arr[end];
            arr[end] = temp;
            start++;
            end--;
        }
    }

    /**
     * Checks if an array is sorted in ascending order.
     */
    public static boolean isSorted(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return true;
        }
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < arr[i - 1]) {
                return false;
            }
        }
        return true;
    }

    // ========================================================================
    // String Utilities
    // ========================================================================

    /**
     * Reverses a string.
     */
    public static String reverseString(String s) {
        if (s == null) {
            return null;
        }
        return new StringBuilder(s).reverse().toString();
    }

    /**
     * Checks if a string is a palindrome (case-insensitive).
     */
    public static boolean isPalindrome(String s) {
        if (s == null) {
            return false;
        }
        String cleaned = s.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return cleaned.equals(new StringBuilder(cleaned).reverse().toString());
    }

    /**
     * Counts the number of vowels in a string.
     */
    public static int countVowels(String s) {
        if (s == null) {
            return 0;
        }
        int count = 0;
        String vowels = "aeiouAEIOU";
        for (char c : s.toCharArray()) {
            if (vowels.indexOf(c) >= 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * Converts a string to title case.
     */
    public static String toTitleCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    /**
     * Converts a string to camelCase.
     */
    public static String toCamelCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        String[] words = s.split("[\\s_\\-]+");
        StringBuilder result = new StringBuilder(words[0].toLowerCase());
        for (int i = 1; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                result.append(words[i].substring(1).toLowerCase());
            }
        }
        return result.toString();
    }

    /**
     * Converts a string to snake_case.
     */
    public static String toSnakeCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("[\\s\\-]+", "_")
                .toLowerCase();
    }

    /**
     * Truncates a string to a given length with an ellipsis.
     */
    public static String truncate(String s, int maxLength) {
        if (s == null || s.length() <= maxLength) {
            return s;
        }
        if (maxLength <= 3) {
            return s.substring(0, maxLength);
        }
        return s.substring(0, maxLength - 3) + "...";
    }

    /**
     * Counts word occurrences in a string.
     */
    public static Map<String, Integer> wordFrequency(String s) {
        Map<String, Integer> freq = new HashMap<>();
        if (s == null || s.isEmpty()) {
            return freq;
        }
        String[] words = s.toLowerCase().split("\\W+");
        for (String word : words) {
            if (!word.isEmpty()) {
                freq.merge(word, 1, Integer::sum);
            }
        }
        return freq;
    }

    /**
     * Checks if two strings are anagrams.
     */
    public static boolean areAnagrams(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        char[] charsA = a.replaceAll("\\s", "").toLowerCase().toCharArray();
        char[] charsB = b.replaceAll("\\s", "").toLowerCase().toCharArray();
        Arrays.sort(charsA);
        Arrays.sort(charsB);
        return Arrays.equals(charsA, charsB);
    }

    /**
     * Compresses a string using run-length encoding.
     */
    public static String runLengthEncode(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder encoded = new StringBuilder();
        int count = 1;
        for (int i = 1; i <= s.length(); i++) {
            if (i < s.length() && s.charAt(i) == s.charAt(i - 1)) {
                count++;
            } else {
                encoded.append(s.charAt(i - 1));
                if (count > 1) {
                    encoded.append(count);
                }
                count = 1;
            }
        }
        return encoded.toString();
    }

    // ========================================================================
    // Math Utilities
    // ========================================================================

    /**
     * Computes the greatest common divisor of two numbers.
     */
    public static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    /**
     * Computes the least common multiple of two numbers.
     */
    public static long lcm(int a, int b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        return Math.abs((long) a * b) / gcd(a, b);
    }

    /**
     * Checks if a number is prime.
     */
    public static boolean isPrime(int n) {
        if (n < 2) {
            return false;
        }
        if (n < 4) {
            return true;
        }
        if (n % 2 == 0 || n % 3 == 0) {
            return false;
        }
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a list of prime numbers up to a given limit using the Sieve of Eratosthenes.
     */
    public static List<Integer> sieveOfEratosthenes(int limit) {
        boolean[] isComposite = new boolean[limit + 1];
        List<Integer> primes = new ArrayList<>();
        for (int i = 2; i <= limit; i++) {
            if (!isComposite[i]) {
                primes.add(i);
                for (long j = (long) i * i; j <= limit; j += i) {
                    isComposite[(int) j] = true;
                }
            }
        }
        return primes;
    }

    /**
     * Computes the nth Fibonacci number iteratively.
     */
    public static long fibonacci(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }
        if (n <= 1) {
            return n;
        }
        long prev = 0;
        long curr = 1;
        for (int i = 2; i <= n; i++) {
            long next = prev + curr;
            prev = curr;
            curr = next;
        }
        return curr;
    }

    /**
     * Computes the factorial of n.
     */
    public static long factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    /**
     * Computes n choose k (binomial coefficient).
     */
    public static long binomialCoefficient(int n, int k) {
        if (k < 0 || k > n) {
            return 0;
        }
        if (k == 0 || k == n) {
            return 1;
        }
        k = Math.min(k, n - k);
        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }

    /**
     * Computes power using fast exponentiation (modular).
     */
    public static long modPow(long base, long exp, long mod) {
        long result = 1;
        base %= mod;
        while (exp > 0) {
            if ((exp & 1) == 1) {
                result = result * base % mod;
            }
            exp >>= 1;
            base = base * base % mod;
        }
        return result;
    }

    /**
     * Clamps a value between a minimum and maximum.
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Linearly interpolates between two values.
     */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Maps a value from one range to another.
     */
    public static double mapRange(double value, double fromMin, double fromMax,
                                   double toMin, double toMax) {
        return (value - fromMin) / (fromMax - fromMin) * (toMax - toMin) + toMin;
    }

    // ========================================================================
    // Sorting Algorithms
    // ========================================================================

    /**
     * Sorts an array using bubble sort.
     */
    public static void bubbleSort(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }
            if (!swapped) {
                break;
            }
        }
    }


}
