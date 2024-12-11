import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Random;
import java.util.concurrent.*;

public class MatrixDeterminantTest {

    private static final int N = 17;
    private static final String FILENAME = "matrix" + N + ".txt";

    @Test
    public void generateMatrix() throws IOException {
        generateMatrixToFile(N, FILENAME);
        System.out.println("Матрица сгенерирована и записана в файл: " + FILENAME);
    }

    @Test
    public void testSingleThreadDeterminant() throws IOException {
        int[][] matrix = readMatrixFromFile(FILENAME, N);

        long startTime = System.currentTimeMillis();
        int determinant = calculateDeterminant(matrix);
        long endTime = System.currentTimeMillis();

        System.out.println("Однопоточный результат: " + determinant);
        System.out.println("Время выполнения (однопоточный): " + (endTime - startTime) + " мс");
    }

    @Test
    public void testMultiThreadDeterminant() throws IOException, ExecutionException, InterruptedException {
        int[][] matrix = readMatrixFromFile(FILENAME, N);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        long startTime = System.currentTimeMillis();
        int determinant = calculateDeterminantParallel(matrix, executor);
        long endTime = System.currentTimeMillis();
        executor.shutdown();

        System.out.println("Многопоточный результат: " + determinant);
        System.out.println("Время выполнения (многопоточный): " + (endTime - startTime) + " мс");
    }

    public static void generateMatrixToFile(int n, String filename) throws IOException {
        Random random = new Random();
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                writer.write((random.nextInt(21) - 10) + " ");
            }
            writer.newLine();
        }
        writer.close();
    }

    public static int[][] readMatrixFromFile(String filename, int n) throws IOException {
        int[][] matrix = new int[n][n];
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        for (int i = 0; i < n; i++) {
            String[] line = reader.readLine().split(" ");
            for (int j = 0; j < n; j++) {
                matrix[i][j] = Integer.parseInt(line[j]);
            }
        }
        reader.close();
        return matrix;
    }

    public static int calculateDeterminant(int[][] matrix) {
        int n = matrix.length;
        if (n == 1) return matrix[0][0];
        if (n == 2) return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];

        int determinant = 0;
        for (int i = 0; i < n; i++) {
            int[][] minor = getMinor(matrix, 0, i);
            determinant += matrix[0][i] * calculateDeterminant(minor) * (i % 2 == 0 ? 1 : -1);
        }
        return determinant;
    }

    public static int calculateDeterminantParallel(int[][] matrix, ExecutorService executor) throws InterruptedException, ExecutionException {
        int n = matrix.length;
        if (n == 1) return matrix[0][0];
        if (n == 2) return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];

        int determinant = 0;
        Future<Integer>[] futures = new Future[n];
        for (int i = 0; i < n; i++) {
            final int col = i;
            futures[i] = executor.submit(() -> {
                int[][] minor = getMinor(matrix, 0, col);
                return matrix[0][col] * calculateDeterminant(minor) * (col % 2 == 0 ? 1 : -1);
            });
        }
        for (Future<Integer> future : futures) {
            determinant += future.get();
        }
        return determinant;
    }

    public static int[][] getMinor(int[][] matrix, int row, int col) {
        int n = matrix.length;
        int[][] minor = new int[n - 1][n - 1];
        int minorRow = 0;
        for (int i = 0; i < n; i++) {
            if (i == row) continue;
            int minorCol = 0;
            for (int j = 0; j < n; j++) {
                if (j == col) continue;
                minor[minorRow][minorCol++] = matrix[i][j];
            }
            minorRow++;
        }
        return minor;
    }
}
