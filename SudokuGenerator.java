// SudokuGenerator.java
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SudokuGenerator {
    @Parameter(names = "--generate")
    private String generate;
    @Parameter(names = "--solve")
    private String solve;
    @Parameter(names = "--export-json")
    private String exportJson;
    @Parameter(names = "--export-csv")
    private String exportCsv;
    @Parameter(names = "--export-txt")
    private String exportTxt;
    @Parameter(names = "--import")
    private String importFile;
    @Parameter(names = "--print")
    private boolean print;

    private int[][] grid = new int[9][9];
    private Random rand = new Random();

    private boolean isValid(int row, int col, int num) {
        for (int i = 0; i < 9; i++) {
            if (grid[row][i] == num || grid[i][col] == num) return false;
        }
        int startRow = (row / 3) * 3;
        int startCol = (col / 3) * 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (grid[startRow + i][startCol + j] == num) return false;
            }
        }
        return true;
    }

    private boolean solve() {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (grid[row][col] == 0) {
                    for (int num = 1; num <= 9; num++) {
                        if (isValid(row, col, num)) {
                            grid[row][col] = num;
                            if (solve()) return true;
                            grid[row][col] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private int countSolutions(int limit) {
        int count = 0;
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (grid[row][col] == 0) {
                    for (int num = 1; num <= 9; num++) {
                        if (isValid(row, col, num)) {
                            grid[row][col] = num;
                            count += countSolutions(limit);
                            grid[row][col] = 0;
                            if (count >= limit) return count;
                        }
                    }
                    return count;
                }
            }
        }
        return 1;
    }

    private int[][] generate(String difficulty) {
        fillDiagonalBlocks();
        solve();
        int[][] solution = new int[9][9];
        for (int i = 0; i < 9; i++) {
            System.arraycopy(grid[i], 0, solution[i], 0, 9);
        }
        int cellsToRemove = switch (difficulty) {
            case "easy" -> 30;
            case "medium" -> 40;
            case "hard" -> 50;
            case "expert" -> 55;
            default -> 40;
        };
        List<int[]> positions = new ArrayList<>();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                positions.add(new int[]{r, c});
            }
        }
        Collections.shuffle(positions, rand);
        int removed = 0;
        for (int[] pos : positions) {
            if (removed >= cellsToRemove) break;
            int r = pos[0], c = pos[1];
            int backup = grid[r][c];
            grid[r][c] = 0;
            SudokuGenerator copy = new SudokuGenerator();
            copy.grid = copyGrid();
            if (copy.countSolutions(2) == 1) {
                removed++;
            } else {
                grid[r][c] = backup;
            }
        }
        return solution;
    }

    private int[][] copyGrid() {
        int[][] copy = new int[9][9];
        for (int i = 0; i < 9; i++) {
            System.arraycopy(grid[i], 0, copy[i], 0, 9);
        }
        return copy;
    }

    private void fillDiagonalBlocks() {
        for (int block = 0; block < 9; block += 3) {
            List<Integer> nums = new ArrayList<>();
            for (int i = 1; i <= 9; i++) nums.add(i);
            Collections.shuffle(nums, rand);
            int idx = 0;
            for (int i = block; i < block + 3; i++) {
                for (int j = block; j < block + 3; j++) {
                    grid[i][j] = nums.get(idx++);
                }
            }
        }
    }

    private void printGrid(boolean color) {
        String reset = color ? "\u001B[0m" : "";
        String blue = color ? "\u001B[34m" : "";
        String white = color ? "\u001B[37m" : "";
        for (int i = 0; i < 9; i++) {
            StringBuilder line = new StringBuilder();
            for (int j = 0; j < 9; j++) {
                int val = grid[i][j];
                String ch = val == 0 ? "." : String.valueOf(val);
                line.append(val == 0 ? blue : white).append(ch).append(reset).append(" ");
                if (j == 2 || j == 5) line.append("| ");
            }
            System.out.println(line);
            if (i == 2 || i == 5) System.out.println("------+-------+------");
        }
    }

    private void exportJson(String filename) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.write(Paths.get(filename), gson.toJson(grid).getBytes());
    }

    private void exportCsv(String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    if (j > 0) pw.print(",");
                    pw.print(grid[i][j]);
                }
                pw.println();
            }
        }
    }

    private void exportTxt(String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    if (j > 0) pw.print(" ");
                    pw.print(grid[i][j]);
                }
                pw.println();
            }
        }
    }

    private void loadGrid(String filename) throws IOException {
        if (filename.endsWith(".json")) {
            String json = new String(Files.readAllBytes(Paths.get(filename)));
            Gson gson = new Gson();
            int[][] loaded = gson.fromJson(json, int[][].class);
            for (int i = 0; i < 9 && i < loaded.length; i++) {
                for (int j = 0; j < 9 && j < loaded[i].length; j++) {
                    grid[i][j] = loaded[i][j];
                }
            }
        } else {
            List<String> lines = Files.readAllLines(Paths.get(filename));
            for (int i = 0; i < 9 && i < lines.size(); i++) {
                String[] parts = lines.get(i).trim().split("\\s+");
                for (int j = 0; j < 9 && j < parts.length; j++) {
                    grid[i][j] = Integer.parseInt(parts[j]);
                }
            }
        }
    }

    public void run() throws Exception {
        if (generate != null) {
            int[][] solution = generate(generate);
            if (print) {
                System.out.println("Generated " + generate + " puzzle:");
                printGrid(true);
            }
            if (exportJson != null) {
                JsonObject obj = new JsonObject();
                JsonArray puzzleArray = new Gson().toJsonTree(grid).getAsJsonArray();
                JsonArray solutionArray = new Gson().toJsonTree(solution).getAsJsonArray();
                obj.add("puzzle", puzzleArray);
                obj.add("solution", solutionArray);
                Files.write(Paths.get(exportJson), new GsonBuilder().setPrettyPrinting().create().toJson(obj).getBytes());
                System.out.println("Exported to " + exportJson);
            }
            if (exportCsv != null) exportCsv(exportCsv);
            if (exportTxt != null) exportTxt(exportTxt);
        } else if (solve != null) {
            loadGrid(solve);
            if (solve()) {
                if (print) {
                    System.out.println("Solved puzzle:");
                    printGrid(true);
                }
                if (exportJson != null) {
                    exportJson(exportJson);
                    System.out.println("Exported to " + exportJson);
                }
            } else {
                System.out.println("No solution found.");
            }
        } else if (importFile != null) {
            loadGrid(importFile);
            if (print) printGrid(true);
        } else {
            System.out.println("Use --help for usage.");
        }
    }

    public static void main(String[] args) throws Exception {
        SudokuGenerator gen = new SudokuGenerator();
        JCommander.newBuilder().addObject(gen).build().parse(args);
        gen.run();
    }
}
