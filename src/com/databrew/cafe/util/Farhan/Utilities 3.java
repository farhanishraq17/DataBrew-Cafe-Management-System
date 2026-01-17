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

    /**
     * Sorts an array using selection sort.
     */
    public static void selectionSort(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (arr[j] < arr[minIdx]) {
                    minIdx = j;
                }
            }
            if (minIdx != i) {
                int temp = arr[i];
                arr[i] = arr[minIdx];
                arr[minIdx] = temp;
            }
        }
    }

    /**
     * Sorts an array using insertion sort.
     */
    public static void insertionSort(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        for (int i = 1; i < arr.length; i++) {
            int key = arr[i];
            int j = i - 1;
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }

    /**
     * Sorts an array using merge sort.
     */
    public static void mergeSort(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        mergeSortHelper(arr, 0, arr.length - 1);
    }

    private static void mergeSortHelper(int[] arr, int left, int right) {
        if (left < right) {
            int mid = left + (right - left) / 2;
            mergeSortHelper(arr, left, mid);
            mergeSortHelper(arr, mid + 1, right);
            merge(arr, left, mid, right);
        }
    }

    private static void merge(int[] arr, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;
        int[] leftArr = new int[n1];
        int[] rightArr = new int[n2];
        System.arraycopy(arr, left, leftArr, 0, n1);
        System.arraycopy(arr, mid + 1, rightArr, 0, n2);
        int i = 0, j = 0, k = left;
        while (i < n1 && j < n2) {
            if (leftArr[i] <= rightArr[j]) {
                arr[k++] = leftArr[i++];
            } else {
                arr[k++] = rightArr[j++];
            }
        }
        while (i < n1) {
            arr[k++] = leftArr[i++];
        }
        while (j < n2) {
            arr[k++] = rightArr[j++];
        }
    }

    /**
     * Sorts an array using quick sort.
     */
    public static void quickSort(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        quickSortHelper(arr, 0, arr.length - 1);
    }

    private static void quickSortHelper(int[] arr, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(arr, low, high);
            quickSortHelper(arr, low, pivotIndex - 1);
            quickSortHelper(arr, pivotIndex + 1, high);
        }
    }

    private static int partition(int[] arr, int low, int high) {
        int pivot = arr[high];
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (arr[j] <= pivot) {
                i++;
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        int temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        return i + 1;
    }

    // ========================================================================
    // Search Algorithms
    // ========================================================================

    /**
     * Performs binary search on a sorted array. Returns index or -1.
     */
    public static int binarySearch(int[] arr, int target) {
        if (arr == null) {
            return -1;
        }
        int left = 0;
        int right = arr.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (arr[mid] == target) {
                return mid;
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return -1;
    }

    /**
     * Performs linear search. Returns index or -1.
     */
    public static int linearSearch(int[] arr, int target) {
        if (arr == null) {
            return -1;
        }
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == target) {
                return i;
            }
        }
        return -1;
    }

    // ========================================================================
    // Collection Utilities
    // ========================================================================

    /**
     * Groups elements of a list by a classifier function.
     */
    public static <T, K> Map<K, List<T>> groupBy(List<T> list, Function<T, K> classifier) {
        if (list == null) {
            return new HashMap<>();
        }
        return list.stream().collect(Collectors.groupingBy(classifier));
    }

    /**
     * Partitions a list into sublists of a given size.
     */
    public static <T> List<List<T>> chunk(List<T> list, int size) {
        if (list == null || size <= 0) {
            return new ArrayList<>();
        }
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(new ArrayList<>(list.subList(i, Math.min(i + size, list.size()))));
        }
        return chunks;
    }

    /**
     * Returns the intersection of two lists.
     */
    public static <T> List<T> intersection(List<T> a, List<T> b) {
        if (a == null || b == null) {
            return new ArrayList<>();
        }
        Set<T> setB = new HashSet<>(b);
        return a.stream().filter(setB::contains).distinct().collect(Collectors.toList());
    }

    /**
     * Returns the union of two lists (no duplicates).
     */
    public static <T> List<T> union(List<T> a, List<T> b) {
        Set<T> set = new HashSet<>();
        List<T> result = new ArrayList<>();
        if (a != null) {
            for (T item : a) {
                if (set.add(item)) {
                    result.add(item);
                }
            }
        }
        if (b != null) {
            for (T item : b) {
                if (set.add(item)) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    /**
     * Returns the difference of two lists (elements in a but not b).
     */
    public static <T> List<T> difference(List<T> a, List<T> b) {
        if (a == null) {
            return new ArrayList<>();
        }
        Set<T> setB = (b != null) ? new HashSet<>(b) : new HashSet<>();
        return a.stream().filter(item -> !setB.contains(item)).collect(Collectors.toList());
    }

    /**
     * Filters a list using a predicate.
     */
    public static <T> List<T> filter(List<T> list, Predicate<T> predicate) {
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * Zips two lists together into a list of pairs.
     */
    public static <A, B> List<Map.Entry<A, B>> zip(List<A> listA, List<B> listB) {
        List<Map.Entry<A, B>> zipped = new ArrayList<>();
        if (listA == null || listB == null) {
            return zipped;
        }
        int size = Math.min(listA.size(), listB.size());
        for (int i = 0; i < size; i++) {
            zipped.add(Map.entry(listA.get(i), listB.get(i)));
        }
        return zipped;
    }

    /**
     * Shuffles a list in place using Fisher-Yates algorithm.
     */
    public static <T> void shuffle(List<T> list) {
        if (list == null || list.size() <= 1) {
            return;
        }
        for (int i = list.size() - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            T temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
        }
    }

    // ========================================================================
    // Data Structure Helpers
    // ========================================================================

    /**
     * Simple generic Pair class.
     */
    public static class Pair<A, B> {
        private final A first;
        private final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        public A getFirst() {
            return first;
        }

        public B getSecond() {
            return second;
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return java.util.Objects.equals(first, pair.first)
                    && java.util.Objects.equals(second, pair.second);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(first, second);
        }
    }

    /**
     * Simple generic stack implementation using ArrayList.
     */
    public static class SimpleStack<T> {
        private final List<T> data = new ArrayList<>();

        public void push(T item) {
            data.add(item);
        }

        public T pop() {
            if (data.isEmpty()) {
                throw new java.util.NoSuchElementException("Stack is empty");
            }
            return data.remove(data.size() - 1);
        }

        public T peek() {
            if (data.isEmpty()) {
                throw new java.util.NoSuchElementException("Stack is empty");
            }
            return data.get(data.size() - 1);
        }

        public boolean isEmpty() {
            return data.isEmpty();
        }

        public int size() {
            return data.size();
        }
    }

    /**
     * Simple generic queue implementation using LinkedList.
     */
    public static class SimpleQueue<T> {
        private final LinkedList<T> data = new LinkedList<>();

        public void enqueue(T item) {
            data.addLast(item);
        }

        public T dequeue() {
            if (data.isEmpty()) {
                throw new java.util.NoSuchElementException("Queue is empty");
            }
            return data.removeFirst();
        }

        public T peek() {
            if (data.isEmpty()) {
                throw new java.util.NoSuchElementException("Queue is empty");
            }
            return data.getFirst();
        }

        public boolean isEmpty() {
            return data.isEmpty();
        }

        public int size() {
            return data.size();
        }
    }
    // ========================================================================
    // Validation Utilities
    // ========================================================================

    /**
     * Validates an email address format.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String pattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(pattern);
    }

    /**
     * Validates an IPv4 address.
     */
    public static boolean isValidIPv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
                if (part.length() > 1 && part.startsWith("0")) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a string contains only digits.
     */
    public static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        return s.chars().allMatch(Character::isDigit);
    }

    /**
     * Checks if a string is a valid hexadecimal color code.
     */
    public static boolean isValidHexColor(String color) {
        if (color == null) {
            return false;
        }
        return color.matches("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$");
    }

    /**
     * Checks if parentheses/brackets in a string are balanced.
     */
    public static boolean isBalanced(String s) {
        if (s == null) {
            return true;
        }
        Stack<Character> stack = new Stack<>();
        Map<Character, Character> matching = Map.of(')', '(', ']', '[', '}', '{');
        for (char c : s.toCharArray()) {
            if ("([{".indexOf(c) >= 0) {
                stack.push(c);
            } else if (matching.containsKey(c)) {
                if (stack.isEmpty() || stack.pop() != matching.get(c)) {
                    return false;
                }
            }
        }
        return stack.isEmpty();
    }

    /** Entry point for quick demo. */
    public static void main(String[] args) {
        System.out.println("Utilities library loaded successfully.");
    }
}
