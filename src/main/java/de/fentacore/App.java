package de.fentacore;

public class App {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {
        App app = new App();
        System.out.println(app.greet("World"));
    }
}