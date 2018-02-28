package md2html;
import java.io.*;
import java.util.*;

public class Md2Html {
    
    private static boolean newBlock = true;
    private static boolean headerOpened = false;
    private static boolean paragraphOpened = false;
    private static boolean read = true;
    
    private static char prev;
    private static char ch;
    private static char next;
    
    private static BufferedReader reader;
    private static BufferedWriter writer;
    
    private static int cellNumber = 1;
    
    static Deque <String>  stack = new ArrayDeque<>();
    private static Map <String, String> HTMLSYMB = new HashMap<>();
    static {
        HTMLSYMB.put("<", "&lt;");
        HTMLSYMB.put(">", "&gt;");
        HTMLSYMB.put("&", "&amp;");
    }
    
    private static Map <String, String> style = new HashMap<>();
    static {
        style.put("--", "s>");
        style.put("++", "u>");
        style.put("*", "em>");
        style.put("_", "em>");
        style.put("**", "strong>");
        style.put("__", "strong>");
        style.put("`", "code>");
        style.put("~", "mark>");
    }
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Please, enter only names of two files: one that contains Markdown markup and second, where equal HTML markup should be written");
            return;
        }
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "utf-8"))) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "utf-8"))) {
                reader = in;
                writer = out;
                ch = ' ';
                int c = 0;
                while (true) {
                    if (read) {
                        prev = ch;
                        ch = (char) reader.read();
                    }
                    if (ch ==  (char) -1) {
                        break;
                    }
                    if (newBlock) {
                        if (ch =='#') {
                            while ((ch = (char) reader.read()) =='#') {
                                cellNumber++;
                            }
                            if (Character.isWhitespace(ch)) {
                                writer.write(openHeader());
                            } else {
                                writer.write(openParagraph());
                                for (int i = 0; i  < cellNumber; i++) {
                                    writer.write("#");
                                }
                                cellNumber = 1;
                            }
                            continue;
                        }
                        next = nextChar();
                        if (isNewLine(ch, next)) {
                        } else {
                            writer.write(openParagraph());
                        }
                    } else {
                        next = nextChar();
                        if (isNewLine(ch, next)) {
                            ch = (char) reader.read();
                            if (ch != (char) - 1) {
                                next = nextChar();
                            } else break;
                            
                            if (isNewLine(ch, next)) {
                                if (headerOpened) {
                                    writer.write(closeHeader());
                                    writer.write(System.lineSeparator());
                                } else {
                                    writer.write(closeParagraph());
                                    writer.write(System.lineSeparator());
                                }
                                
                            } else {
                                writer.write(System.lineSeparator());
                                read = false;
                            }
                            
                        } else if (ch == '-' || ch == '+') {
                            if (next == ch) {
                                writer.write(Tag(String.valueOf(ch) + next));
                                reader.read();
                            } else {
                                writer.write(ch);
                            }
                            read = true;
                        } else if (ch == '\\') {
                            ch = (char) reader.read();
                            writer.write(ch);
                            read = true;
                        } else if (ch == '&' || ch == '<'|| ch== '>') {
                            writer.write(HTMLSYMB.get(String.valueOf(ch)));
                            read = true;
                        } else if (ch == '`') {
                            writer.write(Tag(String.valueOf(ch)));
                            read = true;
                        } else if (ch == '~') {
                            if (Character.isWhitespace(next) && Character.isWhitespace(prev)) {
                                writer.write(ch);
                            } else {
                                writer.write(Tag(String.valueOf(ch)));
                            }
                            read = true;
                        } else if ((ch == '*') || (ch == '_')) {
                            if (Character.isWhitespace(next) && Character.isWhitespace(prev)) {
                                writer.write(ch);
                            } else if (ch == next) {
                                writer.write(Tag(String.valueOf(ch) + next));
                                reader.read();
                            } else {
                                writer.write(Tag(String.valueOf(ch)));
                            }
                            read = true;
                        } else {
                            writer.write(ch);
                            read = true;
                        }
                        
                    }
                }
                if (headerOpened) {
                    writer.write(closeHeader());
                } else {
                    writer.write(closeParagraph());
                }
            } catch (FileNotFoundException e) {
                System.err.println("Error! File " + args[0] + " doesn't exist or can not be opened: "+ e.getMessage());
            } catch (UnsupportedEncodingException e) {
                System.err.println("Error!UTF-8  is not supported");
            } catch (IOException e) {
                System.err.println("Error! Input/output failed: " + e.getMessage());
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error! File " + args[1] + " can not be opened: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            System.err.println("Error! Encoding is not supported: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Output close failure: " + e.getMessage());
        }
    }
    
    private static String openHeader()  {
        StringBuilder sb = new StringBuilder();
        sb.append("<h");
        sb.append(String.valueOf(cellNumber));
        sb.append(">");
        headerOpened = true;
        paragraphOpened = false;
        newBlock = false;
        return String.valueOf(sb);
    }
    private static String closeHeader()  {
        StringBuilder sb = new StringBuilder();
        sb.append("</h");
        sb.append(String.valueOf(cellNumber));
        sb.append(">");
        headerOpened = false;
        cellNumber = 1;
        newBlock = true;
        read = true;
        return String.valueOf(sb);
    }
    private static String openParagraph() {
        paragraphOpened = true;
        headerOpened = false;
        newBlock = false;
        read = false;
        return "<p>";
    }
    private static String closeParagraph() {
        paragraphOpened = false;
        newBlock = true;
        read = true;
        return "</p>";
    }
    private static String Tag (String str) {
        StringBuilder sb = new StringBuilder();
        if (!stack.isEmpty() && str.equals(stack.peek())) {
            sb.append("</");
            stack.pop();
        } else {
            sb.append("<");
            stack.push(str);
        }
        sb.append(style.get(str));
        return String.valueOf(sb);
        
    }
    
    private static char nextChar () throws IOException {
        reader.mark(1);
        char character = (char) reader.read();
        reader.reset();
        return character;
    }
    private static boolean isNewLine(char character, char nextCh) throws IOException {
        final String separator = System.lineSeparator();
        if (separator.length() == 1 && String.valueOf(character).equals(separator)) {
            return true;
        }
        if (separator.length() == 2 && (String.valueOf(character) + nextCh).equals(separator)) {
            reader.read();
            return true;
        }
        return false;
    }
}

