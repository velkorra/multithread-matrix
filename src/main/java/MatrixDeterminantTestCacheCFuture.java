import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.*;

public class MatrixDeterminantTestCacheCFuture {
    private static final ConcurrentHashMap<String, Integer> cache = new ConcurrentHashMap<>();

    private static final int N = 19;
    private static final String FILENAME = "matrix" + N + ".txt";

    public static int calculateDeterminant(int[][] matrix) {
        String key = matrixToString(matrix);

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        int n = matrix.length;
        if (n == 1) return matrix[0][0];
        if (n == 2) return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];

        int determinant = 0;
        for (int i = 0; i < n; i++) {
            int[][] minor = getMinor(matrix, 0, i);
            determinant += matrix[0][i] * calculateDeterminant(minor) * (i % 2 == 0 ? 1 : -1);
        }

        cache.put(key, determinant);

        return determinant;
    }

    public static CompletableFuture<Integer> calculateDeterminantWithMaxDepth(
            int[][] matrix, Executor executor, int depth, int maxDepth) {
        String key = matrixToString(matrix);

        if (cache.containsKey(key)) {
            return CompletableFuture.completedFuture(cache.get(key));
        }

        int n = matrix.length;

        if (n == 1) {
            return CompletableFuture.completedFuture(matrix[0][0]);
        }
        if (n == 2) {
            int result = matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
            return CompletableFuture.completedFuture(result);
        }

        if (depth >= maxDepth) {
            int result = calculateDeterminant(matrix);
            cache.put(key, result);
            return CompletableFuture.completedFuture(result);
        }

        CompletableFuture<Integer>[] futures = new CompletableFuture[n];
        for (int i = 0; i < n; i++) {
            final int col = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                int[][] minor = getMinor(matrix, 0, col);
                return calculateDeterminantWithMaxDepth(minor, executor, depth + 1, maxDepth)
                        .thenApply(value -> value * matrix[0][col] * (col % 2 == 0 ? 1 : -1));
            }, executor).thenCompose(f -> f);
        }

        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    int result = Arrays.stream(futures)
                            .mapToInt(f -> {
                                try {
                                    return f.get();
                                } catch (InterruptedException | ExecutionException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .sum();
                    cache.put(key, result);
                    return result;
                });
    }
    private static String matrixToString(int[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (int[] row : matrix) {
            for (int value : row) {
                sb.append(value).append(",");
            }
            sb.append(";");
        }
        return sb.toString();
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

    @Test
    public void testSingleThreadDeterminantWithCache() throws IOException {
        int[][] matrix = MatrixDeterminantTest.readMatrixFromFile(FILENAME, N);

        cache.clear();

        long startTime = System.currentTimeMillis();
        int determinant1 = calculateDeterminant(matrix);
        long endTime = System.currentTimeMillis();
        System.out.println("Однопоточный результат (без кэша): " + determinant1);
        System.out.println("Время выполнения (первый вызов): " + (endTime - startTime) + " мс");

        startTime = System.currentTimeMillis();
        int determinant2 = calculateDeterminant(matrix);
        endTime = System.currentTimeMillis();
        System.out.println("Однопоточный результат (с кэшем): " + determinant2);
        System.out.println("Время выполнения (второй вызов): " + (endTime - startTime) + " мс");

        assert determinant1 == determinant2 : "Ошибка: результаты определителя не совпадают!";
    }


    @Test
    public void testMultiThreadDeterminantWithCompletableFutureAndDepth() throws IOException, ExecutionException, InterruptedException {
        int[][] matrix = MatrixDeterminantTest.readMatrixFromFile(FILENAME, N);

        cache.clear();

        int maxDepth = 4;
        ForkJoinPool executor = new ForkJoinPool();

        long startTime = System.currentTimeMillis();
        CompletableFuture<Integer> determinantFuture = calculateDeterminantWithMaxDepth(matrix, executor, 0, maxDepth);
        int determinant1 = determinantFuture.get();
        long endTime = System.currentTimeMillis();

        System.out.println("Многопоточный результат с CompletableFuture (с глубиной): " + determinant1);
        System.out.println("Время выполнения: " + (endTime - startTime) + " мс");

        executor.shutdown();

        assert determinant1 != 0 : "Ошибка: результат должен быть ненулевым!";
    }
}