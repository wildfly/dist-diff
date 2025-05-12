public class ClassWithLambdas {


    public void doSomething() {
        new Thread(() -> {
            System.out.println("woohoo");
        }).start();
        new Thread(() -> {
            System.out.println("woohoo2");
        }).start();
    }

    public void doSomethingSimilar() {
        new Thread(() -> {
            System.out.println("woohoo-similar");
        }).start();
    }


}