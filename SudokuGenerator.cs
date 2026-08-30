// SudokuGenerator.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace SudokuGenerator
{
    class Program
    {
        static void Main(string[] args)
        {
            var opts = ParseArgs(args);
            var gen = new SudokuGenerator();
            if (opts.Generate != null)
            {
                var solution = gen.Generate(opts.Generate);
                if (opts.Print)
                {
                    Console.WriteLine($"Generated {opts.Generate} puzzle:");
                    gen.Print();
                }
                if (opts.ExportJson != null)
                {
                    var data = new { puzzle = gen.Grid, solution };
                    string json = JsonSerializer.Serialize(data, new JsonSerializerOptions { WriteIndented = true });
                    File.WriteAllText(opts.ExportJson, json);
                    Console.WriteLine($"Exported to {opts.ExportJson}");
                }
                if (opts.ExportCsv != null) gen.ExportCsv(opts.ExportCsv);
                if (opts.ExportTxt != null) gen.ExportTxt(opts.ExportTxt);
            }
            else if (opts.Solve != null)
            {
                gen.Load(opts.Solve);
                if (gen.Solve())
                {
                    if (opts.Print)
                    {
                        Console.WriteLine("Solved puzzle:");
                        gen.Print();
                    }
                    if (opts.ExportJson != null)
                    {
                        gen.ExportJson(opts.ExportJson);
                        Console.WriteLine($"Exported to {opts.ExportJson}");
                    }
                }
                else
                {
                    Console.WriteLine("No solution found.");
                }
            }
            else if (opts.Import != null)
            {
                gen.Load(opts.Import);
                if (opts.Print) gen.Print();
            }
            else
            {
                Console.WriteLine("Use --help for usage.");
            }
        }

        static Options ParseArgs(string[] args)
        {
            var opts = new Options();
            for (int i = 0; i < args.Length; i++)
            {
                switch (args[i])
                {
                    case "--generate": opts.Generate = args[++i]; break;
                    case "--solve": opts.Solve = args[++i]; break;
                    case "--export-json": opts.ExportJson = args[++i]; break;
                    case "--export-csv": opts.ExportCsv = args[++i]; break;
                    case "--export-txt": opts.ExportTxt = args[++i]; break;
                    case "--import": opts.Import = args[++i]; break;
                    case "--print": opts.Print = true; break;
                }
            }
            return opts;
        }

        class Options
        {
            public string Generate { get; set; }
            public string Solve { get; set; }
            public string ExportJson { get; set; }
            public string ExportCsv { get; set; }
            public string ExportTxt { get; set; }
            public string Import { get; set; }
            public bool Print { get; set; }
        }

        class SudokuGenerator
        {
            private int[,] grid = new int[9, 9];
            private Random rand = new Random();
            public int[,] Grid => grid;

            public bool IsValid(int row, int col, int num)
            {
                for (int i = 0; i < 9; i++)
                    if (grid[row, i] == num || grid[i, col] == num) return false;
                int startRow = (row / 3) * 3;
                int startCol = (col / 3) * 3;
                for (int i = 0; i < 3; i++)
                    for (int j = 0; j < 3; j++)
                        if (grid[startRow + i, startCol + j] == num) return false;
                return true;
            }

            public bool Solve()
            {
                for (int row = 0; row < 9; row++)
                {
                    for (int col = 0; col < 9; col++)
                    {
                        if (grid[row, col] == 0)
                        {
                            for (int num = 1; num <= 9; num++)
                            {
                                if (IsValid(row, col, num))
                                {
                                    grid[row, col] = num;
                                    if (Solve()) return true;
                                    grid[row, col] = 0;
                                }
                            }
                            return false;
                        }
                    }
                }
                return true;
            }

            public int CountSolutions(int limit)
            {
                int count = 0;
                for (int row = 0; row < 9; row++)
                {
                    for (int col = 0; col < 9; col++)
                    {
                        if (grid[row, col] == 0)
                        {
                            for (int num = 1; num <= 9; num++)
                            {
                                if (IsValid(row, col, num))
                                {
                                    grid[row, col] = num;
                                    count += CountSolutions(limit);
                                    grid[row, col] = 0;
                                    if (count >= limit) return count;
                                }
                            }
                            return count;
                        }
                    }
                }
                return 1;
            }

            public int[,] Generate(string difficulty)
            {
                FillDiagonalBlocks();
                Solve();
                int[,] solution = (int[,])grid.Clone();
                int cellsToRemove = difficulty switch
                {
                    "easy" => 30,
                    "medium" => 40,
                    "hard" => 50,
                    "expert" => 55,
                    _ => 40
                };
                var positions = new List<(int, int)>();
                for (int r = 0; r < 9; r++)
                    for (int c = 0; c < 9; c++)
                        positions.Add((r, c));
                positions = positions.OrderBy(_ => rand.Next()).ToList();
                int removed = 0;
                foreach (var (r, c) in positions)
                {
                    if (removed >= cellsToRemove) break;
                    int backup = grid[r, c];
                    grid[r, c] = 0;
                    var copy = Copy();
                    if (copy.CountSolutions(2) == 1)
                    {
                        removed++;
                    }
                    else
                    {
                        grid[r, c] = backup;
                    }
                }
                return solution;
            }

            private SudokuGenerator Copy()
            {
                var copy = new SudokuGenerator();
                for (int i = 0; i < 9; i++)
                    for (int j = 0; j < 9; j++)
                        copy.grid[i, j] = grid[i, j];
                return copy;
            }

            private void FillDiagonalBlocks()
            {
                for (int block = 0; block < 9; block += 3)
                {
                    var nums = Enumerable.Range(1, 9).ToList();
                    nums = nums.OrderBy(_ => rand.Next()).ToList();
                    int idx = 0;
                    for (int i = block; i < block + 3; i++)
                        for (int j = block; j < block + 3; j++)
                            grid[i, j] = nums[idx++];
                }
            }

            public void Print(bool color = true)
            {
                string reset = color ? "\u001B[0m" : "";
                string blue = color ? "\u001B[34m" : "";
                string white = color ? "\u001B[37m" : "";
                for (int i = 0; i < 9; i++)
                {
                    string line = "";
                    for (int j = 0; j < 9; j++)
                    {
                        int val = grid[i, j];
                        string ch = val == 0 ? "." : val.ToString();
                        line += (val == 0 ? blue : white) + ch + reset + " ";
                        if (j == 2 || j == 5) line += "| ";
                    }
                    Console.WriteLine(line);
                    if (i == 2 || i == 5) Console.WriteLine("------+-------+------");
                }
            }

            public void ExportJson(string filename)
            {
                string json = JsonSerializer.Serialize(grid, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(filename, json);
            }

            public void ExportCsv(string filename)
            {
                using var sw = new StreamWriter(filename);
                for (int i = 0; i < 9; i++)
                {
                    for (int j = 0; j < 9; j++)
                    {
                        if (j > 0) sw.Write(",");
                        sw.Write(grid[i, j]);
                    }
                    sw.WriteLine();
                }
            }

            public void ExportTxt(string filename)
            {
                using var sw = new StreamWriter(filename);
                for (int i = 0; i < 9; i++)
                {
                    for (int j = 0; j < 9; j++)
                    {
                        if (j > 0) sw.Write(" ");
                        sw.Write(grid[i, j]);
                    }
                    sw.WriteLine();
                }
            }

            public void Load(string filename)
            {
                string content = File.ReadAllText(filename);
                if (filename.EndsWith(".json"))
                {
                    var data = JsonSerializer.Deserialize<int[][]>(content);
                    for (int i = 0; i < 9 && i < data.Length; i++)
                        for (int j = 0; j < 9 && j < data[i].Length; j++)
                            grid[i, j] = data[i][j];
                }
                else
                {
                    var lines = content.Split('\n');
                    for (int i = 0; i < 9 && i < lines.Length; i++)
                    {
                        var parts = lines[i].Trim().Split(new[] { ' ', ',' }, StringSplitOptions.RemoveEmptyEntries);
                        for (int j = 0; j < 9 && j < parts.Length; j++)
                            grid[i, j] = int.Parse(parts[j]);
                    }
                }
            }
        }
    }
}
