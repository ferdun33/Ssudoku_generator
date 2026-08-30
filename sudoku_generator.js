#!/usr/bin/env node
// sudoku_generator.js
const { program } = require('commander');
const fs = require('fs');
const chalk = require('chalk');

class Sudoku {
    constructor(grid = null) {
        this.size = 9;
        this.boxSize = 3;
        this.grid = grid ? grid.map(row => [...row]) : Array.from({ length: 9 }, () => Array(9).fill(0));
    }

    copy() {
        return new Sudoku(this.grid.map(row => [...row]));
    }

    isValid(row, col, num) {
        for (let i = 0; i < 9; i++) {
            if (this.grid[row][i] === num || this.grid[i][col] === num) return false;
        }
        const startRow = Math.floor(row / 3) * 3;
        const startCol = Math.floor(col / 3) * 3;
        for (let i = 0; i < 3; i++) {
            for (let j = 0; j < 3; j++) {
                if (this.grid[startRow + i][startCol + j] === num) return false;
            }
        }
        return true;
    }

    solve() {
        for (let row = 0; row < 9; row++) {
            for (let col = 0; col < 9; col++) {
                if (this.grid[row][col] === 0) {
                    for (let num = 1; num <= 9; num++) {
                        if (this.isValid(row, col, num)) {
                            this.grid[row][col] = num;
                            if (this.solve()) return true;
                            this.grid[row][col] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    countSolutions(limit = 2) {
        let count = 0;
        for (let row = 0; row < 9; row++) {
            for (let col = 0; col < 9; col++) {
                if (this.grid[row][col] === 0) {
                    for (let num = 1; num <= 9; num++) {
                        if (this.isValid(row, col, num)) {
                            this.grid[row][col] = num;
                            count += this.countSolutions(limit);
                            this.grid[row][col] = 0;
                            if (count >= limit) return count;
                        }
                    }
                    return count;
                }
            }
        }
        return 1;
    }

    generate(difficulty = 'medium') {
        this._fillDiagonalBlocks();
        this.solve();
        const cellsToRemove = { easy: 30, medium: 40, hard: 50, expert: 55 }[difficulty] || 40;
        const solution = this.grid.map(row => [...row]);
        const positions = [];
        for (let r = 0; r < 9; r++) {
            for (let c = 0; c < 9; c++) {
                positions.push([r, c]);
            }
        }
        positions.sort(() => Math.random() - 0.5);
        let removed = 0;
        for (const [r, c] of positions) {
            if (removed >= cellsToRemove) break;
            const backup = this.grid[r][c];
            this.grid[r][c] = 0;
            const copy = this.copy();
            if (copy.countSolutions(2) === 1) {
                removed++;
            } else {
                this.grid[r][c] = backup;
            }
        }
        return solution;
    }

    _fillDiagonalBlocks() {
        for (let block = 0; block < 9; block += 3) {
            const nums = [];
            for (let i = 1; i <= 9; i++) nums.push(i);
            for (let i = nums.length - 1; i > 0; i--) {
                const j = Math.floor(Math.random() * (i + 1));
                [nums[i], nums[j]] = [nums[j], nums[i]];
            }
            let idx = 0;
            for (let i = block; i < block + 3; i++) {
                for (let j = block; j < block + 3; j++) {
                    this.grid[i][j] = nums[idx++];
                }
            }
        }
    }

    print(color = true) {
        const reset = color ? '\x1b[0m' : '';
        const blue = color ? '\x1b[34m' : '';
        const white = color ? '\x1b[37m' : '';
        for (let i = 0; i < 9; i++) {
            let line = '';
            for (let j = 0; j < 9; j++) {
                const val = this.grid[i][j];
                const ch = val === 0 ? '.' : String(val);
                line += (val === 0 ? blue : white) + ch + reset + ' ';
                if (j === 2 || j === 5) line += '| ';
            }
            console.log(line);
            if (i === 2 || i === 5) console.log('------+-------+------');
        }
    }

    toJSON() {
        return this.grid;
    }

    exportTxt(filename) {
        const content = this.grid.map(row => row.join(' ')).join('\n');
        fs.writeFileSync(filename, content);
    }

    exportCsv(filename) {
        const content = this.grid.map(row => row.join(',')).join('\n');
        fs.writeFileSync(filename, content);
    }
}

program
    .option('--generate <level>', 'Generate puzzle: easy, medium, hard, expert')
    .option('--solve <file>', 'Solve puzzle from file')
    .option('--export-json <file>', 'Export to JSON')
    .option('--export-csv <file>', 'Export to CSV')
    .option('--export-txt <file>', 'Export to TXT')
    .option('--import <file>', 'Import puzzle from file')
    .option('--print', 'Print the grid')
    .parse(process.argv);

const opts = program.opts();

async function main() {
    let sudoku = null;

    if (opts.generate) {
        const s = new Sudoku();
        const solution = s.generate(opts.generate);
        sudoku = s;
        if (opts.print) {
            console.log(chalk.cyan(`Generated ${opts.generate} puzzle:`));
            s.print();
        }
        if (opts.exportJson) {
            fs.writeFileSync(opts.exportJson, JSON.stringify({ puzzle: s.grid, solution }, null, 2));
            console.log(chalk.green(`Exported to ${opts.exportJson}`));
        }
        if (opts.exportCsv) s.exportCsv(opts.exportCsv);
        if (opts.exportTxt) s.exportTxt(opts.exportTxt);
    } else if (opts.solve) {
        let data;
        if (opts.solve.endsWith('.json')) {
            data = JSON.parse(fs.readFileSync(opts.solve, 'utf8'));
            sudoku = new Sudoku(data.puzzle || data);
        } else {
            sudoku = new Sudoku();
            const content = fs.readFileSync(opts.solve, 'utf8');
            const rows = content.trim().split('\n');
            sudoku.grid = rows.map(row => row.trim().split(/\s+/).map(Number));
        }
        if (sudoku.solve()) {
            if (opts.print) {
                console.log(chalk.cyan('Solved puzzle:'));
                sudoku.print();
            }
            if (opts.exportJson) {
                fs.writeFileSync(opts.exportJson, JSON.stringify(sudoku.grid));
                console.log(chalk.green(`Exported to ${opts.exportJson}`));
            }
        } else {
            console.log(chalk.red('No solution found.'));
        }
    } else if (opts.import) {
        let data;
        if (opts.import.endsWith('.json')) {
            data = JSON.parse(fs.readFileSync(opts.import, 'utf8'));
            sudoku = new Sudoku(data.puzzle || data);
        } else {
            sudoku = new Sudoku();
            const content = fs.readFileSync(opts.import, 'utf8');
            const rows = content.trim().split('\n');
            sudoku.grid = rows.map(row => row.trim().split(/\s+/).map(Number));
        }
        if (opts.print && sudoku) {
            sudoku.print();
        }
    } else {
        program.help();
    }
}

main().catch(console.error);
