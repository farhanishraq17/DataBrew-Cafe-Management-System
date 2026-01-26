import java.util.*;
import java.io.*;

class RandomUtility {

    public static int randomInt(int min, int max) {
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    public static String randomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random r = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }

        return sb.toString();
    }

    public static List<Integer> randomList(int size) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(randomInt(0, 100));
        }
        return list;
    }

    public static void printList(List<Integer> list) {
        for (Integer i : list) {
            System.out.print(i + " ");
        }
        System.out.println();
    }
}

class DataProcessor {

    public static int sum(List<Integer> list) {
        int total = 0;
        for (int v : list) {
            total += v;
        }
        return total;
    }

    public static double average(List<Integer> list) {
        if (list.isEmpty()) return 0;
        return (double) sum(list) / list.size();
    }

    public static int max(List<Integer> list) {
        int max = Integer.MIN_VALUE;
        for (int v : list) {
            if (v > max) max = v;
        }
        return max;
    }

    public static int min(List<Integer> list) {
        int min = Integer.MAX_VALUE;
        for (int v : list) {
            if (v < min) min = v;
        }
        return min;
    }

    public static List<Integer> sort(List<Integer> list) {
        List<Integer> copy = new ArrayList<>(list);
        Collections.sort(copy);
        return copy;
    }
}

class FileManager {

    public static void writeFile(String file, String content) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(content);
            bw.close();
        } catch (IOException e) {
            System.out.println("Write error: " + e.getMessage());
        }
    }

    public static String readFile(String file) {
        StringBuilder sb = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

            br.close();

        } catch (IOException e) {
            System.out.println("Read error: " + e.getMessage());
        }

        return sb.toString();
    }
}

class Matrix {

    private int rows;
    private int cols;
    private int[][] data;

    public Matrix(int r, int c) {
        rows = r;
        cols = c;
        data = new int[r][c];
    }

    public void randomFill() {
        Random rand = new Random();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = rand.nextInt(100);
            }
        }
    }

    public void print() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.out.print(data[i][j] + "\t");
            }
            System.out.println();
        }
    }

    public Matrix add(Matrix other) {
        Matrix result = new Matrix(rows, cols);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result.data[i][j] = this.data[i][j] + other.data[i][j];
            }
        }

        return result;
    }
}

class User {

    private int id;
    private String name;
    private int age;

    public User(int id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public void print() {
        System.out.println("User ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Age: " + age);
    }
}

class UserDatabase {

    private List<User> users;

    public UserDatabase() {
        users = new ArrayList<>();
    }

    public void addUser(User u) {
        users.add(u);
    }

    public void printUsers() {
        for (User u : users) {
            u.print();
            System.out.println("-----");
        }
    }
}

public class MainProgram {

    public static void main(String[] args) {

        System.out.println("Random Java Program Demo");

        List<Integer> numbers = RandomUtility.randomList(10);
        RandomUtility.printList(numbers);

        System.out.println("Sum: " + DataProcessor.sum(numbers));
        System.out.println("Average: " + DataProcessor.average(numbers));
        System.out.println("Max: " + DataProcessor.max(numbers));
        System.out.println("Min: " + DataProcessor.min(numbers));

        Matrix m1 = new Matrix(3,3);
        Matrix m2 = new Matrix(3,3);

        m1.randomFill();
        m2.randomFill();

        System.out.println("Matrix 1:");
        m1.print();

        System.out.println("Matrix 2:");
        m2.print();

        Matrix result = m1.add(m2);

        System.out.println("Matrix Result:");
        result.print();

        UserDatabase db = new UserDatabase();

        db.addUser(new User(1,"Alice",25));
        db.addUser(new User(2,"Bob",30));
        db.addUser(new User(3,"Charlie",28));

        db.printUsers();

        String randomText = RandomUtility.randomString(20);
        FileManager.writeFile("random.txt", randomText);

        String content = FileManager.readFile("random.txt");
        System.out.println("File Content:");
        System.out.println(content);
    }
}


class MathUtils {

    public static long factorial(int n) {
        long result = 1;
        for(int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    public static int gcd(int a, int b) {
        while(b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    public static int lcm(int a, int b) {
        return (a * b) / gcd(a, b);
    }

    public static boolean isPrime(int n) {
        if(n <= 1) return false;

        for(int i = 2; i <= Math.sqrt(n); i++) {
            if(n % i == 0) return false;
        }
        return true;
    }

    public static List<Integer> generatePrimes(int limit) {
        List<Integer> primes = new ArrayList<>();

        for(int i = 2; i <= limit; i++) {
            if(isPrime(i)) {
                primes.add(i);
            }
        }

        return primes;
    }
}

class StringUtils {

    public static String reverse(String s) {
        return new StringBuilder(s).reverse().toString();
    }

    public static boolean isPalindrome(String s) {
        return s.equals(reverse(s));
    }

    public static int countVowels(String s) {
        int count = 0;
        for(char c : s.toLowerCase().toCharArray()) {
            if("aeiou".indexOf(c) != -1) count++;
        }
        return count;
    }

    public static Map<Character, Integer> frequency(String s) {
        Map<Character, Integer> map = new HashMap<>();

        for(char c : s.toCharArray()) {
            map.put(c, map.getOrDefault(c, 0) + 1);
        }

        return map;
    }
}

class RandomWalker {

    private int x;
    private int y;

    public RandomWalker() {
        x = 0;
        y = 0;
    }

    public void step() {
        int direction = RandomUtility.randomInt(0,3);

        switch(direction) {
            case 0: x++; break;
            case 1: x--; break;
            case 2: y++; break;
            case 3: y--; break;
        }
    }

    public void walk(int steps) {
        for(int i=0;i<steps;i++) {
            step();
        }
    }

    public void printPosition() {
        System.out.println("Walker Position: (" + x + "," + y + ")");
    }
}

class Counter {

    private int value;

    public Counter() {
        value = 0;
    }

    public void increment() {
        value++;
    }

    public void decrement() {
        value--;
    }

    public int getValue() {
        return value;
    }
}

class Timer {

    private long start;

    public void start() {
        start = System.currentTimeMillis();
    }

    public long stop() {
        return System.currentTimeMillis() - start;
    }
}

class StackExample {

    private Stack<Integer> stack = new Stack<>();

    public void pushRandom() {
        stack.push(RandomUtility.randomInt(1,100));
    }

    public void popValue() {
        if(!stack.isEmpty()) {
            stack.pop();
        }
    }

    public void printStack() {
        System.out.println(stack);
    }
}

class QueueExample {

    private Queue<String> queue = new LinkedList<>();

    public void addRandom() {
        queue.add(RandomUtility.randomString(5));
    }

    public void remove() {
        queue.poll();
    }

    public void printQueue() {
        System.out.println(queue);
    }
}

class Simulation {

    public void run() {

        RandomWalker walker = new RandomWalker();
        walker.walk(100);
        walker.printPosition();

        List<Integer> primes = MathUtils.generatePrimes(50);
        System.out.println("Primes: " + primes);

        String text = RandomUtility.randomString(10);

        System.out.println("Text: " + text);
        System.out.println("Reverse: " + StringUtils.reverse(text));
        System.out.println("Vowels: " + StringUtils.countVowels(text));
    }
}


class BubbleSort {

    public static void sort(int[] arr) {

        for(int i=0;i<arr.length;i++) {

            for(int j=0;j<arr.length-i-1;j++) {

                if(arr[j] > arr[j+1]) {

                    int temp = arr[j];
                    arr[j] = arr[j+1];
                    arr[j+1] = temp;

                }

            }

        }

    }

}

class SearchAlgorithms {

    public static int linearSearch(int[] arr, int target) {

        for(int i=0;i<arr.length;i++) {

            if(arr[i] == target)
                return i;

        }

        return -1;

    }

    public static int binarySearch(int[] arr, int target) {

        int left = 0;
        int right = arr.length - 1;

        while(left <= right) {

            int mid = (left + right) / 2;

            if(arr[mid] == target)
                return mid;

            if(arr[mid] < target)
                left = mid + 1;
            else
                right = mid - 1;

        }

        return -1;

    }

}

class RandomArrayGenerator {

    public static int[] generate(int size) {

        int[] arr = new int[size];

        for(int i=0;i<size;i++) {

            arr[i] = RandomUtility.randomInt(1,100);

        }

        return arr;

    }

    public static void print(int[] arr) {

        for(int v : arr) {

            System.out.print(v + " ");

        }

        System.out.println();

    }

}

class GraphNode {

    int value;
    List<GraphNode> neighbors;

    public GraphNode(int value) {

        this.value = value;
        neighbors = new ArrayList<>();

    }

    public void connect(GraphNode node) {

        neighbors.add(node);

    }

}

class GraphTraversal {

    public static void dfs(GraphNode node, Set<GraphNode> visited) {

        if(node == null || visited.contains(node))
            return;

        visited.add(node);

        System.out.println(node.value);

        for(GraphNode n : node.neighbors) {

            dfs(n, visited);

        }

    }

}

class RandomGraphBuilder {

    public static GraphNode buildGraph(int nodes) {

        List<GraphNode> list = new ArrayList<>();

        for(int i=0;i<nodes;i++) {

            list.add(new GraphNode(i));

        }

        Random rand = new Random();

        for(GraphNode node : list) {

            int connections = rand.nextInt(3);

            for(int j=0;j<connections;j++) {

                node.connect(list.get(rand.nextInt(nodes)));

            }

        }

        return list.get(0);

    }

}


class Logger {

    public static void log(String message) {

        System.out.println("[LOG] " + message);

    }

}

class Settings {

    private Map<String,String> map = new HashMap<>();

    public void set(String key,String value) {

        map.put(key,value);

    }

    public String get(String key) {

        return map.getOrDefault(key,"");

    }

}

class RandomGame {

    private int score;

    public void playRound() {

        int points = RandomUtility.randomInt(1,50);

        score += points;

        Logger.log("Round points: " + points);

    }

    public void playGame(int rounds) {

        for(int i=0;i<rounds;i++) {

            playRound();

        }

        Logger.log("Final Score: " + score);

    }

}

class Fibonacci {

    public static int recursive(int n) {

        if(n <= 1)
            return n;

        return recursive(n-1) + recursive(n-2);

    }

    public static int iterative(int n) {

        if(n <= 1)
            return n;

        int a = 0;
        int b = 1;

        for(int i=2;i<=n;i++) {

            int temp = a + b;

            a = b;
            b = temp;

        }

        return b;

    }

}

class RandomStatistics {

    public static double variance(List<Integer> list) {

        double avg = DataProcessor.average(list);

        double sum = 0;

        for(int v : list) {

            sum += Math.pow(v - avg,2);

        }

        return sum / list.size();

    }

    public static double stddev(List<Integer> list) {

        return Math.sqrt(variance(list));

    }

}

class ProgramRunner {

    public static void runAll() {

        Simulation sim = new Simulation();
        sim.run();

        RandomGame game = new RandomGame();
        game.playGame(5);

        int[] arr = RandomArrayGenerator.generate(10);

        RandomArrayGenerator.print(arr);

        BubbleSort.sort(arr);

        System.out.println("Sorted:");

        RandomArrayGenerator.print(arr);

        GraphNode root = RandomGraphBuilder.buildGraph(6);

        System.out.println("DFS Traversal:");

        GraphTraversal.dfs(root,new HashSet<>());

        List<Integer> list = RandomUtility.randomList(20);

        System.out.println("StdDev: " + RandomStatistics.stddev(list));

    }

}
class RandomPasswordGenerator {

    private static final String chars =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%";

    public static String generate(int length) {

        Random rand = new Random();
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < length; i++) {

            sb.append(chars.charAt(rand.nextInt(chars.length())));

        }

        return sb.toString();

    }

}

class TemperatureConverter {

    public static double celsiusToFahrenheit(double c) {

        return (c * 9 / 5) + 32;

    }

    public static double fahrenheitToCelsius(double f) {

        return (f - 32) * 5 / 9;

    }

}

class Dice {

    private Random rand = new Random();

    public int roll() {

        return rand.nextInt(6) + 1;

    }

}

class DiceGame {

    private Dice dice = new Dice();
    private int score;

    public void play(int rounds) {

        for(int i = 0; i < rounds; i++) {

            int value = dice.roll();

            System.out.println("Rolled: " + value);

            score += value;

        }

        System.out.println("Total score: " + score);

    }

}

class NumberGuessGame {

    private int secret;

    public NumberGuessGame() {

        secret = RandomUtility.randomInt(1,100);

    }

    public void guess(int number) {

        if(number == secret) {

            System.out.println("Correct!");

        } else if(number < secret) {

            System.out.println("Too low");

        } else {

            System.out.println("Too high");

        }

    }

}

class RandomGrid {

    private int rows;
    private int cols;
    private int[][] grid;

    public RandomGrid(int r,int c) {

        rows = r;
        cols = c;

        grid = new int[r][c];

    }

    public void fillRandom() {

        Random rand = new Random();

        for(int i=0;i<rows;i++) {

            for(int j=0;j<cols;j++) {

                grid[i][j] = rand.nextInt(10);

            }

        }

    }

    public void print() {

        for(int i=0;i<rows;i++) {

            for(int j=0;j<cols;j++) {

                System.out.print(grid[i][j] + " ");

            }

            System.out.println();

        }

    }

}

class StatisticsCalculator {

    public static int sum(int[] arr) {

        int total = 0;

        for(int v : arr) {

            total += v;

        }

        return total;

    }

    public static double average(int[] arr) {

        return (double) sum(arr) / arr.length;

    }

}

class RandomNameGenerator {

    private static String[] first = {
            "Alex","Jordan","Taylor","Morgan","Sam","Jamie"
    };

    private static String[] last = {
            "Smith","Lee","Brown","Johnson","Garcia","Davis"
    };

    public static String generate() {

        Random rand = new Random();

        return first[rand.nextInt(first.length)]
                + " " +
                last[rand.nextInt(last.length)];

    }

}

class RandomDataPrinter {

    public static void printDemo() {

        System.out.println("Random name: " + RandomNameGenerator.generate());

        System.out.println("Password: " + RandomPasswordGenerator.generate(10));

        double c = RandomUtility.randomInt(0,30);

        System.out.println("Celsius: " + c);

        System.out.println("Fahrenheit: " +
                TemperatureConverter.celsiusToFahrenheit(c));

        DiceGame game = new DiceGame();

        game.play(5);

        RandomGrid grid = new RandomGrid(5,5);

        grid.fillRandom();

        System.out.println("Random grid:");

        grid.print();

        int[] arr = RandomArrayGenerator.generate(10);

        System.out.println("Average: " +
                StatisticsCalculator.average(arr));

    }

}