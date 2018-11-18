# Compiler
A compiler based on book: [Compiler Construction Using Java, JavaCC and Yacc by Anthony J. Dos Reis](https://www.amazon.com/Compiler-Construction-Using-Java-JavaCC/dp/0470949597). This project is done as a part my academic project but the special thing about this compiler is that it generates highly optimized machine language code.

# Optimization Stats
|--------------------| Expected output | Code's output | Optimization % |
|--------------------|-----------------|---------------|----------------|
| Machine code size  | 812             | 686           | 15.52%         | 
| machine instruction| 382             | 283           | 25.92%         |
| Execution time     | 8158            | 6073          | 34.33%         |

# How to run
- Compile the code by following command: ```javac Compiler.java```
- Input: input is a file with extension .s and is made up of statements mentioned in [supported statements]()
- Run the code against input by command: ``` java compiler <fileName>```. 
	- NOTE: command does not take input file's extention
- Output will be generated as <filename>.a in the same folder. 
eg. Input: [input.s](https://github.com/sabhayasaumil/Compiler/blob/master/input.s) and Output: [input.a](https://github.com/sabhayasaumil/Compiler/blob/master/input.a)


# Supported statements
