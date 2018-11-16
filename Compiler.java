// Hand-written Compiler compiler

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

//======================================================
class Compiler {
    public static void main(String[] args) throws IOException {
        System.out.println("Compiler compiler written by Sabhaya Saumil");

        if (args.length != 1) {
            System.out.println("Wrong number cmd line args");
            System.exit(1);
        }

        // set to true to debug token manager
        boolean debug = false;

        // build the input and output file names
        String inFileName = args[0] + ".s";
        String outFileName = args[0] + ".a";

        // construct file objects
        Scanner inFile = new Scanner(new File(inFileName));
        PrintWriter outFile = new PrintWriter(outFileName);

        // identify compiler/author in the output file
        outFile.println("!register");
        outFile.println("; from Compiler compiler written by Sabhaya Saumil");

        // construct objects that make up compiler
        R4DSymTab st = new R4DSymTab();
        R4DTokenMgr tm = new R4DTokenMgr(inFile, outFile, debug);
        R4DCodeGen cg = new R4DCodeGen(outFile, st);
        R4DParser parser = new R4DParser(st, tm, cg);

        // parse and translate
        try {
            parser.parse();
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            outFile.println(e.getMessage());
            outFile.close();
            System.exit(1);
        }

        outFile.close();
    }
}                                           // end of Compiler

//======================================================
interface R4DConstants {
    // integers that identify token kinds
    int EOF = 0;
    int PRINTLN = 1;
    int UNSIGNED = 2;
    int ID = 3;
    int ASSIGN = 4;
    int SEMICOLON = 5;
    int LEFTPAREN = 6;
    int RIGHTPAREN = 7;
    int PLUS = 8;
    int MINUS = 9;
    int TIMES = 10;
    int DIV = 11;
    int OPEN = 12;
    int CLOSE = 13;
    int ERROR = 14;
    int PRINT = 15;
    int STRING = 16;
    int READINT = 17;
    int DO = 18;
    int WHILE = 19;
    int IF = 20;
    int ELSE = 21;

    // tokenImage provides string for each token kind
    String[] tokenImage =
            {
                    "<EOF>",
                    "\"println\"",
                    "<UNSIGNED>",
                    "<ID>",
                    "\"=\"",
                    "\";\"",
                    "\"(\"",
                    "\")\"",
                    "\"+\"",
                    "\"-\"",
                    "\"*\"",
                    "\"/\"",
                    "\"{\"",
                    "\"}\"",
                    "<ERROR>",
                    "\"print\"",
                    "<String>",
                    "\"readint\"",
                    "\"do\"",
                    "\"while\"",
                    "\"if\"",
                    "\"else\""
            };
}                                  // end of R4DConstants

//======================================================
class R4DSymTab {
    private ArrayList<String> symbol;
    private ArrayList<String> dwValue;
    private ArrayList<Boolean> needsdw;

    //-----------------------------------------
    public R4DSymTab() {
        symbol = new ArrayList<String>();
        dwValue = new ArrayList<String>();
        needsdw = new ArrayList<Boolean>();
    }

    //-----------------------------------------
    public int enter(String s, String v, boolean b) {
        int index = symbol.indexOf(s);
        if (index >= 0)
            return index;

        index = symbol.size();
        symbol.add(s);
        dwValue.add(v);
        needsdw.add(b);
        return index;
    }

    //-----------------------------------------
    public String getSymbol(int index) {
        return symbol.get(index);
    }

    //-----------------------------------------
    public int getLabelindex(String s) {
        int index = dwValue.indexOf(s);
        return index;
    }

    //-----------------------------------------
    public Boolean isTemp(int index) {
        String ss = symbol.get(index);
        if (ss.charAt(0) == '@' && ss.charAt(1) == 't') {
            return true;
        } else
            return false;
    }

    //-----------------------------------------
    public int getSize() {
        return symbol.size();
    }

    //------------------------------------------
    public String getdwValue(int index) {
        return dwValue.get(index);
    }

    //------------------------------------------
    public boolean getNeedsdw(int index) {
        return needsdw.get(index);
    }

    //-----------------------------------------
    public void setNeedsdw(int index) {
        needsdw.set(index, true);
    }

    //-----------------------------------------
    public boolean isLDCConstant(int index) {
        String s = getSymbol(index);
        if (s.charAt(0) == '@') {
            int i = 1;
            if (s.charAt(1) == '_')
                i++;

            s = s.substring(i);
            try {
                Integer.parseInt(s);

            } catch (NumberFormatException e) {
                return false;
            }

            return true;
        } else {
            return false;
        }

    }

}                                     // end of R4DSymTab

//======================================================
class R4DTokenMgr implements R4DConstants {
    private Scanner inFile;
    private PrintWriter outFile;
    private boolean debug;
    private char currentChar;
    private int currentColumnNumber;
    private int currentLineNumber;
    private String inputLine;    // holds 1 line of input
    private Token token;         // holds 1 token
    private StringBuffer buffer; // token image built here

    //-----------------------------------------
    public R4DTokenMgr(Scanner inFile,
                       PrintWriter outFile, boolean debug) {
        this.inFile = inFile;
        this.outFile = outFile;
        this.debug = debug;
        currentChar = '\n';        //  '\n' triggers read
        currentLineNumber = 0;
        buffer = new StringBuffer();
    }

    //-----------------------------------------
    public Token getNextToken() {
        // skip whitespace
        token = new Token();
        token.next = null;

        while (Character.isWhitespace(currentChar))
            getNextChar();

        if (currentChar == '/')                    //For Comments
        {
            if (inputLine.charAt(currentColumnNumber) == '/') {
                while (true) {
                    currentChar = '\n';
                    getNextChar();
                    while (Character.isWhitespace(currentChar))
                        getNextChar();
                    if (currentChar != '/') {
                        break;
                    } else if (inputLine.charAt(currentColumnNumber) != '/') {
                        break;
                    }
                }
            }
        }
        // construct token to be returned to parser
        // save start-of-token position
        token.beginLine = currentLineNumber;
        token.beginColumn = currentColumnNumber;

        // check for EOF
        if (currentChar == EOF) {
            token.image = "<EOF>";
            token.endLine = currentLineNumber;
            token.endColumn = currentColumnNumber;
            token.kind = EOF;
        } else  // check for unsigned int
            if (Character.isDigit(currentChar)) {
                buffer.setLength(0);  // clear buffer
                do  // build token image in buffer
                {
                    buffer.append(currentChar);
                    token.endLine = currentLineNumber;
                    token.endColumn = currentColumnNumber;
                    getNextChar();
                } while (Character.isDigit(currentChar));
                // save buffer as String in token.image
                token.image = buffer.toString();
                token.kind = UNSIGNED;
            } else  // check for identifier
                if (Character.isLetter(currentChar)) {
                    buffer.setLength(0);  // clear buffer
                    do  // build token image in buffer
                    {
                        buffer.append(currentChar);
                        token.endLine = currentLineNumber;
                        token.endColumn = currentColumnNumber;
                        getNextChar();
                    } while (Character.isLetterOrDigit(currentChar));
                    // save buffer as String in token.image
                    token.image = buffer.toString();

                    // check if keyword
                    if (token.image.equals("println"))
                        token.kind = PRINTLN;
                    else if (token.image.equals("print"))
                        token.kind = PRINT;
                    else if (token.image.equals("readint"))
                        token.kind = READINT;
                    else if (token.image.equals("do"))
                        token.kind = DO;
                    else if (token.image.equals("while"))
                        token.kind = WHILE;
                    else if (token.image.equals("if"))
                        token.kind = IF;
                    else if (token.image.equals("else"))
                        token.kind = ELSE;
                    else  // not a keyword so kind is ID
                        token.kind = ID;
                } else  // check for identifier
                    if (currentChar == '\"') {

                        buffer.setLength(0);  // clear buffer
                        buffer.append("\"");
                        getNextChar();
                        while (currentChar != '\"')  // build token image in buffer
                        {

                            if (currentChar == '\\') {

                                getNextChar();
                                if (currentChar == '\n') {
                                    getNextChar();
                                } else {
                                    buffer.append('\\');
                                }
                            }
                            buffer.append(currentChar);
                            getNextChar();
                        }
                        buffer.append("\"");
                        token.endLine = currentLineNumber;
                        token.endColumn = currentColumnNumber;
                        // save buffer as String in token.image
                        token.image = buffer.toString();
                        //System.out.println(token.image);
                        token.kind = STRING;
                        getNextChar();
                        // check if keyword

                    } else  // process single-character token
                    {
                        switch (currentChar) {
                            case '=':
                                token.kind = ASSIGN;
                                break;
                            case ';':
                                token.kind = SEMICOLON;
                                break;
                            case '(':
                                token.kind = LEFTPAREN;
                                break;
                            case ')':
                                token.kind = RIGHTPAREN;
                                break;
                            case '+':
                                token.kind = PLUS;
                                break;
                            case '-':
                                token.kind = MINUS;
                                break;
                            case '*':
                                token.kind = TIMES;
                                break;
                            case '/':
                                token.kind = DIV;
                                break;
                            case '{':
                                token.kind = OPEN;
                                break;
                            case '}':
                                token.kind = CLOSE;
                                break;
                            default:
                                token.kind = ERROR;
                                break;
                        }

                        // save currentChar as String in token.image
                        token.image = Character.toString(currentChar);

                        // save end-of-token position
                        token.endLine = currentLineNumber;
                        token.endColumn = currentColumnNumber;

                        getNextChar();  // read beyond end of token
                    }

        // token trace appears as comments in output file
        if (debug)
            outFile.printf(
                    "; kd=%3d bL=%3d bC=%3d eL=%3d eC=%3d im=%s%n",
                    token.kind, token.beginLine, token.beginColumn,
                    token.endLine, token.endColumn, token.image);


        return token;     // return token to parser
    }

    //-----------------------------------------
    private void getNextChar() {
        if (currentChar == EOF)
            return;

        if (currentChar == '\n')        // need next line?
        {
            if (inFile.hasNextLine())     // any lines left?
            {
                inputLine = inFile.nextLine();  // get next line
                // output source line as comment
                outFile.println("; " + inputLine);
                inputLine = inputLine + "\n";   // mark line end
                currentColumnNumber = 0;
                currentLineNumber++;
            } else  // at end of file
            {
                currentChar = EOF;
                return;
            }
        }

        // get next char from inputLine
        currentChar =
                inputLine.charAt(currentColumnNumber++);

        // in Compiler, test for single-line comment goes here
    }
}                                   // end of R4DTokenMgr

//======================================================
class R4DParser implements R4DConstants {
    private R4DSymTab st;
    private R4DTokenMgr tm;
    private R4DCodeGen cg;
    private Token currentToken;
    private Token previousToken;

    //-----------------------------------------
    public R4DParser(R4DSymTab st, R4DTokenMgr tm,
                     R4DCodeGen cg) {
        this.st = st;
        this.tm = tm;
        this.cg = cg;
        // prime currentToken with first token
        currentToken = tm.getNextToken();
        previousToken = null;
    }

    //-----------------------------------------
    // Construct and return an exception that contains
    // a message consisting of the image of the current
    // token, its location, and the expected tokens.
    //
    private RuntimeException genEx(String errorMessage) {
        return new RuntimeException("Encountered \"" +
                currentToken.image + "\" on line " +
                currentToken.beginLine + ", column " +
                currentToken.beginColumn + "." +
                System.getProperty("line.separator") +
                errorMessage);
    }

    //-----------------------------------------
    // Advance currentToken to next token.
    //
    private void advance() {
        previousToken = currentToken;

        // If next token is on token list, advance to it.
        if (currentToken.next != null)
            currentToken = currentToken.next;

            // Otherwise, get next token from token mgr and
            // put it on the list.
        else
            currentToken =
                    currentToken.next = tm.getNextToken();
    }

    //-----------------------------------------
    // getToken(i) returns ith token without advancing
    // in token stream.  getToken(0) returns
    // previousToken.  getToken(1) returns currentToken.
    // getToken(2) returns next token, and so on.
    //
    private Token getToken(int i) {
        if (i <= 0)
            return previousToken;

        Token t = currentToken;
        for (int j = 1; j < i; j++)  // loop to ith token
        {
            // if next token is on token list, move t to it
            if (t.next != null)
                t = t.next;

                // Otherwise, get next token from token mgr and
                // put it on the list.
            else
                t = t.next = tm.getNextToken();
        }
        return t;
    }

    //-----------------------------------------
    // If the kind of the current token matches the
    // expected kind, then consume advances to the next
    // token. Otherwise, it throws an exception.
    //
    private void consume(int expected) {
        if (currentToken.kind == expected)
            advance();
        else
            throw genEx("Expecting " + tokenImage[expected]);
    }

    //-----------------------------------------
    public void parse() {
        program();   // program is start symbol for grammar
    }

    //-----------------------------------------
    private void program() {
        statementList();
        cg.endCode();
        if (currentToken.kind != EOF)  //garbage at end?
            throw genEx("Expecting <EOF>");
    }

    //-----------------------------------------
    private void statementList() {
        switch (currentToken.kind) {
            case ID:
            case PRINTLN:
            case PRINT:
            case READINT:
            case DO:
            case WHILE:
            case IF:
                statement();
                statementList();
                break;
            case SEMICOLON:
                nullStatement();
                statementList();
                break;
            case OPEN:
                compoundStatement();
                statementList();
                break;
            case CLOSE:
            case EOF:
                ;
                break;
            default:
                throw genEx("Expecting statement or <EOF>");
        }
    }

    //-----------------------------------------
    private void statement() {
        switch (currentToken.kind) {
            case ID:
                assignmentStatement();
                break;
            case PRINTLN:
                printlnStatement();
                break;
            case PRINT:
                printStatement();
                break;
            case DO:
                doStatement();
                break;
            case WHILE:
                whileStatement();
                break;
            case IF:
                ifStatement();
                break;
            case READINT:
                readInt();
                break;
            case OPEN:
                compoundStatement();
                break;

            default:
                throw genEx("Expecting statement");
        }
    }

    //-----------------------------------------
    private void assignmentStatement() {
        Token t;
        int left;
        int expVal;
        int temp = cg.getTempIndex();
        t = currentToken;
        consume(ID);
        left = st.enter(t.image, "0", true);
        consume(ASSIGN);
        expVal = assignmentTail();
        cg.assign(left, expVal);
        consume(SEMICOLON);
        cg.setTemp(temp);

    }

    //-----------------------------------------
    private int assignmentTail() {
        Token t;
        int left;
        int expVal;
        int temp = cg.getTempIndex();
        t = currentToken;
        if (getToken(1).kind == ID && getToken(2).kind == ASSIGN) {
            consume(ID);
            left = st.enter(t.image, "0", true);
            consume(ASSIGN);
            expVal = assignmentTail();
            cg.assign(left, expVal);
            return left;
        } else {
            expVal = expr();
            return expVal;
        }

    }

    //-----------------------------------------
    private void printlnStatement() {
        consume(PRINTLN);
        consume(LEFTPAREN);
        int expVal;
        Token t = currentToken;
        if (t.kind == STRING) {

            int index = st.getLabelindex(currentToken.image);

            if (index > 0) {
                String Label = st.getSymbol(index);
                if (Label.charAt(0) == '@' && Label.charAt(1) == 'L') {
                    expVal = index;
                } else {
                    String label = cg.getLabel();
                    expVal = st.enter(label, currentToken.image, true);
                }
            } else {
                String label = cg.getLabel();
                expVal = st.enter(label, currentToken.image, true);
            }
            consume(STRING);
            cg.print(expVal, 0);
        } else if (t.kind != RIGHTPAREN) {
            expVal = expr();
            cg.print(expVal, 1);
        }
        cg.println();
        consume(RIGHTPAREN);
        consume(SEMICOLON);
    }

    //-----------------------------------------
    private void printStatement() {
        consume(PRINT);
        consume(LEFTPAREN);
        int expVal;
        Token t = currentToken;
        if (t.kind == STRING) {
            int index = st.getLabelindex(currentToken.image);

            if (index > 0) {
                String Label = st.getSymbol(index);
                if (Label.charAt(0) == '@' && Label.charAt(1) == 'L') {
                    expVal = index;
                } else {
                    String label = cg.getLabel();
                    expVal = st.enter(label, currentToken.image, true);
                }
            } else {
                String label = cg.getLabel();
                expVal = st.enter(label, currentToken.image, true);
            }
            consume(STRING);
            cg.print(expVal, 0);
        } else if (t.kind != RIGHTPAREN) {
            expVal = expr();
            cg.print(expVal, 1);
        }
        consume(RIGHTPAREN);
        consume(SEMICOLON);
    }

    //-----------------------------------------
    private void nullStatement() {
        consume(SEMICOLON);
    }

    //-----------------------------------------
    private void doStatement() {
        Token T;

        String Start = cg.getLabel();
        cg.emitLabel(Start);

        consume(DO);

        statement();

        consume(WHILE);
        consume(LEFTPAREN);

        T = currentToken;

        consume(ID);

        int index = st.enter(T.image, "0", true);

        cg.doWhile(index, Start);

        consume(RIGHTPAREN);
        consume(SEMICOLON);

    }

    //-----------------------------------------
    private void whileStatement() {
        consume(WHILE);
        consume(LEFTPAREN);

        String Start = cg.getLabel();
        String End = cg.getLabel();


        Token T = currentToken;

        consume(ID);

        int index = st.enter(T.image, "0", true);

        //cg.emitLabel(Start);
        cg.While(index, End, Start);


        consume(RIGHTPAREN);
        statement();
        cg.emitLoad(index);
        cg.emitInstruction("ja", Start);
        cg.emitLabel(End);

    }

    //-----------------------------------------
    private void ifStatement() {
        String Else = cg.getLabel();

        consume(IF);
        consume(LEFTPAREN);

        Token T = currentToken;
        ;
        consume(ID);
        int index = st.enter(T.image, "0", true);
        int ac = cg.IfCondition(index, Else);

        consume(RIGHTPAREN);

        statement();

        if (currentToken.kind == ELSE) {

            String End = cg.getLabel();
            cg.emitInstruction("ja", End);
            do {
                cg.emitLabel(Else);
                consume(ELSE);


                if (currentToken.kind == IF) {
                    Else = cg.getLabel();

                    cg.setAC(ac);

                    consume(IF);
                    consume(LEFTPAREN);

                    T = currentToken;
                    ;
                    consume(ID);


                    index = st.enter(T.image, "0", true);
                    ac = cg.IfCondition(index, Else);
                    consume(RIGHTPAREN);

                    statement();
                    cg.emitInstruction("ja", End);

                } else {
                    cg.setAC(ac);
                    statement();
                }


            }
            while (currentToken.kind == ELSE);

            cg.emitLabel(End);

        } else {
            cg.emitLabel(Else);
        }

        cg.setAC(-1);
    }

    //-----------------------------------------
    private void readInt() {
        Token t;
        consume(READINT);
        consume(LEFTPAREN);
        t = currentToken;
        cg.getInt(t.image);
        consume(ID);
        consume(RIGHTPAREN);
        consume(SEMICOLON);
    }

    //-----------------------------------------
    private void compoundStatement() {
        consume(OPEN);

        statementList();
        consume(CLOSE);
    }

    //-----------------------------------------
    private int expr() {
        int left;
        int expVal;
        int temp = cg.getTempIndex();
        left = term();
        expVal = termList(left);
        cg.setTemp(temp);
        return expVal;
    }

    //-----------------------------------------
    private int termList(int left) {
        int temp;
        int right;
        int expVal;
        switch (currentToken.kind) {
            case PLUS:
                consume(PLUS);
                right = term();
                if (st.isLDCConstant(left) && st.isLDCConstant(right)) {
                    int result = Integer.parseInt(st.getdwValue(left)) + Integer.parseInt(st.getdwValue(right));
                    if (result >= 0) {
                        temp = st.enter("@" + result, "" + result, false);
                    } else {
                        temp = st.enter("@_" + (result * (-1)), "" + result, false);
                    }
                } else {
                    temp = cg.add(left, right);
                }
                expVal = termList(temp);
                return expVal;

            case MINUS:
                consume(MINUS);
                right = term();
                if (st.isLDCConstant(left) && st.isLDCConstant(right)) {
                    int result = Integer.parseInt(st.getdwValue(left)) - Integer.parseInt(st.getdwValue(right));
                    if (result >= 0) {
                        temp = st.enter("@" + result, "" + result, false);
                    } else {
                        temp = st.enter("@_" + (result * (-1)), "" + result, false);
                    }
                } else {
                    temp = cg.sub(left, right);
                }
                expVal = termList(temp);
                return expVal;

            case RIGHTPAREN:
            case SEMICOLON:
                ;
                return left;
            default:
                throw genEx("Expecting \"+\", \"-\", \")\", or \";\"");
        }
    }

    //-----------------------------------------
    private int term() {
        int left;
        int termVal;
        int temp = cg.getTempIndex();
        left = factor();
        termVal = factorList(left);
        cg.setTemp(temp);
        return termVal;
    }

    //-----------------------------------------
    private int factorList(int left) {
        int right, temp, termVal;

        switch (currentToken.kind) {
            case TIMES:
                consume(TIMES);
                right = factor();
                if (st.isLDCConstant(left) && st.isLDCConstant(right)) {
                    int result = Integer.parseInt(st.getdwValue(left)) * Integer.parseInt(st.getdwValue(right));
                    if (result >= 0) {
                        temp = st.enter("@" + result, "" + result, false);
                    } else {
                        temp = st.enter("@_" + (result * (-1)), "" + result, false);
                    }
                } else {
                    temp = cg.mult(left, right);
                }

                termVal = factorList(temp);
                return termVal;

            case DIV:
                consume(DIV);
                right = factor();
                if (st.isLDCConstant(left) && st.isLDCConstant(right)) {
                    int result = Integer.parseInt(st.getdwValue(left)) / Integer.parseInt(st.getdwValue(right));
                    if (result >= 0) {
                        temp = st.enter("@" + result, "" + result, false);
                    } else {
                        temp = st.enter("@_" + (result * (-1)), "" + result, false);
                    }
                } else {
                    temp = cg.div(left, right);
                }

                termVal = factorList(temp);
                return termVal;
            case PLUS:
            case MINUS:
            case RIGHTPAREN:

            case SEMICOLON:
                ;
                return left;
            default:
                throw genEx("Expecting op, \")\", or \";\"");
        }
    }

    //-----------------------------------------
    private int factor() {
        Token t;
        int index;
        t = currentToken;

        switch (currentToken.kind) {
            case UNSIGNED:
                consume(UNSIGNED);
                index = st.enter("@" + t.image, t.image, false);
                return index;
            case PLUS:
                consume(PLUS);
                index = factor();
                return index;
            case MINUS:
                int i = -1;
                consume(MINUS);
                while (currentToken.kind == MINUS || currentToken.kind == PLUS) {
                    if (currentToken.kind == MINUS) {
                        i = (-1) * i;
                        consume(MINUS);
                    } else {
                        consume(PLUS);
                    }
                }
                if (i == 1) {
                    index = factor();
                    return index;
                } else {
                    int temp;
                    index = factor();
                    if (st.isLDCConstant(index)) {
                        int result = Integer.parseInt(st.getdwValue(index));

                        if (result > 0) {
                            temp = st.enter("@_" + result, "-" + result, false);
                        } else {
                            temp = st.enter("@" + (result * (-1)), "" + result * (-1), false);
                        }
                        return temp;
                    } else {
                        temp = st.enter("@_1", "-1", false);
                        int y = cg.mult(temp, index);
                        return y;
                    }
                }
            case ID:
                index = st.enter(t.image, "0", true);
                consume(ID);
                return index;
            case LEFTPAREN:
                consume(LEFTPAREN);
                index = expr();
                consume(RIGHTPAREN);
                return index;
            default:
                throw genEx("Expecting factor");
        }
    }
}                                     // end of R4DParser

//======================================================
class R4DCodeGen {
    private PrintWriter outFile;
    private R4DSymTab st;
    static int tempIndex = 0;
    static int ac = -1;
    static int label = 0;

    //-----------------------------------------
    public R4DCodeGen(PrintWriter outFile, R4DSymTab st) {
        this.outFile = outFile;
        this.st = st;
    }

    //-----------------------------------------
    //------------------------------------------
    public int add(int left, int right) {

        if (ac == left) {

            emitInstruction("add", right);
            st.setNeedsdw(right);
        } else if (ac == right) {
            emitInstruction("add", left);
            st.setNeedsdw(left);
        } else {
            if (ac != -1) {
                if (st.isTemp(ac)) {
                    emitInstruction("st", ac);
                    st.setNeedsdw(ac);
                }
            }
            emitLoad(left);
            emitInstruction("add", right);
            st.setNeedsdw(right);
        }
        int temp = getTemp();
        ac = temp;
        return temp;
    }

    //-----------------------------------------
    public int mult(int left, int right) {

        if (ac == left) {
            emitInstruction("mult", right);
            st.setNeedsdw(right);
        } else if (ac == right) {
            emitInstruction("mult", left);
            st.setNeedsdw(left);
        } else {
            if (ac != -1) {
                if (st.isTemp(ac)) {
                    emitInstruction("st", ac);
                    st.setNeedsdw(ac);
                }
            }
            emitLoad(left);
            emitInstruction("mult", right);
            st.setNeedsdw(right);
        }
        int temp = getTemp();
        ac = temp;

        return temp;
    }

    //--------------------------------------
    public int div(int left, int right) {
        if (ac != left) {
            if (ac != -1) {
                if (st.isTemp(ac)) {
                    emitInstruction("st", ac);
                    st.setNeedsdw(ac);
                }
            }
            emitLoad(left);

        }
        emitInstruction("div", right);
        st.setNeedsdw(right);
        int temp = getTemp();
        ac = temp;
        return temp;
    }

    //----------------------------------------
    public int sub(int left, int right) {

        if (ac != left) {
            if (ac != -1) {
                if (st.isTemp(ac)) {
                    emitInstruction("st", ac);
                    st.setNeedsdw(ac);
                }
            }
            emitLoad(left);
        }
        emitInstruction("sub", right);
        st.setNeedsdw(right);
        int temp = getTemp();
        ac = temp;
        return ac;
    }

    //---------------------------------------
    public void assign(int left, int expVal) {
        if (ac != expVal)
            emitLoad(expVal);
        emitInstruction("st", left);
        ac = left;

    }

    //---------------------------------------
    public void println() {
        emitInstruction("ldc", "'\\n'");
        emitInstruction("aout");
        ac = -1;
    }

    //---------------------------------------
    public void print(int expVal, int i) {
        if (i == 1) {
            if (ac != expVal) {
                emitLoad(expVal);
                ac = expVal;
            }
            emitInstruction("dout");

        } else {
            emitInstruction("ldc", expVal);
            emitInstruction("sout");
            ac = -1;
        }
    }

    //-----------------------------------------
    public void getInt(String op) {
        int index = st.enter(op, "0", true);
        emitInstruction("din");
        emitInstruction("st", index);
        ac = -1;
    }

    //-----------------------------------------
    public int getTemp() {

        String temp;
        temp = "@t" + tempIndex++;
        return st.enter(temp, "0", false);
    }

    //--------------------------------------------
    public int getTempIndex() {

        return tempIndex;
    }

    //--------------------------------------------
    public void setTemp(int temp) {
        tempIndex = temp;
    }

    //--------------------------------------------
    public String getLabel() {

        return "@L" + label++;
    }

    //--------------------------------------------
    public void doWhile(int opnd, String Start) {
        String End = getLabel();
        if (ac != opnd)
            emitLoad(opnd);
        emitInstruction("JZ", End);
        emitInstruction("JA", Start);
        emitLabel(End);
    }

    //--------------------------------------------
    public void While(int opnd, String End, String Start) {

        emitLoad(opnd);
        emitLabel(Start);
        emitInstruction("JZ", End);
    }

    //--------------------------------------------
    public int IfCondition(int opnd, String Else) {
        String End = getLabel();
        if (ac != opnd)
            emitLoad(opnd);
        emitInstruction("JZ", Else);
        return ac;
    }

    //--------------------------------------------
    public void setAC(int AC) {
        ac = AC;
    }

    public void emitLabel(String op) {
        outFile.printf("%-4s:%n", op);
    }

    //--------------------------------------------
    private void emitInstruction(String op) {
        outFile.printf("          %-4s%n", op);
    }

    //-----------------------------------------
    public void emitInstruction(String op, String opnd) {
        outFile.printf("          %-4s      %s%n", op, opnd);
    }

    //-----------------------------------------
    public void emitLoad(int opnd) {

        if (st.isLDCConstant(opnd) && Integer.parseInt(st.getdwValue(opnd)) < 4095 && Integer.parseInt(st.getdwValue(opnd)) >= 0) {
            emitInstruction("ldc", st.getdwValue(opnd));
            ac = -1;
        } else if (opnd != ac) {

            emitInstruction("ld", opnd);
            st.setNeedsdw(opnd);
            ac = opnd;
        }
    }

    //-----------------------------------------
    private void emitdw(String label, String value) {
        outFile.printf(
                "%-9s dw        %s%n", label + ":", value);
    }

    //--------------------------------------------
    private void emitInstruction(String op, int opndIndex) {
        emitInstruction(op, st.getSymbol(opndIndex));
    }

    //-----------------------------------------
    public void endCode() {
        outFile.println();
        emitInstruction("            halt");

        int size = st.getSize();
        // emit dw for each symbol in the symbol table
        for (int i = 0; i < size; i++)
            if (st.getNeedsdw(i))
                emitdw(st.getSymbol(i), st.getdwValue(i));
    }
}                                    // end of R4DCodeGen


class Token {
    int kind;
    int beginLine;
    int beginColumn;
    int endLine;
    int endColumn;
    String image;
    Token next;
}