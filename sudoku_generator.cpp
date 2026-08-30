// sudoku_generator.cpp
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <algorithm>
#include <random>
#include <chrono>
#include <cstring>
#include <json/json.h> // using jsoncpp

using namespace std;

class Sudoku {
private:
    int grid[9][9];
    mt19937 rng;

public:
    Sudoku() {
        memset(grid, 0, sizeof(grid));
        rng.seed(chrono::steady_clock::now().time_since_epoch().count());
    }

    bool isValid(int row, int col, int num) {
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

    bool solve() {
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

    int countSolutions(int limit) {
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

    void fillDiagonalBlocks() {
        for (int block = 0; block < 9; block += 3) {
            vector<int> nums(9);
            for (int i = 0; i < 9; i++) nums[i] = i + 1;
            shuffle(nums.begin(), nums.end(), rng);
            int idx = 0;
            for (int i = block; i < block + 3; i++) {
                for (int j = block; j < block + 3; j++) {
                    grid[i][j] = nums[idx++];
                }
            }
        }
    }

    void generate(const string& difficulty) {
        fillDiagonalBlocks();
        solve();
        int cellsToRemove;
        if (difficulty == "easy") cellsToRemove = 30;
        else if (difficulty == "medium") cellsToRemove = 40;
        else if (difficulty == "hard") cellsToRemove = 50;
        else if (difficulty == "expert") cellsToRemove = 55;
        else cellsToRemove = 40;

        vector<pair<int,int>> positions;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                positions.push_back({r, c});
            }
        }
        shuffle(positions.begin(), positions.end(), rng);
        int removed = 0;
        for (auto& pos : positions) {
            if (removed >= cellsToRemove) break;
            int r = pos.first, c = pos.second;
            int backup = grid[r][c];
            grid[r][c] = 0;
            Sudoku copy = *this;
            if (copy.countSolutions(2) == 1) {
                removed++;
            } else {
                grid[r][c] = backup;
            }
        }
    }

    void print(bool color) {
        string reset = color ? "\033[0m" : "";
        string blue = color ? "\033[34m" : "";
        string white = color ? "\033[37m" : "";
        for (int i = 0; i < 9; i++) {
            string line;
            for (int j = 0; j < 9; j++) {
                int val = grid[i][j];
                string ch = val == 0 ? "." : to_string(val);
                line += (val == 0 ? blue : white) + ch + reset + " ";
                if (j == 2 || j == 5) line += "| ";
            }
            cout << line << endl;
            if (i == 2 || i == 5) cout << "------+-------+------" << endl;
        }
    }

    void exportJSON(const string& filename) {
        Json::Value root(Json::arrayValue);
        for (int i = 0; i < 9; i++) {
            Json::Value row(Json::arrayValue);
            for (int j = 0; j < 9; j++) {
                row.append(grid[i][j]);
            }
            root.append(row);
        }
        ofstream ofs(filename);
        ofs << root.toStyledString();
    }

    void exportCSV(const string& filename) {
        ofstream ofs(filename);
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (j > 0) ofs << ",";
                ofs << grid[i][j];
            }
            ofs << "\n";
        }
    }

    void exportTXT(const string& filename) {
        ofstream ofs(filename);
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (j > 0) ofs << " ";
                ofs << grid[i][j];
            }
            ofs << "\n";
        }
    }

    void load(const string& filename) {
        ifstream ifs(filename);
        if (filename.find(".json") != string::npos) {
            Json::Value root;
            ifs >> root;
            for (int i = 0; i < 9 && i < (int)root.size(); i++) {
                for (int j = 0; j < 9 && j < (int)root[i].size(); j++) {
                    grid[i][j] = root[i][j].asInt();
                }
            }
        } else {
            string line;
            for (int i = 0; i < 9 && getline(ifs, line); i++) {
                stringstream ss(line);
                for (int j = 0; j < 9; j++) {
                    ss >> grid[i][j];
                }
            }
        }
    }
};

int main(int argc, char* argv[]) {
    string generate, solve, exportJson, exportCsv, exportTxt, importFile;
    bool printGrid = false;

    for (int i = 1; i < argc; ++i) {
        string arg = argv[i];
        if (arg == "--generate" && i+1 < argc) generate = argv[++i];
        else if (arg == "--solve" && i+1 < argc) solve = argv[++i];
        else if (arg == "--export-json" && i+1 < argc) exportJson = argv[++i];
        else if (arg == "--export-csv" && i+1 < argc) exportCsv = argv[++i];
        else if (arg == "--export-txt" && i+1 < argc) exportTxt = argv[++i];
        else if (arg == "--import" && i+1 < argc) importFile = argv[++i];
        else if (arg == "--print") printGrid = true;
    }

    Sudoku sudoku;

    if (!generate.empty()) {
        sudoku.generate(generate);
        if (printGrid) {
            cout << "Generated " << generate << " puzzle:" << endl;
            sudoku.print(true);
        }
        if (!exportJson.empty()) sudoku.exportJSON(exportJson);
        if (!exportCsv.empty()) sudoku.exportCSV(exportCsv);
        if (!exportTxt.empty()) sudoku.exportTXT(exportTxt);
    } else if (!solve.empty()) {
        sudoku.load(solve);
        if (sudoku.solve()) {
            if (printGrid) {
                cout << "Solved puzzle:" << endl;
                sudoku.print(true);
            }
            if (!exportJson.empty()) sudoku.exportJSON(exportJson);
        } else {
            cout << "No solution found." << endl;
        }
    } else if (!importFile.empty()) {
        sudoku.load(importFile);
        if (printGrid) sudoku.print(true);
    } else {
        cout << "Use --help for usage." << endl;
    }
    return 0;
}
