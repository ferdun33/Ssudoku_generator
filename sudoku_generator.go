// sudoku_generator.go
package main

import (
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"math/rand"
	"os"
	"strconv"
	"strings"
	"time"
)

type Sudoku struct {
	grid [9][9]int
}

func NewSudoku() *Sudoku {
	return &Sudoku{}
}

func (s *Sudoku) Copy() *Sudoku {
	copy := &Sudoku{}
	for i := 0; i < 9; i++ {
		for j := 0; j < 9; j++ {
			copy.grid[i][j] = s.grid[i][j]
		}
	}
	return copy
}

func (s *Sudoku) IsValid(row, col, num int) bool {
	for i := 0; i < 9; i++ {
		if s.grid[row][i] == num || s.grid[i][col] == num {
			return false
		}
	}
	startRow, startCol := (row/3)*3, (col/3)*3
	for i := 0; i < 3; i++ {
		for j := 0; j < 3; j++ {
			if s.grid[startRow+i][startCol+j] == num {
				return false
			}
		}
	}
	return true
}

func (s *Sudoku) Solve() bool {
	for row := 0; row < 9; row++ {
		for col := 0; col < 9; col++ {
			if s.grid[row][col] == 0 {
				for num := 1; num <= 9; num++ {
					if s.IsValid(row, col, num) {
						s.grid[row][col] = num
						if s.Solve() {
							return true
						}
						s.grid[row][col] = 0
					}
				}
				return false
			}
		}
	}
	return true
}

func (s *Sudoku) CountSolutions(limit int) int {
	count := 0
	for row := 0; row < 9; row++ {
		for col := 0; col < 9; col++ {
			if s.grid[row][col] == 0 {
				for num := 1; num <= 9; num++ {
					if s.IsValid(row, col, num) {
						s.grid[row][col] = num
						count += s.CountSolutions(limit)
						s.grid[row][col] = 0
						if count >= limit {
							return count
						}
					}
				}
				return count
			}
		}
	}
	return 1
}

func (s *Sudoku) Generate(difficulty string) [9][9]int {
	// Заполняем диагональные блоки
	s.fillDiagonalBlocks()
	s.Solve()
	solution := s.grid
	cellsToRemove := map[string]int{
		"easy":   30,
		"medium": 40,
		"hard":   50,
		"expert": 55,
	}[difficulty]
	if cellsToRemove == 0 {
		cellsToRemove = 40
	}
	positions := make([][2]int, 0, 81)
	for r := 0; r < 9; r++ {
		for c := 0; c < 9; c++ {
			positions = append(positions, [2]int{r, c})
		}
	}
	rand.Shuffle(len(positions), func(i, j int) {
		positions[i], positions[j] = positions[j], positions[i]
	})
	removed := 0
	for _, pos := range positions {
		if removed >= cellsToRemove {
			break
		}
		r, c := pos[0], pos[1]
		backup := s.grid[r][c]
		s.grid[r][c] = 0
		copy := s.Copy()
		if copy.CountSolutions(2) == 1 {
			removed++
		} else {
			s.grid[r][c] = backup
		}
	}
	return solution
}

func (s *Sudoku) fillDiagonalBlocks() {
	for block := 0; block < 9; block += 3 {
		nums := make([]int, 9)
		for i := 0; i < 9; i++ {
			nums[i] = i + 1
		}
		rand.Shuffle(len(nums), func(i, j int) {
			nums[i], nums[j] = nums[j], nums[i]
		})
		idx := 0
		for i := block; i < block+3; i++ {
			for j := block; j < block+3; j++ {
				s.grid[i][j] = nums[idx]
				idx++
			}
		}
	}
}

func (s *Sudoku) Print(color bool) {
	reset, blue, white := "", "", ""
	if color {
		reset = "\033[0m"
		blue = "\033[34m"
		white = "\033[37m"
	}
	for i := 0; i < 9; i++ {
		line := ""
		for j := 0; j < 9; j++ {
			val := s.grid[i][j]
			ch := "."
			if val != 0 {
				ch = strconv.Itoa(val)
			}
			if val == 0 {
				line += blue + ch + reset + " "
			} else {
				line += white + ch + reset + " "
			}
			if j == 2 || j == 5 {
				line += "| "
			}
		}
		fmt.Println(line)
		if i == 2 || i == 5 {
			fmt.Println("------+-------+------")
		}
	}
}

func (s *Sudoku) ExportJSON(filename string) error {
	data, err := json.MarshalIndent(s.grid, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(filename, data, 0644)
}

func (s *Sudoku) ExportCSV(filename string) error {
	f, err := os.Create(filename)
	if err != nil {
		return err
	}
	defer f.Close()
	w := csv.NewWriter(f)
	defer w.Flush()
	for _, row := range s.grid {
		strRow := make([]string, 9)
		for i, v := range row {
			strRow[i] = strconv.Itoa(v)
		}
		if err := w.Write(strRow); err != nil {
			return err
		}
	}
	return nil
}

func (s *Sudoku) ExportTXT(filename string) error {
	content := ""
	for _, row := range s.grid {
		for i, v := range row {
			if i > 0 {
				content += " "
			}
			content += strconv.Itoa(v)
		}
		content += "\n"
	}
	return os.WriteFile(filename, []byte(content), 0644)
}

func loadGrid(filename string) (*Sudoku, error) {
	data, err := os.ReadFile(filename)
	if err != nil {
		return nil, err
	}
	var grid [9][9]int
	if strings.HasSuffix(filename, ".json") {
		if err := json.Unmarshal(data, &grid); err != nil {
			return nil, err
		}
	} else {
		lines := strings.Split(string(data), "\n")
		for i := 0; i < 9 && i < len(lines); i++ {
			fields := strings.Fields(lines[i])
			for j := 0; j < 9 && j < len(fields); j++ {
				grid[i][j], _ = strconv.Atoi(fields[j])
			}
		}
	}
	return &Sudoku{grid: grid}, nil
}

func main() {
	rand.Seed(time.Now().UnixNano())
	var (
		generate   string
		solve      string
		exportJson string
		exportCsv  string
		exportTxt  string
		importFile string
		printGrid  bool
	)
	flag.StringVar(&generate, "generate", "", "easy, medium, hard, expert")
	flag.StringVar(&solve, "solve", "", "Solve puzzle from file")
	flag.StringVar(&exportJson, "export-json", "", "Export to JSON")
	flag.StringVar(&exportCsv, "export-csv", "", "Export to CSV")
	flag.StringVar(&exportTxt, "export-txt", "", "Export to TXT")
	flag.StringVar(&importFile, "import", "", "Import puzzle from file")
	flag.BoolVar(&printGrid, "print", false, "Print the grid")
	flag.Parse()

	var sudoku *Sudoku

	if generate != "" {
		s := NewSudoku()
		solution := s.Generate(generate)
		sudoku = s
		if printGrid {
			fmt.Printf("Generated %s puzzle:\n", generate)
			s.Print(true)
		}
		if exportJson != "" {
			data := map[string]interface{}{"puzzle": s.grid, "solution": solution}
			jsonData, _ := json.MarshalIndent(data, "", "  ")
			os.WriteFile(exportJson, jsonData, 0644)
			fmt.Printf("Exported to %s\n", exportJson)
		}
		if exportCsv != "" {
			s.ExportCSV(exportCsv)
		}
		if exportTxt != "" {
			s.ExportTXT(exportTxt)
		}
	} else if solve != "" {
		s, err := loadGrid(solve)
		if err != nil {
			fmt.Printf("Error loading file: %v\n", err)
			os.Exit(1)
		}
		sudoku = s
		if s.Solve() {
			if printGrid {
				fmt.Println("Solved puzzle:")
				s.Print(true)
			}
			if exportJson != "" {
				s.ExportJSON(exportJson)
				fmt.Printf("Exported to %s\n", exportJson)
			}
		} else {
			fmt.Println("No solution found.")
		}
	} else if importFile != "" {
		s, err := loadGrid(importFile)
		if err != nil {
			fmt.Printf("Error loading file: %v\n", err)
			os.Exit(1)
		}
		sudoku = s
		if printGrid {
			s.Print(true)
		}
	} else {
		fmt.Println("Use -h for help")
	}
}
