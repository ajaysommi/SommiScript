# SommiScript  

SommiScript is a simple and expressive scripting language designed for learning and experimentation. It provides an intuitive syntax and lightweight interpreter, making it easy to explore language design and scripting concepts.  

![GitHub last commit](https://img.shields.io/github/last-commit/ajaysommi/sommiscript) ![GitHub release (latest by date)](https://img.shields.io/github/v/release/ajaysommi/sommiscript?color=blue)




## Features  
- Lightweight and easy to learn
- Based on JavaScript, Python, and BASIC
- Readable, expressive syntax  
- Built-in interpreter for quick execution


## Setting Up Gradle  

SommiScript requires **Gradle** to build and run. Follow the instructions below based on your operating system.

### Windows  
1. Download Gradle from the official website: [Gradle Releases](https://gradle.org/releases/)  
2. Extract the ZIP file to `C:\Gradle`  
3. Add `C:\Gradle\bin` to your **System Environment Variables**:  
   - Search for "Environment Variables" in Windows  
   - Edit the `Path` variable and add `C:\Gradle\bin`  
4. Verify installation:  
   ```sh
   gradle -v
   ```

### macOS
1. Install Gradle using HomeBrew:
   ```sh
   brew install gradle
   ```
2. Verify Installation:
   ```sh
   gradle -v
   ```

### Linux
1. Install Gradle using a package manager:
   ```sh
   sudo apt update && sudo apt install gradle  # Debian/Ubuntu  
   sudo dnf install gradle  # Fedora  
   ```
2. Verify Installation:
   ```sh
   gradle -v
   ```


## Installation & Setup  
Clone the repository and get started:  
```sh
git clone https://github.com/ajaysommi/sommiscript.git
cd sommiscript
```

Navigate to the main file:
```sh
cd src/main/java/plc/project
```

Run the main Java file:
```sh
java Main.java
```
## Usage Example  
Write a simple SommiScript program:  

```sommiscript
LET x = 10;
IF x > 5 DO
    PRINT "x is greater than 5";
END
```
## Roadmap  
- [ ] Add analyzer, evaluator, and generator implementation
- [ ] Implement interpretation and compilation
- [ ] Improve error handling  
- [ ] Create a Python-based runner for easier execution  
