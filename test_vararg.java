public class test_vararg {
    public static void main(String[] args) {
        Object[] arr = new Object[] { "Hello" };
        System.out.println(String.format("test: %1$s", arr));
    }
}
