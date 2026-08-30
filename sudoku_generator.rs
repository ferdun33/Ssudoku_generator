// sudoku_generator.rs
use clap::{App, Arg};
use rand::prelude::*;
use serde::{Deserialize, Serialize};
use serde_json;
use std::fs;
use std::io::Write;
use colored::*;

#[derive(Serialize, Deserialize, Clone)]
struct Sudoku {
    grid: [[u8; 9]; 9],
}

impl Sudoku {
    fn new() -> Self {
        Sudoku { grid: [[0; 9]; 9] }
    }

    fn copy(&self) -> Self {
        let mut g = [[0; 9]; 9];
        for i in 0..9 {
            for j in 0..9 {
                g[i][j] = self.grid[i][j];
            }
        }
        Sudoku { grid: g }
    }

    fn is_valid(&self, row: usize, col: usize, num: u8) -> bool {
        for i in 0..9 {
            if self.grid[row][i] == num || self.grid[i][col] == num {
                return false;
            }
        }
        let start_row = (row / 3) * 3;
        let start_col = (col / 3) * 3;
        for i in 0..3 {
            for j in 0..3 {
                if self.grid[start_row + i][start_col + j] == num {
                    return false;
                }
            }
        }
        true
    }

    fn solve(&mut self) -> bool {
        for row in 0..9 {
            for col in 0..9 {
                if self.grid[row][col] == 0 {
                    for num in 1..=9 {
                        if self.is_valid(row, col, num) {
                            self.grid[row][col] = num;
                            if self.solve() {
                                return true;
                            }
                            self.grid[row][col] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        true
    }

    fn count_solutions(&mut self, limit: u8) -> u8 {
        let mut count = 0;
        for row in 0..9 {
            for col in 0..9 {
                if self.grid[row][col] == 0 {
                    for num in 1..=9 {
                        if self.is_valid(row, col, num) {
                            self.grid[row][col] = num;
                            count += self.count_solutions(limit);
                            self.grid[row][col] = 0;
                            if count >= limit {
                                return count;
                            }
                        }
                    }
                    return count;
                }
            }
        }
        1
    }

    fn generate(&mut self, difficulty: &str) -> [[u8; 9]; 9] {
        self.fill_diagonal_blocks();
        self.solve();
        let solution = self.grid;
        let cells_to_remove = match difficulty {
            "easy" => 30,
            "medium" => 40,
            "hard" => 50,
            "expert" => 55,
            _ => 40,
        };
        let mut positions = Vec::new();
        for r in 0..9 {
            for c in 0..9 {
                positions.push((r, c));
            }
        }
        positions.shuffle(&mut rand::thread_rng());
        let mut removed = 0;
        for (r, c) in positions {
            if removed >= cells_to_remove {
                break;
            }
            let backup = self.grid[r][c];
            self.grid[r][c] = 0;
            let mut copy = self.copy();
            if copy.count_solutions(2) == 1 {
                removed += 1;
            } else {
                self.grid[r][c] = backup;
            }
        }
        solution
    }

    fn fill_diagonal_blocks(&mut self) {
        let mut rng = rand::thread_rng();
        for block in (0..9).step_by(3) {
            let mut nums: Vec<u8> = (1..=9).collect();
            nums.shuffle(&mut rng);
            let mut idx = 0;
            for i in block..block + 3 {
                for j in block..block + 3 {
                    self.grid[i][j] = nums[idx];
                    idx += 1;
                }
            }
        }
    }

    fn print(&self, color: bool) {
        let reset = if color { "\x1b[0m" } else { "" };
        let blue = if color { "\x1b[34m" } else { "" };
        let white = if color { "\x1b[37m" } else { "" };
        for i in 0..9 {
            let mut line = String::new();
            for j in 0..9 {
                let val = self.grid[i][j];
                let ch = if val == 0 { ".".to_string() } else { val.to_string() };
                let colored = if val == 0 { blue } else { white };
                line.push_str(&format!("{}{}{} ", colored, ch, reset));
                if j == 2 || j == 5 {
                    line.push_str("| ");
                }
            }
            println!("{}", line);
            if i == 2 || i == 5 {
                println!("------+-------+------");
            }
        }
    }

    fn export_json(&self, filename: &str) -> Result<(), Box<dyn std::error::Error>> {
        let json = serde_json::to_string_pretty(&self.grid)?;
        fs::write(filename, json)?;
        Ok(())
    }

    fn export_csv(&self, filename: &str) -> Result<(), Box<dyn std::error::Error>> {
        let mut wtr = csv::Writer::from_path(filename)?;
        for row in &self.grid {
            let row_str: Vec<String> = row.iter().map(|v| v.to_string()).collect();
            wtr.write_record(&row_str)?;
        }
        wtr.flush()?;
        Ok(())
    }

    fn export_txt(&self, filename: &str) -> Result<(), Box<dyn std::error::Error>> {
        let mut content = String::new();
        for row in &self.grid {
            for (i, v) in row.iter().enumerate() {
                if i > 0 {
                    content.push(' ');
                }
                content.push_str(&v.to_string());
            }
            content.push('\n');
        }
        fs::write(filename, content)?;
        Ok(())
    }

    fn load(filename: &str) -> Result<Self, Box<dyn std::error::Error>> {
        let content = fs::read_to_string(filename)?;
        let mut grid = [[0; 9]; 9];
        if filename.ends_with(".json") {
            let data: [[u8; 9]; 9] = serde_json::from_str(&content)?;
            grid = data;
        } else {
            for (i, line) in content.lines().enumerate() {
                if i >= 9 { break; }
                let nums: Vec<u8> = line.split_whitespace()
                    .filter_map(|s| s.parse().ok())
                    .collect();
                for (j, &v) in nums.iter().enumerate() {
                    if j < 9 { grid[i][j] = v; }
                }
            }
        }
        Ok(Sudoku { grid })
    }
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let matches = App::new("Sudoku Generator")
        .arg(Arg::with_name("generate").long("generate").takes_value(true).possible_values(&["easy", "medium", "hard", "expert"]))
        .arg(Arg::with_name("solve").long("solve").takes_value(true))
        .arg(Arg::with_name("export-json").long("export-json").takes_value(true))
        .arg(Arg::with_name("export-csv").long("export-csv").takes_value(true))
        .arg(Arg::with_name("export-txt").long("export-txt").takes_value(true))
        .arg(Arg::with_name("import").long("import").takes_value(true))
        .arg(Arg::with_name("print").long("print"))
        .get_matches();

    let mut sudoku = Sudoku::new();

    if let Some(level) = matches.value_of("generate") {
        let solution = sudoku.generate(level);
        if matches.is_present("print") {
            println!("Generated {} puzzle:", level);
            sudoku.print(true);
        }
        if let Some(file) = matches.value_of("export-json") {
            let data = serde_json::json!({ "puzzle": sudoku.grid, "solution": solution });
            fs::write(file, serde_json::to_string_pretty(&data)?)?;
            println!("Exported to {}", file);
        }
        if let Some(file) = matches.value_of("export-csv") {
            sudoku.export_csv(file)?;
        }
        if let Some(file) = matches.value_of("export-txt") {
            sudoku.export_txt(file)?;
        }
    } else if let Some(file) = matches.value_of("solve") {
        let mut s = Sudoku::load(file)?;
        if s.solve() {
            if matches.is_present("print") {
                println!("Solved puzzle:");
                s.print(true);
            }
            if let Some(out) = matches.value_of("export-json") {
                s.export_json(out)?;
                println!("Exported to {}", out);
            }
        } else {
            println!("No solution found.");
        }
    } else if let Some(file) = matches.value_of("import") {
        let s = Sudoku::load(file)?;
        sudoku = s;
        if matches.is_present("print") {
            sudoku.print(true);
        }
    } else {
        println!("Use --help for usage.");
    }
    Ok(())
}
