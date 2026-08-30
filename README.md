# Генератор судоку (сложный)

Многоязычная утилита для генерации и решения судоку с поддержкой различных уровней сложности.  
Создаёт полностью решённые сетки и удаляет ячейки, формируя головоломки с уникальным решением. Поддерживает проверку решений и экспорт в различные форматы.

## Особенности
- Генерация судоку с заданным уровнем сложности (easy, medium, hard, expert).
- Проверка уникальности решения (гарантированно одно решение).
- Встроенный решатель с несколькими стратегиями (backtracking, naked singles, hidden singles).
- Отображение сетки в цветном консольном выводе.
- Экспорт и импорт головоломок в форматах: JSON, CSV, TXT.
- Создание PDF-файла с сеткой (в некоторых языках).
- Поддержка аргументов командной строки для генерации и решения.
- Кроссплатформенность (Windows, Linux, macOS).

## Установка и запуск
Для каждого языка требуются соответствующие инструменты и зависимости (указаны ниже).

### Запуск на разных языках

1. **Python**  
   Установка: `pip install colorama`  
   Запуск: `python sudoku_generator.py --generate hard --export-json puzzle.json`

2. **JavaScript (Node.js)**  
   Установка: `npm install commander chalk`  
   Запуск: `node sudoku_generator.js --generate hard --export-json puzzle.json`

3. **Go**  
   Запуск: `go run sudoku_generator.go --generate hard --export-json puzzle.json`

4. **Rust**  
   Сборка: `cargo build --release`  
   Запуск: `cargo run -- --generate hard --export-json puzzle.json`

5. **Java**  
   Сборка: `javac -cp gson.jar SudokuGenerator.java`  
   Запуск: `java -cp .;gson.jar SudokuGenerator --generate hard --export-json puzzle.json`

6. **C# (.NET Core)**  
   Установка: `dotnet add package Newtonsoft.Json`  
   Запуск: `dotnet run -- --generate hard --export-json puzzle.json`

7. **C++ (Linux)**  
   Сборка: `g++ -std=c++11 -o sudoku_generator sudoku_generator.cpp -ljsoncpp`  
   Запуск: `./sudoku_generator --generate hard --export-json puzzle.json`

8. **Kotlin (JVM)**  
   Сборка: `kotlinc -cp gson.jar SudokuGenerator.kt`  
   Запуск: `kotlin -cp .;gson.jar SudokuGeneratorKt --generate hard --export-json puzzle.json`

## Использование

Общие аргументы командной строки (везде, где поддерживается):

- `--generate <easy|medium|hard|expert>` – сгенерировать головоломку указанного уровня.
- `--solve <файл>` – решить головоломку из файла (JSON или CSV).
- `--export-json <файл>` – экспортировать головоломку в JSON.
- `--export-csv <файл>` – экспортировать головоломку в CSV.
- `--export-txt <файл>` – экспортировать головоломку в TXT.
- `--import <файл>` – импортировать головоломку из файла.
- `--print` – вывести сетку в консоль.
- `--help` – справка.

Пример (Python):
```bash
python sudoku_generator.py --generate hard --print --export-json puzzle.json
python sudoku_generator.py --solve puzzle.json --print
Структура репозитория
text
/
├── README.md
├── sudoku_generator.py
├── sudoku_generator.js
├── sudoku_generator.go
├── sudoku_generator.rs
├── SudokuGenerator.java
├── SudokuGenerator.cs
├── sudoku_generator.cpp
└── SudokuGenerator.kt
Лицензия
MIT
