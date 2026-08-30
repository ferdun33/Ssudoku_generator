
#!/usr/bin/env python3
# sudoku_generator.py
import argparse
import json
import csv
import sys
import random
import math
from colorama import init, Fore, Style

init(autoreset=True)

class Sudoku:
    def __init__(self, grid=None):
        self.size = 9
        self.box_size = 3
        self.grid = grid if grid else [[0 for _ in range(9)] for _ in range(9)]

    def copy(self):
        return Sudoku([row[:] for row in self.grid])

    def is_valid(self, row, col, num):
        # Проверка строки и столбца
        for i in range(9):
            if self.grid[row][i] == num or self.grid[i][col] == num:
                return False
        # Проверка блока 3x3
        start_row, start_col = (row // 3) * 3, (col // 3) * 3
        for i in range(3):
            for j in range(3):
                if self.grid[start_row + i][start_col + j] == num:
                    return False
        return True

    def solve(self):
        for row in range(9):
            for col in range(9):
                if self.grid[row][col] == 0:
                    for num in range(1, 10):
                        if self.is_valid(row, col, num):
                            self.grid[row][col] = num
                            if self.solve():
                                return True
                            self.grid[row][col] = 0
                    return False
        return True

    def count_solutions(self, limit=2):
        count = 0
        for row in range(9):
            for col in range(9):
                if self.grid[row][col] == 0:
                    for num in range(1, 10):
                        if self.is_valid(row, col, num):
                            self.grid[row][col] = num
                            count += self.count_solutions(limit)
                            self.grid[row][col] = 0
                            if count >= limit:
                                return count
                    return count
        return 1

    def generate(self, difficulty='medium'):
        # Заполняем диагональные блоки
        self._fill_diagonal_blocks()
        self.solve()
        # Удаляем ячейки в зависимости от сложности
        cells_to_remove = {
            'easy': 30,
            'medium': 40,
            'hard': 50,
            'expert': 55
        }.get(difficulty, 40)
        
        solution = [row[:] for row in self.grid]
        positions = [(r, c) for r in range(9) for c in range(9)]
        random.shuffle(positions)
        
        removed = 0
        for r, c in positions:
            if removed >= cells_to_remove:
                break
            backup = self.grid[r][c]
            self.grid[r][c] = 0
            # Проверяем уникальность решения
            copy = self.copy()
            if copy.count_solutions(2) == 1:
                removed += 1
            else:
                self.grid[r][c] = backup
        return solution

    def _fill_diagonal_blocks(self):
        for block in range(0, 9, 3):
            numbers = list(range(1, 10))
            random.shuffle(numbers)
            idx = 0
            for i in range(block, block + 3):
                for j in range(block, block + 3):
                    self.grid[i][j] = numbers[idx]
                    idx += 1

    def is_complete(self):
        for row in self.grid:
            if 0 in row:
                return False
        return True

    def to_json(self):
        return self.grid

    @classmethod
    def from_json(cls, data):
        return cls(data)

    def print_grid(self, color=True):
        if color:
            for i, row in enumerate(self.grid):
                line = ''
                for j, val in enumerate(row):
                    if val == 0:
                        line += Fore.BLUE + '. ' + Style.RESET_ALL
                    else:
                        line += Fore.WHITE + str(val) + ' ' + Style.RESET_ALL
                    if j == 2 or j == 5:
                        line += '| '
                print(line)
                if i == 2 or i == 5:
                    print('------+-------+------')
        else:
            for i, row in enumerate(self.grid):
                line = ' '.join(str(v) if v != 0 else '.' for v in row)
                if i == 2 or i == 5:
                    print('------+-------+------')
                print(line)

    def export_txt(self, filename):
        with open(filename, 'w') as f:
            for row in self.grid:
                f.write(' '.join(str(v) if v != 0 else '0' for v in row) + '\n')

    def export_csv(self, filename):
        with open(filename, 'w', newline='') as f:
            writer = csv.writer(f)
            for row in self.grid:
                writer.writerow(row)

def main():
    parser = argparse.ArgumentParser(description="Sudoku Generator")
    parser.add_argument("--generate", choices=['easy', 'medium', 'hard', 'expert'], help="Generate puzzle")
    parser.add_argument("--solve", help="Solve puzzle from file")
    parser.add_argument("--export-json", help="Export to JSON")
    parser.add_argument("--export-csv", help="Export to CSV")
    parser.add_argument("--export-txt", help="Export to TXT")
    parser.add_argument("--import", dest="import_file", help="Import puzzle from file")
    parser.add_argument("--print", action="store_true", help="Print the grid")
    args = parser.parse_args()

    sudoku = None

    if args.generate:
        s = Sudoku()
        solution = s.generate(args.generate)
        sudoku = s
        if args.print:
            print(f"Generated {args.generate} puzzle:")
            sudoku.print_grid()
        if args.export_json:
            with open(args.export_json, 'w') as f:
                json.dump({"puzzle": sudoku.grid, "solution": solution}, f)
            print(f"Exported to {args.export_json}")
        if args.export_csv:
            sudoku.export_csv(args.export_csv)
        if args.export_txt:
            sudoku.export_txt(args.export_txt)

    elif args.solve:
        # Загрузка из файла
        if args.solve.endswith('.json'):
            with open(args.solve, 'r') as f:
                data = json.load(f)
                grid = data.get('puzzle', data)
                sudoku = Sudoku(grid)
        else:
            sudoku = Sudoku.from_txt(args.solve)
        if sudoku.solve():
            if args.print:
                print("Solved puzzle:")
                sudoku.print_grid()
            if args.export_json:
                with open(args.export_json, 'w') as f:
                    json.dump(sudoku.grid, f)
                print(f"Exported to {args.export_json}")
        else:
            print("No solution found.")

    elif args.import_file:
        if args.import_file.endswith('.json'):
            with open(args.import_file, 'r') as f:
                data = json.load(f)
                grid = data.get('puzzle', data)
                sudoku = Sudoku(grid)
        else:
            sudoku = Sudoku.from_txt(args.import_file)
        if args.print and sudoku:
            sudoku.print_grid()

    else:
        parser.print_help()

if __name__ == "__main__":
    main()
